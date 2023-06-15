package {{root-package}}.{{module}};

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Integration tests for {{capitalizeFirst name}}Repository.
 */
@SpringBootTest
class {{capitalizeFirst name}}RepositoryIntegrationTests {

	@Autowired {{capitalizeFirst name}}Repository repository;

	@Test
	void repositoryBootstrapped() {
		assertThat(repository).isNotNull();
	}
}
