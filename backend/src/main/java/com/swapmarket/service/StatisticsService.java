package com.swapmarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.swapmarket.common.CacheKeyConstants;
import com.swapmarket.dto.StatisticsQueryDTO;
import com.swapmarket.entity.Category;
import com.swapmarket.entity.Item;
import com.swapmarket.entity.SwapOffer;
import com.swapmarket.entity.User;
import com.swapmarket.mapper.CategoryMapper;
import com.swapmarket.mapper.ItemMapper;
import com.swapmarket.mapper.SwapOfferMapper;
import com.swapmarket.mapper.UserMapper;
import com.swapmarket.vo.CategoryRatioVO;
import com.swapmarket.vo.DashboardStatisticsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticsService {

    private final UserMapper userMapper;
    private final ItemMapper itemMapper;
    private final SwapOfferMapper swapOfferMapper;
    private final CategoryMapper categoryMapper;
    private final RedisCacheService redisCacheService;

    public DashboardStatisticsVO getDashboardStatistics(StatisticsQueryDTO queryDTO) {
        String cacheKey = buildCacheKey(queryDTO);
        DashboardStatisticsVO cached = redisCacheService.getObject(cacheKey, DashboardStatisticsVO.class);
        if (cached != null) {
            return cached;
        }

        LocalDate[] dateRange = resolveDateRange(queryDTO);
        LocalDate startDate = dateRange[0];
        LocalDate endDate = dateRange[1];
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        DashboardStatisticsVO vo = new DashboardStatisticsVO();

        vo.setTotalUsers(countTotalUsers());
        vo.setTotalItems(countTotalItems());
        vo.setPendingOffers(countPendingOffers());
        vo.setNewItemsThisMonth(countNewItemsThisMonth());
        vo.setOfferSuccessRate(calculateOfferSuccessRate(startDateTime, endDateTime));
        vo.setCategoryRatios(calculateCategoryRatios(startDateTime, endDateTime));

        vo.setTimeRange(queryDTO.getTimeRange() != null ? queryDTO.getTimeRange() : "custom");
        vo.setStartDate(startDate.toString());
        vo.setEndDate(endDate.toString());

        redisCacheService.set(cacheKey, vo,
                CacheKeyConstants.STATISTICS_DASHBOARD_TTL,
                CacheKeyConstants.STATISTICS_DASHBOARD_TTL_UNIT);

        return vo;
    }

    public void evictStatisticsCache() {
        redisCacheService.deleteByPattern(CacheKeyConstants.STATISTICS_DASHBOARD_KEY + "*");
    }

    private String buildCacheKey(StatisticsQueryDTO queryDTO) {
        StringBuilder sb = new StringBuilder(CacheKeyConstants.STATISTICS_DASHBOARD_KEY);
        if (queryDTO.getTimeRange() != null) {
            sb.append(queryDTO.getTimeRange());
        } else if (queryDTO.getStartDate() != null && queryDTO.getEndDate() != null) {
            sb.append("custom:")
                    .append(queryDTO.getStartDate().toString())
                    .append(":")
                    .append(queryDTO.getEndDate().toString());
        } else {
            sb.append("default");
        }
        return sb.toString();
    }

    private LocalDate[] resolveDateRange(StatisticsQueryDTO queryDTO) {
        LocalDate today = LocalDate.now();
        String timeRange = queryDTO.getTimeRange();

        if ("week".equalsIgnoreCase(timeRange)) {
            LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate sunday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
            return new LocalDate[]{monday, sunday};
        } else if ("month".equalsIgnoreCase(timeRange)) {
            LocalDate firstDay = today.with(TemporalAdjusters.firstDayOfMonth());
            LocalDate lastDay = today.with(TemporalAdjusters.lastDayOfMonth());
            return new LocalDate[]{firstDay, lastDay};
        } else if (queryDTO.getStartDate() != null && queryDTO.getEndDate() != null) {
            return new LocalDate[]{queryDTO.getStartDate(), queryDTO.getEndDate()};
        }
        return new LocalDate[]{today.minusMonths(1), today};
    }

    private Long countTotalUsers() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        return userMapper.selectCount(wrapper);
    }

    private Long countTotalItems() {
        LambdaQueryWrapper<Item> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Item::getStatus, "published");
        return itemMapper.selectCount(wrapper);
    }

    private Long countPendingOffers() {
        LambdaQueryWrapper<SwapOffer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SwapOffer::getStatus, "pending");
        return swapOfferMapper.selectCount(wrapper);
    }

    private Long countNewItemsThisMonth() {
        LocalDate today = LocalDate.now();
        LocalDateTime firstDayOfMonth = today.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
        LocalDateTime lastDayOfMonth = today.with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX);
        LambdaQueryWrapper<Item> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(Item::getCreateTime, firstDayOfMonth, lastDayOfMonth);
        return itemMapper.selectCount(wrapper);
    }

    private Double calculateOfferSuccessRate(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        LambdaQueryWrapper<SwapOffer> totalWrapper = new LambdaQueryWrapper<>();
        totalWrapper.between(SwapOffer::getCreateTime, startDateTime, endDateTime);
        Long totalOffers = swapOfferMapper.selectCount(totalWrapper);

        if (totalOffers == null || totalOffers == 0) {
            return 0.0;
        }

        LambdaQueryWrapper<SwapOffer> acceptedWrapper = new LambdaQueryWrapper<>();
        acceptedWrapper.between(SwapOffer::getCreateTime, startDateTime, endDateTime)
                .eq(SwapOffer::getStatus, "accepted");
        Long acceptedOffers = swapOfferMapper.selectCount(acceptedWrapper);

        long accepted = acceptedOffers != null ? acceptedOffers : 0;
        BigDecimal rate = BigDecimal.valueOf(accepted)
                .divide(BigDecimal.valueOf(totalOffers), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        return rate.doubleValue();
    }

    private List<CategoryRatioVO> calculateCategoryRatios(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        LambdaQueryWrapper<Item> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.between(Item::getCreateTime, startDateTime, endDateTime);
        List<Item> items = itemMapper.selectList(itemWrapper);

        if (items == null || items.isEmpty()) {
            return new ArrayList<>();
        }

        long totalCount = items.size();
        Map<Long, Long> categoryCountMap = items.stream()
                .filter(item -> item.getCategoryId() != null)
                .collect(Collectors.groupingBy(Item::getCategoryId, Collectors.counting()));

        List<Category> categories = categoryMapper.selectList(new LambdaQueryWrapper<>());
        Map<Long, String> categoryNameMap = categories.stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        List<CategoryRatioVO> result = new ArrayList<>();
        for (Map.Entry<Long, Long> entry : categoryCountMap.entrySet()) {
            Long categoryId = entry.getKey();
            Long count = entry.getValue();
            String categoryName = categoryNameMap.getOrDefault(categoryId, "未知分类");
            BigDecimal ratio = BigDecimal.valueOf(count)
                    .divide(BigDecimal.valueOf(totalCount), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            result.add(new CategoryRatioVO(categoryId, categoryName, count, ratio.doubleValue()));
        }

        result.sort((a, b) -> Long.compare(b.getItemCount(), a.getItemCount()));
        return result;
    }
}
