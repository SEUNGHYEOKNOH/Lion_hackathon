package com.kbsw.algorigm_test.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@Entity
public class UserProfile {
    @Id
    @GeneratedValue
    private Long id;
    private String name;
    private String career;  // ex: "AI backend engineer"

    // getter, setter, constructor 생략
}