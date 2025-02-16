/*
 * Copyright © 2017-2022 Dominic Heutelbeck (dominic@heutelbeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.sapl.prp.index.canonical;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import io.sapl.api.interpreter.PolicyEvaluationException;
import io.sapl.grammar.sapl.SAPL;
import io.sapl.grammar.sapl.impl.SAPLImplCustom;
import io.sapl.interpreter.functions.FunctionContext;
import io.sapl.interpreter.pip.AttributeContext;
import io.sapl.prp.PolicyRetrievalResult;
import io.sapl.prp.PrpUpdateEvent;
import io.sapl.prp.PrpUpdateEvent.Type;
import io.sapl.prp.index.ImmutableParsedDocumentIndex;
import io.sapl.prp.index.canonical.ordering.DefaultPredicateOrderStrategy;
import io.sapl.prp.index.canonical.ordering.PredicateOrderStrategy;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class CanonicalImmutableParsedDocumentIndex implements ImmutableParsedDocumentIndex {

	private final CanonicalIndexDataContainer indexDataContainer;

	private final Map<String, SAPL> documents;

	private final PredicateOrderStrategy predicateOrderStrategy;

	private final boolean consistent;

	private final AttributeContext attributeCtx;

	private final FunctionContext functionCtx;

	public CanonicalImmutableParsedDocumentIndex(PredicateOrderStrategy predicateOrderStrategy,
			AttributeContext attributeCtx, FunctionContext functionCtx) {
		this(Collections.emptyMap(), predicateOrderStrategy, true, attributeCtx, functionCtx);
	}

	public CanonicalImmutableParsedDocumentIndex(AttributeContext attributeCtx, FunctionContext functionCtx) {
		this(Collections.emptyMap(), new DefaultPredicateOrderStrategy(), true, attributeCtx, functionCtx);
	}

	private CanonicalImmutableParsedDocumentIndex(Map<String, SAPL> updatedDocuments,
			PredicateOrderStrategy predicateOrderStrategy,
			boolean consistent, AttributeContext attributeCtx, FunctionContext functionCtx) {
		this.documents              = updatedDocuments;
		this.predicateOrderStrategy = predicateOrderStrategy;
		this.consistent             = consistent;
		this.attributeCtx           = attributeCtx;
		this.functionCtx            = functionCtx;

		Map<String, DisjunctiveFormula> targets = this.documents.entrySet().stream().collect(
				Collectors.toMap(Entry::getKey, entry -> retainTarget(entry.getValue())));

		this.indexDataContainer = new CanonicalIndexDataCreationStrategy(predicateOrderStrategy).constructNew(documents,
				targets);
	}

	CanonicalImmutableParsedDocumentIndex recreateIndex(Map<String, SAPL> updatedDocuments, boolean consistent) {
		return new CanonicalImmutableParsedDocumentIndex(updatedDocuments, predicateOrderStrategy, consistent,
				attributeCtx, functionCtx);
	}

	@Override
	public Mono<PolicyRetrievalResult> retrievePolicies() {
		if (!consistent) {
			return Mono.just(new PolicyRetrievalResult(new ArrayList<>(), true, false));
		}
		try {
			return CanonicalIndexAlgorithm.match(indexDataContainer);
		} catch (PolicyEvaluationException e) {
			log.error("error while retrieving policies", e);
			return Mono.just(new PolicyRetrievalResult(new ArrayList<>(), true, true));
		}
	}

	@Override
	public ImmutableParsedDocumentIndex apply(PrpUpdateEvent event) {
		var newDocuments        = new HashMap<>(documents);
		var newConsistencyState = consistent;
		for (var update : event.getUpdates()) {
			if (update.getType() == Type.CONSISTENT) {
				newConsistencyState = true;
			} else if (update.getType() == Type.INCONSISTENT) {
				newConsistencyState = false;
			} else {
				applyUpdate(newDocuments, update);
			}
		}
		log.debug("returning updated index containing {} documents", newDocuments.size());
		return recreateIndex(newDocuments, newConsistencyState);
	}

	// only PUBLISH or WITHDRAW
	void applyUpdate(Map<String, SAPL> newDocuments, PrpUpdateEvent.Update update) {
		var name = update.getDocument().getPolicyElement().getSaplName();
		if (update.getType() == Type.WITHDRAW) {
			newDocuments.remove(name);
		} else {
			if (newDocuments.containsKey(name)) {
				throw new RuntimeException("Fatal error. Policy name collision. A document with a name ('" + name
						+ "') identical to an existing document was published to the PRP.");
			}
			newDocuments.put(name, update.getDocument());
		}
	}

	private DisjunctiveFormula retainTarget(SAPL sapl) {
		try {
			var                targetExpression = sapl.getPolicyElement().getTargetExpression();
			DisjunctiveFormula targetFormula;
			if (targetExpression == null) {
				targetFormula = new DisjunctiveFormula(new ConjunctiveClause(new Literal(new Bool(true))));
			} else {
				Map<String, String> imports = SAPLImplCustom.fetchImports(sapl, attributeCtx, functionCtx);
				targetFormula = TreeWalker.walk(targetExpression, imports);
			}

			return targetFormula;
		} catch (PolicyEvaluationException e) {
			log.error("exception while retaining target for document {}", sapl.getPolicyElement().getSaplName(), e);
			return new DisjunctiveFormula(new ConjunctiveClause(new Literal(new Bool(true))));
		}
	}

}
