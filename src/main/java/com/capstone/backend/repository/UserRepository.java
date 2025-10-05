package com.capstone.backend.repository;

import com.capstone.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

// JpaRepository<관리할 엔티티, 엔티티의 PK 타입> 를 상속받습니다.
public interface UserRepository extends JpaRepository<User, Long> {

    // 'findBy' + '필드이름' 형식으로 메소드 이름을 지으면,
    // Spring Data JPA가 메소드 이름을 분석해서 자동으로 해당 필드로 데이터를 조회하는 쿼리를 생성해줍니다.
    // Optional<User>는 User가 존재할 수도, 존재하지 않을 수도 있다는 것을 명확하게 표현해줍니다.
    Optional<User> findByPhoneNumber(String phoneNumber);
}
