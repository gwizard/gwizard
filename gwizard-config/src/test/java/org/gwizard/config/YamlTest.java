package org.gwizard.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.InputStream;
import java.util.Map;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;

public class YamlTest {

	//@Test
	public void readYamlNull() throws Exception {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

		InputStream input = getClass().getResourceAsStream("/has_null.yml");

		Map<String, Object> map = mapper.readValue(input, Map.class);

		// Literal 'null' seems to produce the string "null"
		assert map.containsKey("first");
		assertThat(map.get("first"), nullValue());

		// Literal '~' seems to produce the string "~"
		assert map.containsKey("second");
		assertThat(map.get("second"), nullValue());

		// Literal '!!null' seems to produce an empty string
		assert map.containsKey("third");
		assertThat(map.get("third"), nullValue());
	}

}
