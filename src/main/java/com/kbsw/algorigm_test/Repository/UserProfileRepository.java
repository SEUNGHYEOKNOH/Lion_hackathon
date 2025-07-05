package com.kbsw.algorigm_test.Repository;

import com.kbsw.algorigm_test.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    // 필요하다면 추가 메서드 정의 가능
}