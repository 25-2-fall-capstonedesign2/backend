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
    @Column(name = "voice_data", nullable = false, columnDefinition = "LONGBLOB")
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