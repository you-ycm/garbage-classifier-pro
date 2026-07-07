package com.example.demo.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class BatchClassifyResponse {
    private int total;
    private int successCount;
    private int failCount;
    private List<ClassifyResult> results;

    @Data
    @Builder
    public static class ClassifyResult {
        private String fileName;
        private String itemName;
        private String category;
        private Double confidence;
        private Boolean success;
        private String errorMsg;
    }
}