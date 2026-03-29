package com.shopsphere.adminservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
/*
 * What:
 * Swagger/OpenAPI config for adminservice.
 *
 * Why:
 * Gives readable API documentation and interactive testing page.
 *
 * How:
 * Exposes one OpenAPI bean with service title, version, and description.
 */
public class SwaggerOpenApiConfig {

    /*
     * What: Builds OpenAPI metadata object.
     * Why: Swagger UI uses this info as API header details.
     * How: Return new OpenAPI().info(...).
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ShopSphere Admin Service API")
                        .version("1.0")
                        .description("APIs for admin dashboard, reports, products, and order management"));
    }
}

