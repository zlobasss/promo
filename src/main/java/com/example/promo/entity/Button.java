package com.example.promo.entity;

import com.vk.api.sdk.objects.messages.KeyboardButton;
import com.vk.api.sdk.objects.messages.KeyboardButtonActionText;
import com.vk.api.sdk.objects.messages.KeyboardButtonActionTextType;
import com.vk.api.sdk.objects.messages.KeyboardButtonColor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Button {
    private String text;
    private String command;
    private KeyboardButtonColor color;
    private List<Section> sections;
    private Boolean isAdminButton;
    private Integer x;
    private Integer y;

    public Button(String text, KeyboardButtonColor color, Boolean isAdminButton, Integer x, Integer y) {
        this.text = text;
        this.color = color;
        this.isAdminButton = isAdminButton;
        this.x = x;
        this.y = y;
        sections = new ArrayList<>();
    }
    
    public Button setCommand(String command) {
        this.command = command;
        return this;
    }

    public Button addSection(Section section) {
        sections.add(section);
        return this;
    }

    public KeyboardButton getKeyboardButton() {
        return new KeyboardButton()
                .setAction(new KeyboardButtonActionText()
                        .setLabel(text)
                        .setType(KeyboardButtonActionTextType.TEXT)
                        .setPayload("{\"command\":\"" + command + "\"}"))
                
                .setColor(color);
    }
}
