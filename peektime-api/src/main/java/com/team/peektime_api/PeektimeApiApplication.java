package com.team.peektime_api;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@EnableJpaAuditing
@EnableAsync
@EnableScheduling
@SpringBootApplication
public class PeektimeApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(PeektimeApiApplication.class, args);
	}


	@PostConstruct
	public void started() {
		// 이 설정을 해야 LocalDate.now()가 완벽하게 드라이버와 싱크가 맞습니다.
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
	}

}
