package com.example.Blogging_Platform_III;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Starts the whole application on the test database. It fails if an entity does not match its
 * table (ddl-auto=validate) or if a JPQL query in a repository is wrong.
 */
@SpringBootTest
@ActiveProfiles("test")
class BloggingPlatformIiiApplicationTests {

	@Test
	void contextLoads() {
	}

}
