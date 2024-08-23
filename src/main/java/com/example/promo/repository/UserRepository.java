package com.example.promo.repository;

import com.example.promo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByVkId(String vkId);
    List<User> findByIsAdmin(boolean admin);
}
