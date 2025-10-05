package com.capstone.backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
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

    // '여러개'의 CallSession이 '하나'의 User에 속함을 나타냅니다. (N:1 관계)
    // @JoinColumn: 외래 키(FK)를 매핑할 때 사용합니다. `user_id` 컬럼을 통해 User와 조인합니다.
    @ManyToOne(fetch = FetchType.LAZY) // LAZY: 실제로 User 객체를 사용할 때 DB에서 조회합니다. (성능 최적화)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @OneToMany(mappedBy = "callSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Message> messages = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        this.startTime = LocalDateTime.now();
    }
}