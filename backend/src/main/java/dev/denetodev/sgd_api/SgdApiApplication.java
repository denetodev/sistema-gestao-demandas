package dev.denetodev.sgd_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SgdApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(SgdApiApplication.class, args);
	}

}
