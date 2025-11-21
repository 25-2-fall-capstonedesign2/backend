package com.capstone.backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users") // 실제 DB 테이블 이름 지정
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA는 기본 생성자가 필요합니다.
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "phone_number", nullable = false, unique = true, length = 20)
    private String phoneNumber; // 명세서의 'phone' (숫자-only) [cite: 120]

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "display_name", nullable = false, length = 50)
    private String displayName;

    @CreationTimestamp // 엔티티가 생성될 때 자동으로 시간이 입력됩니다.
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 'User'가 'CallSession'을 여러 개 가질 수 있습니다 (One-to-Many)
    // 'mappedBy = "user"'는 CallSession 엔티티의 'user' 필드에 의해 매핑된다는 의미입니다.
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CallSession> callSessions = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VoiceProfile> voiceProfiles = new ArrayList<>();

    @Builder
    public User(String phoneNumber, String password, String displayName) {
        this.phoneNumber = phoneNumber;
        this.password = password;
        this.displayName = displayName;
    }
}
