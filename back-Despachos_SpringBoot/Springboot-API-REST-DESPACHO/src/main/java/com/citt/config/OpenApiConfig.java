package com.citt.config; 

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "API REST Despachos",
                version = "1.0",
                description = "API REST para gestión de despachos"
        ),
        servers = {
                @Server(url = "/api-despachos", description = "Servidor Despachos")
        }
)
public class OpenApiConfig {
}