package com.swapmarket.controller;

import com.swapmarket.common.PageResult;
import com.swapmarket.common.Result;
import com.swapmarket.entity.Report;
import com.swapmarket.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;

    @PostMapping("/submit")
    public Result<Report> submitReport(
            @RequestParam Long userId,
            @RequestParam Long itemId,
            @RequestParam String reasonType,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) List<MultipartFile> images) throws IOException {
        return Result.success(reportService.submitReport(userId, itemId, reasonType, description, images));
    }

    @GetMapping("/list")
    public Result<PageResult<Report>> listReports(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return Result.success(reportService.listReports(page, size, status, startTime, endTime));
    }

    @PostMapping("/handle/{id}")
    public Result<Void> handleReport(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Long handlerId,
            @RequestParam String action,
            @RequestParam(required = false) String handleRemark) {
        reportService.handleReport(id, handlerId, action, handleRemark);
        return Result.success();
    }
}
