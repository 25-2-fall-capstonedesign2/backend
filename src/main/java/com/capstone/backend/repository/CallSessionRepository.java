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

}
