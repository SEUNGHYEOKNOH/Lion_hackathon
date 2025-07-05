package com.kbsw.algorigm_test.controller;

import com.kbsw.algorigm_test.entity.CareerItem;
import com.kbsw.algorigm_test.service.TFIDFRecommenderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommend")
public class RecommendController {
    private final TFIDFRecommenderService recommenderService;

    public RecommendController(TFIDFRecommenderService recommenderService) {
        this.recommenderService = recommenderService;
    }

    // 키워드 기반 추천
    @PostMapping
    public List<CareerItem> recommend(@RequestBody List<String> keywords) {
        System.out.println("🔥 POST /api/recommend 도달");
        return recommenderService.recommendByKeywords(keywords);
    }

    // 유저 진로 기반 추천
    @GetMapping("/user/{userId}")
    public List<CareerItem> recommendFromUserProfile(@PathVariable Long userId) {
        return recommenderService.recommendByUserProfile(userId);
    }

    @PostMapping("/test-llm")
    public void testLlm(@RequestBody String career) {
        List<String> result = recommenderService.translateCareerToEnglishTags(career);
        System.out.println("✅ 변환 결과: " + result);
    }

}