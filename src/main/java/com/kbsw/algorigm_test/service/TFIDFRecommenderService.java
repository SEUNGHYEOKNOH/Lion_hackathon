package com.kbsw.algorigm_test.service;

import com.kbsw.algorigm_test.Repository.CareerItemRepository;
import com.kbsw.algorigm_test.Repository.UserProfileRepository;
import com.kbsw.algorigm_test.entity.CareerItem;
import com.kbsw.algorigm_test.entity.Tag;
import com.kbsw.algorigm_test.entity.UserProfile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class TFIDFRecommenderService {
    private final CareerItemRepository itemRepo;
    private final UserProfileRepository userRepo;
    private final RestTemplate restTemplate;

    @Value("${llama.api.url}")
    private String llamaApiUrl;

    @Value("${llama.api.key}")
    private String llamaApiKey;

    public TFIDFRecommenderService(CareerItemRepository itemRepo, UserProfileRepository userRepo) {
        this.itemRepo = itemRepo;
        this.userRepo = userRepo;
        this.restTemplate = new RestTemplate();
    }

    public List<CareerItem> recommendByUserProfile(Long userId) {
        UserProfile user = userRepo.findById(userId).orElseThrow();
        System.out.println("🔍 조회된 userId: " + userId);
        List<String> parsedTags = translateCareerToEnglishTags(user.getCareer());
        System.out.println("📌 진로 정보: " + user.getCareer());
        return recommendByKeywords(parsedTags);
    }

    public List<String> translateCareerToEnglishTags(String koreanText) {
        System.out.println("🔍 [1] 진로 입력값: " + koreanText);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + llamaApiKey);

        String prompt = "다음 진로 정보를 영어 키워드로 3~5개 추출해줘. 각 키워드는 소문자로 출력해. 예시 형식: [\"backend\", \"developer\"] 진로: " + koreanText;
        System.out.println("📝 [2] 생성된 프롬프트: " + prompt);

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", "openrouter/cypher-alpha:free");
        payload.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        payload.put("max_tokens", 100);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
        System.out.println("📤 [3] LLM 요청 전송 준비 완료");

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(llamaApiUrl, request, String.class);

            System.out.println("📬 [4] 응답 수신");
            System.out.println("🔍 응답 상태: " + response.getStatusCode());
            System.out.println("🔍 응답 본문: " + response.getBody());

            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(response.getBody());

            String content = json.path("choices").get(0).path("message").path("content").asText();
            System.out.println("✅ [5] 파싱된 키워드 리스트 문자열: " + content);

            List<String> result = mapper.readValue(content, List.class);
            System.out.println("🎯 [6] 최종 변환 결과 리스트: " + result);

            return result;

        } catch (Exception e) {
            System.out.println("❌ [에러] LLM 응답 처리 실패: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }

    public List<CareerItem> recommendByKeywords(List<String> inputKeywords) {
        System.out.println("👉 입력 키워드: " + inputKeywords);

        // 🔹 입력 키워드 전처리 (소문자 + 공백 제거)
        List<String> cleanedKeywords = inputKeywords.stream()
                .map(k -> k.trim().toLowerCase())
                .collect(Collectors.toList());

        List<CareerItem> items = itemRepo.findAllWithTags();
        System.out.println("📦 CareerItem 수: " + items.size());

        // 🔹 태그 전처리
        Set<String> allTags = items.stream()
                .flatMap(i -> i.getTags().stream())
                .map(tag -> tag.getTagName().trim().toLowerCase())
                .collect(Collectors.toSet());

        System.out.println("🧾 전체 태그: " + allTags);
        System.out.println("🔍 유저 입력 키워드: " + cleanedKeywords);

        Map<String, Integer> tagIndexMap = new HashMap<>();
        int idx = 0;
        for (String tag : allTags) tagIndexMap.put(tag, idx++);

        int tagSize = tagIndexMap.size();
        Map<Long, double[]> itemVectors = new HashMap<>();
        Map<String, Integer> df = new HashMap<>();

        for (CareerItem item : items) {
            double[] tf = new double[tagSize];
            Set<String> counted = new HashSet<>();
            for (Tag tag : item.getTags()) {
                String t = tag.getTagName().trim().toLowerCase();  // 🔹 전처리
                tf[tagIndexMap.get(t)] += 1.0;
                if (counted.add(t)) df.put(t, df.getOrDefault(t, 0) + 1);
            }
            itemVectors.put(item.getId(), tf);
        }

        // 🔹 TF-IDF 계산
        int docCount = items.size();
        for (Map.Entry<Long, double[]> entry : itemVectors.entrySet()) {
            double[] tfidf = entry.getValue();
            for (int i = 0; i < tfidf.length; i++) {
                String tag = null;
                for (Map.Entry<String, Integer> mapEntry : tagIndexMap.entrySet()) {
                    if (mapEntry.getValue() == i) {
                        tag = mapEntry.getKey();
                        break;
                    }
                }
                double idf = Math.log((double) docCount / (1 + df.getOrDefault(tag, 1)));
                tfidf[i] *= idf;
            }
            double norm = Math.sqrt(Arrays.stream(tfidf).map(x -> x * x).sum());
            for (int i = 0; i < tfidf.length; i++) tfidf[i] /= norm;
        }

        // 🔹 유저 벡터 생성
        double[] userVector = new double[tagSize];
        for (String keyword : cleanedKeywords) {
            if (tagIndexMap.containsKey(keyword)) {
                int index = tagIndexMap.get(keyword);
                userVector[index] = 1.0;
            }
        }
        double norm = Math.sqrt(Arrays.stream(userVector).map(x -> x * x).sum());
        for (int i = 0; i < userVector.length; i++) userVector[i] /= norm;

        // 🔹 코사인 유사도 정렬
        return items.stream()
                .sorted((a, b) -> {
                    double[] va = itemVectors.get(a.getId());
                    double[] vb = itemVectors.get(b.getId());
                    return Double.compare(cosineSim(vb, userVector), cosineSim(va, userVector));
                })
                .limit(5)
                .collect(Collectors.toList());
    }

    private double cosineSim(double[] v1, double[] v2) {
        double dot = 0;
        for (int i = 0; i < v1.length; i++) dot += v1[i] * v2[i];
        return dot;
    }
}
