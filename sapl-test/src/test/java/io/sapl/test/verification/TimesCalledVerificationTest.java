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
package io.sapl.test.verification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.number.OrderingComparison.comparesEqualTo;
import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

import io.sapl.api.interpreter.Val;
import io.sapl.test.mocking.MockCall;

public class TimesCalledVerificationTest {

	@Test
	void test_is() {
		MockRunInformation runInfo = new MockRunInformation("foo");
		runInfo.saveCall(new MockCall(Val.of("bar")));
		Matcher<Integer> matcher = is(1);
		MockingVerification verification = new TimesCalledVerification(matcher);

		boolean isAssertionErrorThrown = false;
		try {
			verification.verify(runInfo);
		}
		catch (AssertionError e) {
			isAssertionErrorThrown = true;
		}

		assertFalse(isAssertionErrorThrown);
	}

	@Test
	void test_comparesEqualTo() {
		MockRunInformation runInfo = new MockRunInformation("foo");
		runInfo.saveCall(new MockCall(Val.of("bar")));
		Matcher<Integer> matcher = comparesEqualTo(1);
		MockingVerification verification = new TimesCalledVerification(matcher);

		boolean isAssertionErrorThrown = false;
		try {
			verification.verify(runInfo);
		}
		catch (AssertionError e) {
			isAssertionErrorThrown = true;
		}

		assertFalse(isAssertionErrorThrown);
	}

	@Test
	void test_comparesEqualTo_multipleCalls() {
		MockRunInformation runInfo = new MockRunInformation("foo");
		runInfo.saveCall(new MockCall(Val.of("bar")));
		runInfo.saveCall(new MockCall(Val.of("xxx")));
		Matcher<Integer> matcher = comparesEqualTo(2);
		MockingVerification verification = new TimesCalledVerification(matcher);

		boolean isAssertionErrorThrown = false;
		try {
			verification.verify(runInfo);
		}
		catch (AssertionError e) {
			isAssertionErrorThrown = true;
		}

		assertFalse(isAssertionErrorThrown);
	}

	@Test
	void test_comparesEqualTo_assertionError() {
		MockRunInformation runInfo = new MockRunInformation("foo");
		Matcher<Integer> matcher = comparesEqualTo(1);
		MockingVerification verification = new TimesCalledVerification(matcher);

		boolean isAssertionErrorThrown = false;
		try {
			verification.verify(runInfo);
		}
		catch (AssertionError e) {
			isAssertionErrorThrown = true;
		}

		assertTrue(isAssertionErrorThrown);
	}

	@Test
	void test_greaterThanOrEqualTo() {
		MockRunInformation runInfo = new MockRunInformation("foo");
		runInfo.saveCall(new MockCall(Val.of("bar")));
		Matcher<Integer> matcher = greaterThanOrEqualTo(1);
		MockingVerification verification = new TimesCalledVerification(matcher);

		boolean isAssertionErrorThrown = false;
		try {
			verification.verify(runInfo);
		}
		catch (AssertionError e) {
			isAssertionErrorThrown = true;
		}

		assertFalse(isAssertionErrorThrown);
	}

	@Test
	void test_greaterThanOrEqualTo_assertionError() {
		MockRunInformation runInfo = new MockRunInformation("foo");
		Matcher<Integer> matcher = greaterThanOrEqualTo(1);
		MockingVerification verification = new TimesCalledVerification(matcher);

		boolean isAssertionErrorThrown = false;
		try {
			verification.verify(runInfo);
		}
		catch (AssertionError e) {
			isAssertionErrorThrown = true;
		}

		assertTrue(isAssertionErrorThrown);
	}

	@Test
	void test_verificationMessageEmpty() {
		MockRunInformation runInfo = new MockRunInformation("foo");
		runInfo.saveCall(new MockCall(Val.of("bar")));
		Matcher<Integer> matcher = is(2);
		MockingVerification verification = new TimesCalledVerification(matcher);

		boolean isAssertionErrorThrown = false;
		AssertionError error = null;
		try {
			verification.verify(runInfo, "");
		}
		catch (AssertionError e) {
			isAssertionErrorThrown = true;
			error = e;
		}
		assertThat(error.getMessage()).isNotEmpty();
		assertTrue(isAssertionErrorThrown);
	}

	@Test
	void test_verificationMessageNull() {
		MockRunInformation runInfo = new MockRunInformation("foo");
		runInfo.saveCall(new MockCall(Val.of("bar")));
		Matcher<Integer> matcher = is(2);
		MockingVerification verification = new TimesCalledVerification(matcher);

		boolean isAssertionErrorThrown = false;
		AssertionError error = null;
		try {
			verification.verify(runInfo, null);
		}
		catch (AssertionError e) {
			isAssertionErrorThrown = true;
			error = e;
		}
		assertThat(error.getMessage()).isNotEmpty();
		assertTrue(isAssertionErrorThrown);
	}

	@Test
	void test_verificationMessageNotEmpty() {
		MockRunInformation runInfo = new MockRunInformation("foo");
		runInfo.saveCall(new MockCall(Val.of("bar")));
		Matcher<Integer> matcher = is(2);
		MockingVerification verification = new TimesCalledVerification(matcher);

		boolean isAssertionErrorThrown = false;
		AssertionError error = null;
		try {
			verification.verify(runInfo, "VerificationMessage");
		}
		catch (AssertionError e) {
			isAssertionErrorThrown = true;
			error = e;
		}
		assertThat(error.getMessage()).contains("VerificationMessage");
		assertTrue(isAssertionErrorThrown);
	}

}
