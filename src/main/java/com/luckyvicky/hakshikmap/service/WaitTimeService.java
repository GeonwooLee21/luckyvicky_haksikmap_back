package com.luckyvicky.hakshikmap.service;

import com.luckyvicky.hakshikmap.dto.WaitTimeApiRes;
import com.luckyvicky.hakshikmap.entity.Votes;
import com.luckyvicky.hakshikmap.repository.VotesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WaitTimeService {

    private final RestTemplate restTemplate;
    private final VotesRepository votesRepository;

    @Value("${python.wait-time-url}")
    private String waitTimeUrl;   // 예: http://43.203.175.98:5000/api/wait-time

    // ✅ 최근 10분만 사용하는 윈도우 (분 단위)
    private static final int WINDOW_MINUTES = 10;

    /**
     * 특정 식당에 대해, 현재 시각 기준
     * 최근 10분 내 votes 데이터를 모아서 파이썬 서버에 보내고,
     * 예측 대기시간(분)을 받아온다.
     *
     * @param restaurantId 식당 ID
     * @param now          기준 시각 (보통 LocalDateTime.now())
     * @return 대기시간(분). 데이터 없음/에러 시 0 (정책에 따라 변경 가능)
     */
    public int getWaitTime(Long restaurantId, LocalDateTime now) {

        LocalDateTime from = now.minusMinutes(WINDOW_MINUTES);

        // 1) votes DB에서 최근 10분 데이터 조회
        List<Votes> votes = votesRepository.findByRestaurantIdAndVotedTimeBetween(
                restaurantId, from, now
        );

        // 2) 파이썬으로 보낼 JSON 바디 만들기
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("restaurantId", restaurantId);
        requestBody.put("baseTime", now.toString());         // ISO-8601 문자열 (예: 2025-11-28T12:30:00)
        requestBody.put("windowMinutes", WINDOW_MINUTES);

        List<Map<String, Object>> voteDtos = new ArrayList<>();
        for (Votes v : votes) {
            Map<String, Object> m = new HashMap<>();
            // LocalDateTime.toString() → "yyyy-MM-ddTHH:mm:ss" 형식
            m.put("votedTime", v.getVotedTime().toString());
            m.put("weight", v.getWeight());
            m.put("waitingTime", v.getWaitingTime());
            voteDtos.add(m);
        }
        requestBody.put("votes", voteDtos);

        try {
            // 3) 파이썬 서버 호출
            log.info("[WaitTime] 요청 → python: url={}, restaurantId={}, voteCount={}, window={}m",
                    waitTimeUrl, restaurantId, votes.size(), WINDOW_MINUTES);

            var response = restTemplate.postForEntity(
                    waitTimeUrl,
                    requestBody,
                    WaitTimeApiRes.class
            );

            WaitTimeApiRes body = response.getBody();

            if (body == null) {
                log.warn("[WaitTime] 응답 body null (restaurantId={})", restaurantId);
                return 0;
            }

            log.info("[WaitTime] 응답 ← python: restaurantId={}, status={}, waitTimeMin={}, voteCount={}",
                    body.getRestaurantId(),
                    body.getStatus(),
                    body.getWaitTimeMin(),
                    body.getVoteCount());

            // 4) 응답 상태에 따라 처리
            if (!"SUCCESS".equalsIgnoreCase(body.getStatus())) {
                // NO_DATA / ERROR 등 → 현재 정책은 0분
                return -1;
            }

            if (body.getWaitTimeMin() == null) {
                return -1;
            }

            int result = body.getWaitTimeMin();
            if (result < 0) result = 0; // 음수 방지

            return result;

        } catch (RestClientException e) {
            log.error("[WaitTime] 파이썬 서버 호출 실패 (restaurantId={})", restaurantId, e);
            // 실패 시 0으로 (혹은 -1 같은 특수값으로 바꿀 수도 있음)
            return 0;
        }
    }
}
