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
    List<CallSession> findAllByUserId(Long userId);

    // [추가] 사용자를 기반으로 중복 제거된 participantName 목록 조회
    @Query("SELECT DISTINCT c.participantName FROM CallSession c WHERE c.user = :user")
    List<String> findDistinctParticipantNameByUser(@Param("user") User user);

    // [추가] 사용자와 participantName으로 모든 CallSession 조회
    List<CallSession> findAllByUserAndParticipantName(User user, String participantName);
}
