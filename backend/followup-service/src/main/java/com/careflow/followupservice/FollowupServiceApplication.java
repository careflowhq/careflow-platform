package com.careflow.followupservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FollowupServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(FollowupServiceApplication.class, args);
	}

}
