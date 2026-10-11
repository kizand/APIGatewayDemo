package com.example.demo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.netty.http.client.HttpClient;

@Configuration
public class GatewayConfig {

    private final WebClient webClient = WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(HttpClient.create().followRedirect(true)))
            .build();

    @Bean
    public RouterFunction<ServerResponse> customRouteLocator() {
        return RouterFunctions
                // Маршрут 1: Для пользователей
                .route(RequestPredicates.path("/users/**"), request ->
                        ServerResponse.ok().body(
                                webClient.method(request.method())
                                        .uri("http://localhost:8081" + request.path())
                                        .headers(h -> h.addAll(request.headers().asHttpHeaders()))
                                        .exchangeToMono(res -> res.bodyToMono(String.class)),
                                String.class)
                )
                // Маршрут 2: Для заказов
                .andRoute(RequestPredicates.path("/orders/**"), request ->
                        ServerResponse.ok().body(
                                webClient.method(request.method())
                                        .uri("http://localhost:8082" + request.path())
                                        .header("X-Request-ID", "12345") // Фильтр из задания
                                        .headers(h -> h.addAll(request.headers().asHttpHeaders()))
                                        .exchangeToMono(res -> res.bodyToMono(String.class)),
                                String.class)
                );
    }
}