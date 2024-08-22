package com.example.promo.service;

import com.example.promo.dto.RatingLineResponse;
import com.example.promo.dto.UserRequest;
import com.example.promo.entity.User;
import com.example.promo.exception.CoinLessZeroException;
import com.example.promo.exception.UserNotFoundException;
import com.example.promo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public User save(UserRequest request) {
        if (userRepository.findByVkId(request.getVkId()) != null) {
            return null;
        }
        User user = User.builder()
                .vkId(request.getVkId())
                .lastName(request.getLastName())
                .firstName(request.getFirstName())
                .coins(0)
                .build();
        return userRepository.save(user);
    }

    private void validateNumCoinsAndUserExist(User user, int coins) {
        if (coins <= 0) {
            throw new CoinLessZeroException("Coins must be greater than zero");
        }
        if (user == null) {
            throw new UserNotFoundException("Not found user");
        }
    }

    public User addCoins(String vkId, int coins) {
        User user = userRepository.findByVkId(vkId);
        validateNumCoinsAndUserExist(user, coins);
        user.setCoins(user.getCoins() + coins);
        return userRepository.save(user);
    }

    public User reduceCoins(String vkId, int coins) {
        User user = userRepository.findByVkId(vkId);
        validateNumCoinsAndUserExist(user, coins);
        user.setCoins(user.getCoins() - coins);
        return userRepository.save(user);
    }

    public String getRating(String vkId) {
        StringBuilder message = new StringBuilder("Топ пользователей по монетам:\n");
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

}
