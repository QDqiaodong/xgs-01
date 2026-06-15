package com.swapmarket.controller;

import com.swapmarket.common.Result;
import com.swapmarket.entity.SwapOffer;
import com.swapmarket.service.SwapOfferService;
import com.swapmarket.vo.OfferTimelineNodeVO;
import com.swapmarket.vo.SwapComparisonVO;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @GetMapping("/{id}")
    public Result<SwapOffer> getOfferDetail(@RequestParam(defaultValue = "1") Long userId, @PathVariable Long id) {
        return Result.success(swapOfferService.getOfferDetail(userId, id));
    }

    @GetMapping("/pending-count")
    public Result<Integer> getPendingOfferCount(@RequestParam(defaultValue = "1") Long userId) {
        return Result.success(swapOfferService.getPendingOfferCount(userId));
    }

    @GetMapping("/{id}/comparison")
    public Result<SwapComparisonVO> getComparisonData(@RequestParam(defaultValue = "1") Long userId,
                                                      @PathVariable Long id) {
        return Result.success(swapOfferService.getComparisonData(userId, id));
    }

    @GetMapping("/{id}/comparison/export")
    public Result<Map<String, Object>> exportComparisonSummary(@RequestParam(defaultValue = "1") Long userId,
                                                               @PathVariable Long id) {
        SwapComparisonVO comparison = swapOfferService.getComparisonData(userId, id);
        Map<String, Object> summary = new HashMap<>();
        summary.put("offerId", comparison.getOfferId());
        summary.put("offerStatus", comparison.getOfferStatus());
        summary.put("createTime", comparison.getCreateTime());
        summary.put("fromUser", comparison.getFromUserNickname());
        summary.put("toUser", comparison.getToUserNickname());
        summary.put("fromItemTitle", comparison.getFromItem() != null ? comparison.getFromItem().getTitle() : "");
        summary.put("toItemTitle", comparison.getToItem() != null ? comparison.getToItem().getTitle() : "");
        summary.put("differentCount", comparison.getDifferentCount());
        summary.put("totalCount", comparison.getTotalCount());
        summary.put("offerMessage", comparison.getOfferMessage());

        List<Map<String, Object>> checklistSummary = comparison.getChecklist().stream()
                .map(item -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("fieldLabel", item.getFieldLabel());
                    map.put("isDifferent", item.getIsDifferent());
                    map.put("fromValue", item.getIsImageField() ? "[图片]" : item.getFromValue());
                    map.put("toValue", item.getIsImageField() ? "[图片]" : item.getToValue());
                    return map;
                })
                .toList();
        summary.put("checklist", checklistSummary);

        return Result.success(summary);
    }

    @PostMapping("/handover/{id}")
    public Result<Void> startHandover(@RequestParam(defaultValue = "1") Long userId, @PathVariable Long id) {
        swapOfferService.startHandover(userId, id);
        return Result.success();
    }

    @PostMapping("/complete/{id}")
    public Result<Void> completeOffer(@RequestParam(defaultValue = "1") Long userId, @PathVariable Long id) {
        swapOfferService.completeOffer(userId, id);
        return Result.success();
    }

    @GetMapping("/{id}/timeline")
    public Result<List<OfferTimelineNodeVO>> getOfferTimeline(@RequestParam(defaultValue = "1") Long userId,
                                                              @PathVariable Long id) {
        return Result.success(swapOfferService.getOfferTimeline(userId, id));
    }

    @Data
    public static class CreateOfferRequest {
        private Long fromUserId = 1L;
        private Long fromItemId;
        private Long toItemId;
        private String message;
    }
}
