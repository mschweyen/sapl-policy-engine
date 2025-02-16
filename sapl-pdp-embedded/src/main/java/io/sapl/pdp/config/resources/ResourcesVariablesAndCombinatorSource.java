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
package io.sapl.pdp.config.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sapl.grammar.sapl.CombiningAlgorithm;
import io.sapl.interpreter.InitializationException;
import io.sapl.interpreter.combinators.CombiningAlgorithmFactory;
import io.sapl.pdp.config.PolicyDecisionPointConfiguration;
import io.sapl.pdp.config.VariablesAndCombinatorSource;
import io.sapl.util.JarUtil;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Slf4j
public class ResourcesVariablesAndCombinatorSource implements VariablesAndCombinatorSource {

	private static final String DEFAULT_CONFIG_PATH = "/policies";

	private static final String CONFIG_FILE = "pdp.json";

	private final ObjectMapper mapper;

	private final PolicyDecisionPointConfiguration config;

	public ResourcesVariablesAndCombinatorSource() throws InitializationException {
		this(DEFAULT_CONFIG_PATH);
	}

	public ResourcesVariablesAndCombinatorSource(String configPath) throws InitializationException {
		this(configPath, new ObjectMapper());
	}

	public ResourcesVariablesAndCombinatorSource(String configPath, ObjectMapper mapper)
			throws InitializationException {
		this(ResourcesVariablesAndCombinatorSource.class, configPath, mapper);
	}

	public ResourcesVariablesAndCombinatorSource(@NonNull Class<?> clazz, @NonNull String configPath,
			@NonNull ObjectMapper mapper) throws InitializationException {
		log.info("Loading the PDP configuration from bundled resources: '{}'", configPath);
		this.mapper = mapper;
		config = readConfig(JarUtil.inferUrlOfResourcesPath(clazz, configPath), configPath);
	}

	private PolicyDecisionPointConfiguration readConfig(URL configFolderUrl, String configPath)
			throws InitializationException {
		try {

			if ("jar".equals(configFolderUrl.getProtocol()))
				return readConfigFromJar(configFolderUrl, configPath);
			return readConfigFromDirectory(configFolderUrl);
		}
		catch (IOException | URISyntaxException e) {
			throw (InitializationException) new InitializationException(
					"Failed to create ResourcesVariablesAndCombinatorSource").initCause(e);
		}
	}

	private PolicyDecisionPointConfiguration readConfigFromJar(URL configFolderUrl, String configPath)
			throws IOException {
		log.info("reading config from jar {}", configFolderUrl);
		var jarFilePath = JarUtil.getJarFilePath(configFolderUrl);
		var pathOfFileInJar = stripLeadingSlashAndAppendConfigFilename(configPath);
		try (ZipFile jarFile = new ZipFile(jarFilePath)) {
			ZipEntry configFile = jarFile.getEntry(pathOfFileInJar);
			if (configFile != null)
				return mapper.readValue(JarUtil.readStringFromZipEntry(jarFile, configFile),
						PolicyDecisionPointConfiguration.class);
		}
		log.info("No PDP configuration found in resources. Using defaults.");
		return new PolicyDecisionPointConfiguration();
	}

	private String stripLeadingSlashAndAppendConfigFilename(String configPath) {
		return configPath.replaceAll("^/+", "") + "/" + CONFIG_FILE;
	}

	private PolicyDecisionPointConfiguration readConfigFromDirectory(URL configFolderUrl)
			throws IOException, URISyntaxException {
		log.debug("reading config from directory {}", configFolderUrl);
		Path configDirectoryPath = Paths.get(configFolderUrl.toURI());
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(configDirectoryPath, CONFIG_FILE)) {
			for (Path filePath : stream) {
				log.info("loading PDP configuration: {}", filePath.toAbsolutePath());
				return mapper.readValue(filePath.toFile(), PolicyDecisionPointConfiguration.class);
			}
		}
		log.info("No PDP configuration found in resources. Using defaults.");
		return new PolicyDecisionPointConfiguration();
	}

	@Override
	public Flux<Optional<CombiningAlgorithm>> getCombiningAlgorithm() {
		return Flux.just(config.getAlgorithm()).map(CombiningAlgorithmFactory::getCombiningAlgorithm).map(Optional::of);
	}

	@Override
	public Flux<Optional<Map<String, JsonNode>>> getVariables() {
		return Flux.just(config.getVariables()).map(HashMap::new).map(Optional::of);
	}

	@Override
	public void dispose() {
		// NOP nothing to dispose
	}

}
