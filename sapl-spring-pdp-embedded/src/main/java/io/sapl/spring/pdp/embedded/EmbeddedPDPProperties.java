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

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import lombok.Data;

@Data
@Validated
@ConfigurationProperties(prefix = "io.sapl.pdp.embedded")
public class EmbeddedPDPProperties {

	/**
	 * Selects the source of configuration and policies:
	 *
	 * The options are:
	 *
	 * - RESOURCES : Loads a fixed set of documents and pdp.json from the bundled
	 * resource. These will be loaded once and cannot be updated at runtime of the system.
	 *
	 * - FILESYSTEM: Monitors directories for documents and configuration. Will
	 * automatically update any changes made to the documents and configuration at
	 * runtime. Changes will directly be reflected in the decisions made in already
	 * existing subscriptions and send new decisions if applicable.
	 */
	@NotNull
	private PDPDataSource pdpConfigType = PDPDataSource.RESOURCES;

	/**
	 * Selects the indexing algorithm used by the PDP.
	 *
	 * The options are:
	 *
	 * - NAIVE : A simple implementation for systems with small numbers of documents.
	 *
	 * - CANONICAL : An improved index for systems with large numbers of documents. Takes
	 * more time to update and initialize but significantly reduces retrieval time.
	 */
	@NotNull
	private IndexType index = IndexType.NAIVE;

	/**
	 * This property sets the path to the folder where the pdp.json configuration file is
	 * located.
	 *
	 * If the pdpConfigType is set to RESOURCES, / is the root of the context path. For
	 * FILESYSTEM, it must be a valid path on the system's filesystem.
	 */
	@NotEmpty
	private String configPath = "/policies";

	/**
	 * This property sets the path to the folder where the *.sapl documents are located.
	 *
	 * If the pdpConfigType is set to RESOURCES, / is the root of the context path. For
	 * FILESYSTEM, it must be a valid path on the system's filesystem.
	 */
	@NotEmpty
	private String policiesPath = "/policies";

	public enum PDPDataSource {

		RESOURCES, FILESYSTEM

	}

	public enum IndexType {

		NAIVE, CANONICAL

	}

}
