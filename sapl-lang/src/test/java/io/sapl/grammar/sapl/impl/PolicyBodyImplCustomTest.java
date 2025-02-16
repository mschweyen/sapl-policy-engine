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

import org.junit.jupiter.api.Test;

import io.sapl.api.pdp.AuthorizationDecision;
import io.sapl.grammar.sapl.impl.util.MockUtil;
import io.sapl.interpreter.DefaultSAPLInterpreter;
import reactor.test.StepVerifier;

class PolicyBodyImplCustomTest {

	private static final DefaultSAPLInterpreter INTERPRETER = new DefaultSAPLInterpreter();

	@Test
	void trueReturnsEntitlement() {
		var policy   = INTERPRETER.parse("policy \"p\" permit true where true; true; true;");
		var expected = AuthorizationDecision.PERMIT;
		StepVerifier.create(policy.evaluate().contextWrite(MockUtil::setUpAuthorizationContext)).expectNext(expected)
				.verifyComplete();
	}

	@Test
	void oneFalseReturnsNotApplicableEntitlement() {
		var policy   = INTERPRETER.parse("policy \"p\" permit true where true; false; true;");
		var expected = AuthorizationDecision.NOT_APPLICABLE;
		StepVerifier.create(policy.evaluate().contextWrite(MockUtil::setUpAuthorizationContext)).expectNext(expected)
				.verifyComplete();
	}

	@Test
	void oneErrorReturnsIndeterminate() {
		var policy   = INTERPRETER.parse("policy \"p\" permit true where true; (10/0); true;");
		var expected = AuthorizationDecision.INDETERMINATE;
		StepVerifier.create(policy.evaluate().contextWrite(MockUtil::setUpAuthorizationContext)).expectNext(expected)
				.verifyComplete();
	}

	@Test
	void valueDefinitionsEvaluateAndScope() {
		var policy   = INTERPRETER
				.parse("policy \"p\" permit true where variable == undefined; var variable = 1; variable == 1;");
		var expected = AuthorizationDecision.PERMIT;
		StepVerifier.create(policy.evaluate().contextWrite(MockUtil::setUpAuthorizationContext)).expectNext(expected)
				.verifyComplete();
	}

	@Test
	void valueDefinitionsDefineUndefined() {
		var policy   = INTERPRETER.parse(
				"policy \"p\" permit true where variable == undefined; var variable = undefined; variable == undefined;");
		var expected = AuthorizationDecision.PERMIT;
		StepVerifier.create(policy.evaluate().contextWrite(MockUtil::setUpAuthorizationContext)).expectNext(expected)
				.verifyComplete();
	}

	@Test
	void valueDefinitionsDefineError() {
		var policy   = INTERPRETER.parse("policy \"p\" permit where var variable = (10/0);");
		var expected = AuthorizationDecision.INDETERMINATE;
		StepVerifier.create(policy.evaluate().contextWrite(MockUtil::setUpAuthorizationContext)).expectNext(expected)
				.verifyComplete();
	}

	@Test
	void valueDefinitionsAttemptToOverwriteDefailtSubscriptionVariableSubjectError() {
		var policy   = INTERPRETER.parse("policy \"p\" permit where var subject = {};");
		var expected = AuthorizationDecision.INDETERMINATE;
		StepVerifier.create(policy.evaluate().contextWrite(MockUtil::setUpAuthorizationContext)).expectNext(expected)
				.verifyComplete();
	}

	@Test
	void valueDefinitionsAttemptToOverwriteDefailtSubscriptionVariableActionError() {
		var policy   = INTERPRETER.parse("policy \"p\" permit where var action = {};");
		var expected = AuthorizationDecision.INDETERMINATE;
		StepVerifier.create(policy.evaluate().contextWrite(MockUtil::setUpAuthorizationContext)).expectNext(expected)
				.verifyComplete();
	}

	@Test
	void valueDefinitionsAttemptToOverwriteDefailtSubscriptionVariableResourceError() {
		var policy   = INTERPRETER.parse("policy \"p\" permit where var resource = {};");
		var expected = AuthorizationDecision.INDETERMINATE;
		StepVerifier.create(policy.evaluate().contextWrite(MockUtil::setUpAuthorizationContext)).expectNext(expected)
				.verifyComplete();
	}

	@Test
	void valueDefinitionsAttemptToOverwriteDefailtSubscriptionVariableEnvironmentError() {
		var policy   = INTERPRETER.parse("policy \"p\" permit where var environment = {};");
		var expected = AuthorizationDecision.INDETERMINATE;
		StepVerifier.create(policy.evaluate().contextWrite(MockUtil::setUpAuthorizationContext)).expectNext(expected)
				.verifyComplete();
	}

	@Test
	void lazyStatementEvaluationVarDef() {
		var policy   = INTERPRETER.parse("policy \"p\" permit true where false; var variable = (10/0);");
		var expected = AuthorizationDecision.NOT_APPLICABLE;
		StepVerifier.create(policy.evaluate().contextWrite(MockUtil::setUpAuthorizationContext)).expectNext(expected)
				.verifyComplete();
	}

	@Test
	void lazyStatementEvaluationVarDefOnError() {
		var policy   = INTERPRETER.parse("policy \"p\" permit true where (10/0); var variable = (10/0);");
		var expected = AuthorizationDecision.INDETERMINATE;
		StepVerifier.create(policy.evaluate().contextWrite(MockUtil::setUpAuthorizationContext)).expectNext(expected)
				.verifyComplete();
	}

	@Test
	void lazyStatementEvaluation() {
		var policy   = INTERPRETER.parse("policy \"p\" permit true where false; (10/0);");
		var expected = AuthorizationDecision.NOT_APPLICABLE;
		StepVerifier.create(policy.evaluate().contextWrite(MockUtil::setUpAuthorizationContext)).expectNext(expected)
				.verifyComplete();
	}

}
