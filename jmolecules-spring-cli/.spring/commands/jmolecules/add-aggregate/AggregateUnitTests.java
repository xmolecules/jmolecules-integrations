package {{root-package}}.{{module}};

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {{capitalizeFirst name}}.
 */
class {{capitalizeFirst name}}UnitTests {

	@Test
	void createsSimple{{capitalizeFirst name}}Instance() {

		var {{name}} = new {{capitalizeFirst name}}();

		assertThat({{name}}.getId()).isNotNull();
	}
}
