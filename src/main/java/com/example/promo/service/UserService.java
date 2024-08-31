package com.example.promo.service;

import com.example.promo.dto.RatingLineResponse;
import com.example.promo.dto.UserRequest;
import com.example.promo.entity.User;
import com.example.promo.exception.CoinLessZeroException;
import com.example.promo.exception.UserNotFoundException;
import com.example.promo.repository.UserRepository;
import com.vk.api.sdk.objects.messages.Keyboard;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private VkApiService vkApiService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public User save(UserRequest request) {
        if (userExists(request.getVkId())) {
            return null;
        }
        User user = User.builder()
                .vkId(request.getVkId())
                .lastName(request.getLastName())
                .firstName(request.getFirstName())
                .coins(0)
                .isAdmin(false)
                .usedPromoCodes(new ArrayList<>())
                .build();
        return userRepository.save(user);
    }

    private Boolean userExists(String vkId) {
        return userRepository.findByVkId(vkId) != null;
    }

    public User update(User user) {
        if (userExists(user.getVkId())) {
            return userRepository.save(user);
        }
        user = User.builder()
                .vkId(user.getVkId())
                .lastName(user.getLastName())
                .firstName(user.getFirstName())
                .coins(user.getCoins())
                .isAdmin(user.getIsAdmin())
                .usedPromoCodes(user.getUsedPromoCodes())
                .build();
        return userRepository.save(user);
    }

    private boolean validateNumCoinsAndUserExist(User user, int coins) {
        return coins > 0 && user != null;
    }

    @Transactional
    public User setAdmin(User user, Boolean admin) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        user.setIsAdmin(admin);
        return userRepository.save(user);
    }

    public void sendMessageByListUser(List<User> userList, String message) {
        for (User user : userList) {
            vkApiService.sendMessage(user.getVkId(), message, new Keyboard());
        }
    }

    public void resetCoinsByListUser(List<User> users) {
        for (User user : users) {
            user.setCoins(0);
            userRepository.save(user);
        }
    }

    public User addCoins(String vkId, int coins) {
        User user = userRepository.findByVkId(vkId);
        if (!validateNumCoinsAndUserExist(user, coins)) {
            return null;
        }
        user.setCoins(user.getCoins() + coins);
        return userRepository.save(user);
    }

    public User reduceCoins(String vkId, int coins) {
        User user = userRepository.findByVkId(vkId);
        if (!validateNumCoinsAndUserExist(user, coins)) {
            return null;
        }
        user.setCoins(user.getCoins() - coins);
        return userRepository.save(user);
    }

    public String getRating(String vkId) {
        StringBuilder message = new StringBuilder("Топ пользователей по энергии ⚡:\n");
        List<RatingLineResponse> top = new ArrayList<>();

        List<Map<String, Object>> topUsers = getTopUsers();
        Map<String, Object> currentUserRank = getUserRank(vkId);

        if (currentUserRank.get("vk_id") == null) {
            throw new UserNotFoundException("Not found user");
        }

        for (Map<String, Object> user : topUsers) {
            Long rank = (Long) user.get("rank");
            String name = user.get("first_name") + " " + user.get("last_name");
            int coins = (int) user.get("coins");
            RatingLineResponse line = new RatingLineResponse(rank, name, coins);
            top.add(line);
        }

        Long currentUserPosition = (Long) currentUserRank.get("rank");
        String currentUserFirstName = (String) currentUserRank.get("first_name");
        String currentUserLastName = (String) currentUserRank.get("last_name");
        int currentUserCoins = (int) currentUserRank.get("coins");

        RatingLineResponse currentUserLine = new RatingLineResponse(currentUserPosition, "Вы", currentUserCoins);

        if (currentUserPosition <= topUsers.size()) {
            for (int i = 0; i < topUsers.size(); i++) {
                if (Objects.equals(top.get(i).getRank(), currentUserPosition) &&
                    top.get(i).getCoins() == currentUserCoins &&
                    top.get(i).getFirstAndLastName().equals(currentUserFirstName + ' ' + currentUserLastName)) {

                    top.set(i, currentUserLine);
                    break;
                }
            }
        }
        else {
            top.add(currentUserLine);
        }

        int maxSize = top.size() < 10 ? top.size() : 9;
        RatingLineResponse line = null;
        if (top.isEmpty()) {
            message.append("Список пуст");
            return message.toString();
        }

        for (int i = 0; i < top.size() && i < 9; i++) {
            line = top.get(i);
            message.append(line.getRank()).append(". ").append(line.getFirstAndLastName()).append(" - ").append(line.getCoins()).append("\n");
        }

        if (top.size() == 11) {
            top.remove(9);
            message.append("...\n");
        }

        if (top.size() == 10) {
            line = top.get(9);
            message.append(line.getRank()).append(". ").append(line.getFirstAndLastName()).append(" - ").append(line.getCoins()).append("\n");

        }

        return message.toString();
    }

    private List<Map<String, Object>> getTopUsers() {
        String sql = "WITH ranked_users AS ("
                + "SELECT "
                + "ROW_NUMBER() OVER (ORDER BY coins DESC) AS rank, "
                + "first_name, last_name, coins "
                + "FROM user_entity) "
                + "SELECT rank, first_name, last_name, coins "
                + "FROM ranked_users "
                + "ORDER BY rank "
                + "LIMIT 10;";

        return jdbcTemplate.queryForList(sql);
    }

    private Map<String, Object> getUserRank(String vkId) {
        String sql = "WITH ranked_users AS ("
                + "SELECT "
                + "ROW_NUMBER() OVER (ORDER BY coins DESC) AS rank, "
                + "vk_id, first_name, last_name, coins "
                + "FROM user_entity) "
                + "SELECT * "
                + "FROM ranked_users "
                + "WHERE vk_id = ?;";

        return jdbcTemplate.queryForMap(sql, vkId);
    }

    public User getUserByVkId(String vkId) {
        return userRepository.findByVkId(vkId);
    }

    public void resetAdmins() {
        List<User> users = userRepository.findByIsAdmin(true);
        for (User user : users) {
            user.setIsAdmin(false);
            userRepository.save(user);
        }
    }
}
