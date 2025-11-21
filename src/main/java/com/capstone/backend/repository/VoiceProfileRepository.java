package com.capstone.backend.repository;

import com.capstone.backend.entity.VoiceProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VoiceProfileRepository extends JpaRepository<VoiceProfile, Long> {

    // 1. 기본적인 save(), findById(), delete() 등은 JpaRepository가 이미 제공하므로
    //    별도로 작성하지 않아도 Controller에서 바로 사용할 수 있습니다.

    // 2. [추가 기능] 특정 사용자가 등록한 모든 목소리 프로필 목록 조회
    //    나중에 앱 화면에서 "내 목소리 목록"을 보여줄 때 유용하게 쓰입니다.
    //    SELECT * FROM voice_profile WHERE user_id = ? 와 같은 쿼리가 자동 생성됩니다.
    List<VoiceProfile> findAllByUserId(Long userId);
}