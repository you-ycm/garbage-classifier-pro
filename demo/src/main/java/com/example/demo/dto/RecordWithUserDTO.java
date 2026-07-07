package com.example.demo.dto;

import com.example.demo.entity.RecognitionRecord;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RecordWithUserDTO {
    private Long id;
    private String itemName;
    private String category;
    private Double confidence;
    private Long userId;
    private String username;
    private String imageUrl;

    // 🟢【核心修复】这里必须改成 LocalDateTime，不要转成 String！
    private LocalDateTime createdAt;

    public RecordWithUserDTO(RecognitionRecord record, String username) {
        this.id = record.getId();
        this.itemName = record.getItemName();
        this.category = record.getCategory();
        this.confidence = record.getConfidence();
        this.userId = record.getUserId();
        this.username = username;
        this.imageUrl = record.getImageUrl();

        // 🟢【核心修复】直接赋值 LocalDateTime 对象，不要在这里转 String！
        this.createdAt = record.getCreatedAt();
    }
}