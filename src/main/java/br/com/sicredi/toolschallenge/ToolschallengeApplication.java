package br.com.sicredi.toolschallenge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ToolschallengeApplication {

	public static void main(String[] args) {
		SpringApplication.run(ToolschallengeApplication.class, args);
	}

}
