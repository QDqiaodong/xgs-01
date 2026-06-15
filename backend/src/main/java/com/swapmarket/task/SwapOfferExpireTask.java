package com.swapmarket.task;

import com.swapmarket.service.SwapOfferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SwapOfferExpireTask {

    private final SwapOfferService swapOfferService;

    @Scheduled(fixedRate = 60000)
    public void processExpiredOffers() {
        try {
            log.debug("开始检查超时互换邀约...");
            int count = swapOfferService.processExpiredOffers();
            if (count > 0) {
                log.info("本次自动失效 {} 条超时互换邀约", count);
            }
        } catch (Exception e) {
            log.error("处理超时互换邀约定时任务异常", e);
        }
    }
}
