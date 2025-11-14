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
@Table(name = "call_session")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CallSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "call_session_id")
    private Long id;

    // 'CallSession'이 'User' 하나와 관계를 맺습니다 (Many-to-One)
    // FetchType.LAZY는 성능 최적화를 위해 이 엔티티를 조회할 때 User 객체를 바로 로딩하지 않도록 합니다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false) // FK 컬럼 지정
    private User user;

    @Column(name = "participant_name", nullable = false, length = 100)
    private String participantName;

    @CreationTimestamp
    @Column(name = "start_time", nullable = false, updatable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    // CallSession이 여러 개의 Message를 가집니다 (One-to-Many)
    @OneToMany(mappedBy = "callSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Message> messages = new ArrayList<>();

    @Builder
    public CallSession(User user, String participantName) {
        this.user = user;
        this.participantName = participantName;
    }

    // (선택) 통화 종료 시 시간을 업데이트하는 편의 메서드
    public void endCall() {
        this.endTime = LocalDateTime.now();
    }
}