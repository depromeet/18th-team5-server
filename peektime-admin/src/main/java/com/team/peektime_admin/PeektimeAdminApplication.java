package com.team.peektime_admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PeektimeAdminApplication {

	public static void main(String[] args) {
		SpringApplication.run(PeektimeAdminApplication.class, args);
	}

}