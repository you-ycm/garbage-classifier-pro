package com.example.demo.controller;

import com.example.demo.dto.Result;
import com.example.demo.entity.Favorite;
import com.example.demo.entity.RecognitionRecord;
import com.example.demo.entity.User;
import com.example.demo.repository.FavoriteRepository;
import com.example.demo.repository.RecognitionRecordRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final RecognitionRecordRepository recordRepository;
    private final EmailService emailService;

    // ===== 添加收藏 =====
    @PostMapping
    public Result<String> addFavorite(@RequestBody Map<String, Long> request) {
        Long recordId = request.get("recordId");
        log.info("收到收藏请求，recordId: {}", recordId);

        if (recordId == null) {
            return Result.error("请选择要收藏的记录");
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户未找到"));

        if (favoriteRepository.existsByUserIdAndRecordId(user.getId(), recordId)) {
            log.info("记录已收藏，recordId: {}", recordId);
            return Result.error("已收藏");
        }

        if (!recordRepository.existsById(recordId)) {
            log.warn("记录不存在，recordId: {}", recordId);
            return Result.error("记录不存在");
        }

        Favorite favorite = new Favorite();
        favorite.setUserId(user.getId());
        favorite.setRecordId(recordId);
        favoriteRepository.save(favorite);

        log.info("收藏成功，recordId: {}, userId: {}", recordId, user.getId());
        return Result.success("收藏成功");
    }

    // ===== 取消收藏 =====
    @DeleteMapping
    public Result<String> removeFavorite(@RequestBody Map<String, Long> request) {
        Long recordId = request.get("recordId");
        log.info("收到取消收藏请求，recordId: {}", recordId);

        if (recordId == null) {
            return Result.error("请选择要取消收藏的记录");
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户未找到"));

        Optional<Favorite> favorite = favoriteRepository.findByUserIdAndRecordId(user.getId(), recordId);
        if (favorite.isEmpty()) {
            log.warn("收藏记录不存在，recordId: {}", recordId);
            return Result.error("该记录未被收藏");
        }

        favoriteRepository.delete(favorite.get());
        log.info("取消收藏成功，recordId: {}", recordId);
        return Result.success("取消收藏成功");
    }

    // ===== 批量取消收藏 =====
    @DeleteMapping("/batch")
    public Result<String> batchRemoveFavorites(@RequestBody Map<String, List<Long>> request) {
        List<Long> recordIds = request.get("recordIds");
        if (recordIds == null || recordIds.isEmpty()) {
            return Result.error("请选择要取消收藏的记录");
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户未找到"));

        List<Favorite> favorites = favoriteRepository.findByUserIdAndRecordIdIn(user.getId(), recordIds);
        if (favorites.isEmpty()) {
            return Result.error("未找到要取消的收藏记录");
        }

        favoriteRepository.deleteAll(favorites);
        log.info("批量取消收藏成功，用户: {}, 数量: {}", username, favorites.size());
        return Result.success("成功取消收藏 " + favorites.size() + " 条记录");
    }

    // ===== 🟢 获取收藏列表（已修复返回结构） =====
    @GetMapping
    public Result<Map<String, Object>> getFavorites(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户未找到"));

        List<Favorite> allFavorites = favoriteRepository.findByUserId(user.getId());

        Map<String, Object> resultMap = new HashMap<>();

        if (allFavorites.isEmpty()) {
            resultMap.put("data", new ArrayList<>());
            resultMap.put("total", 0);
            return Result.success(resultMap);
        }

        List<Long> recordIds = allFavorites.stream()
                .map(Favorite::getRecordId)
                .collect(Collectors.toList());

        List<RecognitionRecord> allRecords = recordRepository.findAllById(recordIds);

        List<RecognitionRecord> filteredRecords = allRecords.stream()
                .filter(record -> {
                    if (keyword != null && !keyword.trim().isEmpty()) {
                        if (record.getItemName() == null ||
                                !record.getItemName().toLowerCase().contains(keyword.trim().toLowerCase())) {
                            return false;
                        }
                    }
                    if (category != null && !category.trim().isEmpty()) {
                        if (record.getCategory() == null || !record.getCategory().equals(category)) {
                            return false;
                        }
                    }
                    if (date != null && !date.trim().isEmpty()) {
                        if (record.getCreatedAt() == null) {
                            return false;
                        }
                        try {
                            LocalDate targetDate = LocalDate.parse(date);
                            if (!record.getCreatedAt().toLocalDate().equals(targetDate)) {
                                return false;
                            }
                        } catch (Exception e) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());

        if ("asc".equalsIgnoreCase(sortOrder)) {
            filteredRecords.sort((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()));
        } else {
            filteredRecords.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        }

        int start = (page - 1) * size;
        int end = Math.min(start + size, filteredRecords.size());
        List<RecognitionRecord> pageContent = filteredRecords.subList(
                start < filteredRecords.size() ? start : filteredRecords.size(),
                end
        );

        resultMap.put("data", pageContent);
        resultMap.put("total", filteredRecords.size());
        return Result.success(resultMap);
    }

    // ===== 检查是否已收藏 =====
    @GetMapping("/check/{recordId}")
    public Result<Map<String, Boolean>> checkFavorite(@PathVariable Long recordId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户未找到"));

        boolean isFavorited = favoriteRepository.existsByUserIdAndRecordId(user.getId(), recordId);
        return Result.success(Map.of("isFavorited", isFavorited));
    }

    // ===== 批量检查收藏状态 =====
    @PostMapping("/check/batch")
    public Result<Map<Long, Boolean>> batchCheckFavorite(@RequestBody Map<String, List<Long>> request) {
        List<Long> recordIds = request.get("recordIds");
        if (recordIds == null || recordIds.isEmpty()) {
            return Result.success(Map.of());
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户未找到"));

        List<Long> favoritedIds = favoriteRepository.findFavoritedRecordIds(user.getId(), recordIds);
        Map<Long, Boolean> result = recordIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        favoritedIds::contains
                ));

        return Result.success(result);
    }

    // ===== 邮箱分享收藏记录 =====
    @PostMapping("/share")
    public Result<String> shareFavorites(@RequestBody Map<String, List<Long>> request) {
        List<Long> recordIds = request.get("recordIds");
        if (recordIds == null || recordIds.isEmpty()) {
            return Result.error("请选择要分享的收藏记录");
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户未找到"));

        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            return Result.error("请先绑定邮箱");
        }

        List<RecognitionRecord> records = recordRepository.findAllById(recordIds);
        if (records.isEmpty()) {
            return Result.error("未找到要分享的记录");
        }

        try {
            emailService.sendShareRecordsEmail(user.getEmail(), user.getUsername(), records);
            log.info("收藏分享邮件已发送至: {}", user.getEmail());
            return Result.success("分享成功，邮件已发送至 " + user.getEmail());
        } catch (Exception e) {
            log.error("发送分享邮件失败: {}", e.getMessage());
            return Result.error("邮件发送失败: " + e.getMessage());
        }
    }

    // ===== 导出收藏记录 CSV（支持搜索/筛选/勾选） =====
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportFavorites(
            @RequestParam(required = false) String ids,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String date,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder) {

        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("用户未找到"));

            List<RecognitionRecord> records = new ArrayList<>();

            if (ids != null && !ids.trim().isEmpty()) {
                String[] idArray = ids.split(",");
                List<Long> idList = new ArrayList<>();
                for (String s : idArray) {
                    try {
                        idList.add(Long.parseLong(s.trim()));
                    } catch (NumberFormatException ignored) {}
                }
                if (!idList.isEmpty()) {
                    records = recordRepository.findAllById(idList);
                }
            } else {
                List<Favorite> allFavorites = favoriteRepository.findByUserId(user.getId());
                if (allFavorites.isEmpty()) {
                    return buildEmptyCsvResponse("我的收藏");
                }

                List<Long> recordIds = allFavorites.stream()
                        .map(Favorite::getRecordId)
                        .collect(Collectors.toList());

                List<RecognitionRecord> allRecords = recordRepository.findAllById(recordIds);

                records = allRecords.stream()
                        .filter(record -> {
                            if (keyword != null && !keyword.trim().isEmpty()) {
                                if (record.getItemName() == null ||
                                        !record.getItemName().toLowerCase().contains(keyword.trim().toLowerCase())) {
                                    return false;
                                }
                            }
                            if (category != null && !category.trim().isEmpty()) {
                                if (record.getCategory() == null || !record.getCategory().equals(category)) {
                                    return false;
                                }
                            }
                            if (date != null && !date.trim().isEmpty()) {
                                if (record.getCreatedAt() == null) {
                                    return false;
                                }
                                try {
                                    LocalDate targetDate = LocalDate.parse(date);
                                    if (!record.getCreatedAt().toLocalDate().equals(targetDate)) {
                                        return false;
                                    }
                                } catch (Exception e) {
                                    return false;
                                }
                            }
                            return true;
                        })
                        .collect(Collectors.toList());

                if ("asc".equalsIgnoreCase(sortOrder)) {
                    records.sort((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()));
                } else {
                    records.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
                }
            }

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
            headers.setContentDispositionFormData("attachment", "我的收藏_" + LocalDate.now() + ".csv");

            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("导出收藏CSV失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private ResponseEntity<byte[]> buildEmptyCsvResponse(String prefix) {
        String csv = "\uFEFF物品名称,分类,置信度,识别时间\n";
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", prefix + "_" + LocalDate.now() + ".csv");
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }
}