package com.swapmarket.controller;

import com.swapmarket.common.Result;
import com.swapmarket.entity.UserReview;
import com.swapmarket.service.UserReviewService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/review")
@RequiredArgsConstructor
public class ReviewController {
    private final UserReviewService userReviewService;

    @PostMapping("/create")
    public Result<UserReview> createReview(@RequestBody CreateReviewRequest request) {
        return Result.success(userReviewService.createReview(
                request.getUserId(),
                request.getOfferId(),
                request.getRating(),
                request.getContent()
        ));
    }

    @GetMapping("/user/{userId}")
    public Result<List<UserReview>> getUserReviews(@PathVariable Long userId) {
        return Result.success(userReviewService.getUserReviews(userId));
    }

    @GetMapping("/user/{userId}/recent")
    public Result<List<UserReview>> getRecentReviews(@PathVariable Long userId,
                                                     @RequestParam(defaultValue = "5") int limit) {
        return Result.success(userReviewService.getRecentReviews(userId, limit));
    }

    @GetMapping("/credit/{userId}")
    public Result<Map<String, Object>> getUserCreditInfo(@PathVariable Long userId) {
        return Result.success(userReviewService.getUserCreditInfo(userId));
    }

    @GetMapping("/offer/{offerId}/status")
    public Result<Map<String, Object>> getOfferReviewStatus(@PathVariable Long offerId,
                                                             @RequestParam(defaultValue = "1") Long userId) {
        return Result.success(userReviewService.getOfferReviewStatus(offerId, userId));
    }

    @Data
    public static class CreateReviewRequest {
        private Long userId = 1L;
        private Long offerId;
        private Integer rating;
        private String content;
    }
}
