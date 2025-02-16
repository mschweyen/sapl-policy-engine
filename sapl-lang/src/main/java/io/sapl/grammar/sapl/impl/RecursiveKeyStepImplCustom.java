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
 * Implements the application of a recursive key step to a previous value, e.g.
 * {@code 'obj..name' or 'arr..["name"]'}.
 *
 * Grammar: {@code Step: '..' ({RecursiveKeyStep} (id=ID | '[' id=STRING ']'))
 * ;}
 */
@Slf4j
public class RecursiveKeyStepImplCustom extends RecursiveKeyStepImpl {

	@Override
	public Flux<Val> apply(@NonNull Val parentValue) {
		if (parentValue.isError()) {
			return Flux.just(parentValue);
		}
		if (parentValue.isUndefined()) {
			return Flux.just(Val.ofEmptyArray());
		}
		return Flux.just(Val.of(collect(parentValue.get(), Val.JSON.arrayNode())));
	}

	private ArrayNode collect(JsonNode node, ArrayNode results) {
		if (node.isArray()) {
			for (var item : node) {
				collect(item, results);
			}
		} else if (node.isObject()) {
			if (node.has(id)) {
				results.add(node.get(id));
			}
			var iter = node.fields();
			while (iter.hasNext()) {
				var item = iter.next().getValue();
				collect(item, results);
			}
		}
		return results;
	}

	@Override
	public Flux<Val> applyFilterStatement(
			@NonNull Val parentValue,
			int stepId,
			@NonNull FilterStatement statement) {
		return applyKeyStepFilterStatement(id, parentValue, stepId, statement);
	}

	private static Flux<Val> applyKeyStepFilterStatement(
			String id,
			Val parentValue,
			int stepId,
			FilterStatement statement) {
		log.trace("apply recursive key step '{}' to: {}", id, parentValue);
		if (parentValue.isObject()) {
			return applyFilterStatementToObject(id, parentValue.getObjectNode(), stepId, statement);
		}

		if (parentValue.isArray()) {
			return applyFilterStatementToArray(id, parentValue.getArrayNode(), stepId, statement);
		}

		// this means the element does not get selected does not get filtered
		return Flux.just(parentValue);
	}

	private static Flux<Val> applyFilterStatementToObject(
			String id,
			ObjectNode object,
			int stepId,
			FilterStatement statement) {
		var fieldFluxes = new ArrayList<Flux<Tuple2<String, Val>>>(object.size());
		var fields      = object.fields();
		while (fields.hasNext()) {
			var field = fields.next();
			log.trace("inspect field {}", field);
			if (field.getKey().equals(id)) {
				log.trace("field matches '{}'", id);
				if (stepId == statement.getTarget().getSteps().size() - 1) {
					// this was the final step. apply filter
					log.trace("final step. select and filter!");
					fieldFluxes.add(FilterComponentImplCustom
							.applyFilterFunction(Val.of(field.getValue()), statement.getArguments(),
									statement.getFsteps(), statement.isEach())
							.map(val -> Tuples.of(field.getKey(), val))
							.contextWrite(ctx -> AuthorizationContext.setRelativeNode(ctx, Val.of(object))));
				} else {
					// there are more steps. descent with them
					log.trace("this step was successful. descent with next step...");
					fieldFluxes.add(statement.getTarget().getSteps().get(stepId + 1)
							.applyFilterStatement(Val.of(field.getValue()), stepId + 1, statement)
							.map(val -> Tuples.of(field.getKey(), val)));
				}
			} else {
				log.trace("field not matching. Do recursive search for first match.");
				fieldFluxes.add(applyKeyStepFilterStatement(id, Val.of(field.getValue()), stepId, statement)
						.map(val -> Tuples.of(field.getKey(), val)));
			}
		}
		return Flux.combineLatest(fieldFluxes, RepackageUtil::recombineObject);
	}

	private static Flux<Val> applyFilterStatementToArray(
			String id,
			ArrayNode array,
			int stepId,
			FilterStatement statement) {
		if (array.isEmpty()) {
			return Flux.just(Val.ofEmptyArray());
		}
		var elementFluxes = new ArrayList<Flux<Val>>(array.size());
		var elements      = array.elements();
		while (elements.hasNext()) {
			var element = elements.next();
			log.trace("inspect element {}", element);
			if (element.isObject()) {
				log.trace("array element is an object. apply this step to the object.");
				elementFluxes.add(
						applyFilterStatementToObject(id, (ObjectNode) element, stepId, statement));
			} else {
				log.trace("array element not an object. Do recursive search for first match.");
				elementFluxes
						.add(applyKeyStepFilterStatement(id, Val.of(element), stepId, statement));
			}
		}
		return Flux.combineLatest(elementFluxes, RepackageUtil::recombineArray);
	}

}
