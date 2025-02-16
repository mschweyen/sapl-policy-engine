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
package io.sapl.grammar.sapl.impl;

import java.util.ArrayList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.sapl.api.interpreter.Val;
import io.sapl.grammar.sapl.FilterStatement;
import io.sapl.interpreter.context.AuthorizationContext;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

/**
 * Implements the application of a recursive index step to a previous array
 * value, e.g. {@code 'arr..[2]'}.
 *
 * Grammar: {@code Step: '..' ({RecursiveIndexStep} '[' index=JSONNUMBER ']') ;}
 */
@Slf4j
public class RecursiveIndexStepImplCustom extends RecursiveIndexStepImpl {

	@Override
	public Flux<Val> apply(@NonNull Val parentValue) {
		if (parentValue.isError()) {
			return Flux.just(parentValue);
		}
		if (parentValue.isUndefined()) {
			return Flux.just(Val.ofEmptyArray());
		}
		return Flux.just(Val.of(collect(index.intValue(), parentValue.get(), Val.JSON.arrayNode())));
	}

	private ArrayNode collect(int index, JsonNode node, ArrayNode results) {
		if (node.isArray()) {
			var idx = normalizeIndex(index, node.size());
			if (node.has(idx)) {
				results.add(node.get(idx));
			}
			for (var item : node) {
				collect(index, item, results);
			}
		} else if (node.isObject()) {
			var iter = node.fields();
			while (iter.hasNext()) {
				var item = iter.next().getValue();
				collect(index, item, results);
			}
		}
		return results;
	}

	private static int normalizeIndex(int idx, int size) {
		// handle negative index values
		return idx < 0 ? size + idx : idx;
	}

	@Override
	public Flux<Val> applyFilterStatement(
			@NonNull Val parentValue,
			int stepId,
			@NonNull FilterStatement statement) {
		return doApplyFilterStatement(index.intValue(), parentValue, stepId, statement);
	}

	private static Flux<Val> doApplyFilterStatement(
			int index,
			Val parentValue,
			int stepId,
			FilterStatement statement) {
		log.trace("apply index step [{}] to: {}", index, parentValue);
		if (parentValue.isObject()) {
			return applyFilterStatementToObject(index, parentValue.getObjectNode(), stepId,
					statement);
		}
		if (!parentValue.isArray()) {
			// this means the element does not get selected does not get filtered
			return Flux.just(parentValue);
		}
		var array         = parentValue.getArrayNode();
		var idx           = normalizeIndex(index, array.size());
		var elementFluxes = new ArrayList<Flux<Val>>(array.size());
		for (var i = 0; i < array.size(); i++) {
			var element = array.get(i);
			log.trace("inspect element [{}]={}", i, element);
			if (i == idx) {
				log.trace("selected. [{}]={}", i, element);
				if (stepId == statement.getTarget().getSteps().size() - 1) {
					// this was the final step. apply filter
					log.trace("final step. apply filter!");
					elementFluxes.add(
							FilterComponentImplCustom
									.applyFilterFunction(Val.of(element), statement.getArguments(),
											statement.getFsteps(), statement.isEach())
									.contextWrite(ctx -> AuthorizationContext.setRelativeNode(ctx, parentValue)));
				} else {
					// there are more steps. descent with them
					log.trace("this step was successful. descent with next step...");
					elementFluxes.add(statement.getTarget().getSteps().get(stepId + 1)
							.applyFilterStatement(Val.of(element), stepId + 1, statement));
				}
			} else {
				log.trace("array element not an object. Do recursive search for first match.");
				elementFluxes.add(doApplyFilterStatement(index, Val.of(element), stepId, statement));
			}
		}
		return Flux.combineLatest(elementFluxes, RepackageUtil::recombineArray);
	}

	private static Flux<Val> applyFilterStatementToObject(
			int idx,
			ObjectNode object,
			int stepId,
			FilterStatement statement) {
		var fieldFluxes = new ArrayList<Flux<Tuple2<String, Val>>>(object.size());
		var fields      = object.fields();
		while (fields.hasNext()) {
			var field = fields.next();
			log.trace("recursion for field {}", field);
			fieldFluxes.add(doApplyFilterStatement(idx, Val.of(field.getValue()), stepId, statement)
					.map(val -> Tuples.of(field.getKey(), val)));
		}
		return Flux.combineLatest(fieldFluxes, RepackageUtil::recombineObject);
	}

}
