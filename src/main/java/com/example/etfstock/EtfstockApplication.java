package com.example.etfstock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EtfstockApplication {

	public static void main(String[] args) {
		SpringApplication.run(EtfstockApplication.class, args);
	}

}
