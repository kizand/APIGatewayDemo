package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.netty.http.client.HttpClient;

import java.util.concurrent.atomic.AtomicLong;

@Configuration
public class GatewayConfig {

    private static final Logger log = LoggerFactory.getLogger(GatewayConfig.class);

    private final WebClient webClient = WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(HttpClient.create().followRedirect(true)))
            .build();

    // НАСТРОЙКИ RATE LIMITER (Аналог конфигурации из лекции)
    private final long BURST_CAPACITY = 20; // Максимальный запас запросов
    private final long REPLENISH_RATE = 10; // Сколько запросов добавляется в секунду

    private final AtomicLong tokens = new AtomicLong(BURST_CAPACITY);
    private long lastRefillTime = System.currentTimeMillis();

    // Метод для проверки и списания токена (алгоритм Token Bucket)
    private synchronized boolean isAllowed() {
        long now = System.currentTimeMillis();
        long timePassed = now - lastRefillTime;

        // Пополняем токены со временем (REPLENISH_RATE в секунду)
        long tokensToAdd = (timePassed * REPLENISH_RATE) / 1000;
        if (tokensToAdd > 0) {
            long newVal = Math.min(BURST_CAPACITY, tokens.get() + tokensToAdd);
            tokens.set(newVal);
            lastRefillTime = now;
        }

        // Если токены есть — уменьшаем счетчик на 1 и разрешаем запрос
        if (tokens.get() > 0) {
            tokens.decrementAndGet();
            return true;
        }
        return false; // Превышен лимит запросов
    }


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
                                        .exchangeToMono(res -> res.bodyToMono(String.class)), String.class)


                )
                // ФИЛЬТР 1: RATE LIMITER (Срабатывает в первую очередь)
                .filter((request, next) -> {
                    if (!isAllowed()) {
                        log.warn("--- [API Gateway] ПРЕДУПРЕЖДЕНИЕ: Превышен лимит запросов (Rate Limit Exceeded) для {}", request.path());
                        // Возвращаем клиенту статус 429 Too Many Requests
                        return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                                .bodyValue("Слишком много запросов! Пожалуйста, подождите.");
                    }
                    return next.handle(request);
                })
                // ФИЛЬТР 2: ЛОГИРОВАНИЕ (Сработает, только если Rate Limiter пропустил запрос)
                .filter((request, next) -> {
                    log.info("--- [API Gateway] Перехвачен запрос: {} {}", request.method(), request.path());
                    return next.handle(request);
                });
    }
}