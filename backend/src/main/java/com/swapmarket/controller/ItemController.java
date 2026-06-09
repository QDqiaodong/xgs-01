package com.swapmarket.controller;

import com.swapmarket.common.PageResult;
import com.swapmarket.common.Result;
import com.swapmarket.entity.Item;
import com.swapmarket.service.ItemService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/item")
@RequiredArgsConstructor
public class ItemController {
    private final ItemService itemService;

    @GetMapping("/top")
    public Result<List<Item>> getTopItems(@RequestParam(required = false) Long userId) {
        return Result.success(itemService.getTopItems(userId));
    }

    @GetMapping("/list")
    public Result<PageResult<Item>> listItems(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String condition,
            @RequestParam(required = false) List<String> conditions,
            @RequestParam(required = false) String timeRange,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean hasImages,
            @RequestParam(required = false) String exchangeKeyword,
            @RequestParam(required = false) Long userId) {
        return Result.success(itemService.listItems(page, size, categoryId, condition, conditions, timeRange, keyword, hasImages, exchangeKeyword, userId));
    }

    @GetMapping("/my")
    public Result<List<Item>> getMyItems(
            @RequestParam(defaultValue = "1") Long userId,
            @RequestParam(required = false) String status) {
        return Result.success(itemService.getMyItems(userId, status, userId));
    }

    @GetMapping("/favorites")
    public Result<List<Item>> getMyFavorites(@RequestParam(defaultValue = "1") Long userId) {
        return Result.success(itemService.getMyFavorites(userId));
    }

    @PostMapping("/favorite/{id}")
    public Result<Void> addFavorite(@RequestParam(defaultValue = "1") Long userId, @PathVariable Long id) {
        itemService.addFavorite(userId, id);
        return Result.success();
    }

    @DeleteMapping("/favorite/{id}")
    public Result<Void> removeFavorite(@RequestParam(defaultValue = "1") Long userId, @PathVariable Long id) {
        itemService.removeFavorite(userId, id);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<Item> getDetail(@PathVariable Long id, @RequestParam(required = false) Long userId) {
        return Result.success(itemService.getDetail(id, userId));
    }

    @PostMapping("/publish")
    public Result<Item> publish(
            @RequestParam Long userId,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam Long categoryId,
            @RequestParam String condition,
            @RequestParam(required = false) String expectedSwap,
            @RequestParam(required = false) List<MultipartFile> images) throws IOException {
        return Result.success(itemService.publish(userId, title, description, categoryId, condition, expectedSwap, images));
    }

    @PostMapping("/offline/{id}")
    public Result<Void> offline(@RequestParam(defaultValue = "1") Long userId, @PathVariable Long id) {
        itemService.offline(userId, id);
        return Result.success();
    }

    @PostMapping("/publish/{id}")
    public Result<Void> republish(@RequestParam(defaultValue = "1") Long userId, @PathVariable Long id) {
        itemService.republish(userId, id);
        return Result.success();
    }
}
