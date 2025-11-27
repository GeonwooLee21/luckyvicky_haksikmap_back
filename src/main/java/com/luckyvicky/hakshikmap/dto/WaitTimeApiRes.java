package com.luckyvicky.hakshikmap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class WaitTimeApiRes {

    private Long restaurantId;   // 파이썬 JSON의 "restaurantId"
    private String time;         // "time"
    private Integer waitTimeMin; // "waitTimeMin"
    private String status;       // "status" (success / fail 등)
    private int voteCount;
}
