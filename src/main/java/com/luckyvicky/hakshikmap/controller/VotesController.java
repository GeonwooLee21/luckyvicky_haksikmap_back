package com.luckyvicky.hakshikmap.controller;

import com.luckyvicky.hakshikmap.dto.VoteReq;
import com.luckyvicky.hakshikmap.service.VoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000",
                        "http://172.20.87.64:3000"})
@RequestMapping("/api/vote")
public class VotesController {

    private final VoteService votesService;

    @PostMapping
    public void vote(@RequestBody VoteReq voteReq) {
        // 이 창에 들어왔다는 것은 이미 유저가 존재한다고 가정
        votesService.vote(voteReq);
        // 반환값 없이 200 OK만 리턴
    }
}
