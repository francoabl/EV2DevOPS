package com.citt.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "API REST Ventas",
                version = "1.0",
                description = "API REST Demo para gestionar ventas de productos. Lanzamiento CITT Duoc UC Viña del Mar 2025"
        )
)
public class OpenApiConfing {

    @Bean
    public OpenAPI customOpenAPI() {

        Server server = new Server();
        server.setUrl("http://98.84.206.17/api-ventas");

        return new OpenAPI()
                .servers(List.of(server));
    }
}