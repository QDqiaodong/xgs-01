package com.swapmarket.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRatioVO {

    private Long categoryId;

    private String categoryName;

    private Long itemCount;

    private Double ratio;
}
