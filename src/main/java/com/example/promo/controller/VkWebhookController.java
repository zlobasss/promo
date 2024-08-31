package com.example.promo.controller;

import com.example.promo.dto.AddCoinsRequest;
import com.example.promo.entity.Product;
import com.example.promo.entity.PromoCode;
import com.example.promo.entity.Section;
import com.example.promo.dto.UserRequest;
import com.example.promo.entity.User;
import com.example.promo.repository.UserRepository;
import com.example.promo.service.*;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.objects.messages.Keyboard;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/webhook")
public class VkWebhookController {

    private final VkApiService vkApiService;
    private final UserService userService;
    private final KeyboardService keyboardService;
    private final PromoCodeService promoCodeService;
    private final Map<String, String> vkIdAndLastCommand;
    private final Map<String, Keyboard> vkIdAndLastKeyboard;
    private final Map<String, String> vkIdAndAddCoinForVkId;
    private final Map<String, PromoCode> vkIdAndPromoCodeRequest;
    private final Map<String, Product> vkIdAndProductRequest;
    private final Map<String, Integer> vkIdAndPageRequest;
    private final ProductService productService;
    private final User manager;
    private final String CONFIRMATION_CODE;
    private final String SECRET_KEY;
    private final UserRepository userRepository;


    public VkWebhookController(VkApiService vkApiService,
                               UserService userService,
                               KeyboardService keyboardService,
                               PromoCodeService promoCodeService,
                               ProductService productService,
                               @Value("${vk.manager}") String vkId,
                               @Value("${vk.confirm.code}") String confirm,
                               @Value("${vk.secret.key}") String secret, UserRepository userRepository) {
        CONFIRMATION_CODE = confirm;
        SECRET_KEY = secret;
        vkIdAndLastCommand = new HashMap<>();
        vkIdAndLastKeyboard = new HashMap<>();
        vkIdAndAddCoinForVkId = new HashMap<>();
        vkIdAndPromoCodeRequest = new HashMap<>();
        vkIdAndProductRequest = new HashMap<>();
        vkIdAndPageRequest = new HashMap<>();
        this.vkApiService = vkApiService;
        this.userService = userService;
        this.keyboardService = keyboardService;
        this.promoCodeService = promoCodeService;
        this.productService = productService;
        try {
            this.manager = userService.getUserByVkId(vkApiService.getUserInfo(vkId).get().getVkId());
            System.out.println("Manager init");
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        this.userRepository = userRepository;
    }


    @PostMapping
    public ResponseEntity<String> handleWebhook(@RequestBody Map<String, Object> payload) throws ClientException, ApiException {
        String type = (String) payload.get("type");

        return switch (type) {
            case "confirmation" -> confirmationCommand(payload);
            case "message_new" -> messageNewCommand(payload);
            default -> ResponseEntity.ok("OK");
        };
    }

    private ResponseEntity<String> messageNewCommand(Map<String, Object> payload) {

        Map<String, Object> object = (Map<String, Object>) payload.get("object");
        Map<String, Object> message = (Map<String, Object>) object.get("message");
        String text = (String) message.get("text");
        Integer userIdInteger = (Integer) message.get("from_id");
        String vkId = String.valueOf(userIdInteger);

        User user = userService.getUserByVkId(vkId);

        if (user == null) {
            UserRequest request = null;
            try {
                request = vkApiService.getUserInfo(vkId).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
            user = userService.save(request);
        }

        Keyboard keyboard = keyboardService.getKeyboardBySectionAndIsAdmin(Section.START, user.getIsAdmin());
        String messageForSend = "Неизвестная команда";

        String lastCommand = vkIdAndLastCommand.get(vkId);

        if (lastCommand != null) {
            keyboard = vkIdAndLastKeyboard.get(vkId);
            vkIdAndLastKeyboard.remove(vkId);
            vkIdAndLastCommand.remove(vkId);
            if (Objects.equals(text, "ОТМЕНА")) {
                messageForSend = "Отменено...";
            }
            else {
                switch (lastCommand) {
                    case "Ввести промокод":
                        messageForSend = promoCodeService.useCode(text, vkId);
                        break;
                    case "Добавить энергию 1":
                        UserRequest userRequest = null;
                        try {
                            userRequest = vkApiService.getUserInfo(text).get();
                        } catch (InterruptedException | ExecutionException e) {
                            throw new RuntimeException(e);
                        }
                        if (userService.getUserByVkId(userRequest.getVkId()) == null) {
                            messageForSend = "Пользователь не найден";
                            break;
                        }
                        vkIdAndLastKeyboard.put(vkId, keyboard);
                        vkIdAndLastCommand.put(vkId, "Добавить энергию 2");
                        vkIdAndAddCoinForVkId.put(vkId, userRequest.getVkId());
                        messageForSend = "Введите количество энергии ⚡...";
                        keyboard = keyboardService.getKeyboardBySectionAndIsAdmin(Section.PROCESS, user.getIsAdmin());
                        break;
                    case "Добавить энергию 2":
                        int coins = 0;
                        try {
                            coins = Integer.parseInt(text);
                        } catch (NumberFormatException e) {
                            messageForSend = "Неверный формат числа";
                            break;
                        }
                        String vkIdRequest = vkIdAndAddCoinForVkId.get(vkId);
                        if (userService.addCoins(vkIdRequest, coins) == null) {
                            messageForSend = "Энергии меньше нуля...";
                            break;
                        }
                        messageForSend = "Добавлено " + coins + " энергии для " + vkIdRequest;
                        break;
                    case "Сделать рассылку 1":
                        userService.sendMessageByListUser(userRepository.findByIsAdmin(false), text);
                        messageForSend = "Рассылка отправлена....";
                        break;
                    case "Сброс энергии 1":
                        if (Objects.equals(text, "Да")) {
                            userService.resetCoinsByListUser(userRepository.findAll());
                        }
                        messageForSend = "Сброс осуществлён....";
                        break;
                    case "Убавить энергии 1":
                        UserRequest userRequestReduce = null;
                        try {
                            userRequestReduce = vkApiService.getUserInfo(text).get();
                        } catch (InterruptedException | ExecutionException e) {
                            throw new RuntimeException(e);
                        }
                        if (userService.getUserByVkId(userRequestReduce.getVkId()) == null) {
                            messageForSend = "Пользователь не найден";
                            break;
                        }
                        vkIdAndLastKeyboard.put(vkId, keyboard);
                        vkIdAndLastCommand.put(vkId, "Убавить энергии 2");
                        vkIdAndAddCoinForVkId.put(vkId, userRequestReduce.getVkId());
                        messageForSend = "Введите количество энергии ⚡...";
                        keyboard = keyboardService.getKeyboardBySectionAndIsAdmin(Section.PROCESS, user.getIsAdmin());
                        break;
                    case "Убавить энергию 2":
                        int coinsReduce = 0;
                        try {
                            coinsReduce = Integer.parseInt(text);
                        } catch (NumberFormatException e) {
                            messageForSend = "Неверный формат числа";
                            break;
                        }
                        String vkIdRequestReduce = vkIdAndAddCoinForVkId.get(vkId);
                        if (userService.reduceCoins(vkIdRequestReduce, coinsReduce) == null) {
                            messageForSend = "Число энергии меньше нуля...";
                            break;
                        }
                        messageForSend = "Уменьшено " + coinsReduce + " энергии для " + vkIdRequestReduce;
                        break;
                    case "Добавить промокод 1":
                        PromoCode promoCode = new PromoCode();
                        if (text.length() <= 0 || text.length() > 32) {
                            messageForSend = "Неверная длина кода";
                            vkIdAndPromoCodeRequest.remove(vkId);
                            break;
                        }
                        promoCode.setCode(text);
                        vkIdAndPromoCodeRequest.put(vkId, promoCode);
                        messageForSend = "Введите количество использований...";
                        vkIdAndLastKeyboard.put(vkId, keyboard);
                        vkIdAndLastCommand.put(vkId, "Добавить промокод 2");
                        keyboard = keyboardService.getKeyboardBySectionAndIsAdmin(Section.PROCESS, user.getIsAdmin());
                        break;
                    case "Добавить промокод 2":
                        int numUse = 0;
                        try {
                            numUse = Integer.parseInt(text);
                            if (numUse <= 0) {
                                throw new NumberFormatException();
                            }
                        } catch (NumberFormatException e) {
                            vkIdAndPromoCodeRequest.remove(vkId);
                            messageForSend = "Неверный формат числа";
                            break;
                        }
                        vkIdAndPromoCodeRequest.get(vkId).setNumUse(numUse);
                        messageForSend = "Введите количество энергии ⚡...";
                        vkIdAndLastKeyboard.put(vkId, keyboard);
                        vkIdAndLastCommand.put(vkId, "Добавить промокод 3");
                        keyboard = keyboardService.getKeyboardBySectionAndIsAdmin(Section.PROCESS, user.getIsAdmin());
                        break;
                    case "Добавить промокод 3":
                        int reward = 0;
                        try {
                            reward = Integer.parseInt(text);
                            if (reward <= 0) {
                                throw new NumberFormatException();
                            }
                        } catch (NumberFormatException e) {
                            messageForSend = "Неверный формат числа";
                            vkIdAndPromoCodeRequest.remove(vkId);
                            break;
                        }
                        vkIdAndPromoCodeRequest.get(vkId).setCoinsReward(reward);
                        messageForSend = "Введите дату истечения... Формат (дд-ММ-гггг ЧЧ:мм)";
                        vkIdAndLastKeyboard.put(vkId, keyboard);
                        vkIdAndLastCommand.put(vkId, "Добавить промокод 4");
                        keyboard = keyboardService.getKeyboardBySectionAndIsAdmin(Section.PROCESS, user.getIsAdmin());
                        break;
                    case "Добавить промокод 4":
                        Date expired;
                        DateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm");
                        try {
                            expired = format.parse(text);
                            if (expired.before(Date.from(Instant.now()))) {
                                throw new ParseException("Дата меньше текущей", 1);
                            }
                        } catch (ParseException e) {
                            messageForSend = "Неверный формат числа";
                            vkIdAndPromoCodeRequest.remove(vkId);
                            break;
                        }
                        PromoCode promoCode1 = vkIdAndPromoCodeRequest.get(vkId);
                        promoCode1.setExpiredDate(expired);
                        promoCodeService.save(promoCode1);
                        messageForSend = "Промокод " + promoCode1.getCode() + " создан";
                        break;
                    case "Добавить товар 1":
                        Product product = new Product();
                        product.setName(text);
                        vkIdAndProductRequest.put(vkId, product);
                        messageForSend = "Введите стоимость товара...";
                        vkIdAndLastKeyboard.put(vkId, keyboard);
                        vkIdAndLastCommand.put(vkId, "Добавить товар 2");
                        keyboard = keyboardService.getKeyboardBySectionAndIsAdmin(Section.PROCESS, user.getIsAdmin());
                        break;
                    case "Добавить товар 2":
                        int price = 0;
                        try {
                            price = Integer.parseInt(text);
                            if (price <= 0) {
                                throw new NumberFormatException();
                            }
                        } catch (NumberFormatException e) {
                            messageForSend = "Неверный формат числа";
                            vkIdAndProductRequest.remove(vkId);
                            break;
                        }
                        vkIdAndProductRequest.get(vkId).setPrice(price);
                        messageForSend = "Введите код товара... (до 8 знаков)";
                        vkIdAndLastKeyboard.put(vkId, keyboard);
                        vkIdAndLastCommand.put(vkId, "Добавить товар 3");
                        keyboard = keyboardService.getKeyboardBySectionAndIsAdmin(Section.PROCESS, user.getIsAdmin());
                        break;
                    case "Добавить товар 3":
                        Product product1 = vkIdAndProductRequest.get(vkId);
                        vkIdAndLastKeyboard.remove(vkId);
                        if (text.length() <= 0 || text.length() > 8) {
                            messageForSend = "Неверная длина кода";
                            break;
                        }
                        product1.setCode(text);
                        productService.save(product1);
                        messageForSend = "Товар <" + product1.getName() + "> создан";
                        break;
                    case "Удалить товар 1":
                        Product product2 = productService.delete(text);
                        messageForSend = "Товар <" + product2.getName() + "> удален";
                        break;

                    case "Посмотреть товары":
                        int page = vkIdAndPageRequest.get(vkId);
                        Page<Product> products = productService.getProducts(page);
                        if (Objects.equals(text, "«") && page != 0) {
                            page--;
                        }
                        else if (Objects.equals(text, ">>") && page != products.getTotalPages()) {
                            page++;
                        } 
                        else if (Objects.equals(text, "Товары")) {
                            vkIdAndPageRequest.remove(vkId);
                            messageForSend = "Вы в разделе товары";
                            break;
                        }
                        boolean isWorked = false;
                        for (Product product3 : products.getContent()) {
                            if (Objects.equals(text, "Купить <" + product3.getName() + ">")) {
                                User user1 = userService.getUserByVkId(vkId);
                                if (user1.getCoins() >= product3.getPrice()) {
                                    // Логика Написания менеджеру
                                    user1.setCoins(user1.getCoins() - product3.getPrice());
                                    userService.update(user1);
                                    messageForSend = "Спасибо за покупку ожидайте нашего менеджера!";
                                    String messageForManager = "Пользователь < https://vk.com/id" + vkId + " > купил товар < " + product3.getName() + " >";
                                    vkApiService.sendMessage(manager.getVkId(), messageForManager, new Keyboard());
                                }
                                else {
                                    messageForSend = "Недостаточно энергии ⚡";
                                }
                                isWorked = true;
                            }
                        }
                        if (isWorked) {
                            vkIdAndLastKeyboard.put(vkId, keyboard);
                            keyboard = keyboardService.getKeyboardForPageProduct(products, page);
                            vkIdAndLastCommand.put(vkId, "Посмотреть товары");
                            break;
                        }
                        Page<Product> productsNewPage = productService.getProducts(page);
                        StringBuilder messageForSendBuilder = new StringBuilder("Список товаров:\n");
                        for (Product product3 : productsNewPage.getContent()) {
                            messageForSendBuilder
                                    .append("[ ")
                                    .append(product3.getCode())
                                    .append(" ] - < ")
                                    .append(product3.getName())
                                    .append(" > = < ")
                                    .append(product3.getPrice())
                                    .append(" энергии ⚡ >\n");
                        }
                        vkIdAndLastKeyboard.put(vkId, keyboard);
                        keyboard = keyboardService.getKeyboardForPageProduct(productsNewPage, page);
                        messageForSend = messageForSendBuilder.toString();
                        vkIdAndLastCommand.put(vkId, "Посмотреть товары");
                        break;
                }
            }
        }
        else {
            switch (text) {
                case "Энергия":
                    keyboard = keyboardService.getKeyboardBySectionAndIsAdmin(Section.COINS, user.getIsAdmin());
                    messageForSend = "Вы в разделе энергии";
                    break;
                case "Главное":
                    keyboard = keyboardService.getKeyboardBySectionAndIsAdmin(Section.START, user.getIsAdmin());
                    messageForSend = "Вы в главном разделе";
                    break;
                case "Товары":
                    keyboard = keyboardService.getKeyboardBySectionAndIsAdmin(Section.PRODUCT, user.getIsAdmin());
                    messageForSend = "Вы в разделе товары";
                    break;
                case "Рейтинг":
                    keyboard = keyboardService.getKeyboardBySectionAndIsAdmin(Section.COINS, user.getIsAdmin());
                    messageForSend = userService.getRating(vkId);
                    break;
                case "Ввести промокод":
                    vkIdAndLastCommand.put(vkId, "Ввести промокод");
                    vkIdAndLastKeyboard.put(vkId, keyboardService.getKeyboardBySectionAndIsAdmin(Section.COINS, user.getIsAdmin()));
                    keyboard = keyboardService.getKeyboardBySectionAndIsAdmin(Section.PROCESS, user.getIsAdmin());
                    messageForSend = "Введите промокод...";
                    break;
                case "Посмотреть товары":
                    vkIdAndLastCommand.put(vkId, "Посмотреть товары");
                    vkIdAndLastKeyboard.put(vkId, keyboardService.getKeyboardBySectionAndIsAdmin(Section.PRODUCT, user.getIsAdmin()));
                    int page = 0;
                    vkIdAndPageRequest.put(vkId, page);
                    Page<Product> products = productService.getProducts(0);
                    keyboard = keyboardService.getKeyboardForPageProduct(products, page);
                    StringBuilder messageForSendBuilder = new StringBuilder("Список товаров:\n");
                    for (Product product : products.getContent()) {
                        messageForSendBuilder
                                .append("[ ")
                                .append(product.getCode())
                                .append(" ] - < ")
                                .append(product.getName())
                                .append(" > = < ")
                                .append(product.getPrice())
                                .append(" энергии ⚡ >\n");
                    }
                    messageForSend = messageForSendBuilder.toString();
                    break;
            }
            if (user.getIsAdmin()) {
                switch (text) {
                    case "Убавить энергию":
                        vkIdAndLastCommand.put(vkId, "Убавить энергию 1");
                        vkIdAndLastKeyboard.put(vkId, keyboardService.getKeyboardBySectionAndIsAdmin(Section.COINS, user.getIsAdmin()));
                        keyboard = keyboardService.getKeyboardBySectionAndIsAdmin(Section.PROCESS, user.getIsAdmin());
                        messageForSend = "Введите id пользователя...";
                        break;
                    case "Добавить энергию":
                        vkIdAndLastCommand.put(vkId, "Добавить энергию 1");
                        vkIdAndLastKeyboard.put(vkId, keyboardService.getKeyboardBySectionAndIsAdmin(Section.COINS, user.getIsAdmin()));
                        keyboard = keyboardService.getKeyboardBySectionAndIsAdmin(Section.PROCESS, user.getIsAdmin());
                        messageForSend = "Введите id пользователя...";
                        break;
                    case "Сделать рассылку":
                        vkIdAndLastCommand.put(vkId, "Сделать рассылку 1");
                        vkIdAndLastKeyboard.put(vkId, keyboardService.getKeyboardBySectionAndIsAdmin(Section.START, user.getIsAdmin()));
                        keyboard = keyboardService.getKeyboardBySectionAndIsAdmin(Section.PROCESS, user.getIsAdmin());
                        messageForSend = "Введите сообщение рассылки...";
                        break;
                    case "Сброс энергии":
                        vkIdAndLastCommand.put(vkId, "Сброс энергии 1");
                        vkIdAndLastKeyboard.put(vkId, keyboardService.getKeyboardBySectionAndIsAdmin(Section.COINS, user.getIsAdmin()));
                        keyboard = keyboardService.getKeyboardBySectionAndIsAdmin(Section.PROCESS, user.getIsAdmin());
                        messageForSend = "Если уверены, то введите \"Да\"...";
                        break;
                    case "Добавить промокод":
                        vkIdAndLastCommand.put(vkId, "Добавить промокод 1");
                        vkIdAndLastKeyboard.put(vkId, keyboardService.getKeyboardBySectionAndIsAdmin(Section.COINS, user.getIsAdmin()));
                        keyboard = keyboardService.getKeyboardBySectionAndIsAdmin(Section.PROCESS, user.getIsAdmin());
                        messageForSend = "Введите промокод... (до 32 символов)";
                        break;
                    case "Добавить товар":
                        vkIdAndLastCommand.put(vkId, "Добавить товар 1");
                        vkIdAndLastKeyboard.put(vkId, keyboardService.getKeyboardBySectionAndIsAdmin(Section.PRODUCT, user.getIsAdmin()));
                        keyboard = keyboardService.getKeyboardBySectionAndIsAdmin(Section.PROCESS, user.getIsAdmin());
                        messageForSend = "Введите название...";
                        break;
                    case "Удалить товар":
                        vkIdAndLastCommand.put(vkId, "Удалить товар 1");
                        vkIdAndLastKeyboard.put(vkId, keyboardService.getKeyboardBySectionAndIsAdmin(Section.PRODUCT, user.getIsAdmin()));
                        keyboard = keyboardService.getKeyboardBySectionAndIsAdmin(Section.PROCESS, user.getIsAdmin());
                        messageForSend = "Введите код товара...";
                        break;
                }
            }
        }
        vkApiService.sendMessage(vkId, messageForSend, keyboard);

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
