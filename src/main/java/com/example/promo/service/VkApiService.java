package com.example.promo.service;

import com.example.promo.dto.UserRequest;
import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.account.responses.GetInfoResponse;
import com.vk.api.sdk.objects.account.responses.GetProfileInfoResponse;
import com.vk.api.sdk.objects.messages.*;
import com.vk.api.sdk.objects.messages.responses.GetIntentUsersResponse;
import com.vk.api.sdk.objects.users.responses.GetResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@AllArgsConstructor
public class VkApiService {
    private static final String ACCESS_TOKEN = "vk1.a.HWMlznfba_yAuMtJ1pl0lM1bkp3aRcQNlsX5tmcPAhsGyNK8wO7PMcMaytzVch-Dgvi4FvHeLXqkj_ma40d3cIxHsHkVqyQG7lrkKnvaAUqAPvCUC5hNm8BTVHZ-BzIkvBLkbcHgL6oM0KG84MVq303A-30uZunUgzx4kgl4k2A0opLhfDj0fAYtnu5W2BcE5al05bXSUMNNFI64jq-RJw";
    private static final Random RANDOM = new Random();
    private static final Long GROUP_ID = (long) 227060287;
    private final TransportClient transportClient = new HttpTransportClient();
    private final VkApiClient vk = new VkApiClient(transportClient);
    private final KeyboardService keyboardService;

    public String sendMessage(Long userId, String message) throws ClientException, ApiException {
        int randomId = RANDOM.nextInt(Integer.MAX_VALUE);
        System.out.println("Send request");
        GroupActor actor = new GroupActor(GROUP_ID, ACCESS_TOKEN);
        Keyboard keyboard = keyboardService.getKeyboardUser();
        Integer response = vk.messages().sendDeprecated(actor).keyboard(keyboard).message(message).randomId(randomId).userId(userId).execute();
        return response.toString();
    }

    public UserRequest getUserInfo(Long userId) throws ClientException, ApiException {
        UserRequest request = new UserRequest();
        GroupActor actor = new GroupActor(GROUP_ID, ACCESS_TOKEN);
        List<GetResponse> response = vk.users().get(actor).userIds(userId.toString()).execute();
        if (response.isEmpty()) {
            return null;
        }
        GetResponse getResponse = response.get(0);
        request.setFirstName(getResponse.getFirstName());
        request.setLastName(getResponse.getLastName());
        request.setVkId(userId.toString());
        return request;
    }

}

