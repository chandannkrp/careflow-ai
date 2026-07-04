package com.careflowai.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI careflowOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("CareFlow AI API")
                .version("0.1.0")
                .description("Backend API documentation for CareFlow AI intake, queue, metrics, staff, agent, allocation, and AI chat workflows.")
                .contact(new Contact()
                    .name("CareFlow AI Team"))
                .license(new License()
                    .name("Internal")));
    }

    @Bean
    public GroupedOpenApi careflowApiGroup() {
        return GroupedOpenApi.builder()
            .group("careflow-api")
            .packagesToScan("com.careflowai")
            .pathsToMatch("/api/**")
            .build();
    }
}
