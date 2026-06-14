package com.swapmarket.vo;

import lombok.Data;

import java.util.List;

@Data
public class DashboardStatisticsVO {

    private Long totalUsers;

    private Long totalItems;

    private Long pendingOffers;

    private Long newItemsThisMonth;

    private Double offerSuccessRate;

    private List<CategoryRatioVO> categoryRatios;

    private String timeRange;

    private String startDate;

    private String endDate;
}
