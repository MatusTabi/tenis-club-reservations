package com.example.tenisclubreservations.apidoc;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityDocumentation {
  @Bean
  OpenAPI securityOpenApi() {
    return new OpenAPI().components(new Components()
        .addSecuritySchemes("bearerAuth", new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"))
        .addSecuritySchemes("basicAuth", new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")));
  }

  @Bean
  OpenApiCustomizer secureOperations() {
    return api -> api.getPaths().forEach((path, item) -> {
      if (!path.startsWith("/api/auth/")) {
        item.readOperations().forEach(operation -> {
          operation.addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
          operation.getResponses().addApiResponse("401", new ApiResponse().description("Authentication required"));
          operation.getResponses().addApiResponse("403", new ApiResponse().description("Insufficient permissions"));
        });
      }
    });
  }
}
