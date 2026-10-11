package com.example.userservice;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users") // 1. Этот префикс обязателен, так как шлюз шлет путь /users/
public class UserController {

    @GetMapping("/info") // 2. Этот метод отвечает за хвостик /info
    public String getUserInfo() {
        return "Успех! Ответ напрямую из UserService (Порт 8081) через API Gateway!";
    }
}
