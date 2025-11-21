package com.capstone.backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "voice_profile")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VoiceProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "voice_profile_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "profile_name", nullable = false, length = 50)
    private String profileName;

    // [핵심] GPU가 가져갈 실제 이진 데이터 (최대 4GB 지원)
    @Lob
    // columnDefinition을 제거하고 length를 크게 잡으세요.
    // Hibernate가 MySQL에서는 LONGBLOB으로, H2에서는 그에 맞는 타입으로 알아서 생성합니다.
    @Column(name = "voice_data", nullable = false, length = 100000000)
    private byte[] voiceData;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public VoiceProfile(User user, String profileName, byte[] voiceData) {
        this.user = user;
        this.profileName = profileName;
        this.voiceData = voiceData;
    }
}