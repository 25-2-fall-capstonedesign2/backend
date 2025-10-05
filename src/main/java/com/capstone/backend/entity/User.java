package com.capstone.backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity // 이 클래스가 데이터베이스 테이블과 매핑되는 JPA 엔티티임을 나타냅니다.
@Table(name = "user") // 실제 매핑될 테이블 이름을 지정합니다. (클래스 이름과 같다면 생략 가능)
@Getter // Lombok: 모든 필드의 Getter 메소드를 자동으로 생성합니다.
@NoArgsConstructor(access = AccessLevel.PROTECTED) // Lombok: 파라미터 없는 기본 생성자를 생성합니다. PROTECTED로 설정하여 무분별한 객체 생성을 막습니다.
public class User {

    @Id // 이 필드가 테이블의 Primary Key (기본 키)임을 나타냅니다.
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 기본 키 값을 데이터베이스가 자동으로 생성(AUTO_INCREMENT)하도록 설정합니다.
    @Column(name = "user_id") // 실제 테이블의 컬럼 이름을 지정합니다.
    private Long id;

    @Column(name = "phone_number", nullable = false, unique = true, length = 20) // nullable=false: NOT NULL 제약조건, unique=true: UNIQUE 제약조건
    private String phoneNumber;

    @Column(nullable = false)
    private String password;

    @Column(name = "display_name", nullable = false, length = 50)
    private String displayName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // '하나'의 User가 '여러개'의 CallSession을 가질 수 있음을 나타냅니다. (1:N 관계)
    // mappedBy = "user": CallSession 엔티티의 'user' 필드에 의해 매핑되었음을 의미합니다. (연관관계의 주인 설정)
    // cascade = CascadeType.ALL: User가 삭제/수정되면 연관된 CallSession도 함께 삭제/수정됩니다.
    // orphanRemoval = true: User와 연관관계가 끊어진 CallSession은 자동으로 삭제됩니다.
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CallSession> callSessions = new ArrayList<>();

    @PrePersist // JPA 엔티티가 데이터베이스에 처음 저장되기 전에 실행되는 메소드입니다.
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}