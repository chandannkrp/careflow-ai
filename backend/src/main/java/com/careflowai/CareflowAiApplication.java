package com.careflowai;

import com.careflowai.ai.OpenAiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(OpenAiProperties.class)
public class CareflowAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(CareflowAiApplication.class, args);
    }
}
