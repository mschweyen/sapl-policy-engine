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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.sapl.api.pdp.PolicyDecisionPoint;
import io.sapl.spring.constraints.ConstraintEnforcementService;
import io.sapl.spring.subscriptions.AuthorizationSubscriptionBuilderService;

class SaplMethodSecurityConfigurationTests {

	@Test
	void whenRan_thenFilterBeansArePresent() {
		new ApplicationContextRunner().withUserConfiguration(NoPrePostEnablingCongiguration.class)
				.withBean(PolicyDecisionPoint.class, () -> mock(PolicyDecisionPoint.class))
				.withBean(ConstraintEnforcementService.class, () -> mock(ConstraintEnforcementService.class))
				.withBean(ObjectMapper.class, () -> mock(ObjectMapper.class)).run(context -> {
					assertThat(context).hasNotFailed();
					assertThat(context).hasSingleBean(MethodInterceptor.class);
				});
	}

	@Test
	void whenRanWithPrePost_thenFilterBeansArePresent() {
		new ApplicationContextRunner().withUserConfiguration(PrePostEnablingCongiguration.class)
				.withBean(PolicyDecisionPoint.class, () -> mock(PolicyDecisionPoint.class))
				.withBean(ConstraintEnforcementService.class, () -> mock(ConstraintEnforcementService.class))
				.withBean(ObjectMapper.class, () -> mock(ObjectMapper.class)).run(context -> {
					assertThat(context).hasNotFailed();
					assertThat(context).hasSingleBean(MethodInterceptor.class);
				});
	}

	@Configuration
	@EnableGlobalMethodSecurity(prePostEnabled = true)
	public static class PrePostEnablingCongiguration extends SaplMethodSecurityConfiguration {

		public PrePostEnablingCongiguration(ObjectFactory<PolicyDecisionPoint> pdpFactory,
				ObjectFactory<ConstraintEnforcementService> constraintHandlerFactory,
				ObjectFactory<ObjectMapper> objectMapperFactory,
				ObjectFactory<AuthorizationSubscriptionBuilderService> subscriptionBuilderFactory) {
			super(pdpFactory, constraintHandlerFactory, objectMapperFactory, subscriptionBuilderFactory);
		}

	}

	@Configuration
	@EnableGlobalMethodSecurity()
	public static class NoPrePostEnablingCongiguration extends SaplMethodSecurityConfiguration {

		public NoPrePostEnablingCongiguration(ObjectFactory<PolicyDecisionPoint> pdpFactory,
				ObjectFactory<ConstraintEnforcementService> constraintHandlerFactory,
				ObjectFactory<ObjectMapper> objectMapperFactory,
				ObjectFactory<AuthorizationSubscriptionBuilderService> subscriptionBuilderFactory) {
			super(pdpFactory, constraintHandlerFactory, objectMapperFactory, subscriptionBuilderFactory);
		}

	}

}
