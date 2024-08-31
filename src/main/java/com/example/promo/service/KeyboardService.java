package com.example.promo.service;

import com.example.promo.entity.Product;
import com.example.promo.entity.Section;
import com.example.promo.entity.Button;
import com.vk.api.sdk.objects.messages.*;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class KeyboardService {

    private final List<Button> allButtons = new ArrayList<>();

    public KeyboardService() {
        allButtons.add(new Button("Энергия", KeyboardButtonColor.DEFAULT, false, 1, 1)
                .addSection(Section.START)
                .addSection(Section.PRODUCT));
        allButtons.add(new Button("Рейтинг", KeyboardButtonColor.DEFAULT, false, 2, 1)
                .addSection(Section.COINS));
        allButtons.add(new Button("Ввести промокод", KeyboardButtonColor.DEFAULT, false, 2, 2)
                .addSection(Section.COINS));
        allButtons.add(new Button("Главное", KeyboardButtonColor.DEFAULT, false, 1, 1)
                .addSection(Section.COINS));
        allButtons.add(new Button("Товары", KeyboardButtonColor.DEFAULT, false, 1, 2)
                .addSection(Section.COINS));
        allButtons.add(new Button("Посмотреть товары", KeyboardButtonColor.DEFAULT, false, 2, 1)
                .addSection(Section.PRODUCT));
        allButtons.add(new Button("Добавить промокод", KeyboardButtonColor.DEFAULT, true, 3, 1)
                .addSection(Section.COINS));
        allButtons.add(new Button("Добавить энергию", KeyboardButtonColor.DEFAULT, true, 4, 1)
                .addSection(Section.COINS));
        allButtons.add(new Button("Убавить энергию", KeyboardButtonColor.DEFAULT, true, 4, 2)
                .addSection(Section.COINS));
        allButtons.add(new Button("Сброс энергии", KeyboardButtonColor.NEGATIVE, true, 5, 1)
                .addSection(Section.COINS));
        allButtons.add(new Button("Добавить товар", KeyboardButtonColor.DEFAULT, true, 3, 1)
                .addSection(Section.PRODUCT));
        allButtons.add(new Button("Удалить товар", KeyboardButtonColor.DEFAULT, true, 3, 2)
                .addSection(Section.PRODUCT));
        allButtons.add(new Button("ОТМЕНА", KeyboardButtonColor.NEGATIVE, false, 1, 1)
                .addSection(Section.PROCESS));
        allButtons.add(new Button("Сделать рассылку", KeyboardButtonColor.POSITIVE, true, 2, 1)
                .addSection(Section.START));
    }

    public Keyboard getKeyboardForPageProduct(Page<Product> products, int page) {
        List<List<Button>> linesWithButtons = new ArrayList<>();
        int x = 1;
        for (Product product : products.getContent()) {
            List<Button> buttons = new ArrayList<>();
            buttons.add(new Button("Купить <" + product.getName() + ">", KeyboardButtonColor.DEFAULT, false, x, 1));
            ++x;
            linesWithButtons.add(buttons);
        }
        List<Button> buttons = new ArrayList<>();
        int y = 1;
        if (page != 0) {
            buttons.add(new Button("«", KeyboardButtonColor.DEFAULT, false, x, y));
            ++y;
        }
        buttons.add(new Button("Товары", KeyboardButtonColor.DEFAULT, false, x, y));
        ++y;
        if (page != products.getTotalPages() - 1) {
            buttons.add(new Button(">>", KeyboardButtonColor.DEFAULT, false, x, y));
        }
        linesWithButtons.add(buttons);
        return getKeyboardByLinesWithButtons(linesWithButtons);
    }

    public Keyboard getKeyboardBySectionAndIsAdmin(Section section, Boolean isAdmin) {
        List<List<Button>> linesWithButtons = new ArrayList<>();
        int lines = 4;
        for (int i = 0; i < lines; i++) {
            List<Button> buttons = new ArrayList<>();
            linesWithButtons.add(buttons);
        }
        for (Button button : allButtons) {
            if (button.getIsAdminButton() && !isAdmin) {
                continue;
            }
            if (!button.getSections().contains(section)) {
                continue;
            }
            List<Button> buttons = linesWithButtons.get(button.getX() - 1);
            buttons.add(button);
            linesWithButtons.set(button.getX() - 1, buttons);
        }
        linesWithButtons.removeIf(List::isEmpty);
        return getKeyboardByLinesWithButtons(linesWithButtons);
    }


    private Keyboard getKeyboardByLinesWithButtons(List<List<Button>> linesWithButtons) {
        Keyboard keyboard = new Keyboard();
        List<List<KeyboardButton>> allButtons = new ArrayList<>();
        for (List<Button> buttons : linesWithButtons) {
            List<KeyboardButton> keyboardButtons = new ArrayList<>();
            for (Button button : buttons) {
                keyboardButtons.add(button.getKeyboardButton());
            }
            allButtons.add(keyboardButtons);
        }
        keyboard.setButtons(allButtons);
        return keyboard;
    }
}
