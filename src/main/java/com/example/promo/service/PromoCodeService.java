package com.example.promo.service;

import com.example.promo.entity.PromoCode;
import com.example.promo.entity.User;
import com.example.promo.exception.UserNotFoundException;
import com.example.promo.repository.PromoCodeRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;

@Service
public class PromoCodeService {

    private final PromoCodeRepository promoCodeRepository;
    private final UserService userService;

    public PromoCodeService(PromoCodeRepository promoCodeRepository,
                            UserService userService) {
        this.promoCodeRepository = promoCodeRepository;
        this.userService = userService;
    }

    public PromoCode save(PromoCode promoCode) {
        return promoCodeRepository.save(PromoCode.builder()
                .code(promoCode.getCode())
                .coinsReward(promoCode.getCoinsReward())
                .numUse(promoCode.getNumUse())
                .expiredDate(promoCode.getExpiredDate())
                .usersActivated(new ArrayList<>())
                .build()
        );
    }

    public PromoCode resetUsers(String code) {
        PromoCode promoCode = promoCodeRepository.findByCode(code);
        if (promoCode == null) {
            return null;
        }
        promoCode.setUsersActivated(new ArrayList<>());
        return promoCodeRepository.save(promoCode);
    }

    public PromoCode findByCode(String code) {
        return promoCodeRepository.findByCode(code);
    }

    public String useCode(String code, String vkId) {
        PromoCode promoCode = findByCode(code);
        if (promoCode == null ||
                promoCode.getNumUse() < 1 ||
                promoCode.getExpiredDate().before(Date.from(Instant.now())) ) {
            return "Промокод недействителен";
        }
        User user = userService.getUserByVkId(vkId);
        if (user == null) {
            throw new UserNotFoundException("User not found");
        }
        if (promoCode.getUsersActivated().contains(user)) {
            return "Вы уже использовали данный промокод";
        }
        if (promoCode.getNumUse() <= promoCode.getUsersActivated().size()) {
            return "Все промокоды уже использованы";
        }
        promoCode.getUsersActivated().add(user);
        userService.addCoins(vkId, promoCode.getCoinsReward());
        return "Начислено " + promoCode.getCoinsReward() + " монет";
    }
}
