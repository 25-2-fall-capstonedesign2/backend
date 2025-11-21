package com.capstone.backend.repository;

import com.capstone.backend.entity.CallSession;
import com.capstone.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CallSessionRepository extends JpaRepository<CallSession, Long>{
    // 1. 특정 사용자의 통화 세션 중, 특정 프로필 이름(예: "엄마")을 가진 세션들을 찾음
    List<CallSession> findAllByUserAndVoiceProfile_ProfileName(User user, String profileName);

    // 2. [추가] 특정 사용자가 통화한 모든 '상대방 이름(프로필 이름)'을 중복 없이 조회
    //    단순 JPA 메서드 이름으로는 길어지므로 @Query를 사용하여 직관적으로 작성합니다.
    @Query("SELECT DISTINCT c.voiceProfile.profileName FROM CallSession c WHERE c.user = :user")
    List<String> findDistinctProfileNamesByUser(@Param("user") User user);
}
