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
package io.sapl.test;

import java.nio.file.Paths;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class SaplTestFixtureTemplateTestImplTest {

	private SaplTestFixtureTemplateTestImpl sud;

	private ObjectMapper mapper;

	@BeforeEach
	void setup() {
		this.sud = new SaplTestFixtureTemplateTestImpl();
		this.mapper = new ObjectMapper();
	}

	@Test
	void test() {
		Assertions.assertThat(this.sud.resolveCoverageBaseDir()).isEqualTo(Paths.get("target", "sapl-coverage"));
	}

	@Test
	void test_withJavaProperty() {
		System.setProperty("io.sapl.test.outputDir", "test-target");
		Assertions.assertThat(this.sud.resolveCoverageBaseDir()).isEqualTo(Paths.get("test-target", "sapl-coverage"));
		System.clearProperty("io.sapl.test.outputDir");
	}

	@Test
	void test_registerVariable() {
		this.sud.registerVariable("test", this.mapper.createObjectNode());
		Assertions.assertThat(this.sud.getVariablesMap().containsKey("test")).isTrue();
	}

	@Test
	void test_registerVariable_twoTimes() {
		this.sud.registerVariable("test", this.mapper.createObjectNode());
		Assertions.assertThatExceptionOfType(SaplTestException.class)
				.isThrownBy(() -> this.sud.registerVariable("test", this.mapper.createObjectNode()))
				.withMessage("The VariableContext already contains a key \"test\"");
	}

}
