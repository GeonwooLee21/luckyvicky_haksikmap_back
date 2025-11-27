package com.luckyvicky.hakshikmap.service;

import com.luckyvicky.hakshikmap.entity.Restaurants;
import com.luckyvicky.hakshikmap.entity.Votes;
import com.luckyvicky.hakshikmap.repository.RestaurantsRepository;
import com.luckyvicky.hakshikmap.repository.VotesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CongestionService {

    private final RestaurantsRepository restaurantsRepository;
    private final VotesRepository votesRepository;



     //1분마다 실행.
     //cron = "0 * * * * *"  → 1분마다 실행 (수정 가능)
    @Transactional
    @Scheduled(cron = "0/5 * * * * *")
    public void updateCurrentCongestions() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = now.minusMinutes(10); // 예: 최근 10분간의 투표만 반영

        List<Restaurants> restaurants = restaurantsRepository.findAll();

        for (Restaurants restaurant : restaurants) {

            List<Votes> votes = votesRepository.findByRestaurantIdAndVotedTimeBetween(
                    restaurant.getId(),
                    from,
                    now
            );


            if (votes.isEmpty()) {
                // 최근 30분간 투표가 없다면, 일단 0으로 두거나,
                // 그대로 유지할지 정책을 정해서 작성
                //db에 있는 값 그대로 사용하기
                continue;
            }

            // 1) 최근 투표들의 weight 평균을 계산 (1~3 사이 값)
            double avgWeight = votes.stream()
                    .mapToInt(Votes::getWeight)
                    .average()
                    .orElse(0.0);

            // 2) 1~3 사이 avgWeight를 0~100 스코어로 변환하는 예시
            //    weight=1 → 약 0점, 2 → 50점대, 3 → 100점 근처
            int score = (int) Math.round((avgWeight - 1) / 2 * 100);
            if (score < 0) score = 0;
            if (score > 100) score = 100;

            restaurant.setCurrentCongestion(score);
        }

        // @Transactional 이라 따로 save() 안 해도 dirty checking 으로 업데이트됨
        log.info("currentCongestion 업데이트 완료: {}", now);
    }
}