package com.example.promo.service;

import com.example.promo.dto.UserRequest;
import com.example.promo.exception.VkApiException;
import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.messages.*;
import com.vk.api.sdk.objects.users.responses.GetResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

@Service
@EnableAsync
public class VkApiService {
    private static final Random RANDOM = new Random();
    private final GroupActor groupActor;
    private final VkApiClient vk;

    public VkApiService(@Value("${vk.token}") String accessToken,
                        @Value("${vk.group}") Long groupId) {

        TransportClient transportClient = new HttpTransportClient();
        this.vk = new VkApiClient(transportClient);
        this.groupActor = new GroupActor(groupId, accessToken);
    }

    @Async
    public void sendMessage(String userId, String message, Keyboard keyboard) {
        int randomId = RANDOM.nextInt(Integer.MAX_VALUE);
        try {
            vk.messages().sendDeprecated(groupActor).keyboard(keyboard).message(message).randomId(randomId).userId(Long.valueOf(userId)).execute();
        } catch (Exception ignored) {
            throw new VkApiException("Don`t send message");
        }
    }

    public UserRequest getUserInfo(String vkId) {
        UserRequest request = new UserRequest();
        List<GetResponse> response;
        try {
            response = vk.users().get(groupActor).userIds(vkId).execute();
        } catch (Exception ignored) {
            throw new VkApiException("Don`t get user info");
        }
        if (response.isEmpty()) {
            return null;
        }
        GetResponse getResponse = response.get(0);
        request.setFirstName(getResponse.getFirstName());
        request.setLastName(getResponse.getLastName());
        request.setVkId(getResponse.getId().toString());
        return request;
    }

}

