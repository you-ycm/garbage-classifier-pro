package com.example.demo.controller;

import com.example.demo.dto.ClassifyResponse;
import com.example.demo.dto.Result;
import com.example.demo.entity.RecognitionRecord;
import com.example.demo.service.GarbageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class GarbageController {

    private final GarbageService garbageService;

    /**
     * 健康检查接口
     * GET /api/health
     */
    @GetMapping("/health")
    public Result<String> health() {
        log.info("健康检查接口被调用");
        return Result.success("服务运行正常");
    }

    /**
     * 垃圾分类识别接口
     * POST /api/classify
     * 参数: file (图片文件)
     */
    @PostMapping("/classify")
    public Result<ClassifyResponse> classify(@RequestParam("file") MultipartFile file) {
        log.info("收到识别请求，文件名: {}, 文件大小: {} bytes",
                file.getOriginalFilename(), file.getSize());

        try {
            ClassifyResponse response = garbageService.classifyImage(file);
            log.info("识别成功: {} -> {}", response.getItemName(), response.getCategory());
            return Result.success(response);
        } catch (Exception e) {
            log.error("识别失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取识别历史记录接口
     * GET /api/history
     */
    @GetMapping("/history")
    public Result<List<RecognitionRecord>> history() {
        log.info("获取历史记录请求");

        try {
            List<RecognitionRecord> records = garbageService.getHistory();
            log.info("获取到 {} 条历史记录", records.size());
            return Result.success(records);
        } catch (Exception e) {
            log.error("获取历史记录失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}