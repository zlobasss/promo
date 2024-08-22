package com.example.promo.controller;

import com.example.promo.dto.UserRequest;
import com.example.promo.service.UserService;
import com.example.promo.service.VkApiService;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/webhook")
public class VkWebhookController {
    @Autowired
    private VkApiService vkApiService;

    @Autowired
    private UserService userService;

    private static final String CONFIRMATION_CODE = "0471cd01";
    private static final String SECRET_KEY = "Noecm3hVEibB2EEggpDPsgsvqkqkW9t";


    @PostMapping
    public ResponseEntity<String> handleWebhook(@RequestBody Map<String, Object> payload) throws ClientException, ApiException {
        String type = (String) payload.get("type");

        System.out.println(payload);

        return switch (type) {
            case "confirmation" -> confirmationCommand(payload);
            case "message_new" -> messageNewCommand(payload);
            default -> ResponseEntity.ok("OK");
        };

    }

    private ResponseEntity<String> messageNewCommand(Map<String, Object> payload) throws ClientException, ApiException {

        Map<String, Object> object = (Map<String, Object>) payload.get("object");
        Map<String, Object> message = (Map<String, Object>) object.get("message");
        String text = (String) message.get("text");
        Integer userIdInteger = (Integer) message.get("from_id");
        Long userId = Long.valueOf(userIdInteger);

        UserRequest request = vkApiService.getUserInfo(userId);
        userService.save(request);

        if (text.equals("Рейтинг")) {
            vkApiService.sendMessage(userId, userService.getRating(request.getVkId()));
        } else if (text.equals("Ввести промокод")) {
            vkApiService.sendMessage(userId, "Вы нажали Кнопку 2");
        }

        return ResponseEntity.ok("OK");
    }

    private ResponseEntity<String> confirmationCommand(Map<String, Object> payload) {
        String secret = (String) payload.get("secret");
        if (secret.equals(SECRET_KEY)) {
            return ResponseEntity.ok(CONFIRMATION_CODE);
        }
        return ResponseEntity.badRequest().body("BAD_REQUEST");
    }
}
