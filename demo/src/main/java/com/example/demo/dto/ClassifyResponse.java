// ===== ClassifyResponse.java（修改后） =====

package com.example.demo.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClassifyResponse {
    private Long id;  // 新增：记录ID，用于收藏
    private String itemName;
    private String category;
    private Double confidence;
}