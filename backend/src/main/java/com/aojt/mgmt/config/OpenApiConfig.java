package com.aojt.mgmt.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SCHEME = "bearerAuth";

    @Bean
    OpenAPI inventrackOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Inventrack API").version("v1"))
                .components(new Components().addSecuritySchemes(SCHEME,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME));
    }

    /** Lists the error statuses so Swagger UI does not label them "Undocumented". */
    @Bean
    OperationCustomizer errorResponses() {
        return (operation, handlerMethod) -> {
            add(operation, "400", "Bad request / validation error (see error.message in the body)");
            add(operation, "401", "Not authenticated, invalid token, or wrong credentials");
            if (!handlerMethod.getBeanType().getSimpleName().startsWith("Authentication")) {
                add(operation, "403", "Insufficient permission");
                add(operation, "404", "Resource not found");
            }
            if (handlerMethod.getBeanType().getSimpleName().startsWith("InventoryCenter")
                    || handlerMethod.getBeanType().getSimpleName().startsWith("Authentication")) {
                add(operation, "409", "Duplicate (center name / email)");
            }
            return operation;
        };
    }

    private static void add(io.swagger.v3.oas.models.Operation operation, String status, String description) {
        if (operation.getResponses().get(status) == null) {
            operation.getResponses().addApiResponse(status, new ApiResponse().description(description));
        }
    }
}
