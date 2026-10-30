package com.worldpinball.leaderboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

@SpringBootApplication
public class LeaderboardSpringBootJavaApplication {

	public static void main(String[] args) {
		if (System.getProperty("spring.profiles.active") == null && System.getenv("SPRING_PROFILES_ACTIVE") == null) {
			System.setProperty("spring.profiles.active", "dev");
		}
		SpringApplication.run(LeaderboardSpringBootJavaApplication.class, args);
	}
	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}
}
