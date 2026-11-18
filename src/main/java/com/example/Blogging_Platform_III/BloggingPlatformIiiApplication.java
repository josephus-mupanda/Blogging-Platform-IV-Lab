package com.example.Blogging_Platform_III;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/** @EnableCaching turns on Spring Cache, so @Cacheable, @CachePut and @CacheEvict work in the services. */
@SpringBootApplication
@EnableCaching
public class BloggingPlatformIiiApplication {

	public static void main(String[] args) {
		SpringApplication.run(BloggingPlatformIiiApplication.class, args);
	}

}
