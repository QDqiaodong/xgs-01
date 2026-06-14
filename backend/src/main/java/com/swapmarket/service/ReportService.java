package com.swapmarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swapmarket.common.PageResult;
import com.swapmarket.entity.Item;
import com.swapmarket.entity.Report;
import com.swapmarket.entity.User;
import com.swapmarket.mapper.ItemMapper;
import com.swapmarket.mapper.ReportMapper;
import com.swapmarket.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final ReportMapper reportMapper;
    private final ItemMapper itemMapper;
    private final UserMapper userMapper;
    private final FileStorageService fileStorageService;

    @Transactional
    public Report submitReport(Long userId, Long itemId, String reasonType,
                               String description, List<MultipartFile> images) throws IOException {
        Item item = itemMapper.selectById(itemId);
        if (item == null || item.getDeleted() == 1) {
            throw new RuntimeException("物品不存在");
        }

        Report existing = reportMapper.selectOne(new LambdaQueryWrapper<Report>()
                .eq(Report::getUserId, userId)
                .eq(Report::getItemId, itemId)
                .eq(Report::getStatus, "pending"));
        if (existing != null) {
            throw new RuntimeException("您已举报过该物品，请等待处理");
        }

        Report report = new Report();
        report.setUserId(userId);
        report.setItemId(itemId);
        report.setReasonType(reasonType);
        report.setDescription(description);
        report.setStatus("pending");

        if (images != null && !images.isEmpty()) {
            List<String> imageUrls = new ArrayList<>();
            for (MultipartFile image : images) {
                String imageUrl = fileStorageService.saveFile(image);
                imageUrls.add(imageUrl);
            }
            report.setImages(String.join(",", imageUrls));
        }

        reportMapper.insert(report);
        return report;
    }

    public PageResult<Report> listReports(int page, int size, String status,
                                           String startTime, String endTime) {
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<Report>()
                .eq(status != null && !status.isEmpty(), Report::getStatus, status)
                .ge(startTime != null && !startTime.isEmpty(), Report::getCreateTime, LocalDateTime.parse(startTime))
                .le(endTime != null && !endTime.isEmpty(), Report::getCreateTime, LocalDateTime.parse(endTime))
                .orderByDesc(Report::getCreateTime);

        Page<Report> reportPage = reportMapper.selectPage(Page.of(page, size), wrapper);
        enrichReports(reportPage.getRecords());

        return PageResult.of(reportPage.getRecords(), reportPage.getTotal(), page, size);
    }

    @Transactional
    public void handleReport(Long reportId, Long handlerId, String action, String handleRemark) {
        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new RuntimeException("举报记录不存在");
        }
        if (!"pending".equals(report.getStatus())) {
            throw new RuntimeException("该举报已被处理");
        }

        if ("approve".equals(action)) {
            report.setStatus("approved");
            Item item = itemMapper.selectById(report.getItemId());
            if (item != null) {
                item.setStatus("offline");
                itemMapper.updateById(item);
            }
        } else if ("reject".equals(action)) {
            report.setStatus("rejected");
        } else {
            throw new RuntimeException("无效的处理操作");
        }

        report.setHandlerId(handlerId);
        report.setHandleRemark(handleRemark);
        reportMapper.updateById(report);
    }

    private void enrichReports(List<Report> reports) {
        if (reports.isEmpty()) return;

        List<Long> itemIds = reports.stream().map(Report::getItemId).distinct().collect(Collectors.toList());
        List<Long> userIds = reports.stream().map(Report::getUserId).distinct().collect(Collectors.toList());
        List<Long> handlerIds = reports.stream()
                .map(Report::getHandlerId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, String> itemTitleMap = itemMapper.selectBatchIds(itemIds).stream()
                .collect(Collectors.toMap(Item::getId, Item::getTitle));

        Map<Long, String> userNicknameMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u.getNickname() != null ? u.getNickname() : u.getUsername()));

        Map<Long, String> handlerNicknameMap = handlerIds.isEmpty() ? Map.of() :
                userMapper.selectBatchIds(handlerIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u.getNickname() != null ? u.getNickname() : u.getUsername()));

        for (Report report : reports) {
            report.setItemTitle(itemTitleMap.getOrDefault(report.getItemId(), "未知物品"));
            report.setReporterNickname(userNicknameMap.getOrDefault(report.getUserId(), "未知用户"));
            if (report.getHandlerId() != null) {
                report.setHandlerNickname(handlerNicknameMap.get(report.getHandlerId()));
            }
        }
    }
}
