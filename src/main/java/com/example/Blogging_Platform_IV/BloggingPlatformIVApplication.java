package com.example.Blogging_Platform_IV;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/** @EnableCaching turns on Spring Cache, so @Cacheable, @CachePut and @CacheEvict work in the services. */
@SpringBootApplication
@EnableCaching
public class BloggingPlatformIVApplication {

	public static void main(String[] args) {
		SpringApplication.run(BloggingPlatformIVApplication.class, args);
	}

}
