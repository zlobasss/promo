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
            UserRequest userRequest = vkApiService.getUserInfo(vkId);
            if (userRequest == null) {
                System.out.println("User not found: " + vkId);
                continue;
            }
            User user = userService.save(userRequest);
            if (user == null) {
                user = userService.getUserByVkId(vkId);
            }
            userService.setAdmin(userRequest, true);
            System.out.println("Admin: " + userRequest.getFirstName() + " " + userRequest.getLastName());
        }
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
