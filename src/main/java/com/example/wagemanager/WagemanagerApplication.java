package com.example.wagemanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WagemanagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(WagemanagerApplication.class, args);
	}

}
