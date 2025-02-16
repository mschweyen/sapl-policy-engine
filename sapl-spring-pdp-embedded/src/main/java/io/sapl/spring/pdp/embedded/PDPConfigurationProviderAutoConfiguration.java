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
package io.sapl.spring.pdp.embedded;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.sapl.interpreter.functions.FunctionContext;
import io.sapl.interpreter.pip.AttributeContext;
import io.sapl.pdp.config.FixedFunctionsAndAttributesPDPConfigurationProvider;
import io.sapl.pdp.config.PDPConfigurationProvider;
import io.sapl.pdp.config.VariablesAndCombinatorSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class PDPConfigurationProviderAutoConfiguration {

	private final AttributeContext attributeCtx;

	private final FunctionContext functionCtx;

	private final VariablesAndCombinatorSource combinatorProvider;

	@Bean
	@ConditionalOnMissingBean
	public PDPConfigurationProvider pdpConfigurationProvider() {
		log.info(
				"Deploying PDP configuration provider with AttributeContext: {} FunctionContext: {} VariablesAndCombinatorSource: {}",
				attributeCtx, functionCtx, combinatorProvider);
		return new FixedFunctionsAndAttributesPDPConfigurationProvider(attributeCtx, functionCtx, combinatorProvider);
	}

}
