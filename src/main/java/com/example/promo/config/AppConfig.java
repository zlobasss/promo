package com.example.promo.config;

import com.example.promo.dto.UserRequest;
import com.example.promo.entity.User;
import com.example.promo.service.UserService;
import com.example.promo.service.VkApiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Configuration
public class AppConfig {

    private final UserService userService;
    private final VkApiService vkApiService;
    private final String vkIds;

    public AppConfig(VkApiService vkApiService,
                     UserService userService,
                     @Value("${vk.admin}") String vkIds) {
        this.vkApiService = vkApiService;
        this.userService = userService;
        this.vkIds = vkIds;
        initAdminUsers();
    }

    private void initAdminUsers() {
        System.out.println("Initializing Admin Users...");
        userService.resetAdmins();

        List<String> idList = List.of(vkIds.split(" "));
        for (String vkId : idList) {
            CompletableFuture<UserRequest> userRequest = vkApiService.getUserInfo(vkId);
            if (userRequest == null) {
                System.out.println("User not found: " + vkId);
                continue;
            }
            User user = null;
            try {
                user = userService.save(userRequest.get());
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
            if (user == null) {
                user = userService.getUserByVkId(vkId);
            }
            try {
                userService.setAdmin(userRequest.get(), true);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
            System.out.println("Admin: " + user.getFirstName() + " " + user.getLastName());
        }
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
