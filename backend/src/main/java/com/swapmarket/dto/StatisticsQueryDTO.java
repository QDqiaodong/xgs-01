package com.swapmarket.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class StatisticsQueryDTO {

    private String timeRange;

    private LocalDate startDate;

    private LocalDate endDate;
}
