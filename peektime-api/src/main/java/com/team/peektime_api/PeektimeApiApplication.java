package com.team.peektime_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableJpaAuditing
@EnableScheduling
@SpringBootApplication
public class PeektimeApiApplication {



	public static void main(String[] args) {
		SpringApplication.run(PeektimeApiApplication.class, args);
	}

}
