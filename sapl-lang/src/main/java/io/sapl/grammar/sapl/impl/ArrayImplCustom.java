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
import java.util.List;

import io.sapl.api.interpreter.Val;
import io.sapl.grammar.sapl.Expression;
import reactor.core.publisher.Flux;

/**
 * Implementation of an array in SAPL.
 *
 * Grammar: Array returns Value: {Array} '[' (items+=Expression (','
 * items+=Expression)*)? ']' ;
 */
public class ArrayImplCustom extends ArrayImpl {

	/**
	 * The semantics of evaluating an array is as follows:
	 *
	 * An array may contain a list of expressions. To get the values of the
	 * individual expressions, these have to be recursively evaluated.
	 *
	 * Returning a Flux this means to subscribe to all expression result Fluxes and
	 * to combineLatest into a new array each time one of the expression Fluxes
	 * emits a new value.
	 */
	@Override
	public Flux<Val> evaluate() {
		// handle the empty array
		if (getItems().size() == 0) {
			return Flux.just(Val.of(Val.JSON.arrayNode()));
		}
		// aggregate child fluxes into a flux of a JSON array
		final List<Flux<Val>> itemFluxes = new ArrayList<>(getItems().size());
		for (Expression item : getItems()) {
			itemFluxes.add(item.evaluate());
		}
		return Flux.combineLatest(itemFluxes, this::collectValuesToArrayNode);
	}

	/**
	 * Collects a concrete evaluation of all expressions in the array into a single
	 * Array. We do not allow for returning 'undefined'/Optional.empty() as fields
	 * in the array. At runtime, this is primarily a constraint due to usage of
	 * Jackson JsonNodes which do not have a concept of 'undefined'. Also, as we
	 * want to return valid JSON values 'undefined' may not occur anywhere.
	 */
	private Val collectValuesToArrayNode(Object[] values) {
		var resultArr = Val.JSON.arrayNode();
		for (var oVal : values) {
			Val val = (Val) oVal;
			if (val.isError()) {
				return val;
			}
			if (val.isDefined()) {
				// drop undefined
				resultArr.add(val.get());
			}
		}
		return Val.of(resultArr);
	}

}
