package com.swapmarket.controller;

import com.swapmarket.common.Result;
import com.swapmarket.dto.StatisticsQueryDTO;
import com.swapmarket.service.StatisticsService;
import com.swapmarket.vo.DashboardStatisticsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/dashboard")
    public Result<DashboardStatisticsVO> getDashboardStatistics(
            @RequestParam(required = false) String timeRange,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        StatisticsQueryDTO queryDTO = new StatisticsQueryDTO();
        queryDTO.setTimeRange(timeRange);
        queryDTO.setStartDate(startDate);
        queryDTO.setEndDate(endDate);

        return Result.success(statisticsService.getDashboardStatistics(queryDTO));
    }

    @PostMapping("/cache/evict")
    public Result<Void> evictStatisticsCache() {
        statisticsService.evictStatisticsCache();
        return Result.success();
    }
}
