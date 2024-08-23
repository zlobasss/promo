package com.example.promo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PromoCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String code;
    @Column(nullable = false)
    private int numUse;
    @Column(nullable = false)
    private int coinsReward;
    @Column(nullable = false)
    private Date expiredDate;
    @ManyToMany
    private List<User> usersActivated;
}
