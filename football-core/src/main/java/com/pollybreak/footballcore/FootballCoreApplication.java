package com.pollybreak.footballcore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FootballCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(FootballCoreApplication.class, args);
    }

}
