package com.example.orderservice;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @GetMapping("/info")
    public String getOrderInfo() {
        return "Успех! Ответ напрямую из OrderService (Порт 8082) через API Gateway!";
    }

    // Добавляем этот метод для проверки админа!
    @PostMapping("/create")
    public String createOrder() {
        return "Заказ успешно создан в OrderService (Порт 8082) правами Администратора!";
    }
}
