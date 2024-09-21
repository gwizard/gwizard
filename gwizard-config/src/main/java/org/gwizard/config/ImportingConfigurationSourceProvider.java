package org.gwizard.config;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.dropwizard.configuration.ConfigurationSourceProvider;
import lombok.RequiredArgsConstructor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * We have to open the file and look for import statements in the yaml, then merge together
 * what snippets we find.
 */
@RequiredArgsConstructor
public class ImportingConfigurationSourceProvider implements ConfigurationSourceProvider {

	static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory().configure(Feature.INCLUDE_SOURCE_IN_LOCATION, true));

	@Override
	public InputStream open(final String path) throws IOException {
		final File file = new File(path);
		if (!file.exists()) {
			throw new FileNotFoundException("File '" + file + "' not found");
		}

		final ObjectNode root = (ObjectNode) MAPPER.readTree(file);
		processImports(root, file);

		final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		MAPPER.writeValue(buffer, root);

		return new ByteArrayInputStream(buffer.toByteArray());
	}

	/**
	 * Look for any imports, and if they exist, load them and merge the results into a single tree.
	 * Mangles the root node with side-effects.
	 */
	private void processImports(final ObjectNode root, final File rootFile) throws IOException {
		final JsonNode imports = root.path("gwizard").path("import");
		if (imports.isEmpty())
			return;

		root.remove("gwizard");

		for (final JsonNode anImport : imports) {
			final ObjectNode imported = importFile(anImport.textValue(), rootFile);
			merge(root, imported);
		}
	}

	private ObjectNode importFile(final String path, final File rootFile) throws IOException {
		final File file = resolveFile(path, rootFile);
		if (!file.exists()) {
			throw new FileNotFoundException("Import file '" + file + "' not found");
		}

		return (ObjectNode) MAPPER.readTree(file);
	}

	private File resolveFile(final String path, final File rootFile) {
		if (path.startsWith(File.separator)) {
			// Absolute path
			return new File(path);
		} else {
			return new File(rootFile.getParent(), path);
		}
	}

	private void merge(final ObjectNode root, final ObjectNode imported) {
		imported.fieldNames().forEachRemaining(fieldName -> {
			final JsonNode valueToBeUpdated = root.get(fieldName);
			final JsonNode valueToUpdate = imported.get(fieldName);

			// If both nodes are object nodes, merge recursively
			if (valueToBeUpdated != null && valueToBeUpdated.isObject() && valueToUpdate != null && valueToUpdate.isObject()) {
				merge((ObjectNode) valueToBeUpdated, (ObjectNode) valueToUpdate);
			} else {
				root.set(fieldName, valueToUpdate);
			}
		});
	}
}
