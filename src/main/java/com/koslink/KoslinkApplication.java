package com.koslink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@EnableFeignClients
@ConfigurationPropertiesScan
@SpringBootApplication
public class KoslinkApplication {

	public static void main(String[] args) {
		SpringApplication.run(KoslinkApplication.class, args);
	}

}
