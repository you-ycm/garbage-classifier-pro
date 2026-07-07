package com.example.demo.dto;

import lombok.Data;
import java.util.List;

@Data
public class ShareRequest {
    private List<Long> ids;
}