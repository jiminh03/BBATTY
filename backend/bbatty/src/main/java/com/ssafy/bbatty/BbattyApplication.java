package com.ssafy.bbatty;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class BbattyApplication {

	public static void main(String[] args) {
		SpringApplication.run(BbattyApplication.class, args);
	}

}
