package com.example.demo.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ClassifyRequest {
    private MultipartFile file;
}