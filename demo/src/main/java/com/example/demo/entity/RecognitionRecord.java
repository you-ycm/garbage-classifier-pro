package com.example.demo.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "recognition_record")
public class RecognitionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "item_name", length = 100)
    private String itemName;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // 【新增】关联用户ID
    @Column(name = "user_id")
    private Long userId;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}