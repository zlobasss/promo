package com.example.promo.config;

import com.example.promo.dto.UserRequest;
import com.example.promo.entity.PhotoEntity;
import com.example.promo.entity.User;
import com.example.promo.service.PhotoService;
import com.example.promo.service.UserService;
import com.example.promo.service.VkApiService;
import com.vk.api.sdk.objects.photos.Photo;
import com.vk.api.sdk.objects.photos.responses.GetMessagesUploadServerResponse;
import com.vk.api.sdk.objects.photos.responses.MessageUploadResponse;
import com.vk.api.sdk.objects.photos.responses.SaveMessagesPhotoResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Configuration
public class AppConfig {

    @Autowired
    private final UserService userService;
    @Autowired
    private final VkApiService vkApiService;
    private final String vkIds;

    public AppConfig(VkApiService vkApiService,
                     UserService userService,
                     @Value("${vk.admin}") String vkIds,
                     PhotoService photoService) {
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
            User user;
            UserRequest userRequest1;
            System.out.print("vkId: " + vkId);
            try {
                userRequest1 = userRequest.get();
                System.out.println(" : Success : " + userRequest1.getFirstName() + ' ' + userRequest1.getLastName());
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
            System.out.println("Save...");
            user = userService.save(userRequest1);
            if (user == null) {
                System.out.println("Search...");
                user = userService.getUserByVkId(userRequest1.getVkId());
            }
            System.out.println("Set...");
            userService.setAdmin(user, true);
            System.out.println("Admin: " + user.getFirstName() + " " + user.getLastName());
        }
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
