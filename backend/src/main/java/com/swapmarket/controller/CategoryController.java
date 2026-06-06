package com.swapmarket.controller;

import com.swapmarket.common.Result;
import com.swapmarket.entity.Category;
import com.swapmarket.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/category")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping("/list")
    public Result<List<Category>> list() {
        return Result.success(categoryService.list());
    }
}
