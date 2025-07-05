package com.kbsw.algorigm_test.Repository;

import com.kbsw.algorigm_test.entity.CareerItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CareerItemRepository extends JpaRepository<CareerItem, Long> {
    @Query("SELECT DISTINCT c FROM CareerItem c JOIN FETCH c.tags")
    List<CareerItem> findAllWithTags();
}