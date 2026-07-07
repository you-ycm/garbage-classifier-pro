// ===== GarbageController.java =====

package com.example.demo.controller;

import com.example.demo.dto.BatchClassifyResponse;
import com.example.demo.dto.ClassifyResponse;
import com.example.demo.dto.PageResult;
import com.example.demo.dto.Result;
import com.example.demo.dto.ShareRequest;
import com.example.demo.entity.RecognitionRecord;
import com.example.demo.entity.User;
import com.example.demo.repository.RecognitionRecordRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.EmailService;
import com.example.demo.service.GarbageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class GarbageController {

    private final GarbageService garbageService;
    private final UserRepository userRepository;
    private final RecognitionRecordRepository recordRepository;
    private final EmailService emailService;

    @GetMapping("/health")
    public Result<String> health() {
        log.info("健康检查接口被调用");
        return Result.success("服务运行正常");
    }

    // ===== 单张识别（支持压缩质量） =====
    @PostMapping("/classify")
    public Result<ClassifyResponse> classify(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false, defaultValue = "medium") String quality) {
        log.info("收到识别请求，文件名: {}, 文件大小: {} bytes, 压缩质量: {}",
                file.getOriginalFilename(), file.getSize(), quality);

        try {
            ClassifyResponse response = garbageService.classifyImageWithQuality(file, quality);
            log.info("识别成功: {} -> {}", response.getItemName(), response.getCategory());
            return Result.success(response);
        } catch (Exception e) {
            log.error("识别失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    // ===== 批量识别 =====
    @PostMapping("/classify/batch")
    public Result<BatchClassifyResponse> classifyBatch(@RequestParam("files") List<MultipartFile> files) {
        log.info("收到批量识别请求，文件数: {}", files.size());

        if (files == null || files.isEmpty()) {
            return Result.error("请选择要识别的图片");
        }

        if (files.size() > 10) {
            return Result.error("一次最多识别10张图片");
        }

        try {
            BatchClassifyResponse response = garbageService.classifyBatch(files);
            return Result.success(response);
        } catch (Exception e) {
            log.error("批量识别失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    // ===== 分页查询历史记录 =====
    @GetMapping("/history")
    public Result<PageResult<RecognitionRecord>> history(
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int size) {

        log.info("获取历史记录请求，分类: {}, 排序: {}, 日期: {}, 关键词: {}, 页码: {}, 每页: {}",
                category == null ? "全部" : category, sortOrder,
                date == null ? "全部" : date,
                keyword == null ? "无" : keyword, page, size);

        try {
            PageResult<RecognitionRecord> result;
            if (keyword != null && !keyword.trim().isEmpty()) {
                result = garbageService.searchHistory(category, keyword, date, page, size);
            } else {
                result = garbageService.getHistoryWithPage(category, sortOrder, date, page, size);
            }
            log.info("获取到 {} 条历史记录", result.getData().size());
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取历史记录失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/history/recent")
    public Result<List<RecognitionRecord>> getRecentHistory() {
        log.info("获取最近识别记录请求");
        try {
            List<RecognitionRecord> records = garbageService.getRecentHistory(5);
            log.info("获取到 {} 条最近记录", records.size());
            return Result.success(records);
        } catch (Exception e) {
            log.error("获取最近记录失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    // ===== 分享记录到邮箱 =====
    @PostMapping("/history/share")
    public Result<String> shareRecords(@RequestBody ShareRequest request) {
        log.info("分享识别记录请求，记录数: {}", request.getIds().size());

        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("用户未找到"));

            if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
                return Result.error("请先绑定邮箱");
            }

            List<RecognitionRecord> records = garbageService.getRecordsByIds(request.getIds());

            for (RecognitionRecord record : records) {
                if (!record.getUserId().equals(user.getId())) {
                    return Result.error("存在不属于您的记录，分享失败");
                }
            }

            emailService.sendShareRecordsEmail(user.getEmail(), user.getUsername(), records);

            return Result.success("分享成功，邮件已发送至 " + user.getEmail());

        } catch (Exception e) {
            log.error("分享记录失败: {}", e.getMessage());
            return Result.error("分享失败: " + e.getMessage());
        }
    }

    // ===== 批量删除记录 =====
    @DeleteMapping("/history/batch")
    public Result<String> batchDeleteRecords(@RequestBody ShareRequest request) {
        log.info("批量删除记录请求，记录数: {}", request.getIds().size());

        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("用户未找到"));

            List<RecognitionRecord> records = garbageService.getRecordsByIds(request.getIds());

            for (RecognitionRecord record : records) {
                if (!record.getUserId().equals(user.getId())) {
                    return Result.error("存在不属于您的记录，删除失败");
                }
            }

            garbageService.batchDeleteRecords(request.getIds());
            log.info("批量删除成功: {} 条记录", request.getIds().size());
            return Result.success("删除成功");

        } catch (Exception e) {
            log.error("批量删除失败: {}", e.getMessage());
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    // ===== 获取图片 =====
    @GetMapping("/images/{userId}/{filename}")
    public ResponseEntity<byte[]> getImage(
            @PathVariable Long userId,
            @PathVariable String filename) {

        try {
            String decodedFilename = URLDecoder.decode(filename, StandardCharsets.UTF_8.name());
            String baseDir = "/home/ycm/images/" + userId;
            File imageFile = new File(baseDir, decodedFilename);

            if (!imageFile.exists()) {
                log.warn("图片不存在: {}", imageFile.getAbsolutePath());
                return ResponseEntity.notFound().build();
            }

            byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(imageBytes);

        } catch (IOException e) {
            log.error("读取图片失败: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ===== 导出CSV（终极修复版） =====
    @GetMapping("/history/export")
    public ResponseEntity<byte[]> exportHistory(
            @RequestParam(required = false) String ids, // 🟢 接收逗号分隔的字符串 ID
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String date,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder) {

        try {
            // 1. 获取当前登录用户
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            Long currentUserId = userOpt.get().getId();

            List<RecognitionRecord> records = new ArrayList<>();

            // 2. 优先逻辑：处理勾选导出 (ids 不为空)
            if (ids != null && !ids.trim().isEmpty()) {
                String[] idArray = ids.split(",");
                List<Long> idList = new ArrayList<>();
                for (String s : idArray) {
                    try {
                        idList.add(Long.parseLong(s.trim()));
                    } catch (NumberFormatException ignored) {}
                }
                if (!idList.isEmpty()) {
                    // 只要勾选了的 ID，一定是属于当前用户自己的，直接查出来
                    List<RecognitionRecord> allSelected = recordRepository.findAllById(idList);
                    // 这里为了安全，再过滤一遍是否属于当前用户
                    for (RecognitionRecord r : allSelected) {
                        if (r.getUserId().equals(currentUserId)) {
                            records.add(r);
                        }
                    }
                }
            } else {
                // 3. 兜底逻辑：处理按搜索条件导出 (分类、关键字、日期)
                Sort sort = "asc".equalsIgnoreCase(sortOrder) ?
                        Sort.by(Sort.Direction.ASC, "createdAt") :
                        Sort.by(Sort.Direction.DESC, "createdAt");

                // 先按条件查出所有记录
                List<RecognitionRecord> allUserRecords = recordRepository.findByUserId(currentUserId, sort);

                for (RecognitionRecord record : allUserRecords) {
                    boolean match = true;

                    // 过滤分类
                    if (category != null && !category.isEmpty()) {
                        if (!category.equals(record.getCategory())) {
                            match = false;
                        }
                    }

                    // 过滤物品名称 (模糊匹配)
                    if (match && keyword != null && !keyword.isEmpty()) {
                        if (record.getItemName() == null || !record.getItemName().toLowerCase().contains(keyword.toLowerCase())) {
                            match = false;
                        }
                    }

                    // 过滤日期
                    if (match && date != null && !date.isEmpty()) {
                        try {
                            LocalDate targetDate = LocalDate.parse(date);
                            LocalDate recordDate = record.getCreatedAt().toLocalDate();
                            if (!targetDate.equals(recordDate)) {
                                match = false;
                            }
                        } catch (Exception e) {
                            match = false;
                        }
                    }

                    if (match) {
                        records.add(record);
                    }
                }
            }

            // 4. 生成 CSV
            StringBuilder csv = new StringBuilder();
            csv.append("\uFEFF");
            csv.append("物品名称,分类,置信度,识别时间\n");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (RecognitionRecord record : records) {
                csv.append(record.getItemName() != null ? record.getItemName() : "").append(",");
                csv.append(record.getCategory() != null ? record.getCategory() : "").append(",");
                csv.append(String.format("%.2f%%", record.getConfidence() * 100)).append(",");

                String timeStr = "";
                if (record.getCreatedAt() != null) {
                    timeStr = formatter.format(record.getCreatedAt());
                }
                csv.append(timeStr).append("\n");
            }

            byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
            headers.setContentDispositionFormData("attachment", "识别历史_" + LocalDate.now() + ".csv");

            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("导出CSV失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}