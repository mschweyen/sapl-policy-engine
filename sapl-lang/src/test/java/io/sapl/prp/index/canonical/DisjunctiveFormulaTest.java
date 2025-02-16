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

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DisjunctiveFormulaTest {

	@Test
	void should_throw_illegal_argument_exception_when_initialized_with_empty_collection() {
		assertThrows(NullPointerException.class, () -> new DisjunctiveFormula((Collection<ConjunctiveClause>) null));
		assertThrows(IllegalArgumentException.class, () -> new DisjunctiveFormula(Collections.emptyList()));
	}

	@Test
	void testImmutable() {
		var immutableClause = new ConjunctiveClause(new Literal(new Bool(true)));
		var immutableFormula = new DisjunctiveFormula(immutableClause);
		var clauseMock = mock(ConjunctiveClause.class);
		when(clauseMock.isImmutable()).thenReturn(false);
		var f1 = new DisjunctiveFormula(clauseMock);

		assertThat(immutableClause.isImmutable(), is(true));
		assertThat(immutableFormula.size(), is(1));
		assertThat(immutableFormula.isImmutable(), is(true));
		assertThat(f1.isImmutable(), is(false));
	}

	@Test
	void testEvaluate() {

		var trueClause = new ConjunctiveClause(new Literal(new Bool(true)));
		var falseClause = new ConjunctiveClause(new Literal(new Bool(false)));
		var f1 = new DisjunctiveFormula(trueClause, falseClause);
		var f2 = new DisjunctiveFormula(falseClause, trueClause);
		var f3 = new DisjunctiveFormula(falseClause);

		assertThat(f1.evaluate(), is(true));
		assertThat(f2.evaluate(), is(true));
		assertThat(f3.evaluate(), is(false));
	}

	@Test
	@SuppressWarnings("unlikely-arg-type")
	void testEquals() {
		var trueClause = new ConjunctiveClause(new Literal(new Bool(true)));
		var falseClause = new ConjunctiveClause(new Literal(new Bool(false)));
		var f1 = new DisjunctiveFormula(trueClause, falseClause);
		var f2 = new DisjunctiveFormula(falseClause, trueClause);
		var f3 = new DisjunctiveFormula(falseClause);
		var f4 = new DisjunctiveFormula(falseClause, falseClause);
		var f5 = new DisjunctiveFormula(trueClause, trueClause);

		assertThat(f1.equals(f1), is(true));
		assertThat(f1.equals(f2), is(true));
		assertThat(f1.equals(f3), is(false));
		assertThat(f1.equals(f4), is(false));
		assertThat(f4.equals(f5), is(false));
		assertThat(f5.equals(f4), is(false));

		assertThat(f1.equals(null), is(false));
		assertThat(f1.equals(""), is(false));
	}

}
