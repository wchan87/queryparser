package org.chanwr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

    // FIXME fix "Application: Failed to retrieve application JMX service URL"
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
    
}
