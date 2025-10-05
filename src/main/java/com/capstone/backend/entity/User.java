package com.capstone.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String phoneNumber; // 명세서의 'phone' (숫자-only) [cite: 120]

    @Column(nullable = false)
    private String password; // 명세서의 'passwordHash' (bcrypt로 해싱됨) [cite: 121]

    private String displayName; // 명세서의 'displayName' [cite: 122]
}
