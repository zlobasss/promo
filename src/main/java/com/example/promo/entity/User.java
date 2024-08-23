package com.example.promo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "user_entity")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String vkId;
    private String firstName;
    private String lastName;
    @Column(nullable = false)
    private Integer coins;
    private Boolean isAdmin;
    @ManyToMany
    private List<PromoCode> usedPromoCodes;
}
