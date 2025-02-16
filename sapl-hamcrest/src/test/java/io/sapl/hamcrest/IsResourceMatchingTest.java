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
package io.sapl.hamcrest;

import static io.sapl.hamcrest.Matchers.isResourceMatching;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;
import java.util.function.Predicate;

import org.hamcrest.StringDescription;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.sapl.api.pdp.AuthorizationDecision;
import io.sapl.api.pdp.Decision;

class IsResourceMatchingTest {

	@Test
	public void test() {
		Predicate<JsonNode> pred = (JsonNode jsonNode) -> jsonNode.has("foo");
		var sut = isResourceMatching(pred);

		ObjectMapper mapper = new ObjectMapper();
		ObjectNode resource = mapper.createObjectNode();
		resource.put("foo", "bar");
		AuthorizationDecision dec = new AuthorizationDecision(Decision.PERMIT, Optional.of(resource), null, null);

		assertThat(dec, is(sut));
	}

	@Test
	public void test_neg() {
		Predicate<JsonNode> pred = (JsonNode jsonNode) -> jsonNode.has("xxx");
		var sut = isResourceMatching(pred);

		ObjectMapper mapper = new ObjectMapper();
		ObjectNode resource = mapper.createObjectNode();
		resource.put("foo", "bar");
		AuthorizationDecision dec = new AuthorizationDecision(Decision.PERMIT, Optional.of(resource), null, null);

		assertThat(dec, not(is(sut)));
	}

	@Test
	public void test_nullDecision() {
		Predicate<JsonNode> pred = (JsonNode jsonNode) -> jsonNode.has("foo");
		var sut = isResourceMatching(pred);
		assertThat(null, not(is(sut)));
	}

	@Test
	public void test_resourceEmpty() {
		Predicate<JsonNode> pred = (JsonNode jsonNode) -> jsonNode.has("foo");
		var sut = isResourceMatching(pred);
		AuthorizationDecision dec = new AuthorizationDecision(Decision.PERMIT, Optional.empty(), null, null);
		assertThat(dec, not(is(sut)));
	}

	@Test
	public void test_nullPredicate() {
		assertThrows(NullPointerException.class, () -> isResourceMatching(null));
	}

	@Test
	void testDescriptionForMatcher() {
		Predicate<JsonNode> pred = (JsonNode jsonNode) -> jsonNode.has("foo");
		var sut = isResourceMatching(pred);
		final StringDescription description = new StringDescription();
		sut.describeTo(description);
		assertThat(description.toString(), is("the decision has a resource matching the predicate"));
	}

}
