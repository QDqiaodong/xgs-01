package com.swapmarket.controller;

import com.swapmarket.common.Result;
import com.swapmarket.entity.SwapOffer;
import com.swapmarket.service.SwapOfferService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/offer")
@RequiredArgsConstructor
public class OfferController {
    private final SwapOfferService swapOfferService;

    @PostMapping("/create")
    public Result<SwapOffer> createOffer(@RequestBody CreateOfferRequest request) {
        return Result.success(swapOfferService.createOffer(
                request.getFromUserId(),
                request.getFromItemId(),
                request.getToItemId(),
                request.getMessage()
        ));
    }

    @GetMapping("/list")
    public Result<List<SwapOffer>> listOffers(@RequestParam(defaultValue = "1") Long userId,
                                              @RequestParam(required = false) String type) {
        if ("sent".equals(type)) {
            return Result.success(swapOfferService.getSentOffers(userId));
        }
        return Result.success(swapOfferService.getReceivedOffers(userId));
    }

    @GetMapping("/received")
    public Result<List<SwapOffer>> getReceivedOffers(@RequestParam(defaultValue = "1") Long userId) {
        return Result.success(swapOfferService.getReceivedOffers(userId));
    }

    @GetMapping("/sent")
    public Result<List<SwapOffer>> getSentOffers(@RequestParam(defaultValue = "1") Long userId) {
        return Result.success(swapOfferService.getSentOffers(userId));
    }

    @PostMapping("/accept/{id}")
    public Result<Void> acceptOffer(@RequestParam(defaultValue = "1") Long userId, @PathVariable Long id) {
        swapOfferService.acceptOffer(userId, id);
        return Result.success();
    }

    @PostMapping("/reject/{id}")
    public Result<Void> rejectOffer(@RequestParam(defaultValue = "1") Long userId, @PathVariable Long id) {
        swapOfferService.rejectOffer(userId, id);
        return Result.success();
    }

    @Data
    public static class CreateOfferRequest {
        private Long fromUserId = 1L;
        private Long fromItemId;
        private Long toItemId;
        private String message;
    }
}
