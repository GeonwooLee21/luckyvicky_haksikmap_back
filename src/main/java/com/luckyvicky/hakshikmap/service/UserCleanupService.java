package com.luckyvicky.hakshikmap.service;

import com.luckyvicky.hakshikmap.repository.UsersRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCleanupService {

    private final UsersRepository usersRepository;

    /**
     * 매일 0시(한국시간 기준)에 모든 유저 삭제
     */
    @Transactional
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void deleteAllUsersAtMidnight() {
        long count = usersRepository.count();
        usersRepository.deleteAll();
        log.info("매일 0시 유저 초기화: {}명 삭제 완료", count);
    }
}