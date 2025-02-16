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
package io.sapl.spring.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AdviceMode;

public class ReactiveSaplMethodSecuritySelectorTests {

	@Test
	void when_AdviceModeNotProxy_throwIllegalState() {
		var sut = new ReactiveSaplMethodSecuritySelector();
		assertThrows(IllegalStateException.class, () -> sut.selectImports(AdviceMode.ASPECTJ));
	}

	@Test
	void when_AdviceModeProxy_thenRegistrarAndSaplConfigIncludedInSelectImports() {
		var sut = new ReactiveSaplMethodSecuritySelector();
		var actual = sut.selectImports(AdviceMode.PROXY);
		assertThat(actual, is(arrayContainingInAnyOrder("org.springframework.context.annotation.AutoProxyRegistrar",
				"io.sapl.spring.config.ReactiveSaplMethodSecurityConfiguration")));
	}

}
