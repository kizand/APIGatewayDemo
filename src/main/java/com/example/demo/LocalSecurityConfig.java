package com.example.demo;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Configuration
public class LocalSecurityConfig {

    // Секретный ключ (должен быть не менее 256 бит / 32 символов!)
    private final String SECRET_STRING = "mySecretKeyForJwtAuthentication2026!!!";
    private final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(SECRET_STRING.getBytes(StandardCharsets.UTF_8));

    @Bean
    public WebFilter jwtValidationFilter() {
        return (ServerWebExchange exchange, WebFilterChain chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();
            HttpMethod method = request.getMethod();

            // 1. Вытаскиваем заголовок Authorization
            String authHeader = request.getHeaders().getFirst("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                // Если токена нет — возвращаем 401 Unauthorized
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            String token = authHeader.substring(7);

            try {
                // 2. Валидируем токен локальным ключом и читаем Claims (данные внутри)
                Claims claims = Jwts.parser()
                        .verifyWith(SECRET_KEY)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                // Извлекаем роль из токена (например, сохранили её под ключом "role")
                String role = claims.get("role", String.class);

                // 3. Проверяем правила авторизации из вашего задания:

                // Админы могут выполнять POST на /orders
                if (path.startsWith("/orders") && method == HttpMethod.POST) {
                    if (!"ADMIN".equals(role)) {
                        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN); // 403
                        return exchange.getResponse().setComplete();
                    }
                }

                // Пользователи могут только GET на /users
                if (path.startsWith("/users") && method == HttpMethod.GET) {
                    if (!"USER".equals(role) && !"ADMIN".equals(role)) {
                        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN); // 403
                        return exchange.getResponse().setComplete();
                    }
                }

                // Если проверка пройдена, пропускаем запрос дальше в ваш GatewayConfig к микросервисам
                return chain.filter(exchange);

            } catch (Exception e) {
                // Если токен протух, подпись не совпала или он сломан
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        };
    }
}
