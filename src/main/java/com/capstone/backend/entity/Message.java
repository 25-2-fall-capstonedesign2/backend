package com.capstone.backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "message")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long id;

    // 'Message'가 'CallSession' 하나와 관계를 맺습니다 (Many-to-One)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "call_session_id", nullable = false)
    private CallSession callSession;

    // 위에서 만든 Enum을 사용합니다.
    // EnumType.STRING은 DB에 "USER", "AI" 문자열로 저장하게 합니다.
    @Enumerated(EnumType.STRING)
    @Column(name = "sender", nullable = false, length = 10)
    private Sender sender;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @Builder
    public Message(CallSession callSession, Sender sender, String content) {
        this.callSession = callSession;
        this.sender = sender;
        this.content = content;
    }
}
