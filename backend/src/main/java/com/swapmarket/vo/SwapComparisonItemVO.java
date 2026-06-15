package com.swapmarket.vo;

import lombok.Data;
import java.util.List;

@Data
public class SwapComparisonItemVO {

    private Long itemId;

    private String title;

    private List<String> images;

    private String description;

    private String condition;

    private String expectedSwap;

    private String categoryName;

    private String publisherName;
}
