package com.example.promo.service;

import com.vk.api.sdk.objects.messages.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class KeyboardService {

    private KeyboardButton getButton(String text, KeyboardButtonColor color) {
        return new KeyboardButton()
                .setAction(new KeyboardButtonActionText()
                        .setLabel(text)
                        .setType(KeyboardButtonActionTextType.TEXT))
                .setColor(color);
    }

    public Keyboard getKeyboardUser() {
        Keyboard keyboard = new Keyboard();
        List<List<KeyboardButton>> allKey = new ArrayList<>();
        List<KeyboardButton> line1 = new ArrayList<>();
        line1.add(getButton("Рейтинг", KeyboardButtonColor.DEFAULT));
        line1.add(getButton("Ввести промокод", KeyboardButtonColor.POSITIVE));
        allKey.add(line1);
        keyboard.setButtons(allKey);
        return keyboard;

    }
}
