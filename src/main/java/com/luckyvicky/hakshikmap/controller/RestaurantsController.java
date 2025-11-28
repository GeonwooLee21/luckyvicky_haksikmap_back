package com.luckyvicky.hakshikmap.controller;

import com.luckyvicky.hakshikmap.dto.RestaurantCongestionRes;
import com.luckyvicky.hakshikmap.dto.RestaurantWaitTimeRes;
import com.luckyvicky.hakshikmap.repository.RestaurantsRepository;
import com.luckyvicky.hakshikmap.service.RestaurantService;
import com.luckyvicky.hakshikmap.service.WaitTimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor // 생성자 자동으로 만들어 줌
@CrossOrigin(origins = {"http://localhost:3000",
                        "http://172.20.87.64:3000"})
@RequestMapping("/api/restaurant")
public class RestaurantsController {
    private final RestaurantsRepository restaurantsRepository;
    private final RestaurantService restaurantService;
    private final WaitTimeService waitTimeService;

    @GetMapping
    public List<RestaurantCongestionRes> getAllCongestions() {
        return restaurantService.getAllCongestions();
    }

    @GetMapping("/{restaurantid}")
    public RestaurantCongestionRes getCongestion(
            @PathVariable Long restaurantid
    ) {
       return restaurantService.getCongestion(restaurantid);
    }
    @GetMapping("/{restaurant-id}/wait-time")
    public ResponseEntity<RestaurantWaitTimeRes> getWaitTime(
            @PathVariable("restaurant-id") Long restaurantId
    ) {
        int waitTimeMin = waitTimeService.getWaitTime(restaurantId, LocalDateTime.now());

        RestaurantWaitTimeRes res = new RestaurantWaitTimeRes(restaurantId, waitTimeMin);
        return ResponseEntity.ok(res);
    }
}
