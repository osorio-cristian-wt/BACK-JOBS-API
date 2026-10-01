package com.uap.proiv.jobs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.server.webmvc.GraphQlHttpHandler;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration
public class GraphQlConfig {

    @Bean
    public RouterFunction<ServerResponse> customGraphQlRouterFunction(GraphQlHttpHandler httpHandler) {
        // Esto intercepta explícitamente la ruta /test y mapea el handler oficial de GraphQL
        return RouterFunctions.route()
                .POST("/test", httpHandler::handleRequest)
                .build();
    }
}

