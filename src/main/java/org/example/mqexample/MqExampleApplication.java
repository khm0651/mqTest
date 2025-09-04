package org.example.mqexample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jms.annotation.EnableJms;

@EnableJms
@SpringBootApplication
public class MqExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(MqExampleApplication.class, args);
    }

}
