package com.assignment.datasetops.api;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Top-level OpenAPI metadata served at {@code /v3/api-docs} and rendered by Swagger UI. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI datasetOpsOpenApi() {
        return new OpenAPI().info(new Info()
                .title("JSON Dataset Operators API")
                .description("Store JSON records in named datasets and query them with group-by and sort-by "
                        + "operators. Field names may be dot-paths (e.g. address.city) to reach nested values.")
                .version("1.0.0"));
    }
}
