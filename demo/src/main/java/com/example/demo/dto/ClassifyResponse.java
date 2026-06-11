package com.example.demo.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClassifyResponse {
    private String itemName;
    private String category;
    private Double confidence;
}