package com.example.demo.controller;

import com.example.demo.dto.CategoryStatsDTO;
import com.example.demo.dto.PageResult;
import com.example.demo.dto.RecordWithUserDTO;
import com.example.demo.dto.Result;
import com.example.demo.dto.ShareRequest;
import com.example.demo.entity.Favorite;
import com.example.demo.entity.KnowledgeBase;
import com.example.demo.entity.OperationLog;
import com.example.demo.entity.RecognitionRecord;
import com.example.demo.entity.User;
import com.example.demo.repository.FavoriteRepository;
import com.example.demo.repository.KnowledgeBaseRepository;
import com.example.demo.repository.RecognitionRecordRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.AdminService;
import com.example.demo.service.EmailService;
import com.example.demo.service.KnowledgeBaseService;
import com.example.demo.service.OperationLogService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final AdminService adminService;
    private final EmailService emailService;
    private final RecognitionRecordRepository recordRepository;
    private final OperationLogService operationLogService;
    private final FavoriteRepository favoriteRepository;
    private final KnowledgeBaseService knowledgeBaseService;

    // ===== 用户管理 =====
    @GetMapping("/users")
    public Result<List<Map<String, Object>>> getAllUsers(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String date,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder) {

        List<User> users = userRepository.findAll();

        List<User> filteredUsers = users.stream()
                .filter(user -> {
                    if (username != null && !username.isEmpty()) {
                        if (!user.getUsername().toLowerCase().contains(username.toLowerCase())) {
                            return false;
                        }
                    }
                    if (email != null && !email.isEmpty()) {
                        if (user.getEmail() == null || !user.getEmail().toLowerCase().contains(email.toLowerCase())) {
                            return false;
                        }
                    }
                    if (role != null && !role.isEmpty()) {
                        if (!role.equals(user.getRole())) {
                            return false;
                        }
                    }
                    if (date != null && !date.isEmpty()) {
                        if (user.getCreatedAt() == null) {
                            return false;
                        }
                        try {
                            LocalDate targetDate = LocalDate.parse(date);
                            if (!user.getCreatedAt().toLocalDate().equals(targetDate)) {
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
            filteredUsers.sort((u1, u2) -> u1.getCreatedAt() != null && u2.getCreatedAt() != null
                    ? u1.getCreatedAt().compareTo(u2.getCreatedAt()) : 0);
        } else {
            filteredUsers.sort((u1, u2) -> u1.getCreatedAt() != null && u2.getCreatedAt() != null
                    ? u2.getCreatedAt().compareTo(u1.getCreatedAt()) : 0);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<Map<String, Object>> safeUsers = filteredUsers.stream()
                .map(user -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", user.getId());
                    map.put("username", user.getUsername());
                    map.put("email", user.getEmail());
                    map.put("role", user.getRole());
                    String timeStr = "暂无记录";
                    if (user.getCreatedAt() != null) {
                        timeStr = formatter.format(user.getCreatedAt());
                    }
                    map.put("createdAt", timeStr);
                    return map;
                })
                .collect(Collectors.toList());

        return Result.success(safeUsers);
    }

    @DeleteMapping("/users/batch")
    public Result<String> batchDeleteUsers(@RequestBody ShareRequest request) {
        if (request.getIds() == null || request.getIds().isEmpty()) {
            return Result.error("请选择要删除的用户");
        }
        userRepository.deleteAllByIdInBatch(request.getIds());
        return Result.success("成功删除 " + request.getIds().size() + " 个用户");
    }

    @DeleteMapping("/users/{id}")
    public Result<String> deleteUser(@PathVariable Long id) {
        userRepository.deleteById(id);
        return Result.success("用户删除成功");
    }

    @GetMapping("/users/export")
    public ResponseEntity<byte[]> exportUsers(
            @RequestParam(required = false) String ids,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String date,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder) {

        try {
            List<User> users;

            if (ids != null && !ids.trim().isEmpty()) {
                String[] idArray = ids.split(",");
                List<Long> idList = new ArrayList<>();
                for (String s : idArray) {
                    try {
                        idList.add(Long.parseLong(s.trim()));
                    } catch (NumberFormatException ignored) {}
                }
                if (!idList.isEmpty()) {
                    users = userRepository.findAllById(idList);
                } else {
                    users = new ArrayList<>();
                }
            } else {
                users = userRepository.findAll();

                users = users.stream()
                        .filter(user -> {
                            if (username != null && !username.isEmpty()) {
                                if (!user.getUsername().toLowerCase().contains(username.toLowerCase())) {
                                    return false;
                                }
                            }
                            if (email != null && !email.isEmpty()) {
                                if (user.getEmail() == null || !user.getEmail().toLowerCase().contains(email.toLowerCase())) {
                                    return false;
                                }
                            }
                            if (role != null && !role.isEmpty()) {
                                if (!role.equals(user.getRole())) {
                                    return false;
                                }
                            }
                            if (date != null && !date.isEmpty()) {
                                if (user.getCreatedAt() == null) {
                                    return false;
                                }
                                try {
                                    LocalDate targetDate = LocalDate.parse(date);
                                    if (!user.getCreatedAt().toLocalDate().equals(targetDate)) {
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
                    users.sort((u1, u2) -> u1.getCreatedAt() != null && u2.getCreatedAt() != null
                            ? u1.getCreatedAt().compareTo(u2.getCreatedAt()) : 0);
                } else {
                    users.sort((u1, u2) -> u1.getCreatedAt() != null && u2.getCreatedAt() != null
                            ? u2.getCreatedAt().compareTo(u1.getCreatedAt()) : 0);
                }
            }

            StringBuilder csv = new StringBuilder();
            csv.append("\uFEFF");
            csv.append("用户ID,用户名,邮箱,角色,注册时间\n");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (User user : users) {
                csv.append(user.getId()).append(",");
                csv.append(user.getUsername()).append(",");
                csv.append(user.getEmail() != null ? user.getEmail() : "").append(",");

                String roleStr = "普通用户";
                if ("ROLE_ADMIN".equals(user.getRole())) {
                    roleStr = "管理员";
                }
                csv.append(roleStr).append(",");

                String timeStr = "";
                if (user.getCreatedAt() != null) {
                    timeStr = formatter.format(user.getCreatedAt());
                }
                csv.append(timeStr).append("\n");
            }

            byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
            headers.setContentDispositionFormData("attachment", "用户管理_" + LocalDate.now() + ".csv");

            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("导出用户CSV失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ===== 识别记录管理 =====
    @GetMapping("/records")
    public Result<PageResult<RecordWithUserDTO>> getRecords(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String date,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int size) {

        PageResult<RecordWithUserDTO> result = adminService.getRecordsWithPage(
                category, keyword, username, date, sortOrder, page, size);
        return Result.success(result);
    }

    @DeleteMapping("/records/batch")
    public Result<String> batchDeleteRecords(@RequestBody ShareRequest request) {
        if (request.getIds() == null || request.getIds().isEmpty()) {
            return Result.error("请选择要删除的记录");
        }
        adminService.batchDeleteRecords(request.getIds());
        return Result.success("成功删除 " + request.getIds().size() + " 条记录");
    }

    @PostMapping("/records/share")
    public Result<String> shareRecords(@RequestBody ShareRequest request) {
        log.info("管理员分享识别记录请求，记录数: {}", request.getIds().size());

        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User admin = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("管理员未找到"));

            if (admin.getEmail() == null || admin.getEmail().trim().isEmpty()) {
                return Result.error("请先绑定邮箱");
            }

            List<RecognitionRecord> records = adminService.getRecordsByIds(request.getIds());
            emailService.sendAdminShareEmail(admin.getEmail(), admin.getUsername(), records);

            return Result.success("分享成功，邮件已发送至 " + admin.getEmail());

        } catch (Exception e) {
            log.error("管理员分享记录失败: {}", e.getMessage());
            return Result.error("分享失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/records/{id}")
    public Result<String> deleteRecord(@PathVariable Long id) {
        recordRepository.deleteById(id);
        return Result.success("记录删除成功");
    }

    // ===== 统计 =====
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        long userCount = userRepository.count();
        long recordCount = recordRepository.count();
        long favoriteCount = favoriteRepository.count();
        List<CategoryStatsDTO> categoryStats = adminService.getCategoryStats();
        List<Map<String, Object>> trendStats = adminService.getTrendStats();

        return Result.success(Map.of(
                "userCount", userCount,
                "recordCount", recordCount,
                "favoriteCount", favoriteCount,
                "categoryStats", categoryStats,
                "trendStats", trendStats
        ));
    }

    // ===== 操作日志 =====
    @GetMapping("/logs")
    public Result<Page<OperationLog>> getLogs(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) String date,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            HttpServletRequest request) {

        Page<OperationLog> result = operationLogService.getLogsWithSearch(
                username, operation, ip, date, sortOrder, page, size);
        return Result.success(result);
    }

    @GetMapping("/logs/recent")
    public Result<List<OperationLog>> getRecentLogs() {
        return Result.success(operationLogService.getRecentLogs());
    }

    @DeleteMapping("/logs/batch")
    public Result<String> batchDeleteLogs(@RequestBody ShareRequest request) {
        if (request.getIds() == null || request.getIds().isEmpty()) {
            return Result.error("请选择要删除的记录");
        }
        operationLogService.batchDeleteLogs(request.getIds());
        return Result.success("成功删除 " + request.getIds().size() + " 条日志记录");
    }

    @GetMapping("/logs/export")
    public ResponseEntity<byte[]> exportLogs(
            @RequestParam(required = false) String ids,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) String date,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder) {

        try {
            List<OperationLog> logs = new ArrayList<>();

            if (ids != null && !ids.trim().isEmpty()) {
                String[] idArray = ids.split(",");
                List<Long> idList = new ArrayList<>();
                for (String s : idArray) {
                    try {
                        idList.add(Long.parseLong(s.trim()));
                    } catch (NumberFormatException ignored) {}
                }
                if (!idList.isEmpty()) {
                    logs = operationLogService.getLogsByIds(idList);
                } else {
                    logs = new ArrayList<>();
                }
            } else {
                LocalDateTime startTime = null;
                LocalDateTime endTime = null;
                if (date != null && !date.isEmpty()) {
                    LocalDate targetDate = LocalDate.parse(date);
                    startTime = targetDate.atStartOfDay();
                    endTime = targetDate.atTime(LocalTime.MAX);
                }

                Pageable pageable = PageRequest.of(0, 10000,
                        "asc".equalsIgnoreCase(sortOrder) ?
                                Sort.by(Sort.Direction.ASC, "createdAt") :
                                Sort.by(Sort.Direction.DESC, "createdAt"));

                Page<OperationLog> result = operationLogService.getLogsWithSearch(
                        username, operation, ip, date, sortOrder, 1, 10000);
                logs = result.getContent();
            }

            StringBuilder csv = new StringBuilder();
            csv.append("\uFEFF");
            csv.append("日志ID,用户,操作,详情,IP地址,操作时间\n");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (OperationLog log : logs) {
                csv.append(log.getId()).append(",");
                csv.append(log.getUsername() != null ? log.getUsername() : "").append(",");
                csv.append(log.getOperation() != null ? log.getOperation() : "").append(",");
                csv.append(log.getDetail() != null ? log.getDetail() : "").append(",");
                csv.append(log.getIp() != null ? log.getIp() : "").append(",");

                String timeStr = "";
                if (log.getCreatedAt() != null) {
                    timeStr = formatter.format(log.getCreatedAt());
                }
                csv.append(timeStr).append("\n");
            }

            byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
            headers.setContentDispositionFormData("attachment", "操作日志_" + LocalDate.now() + ".csv");

            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("导出操作日志CSV失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ===== 🟢 彻底修复识别记录导出CSV =====
    @GetMapping("/records/export")
    public ResponseEntity<byte[]> exportRecords(
            @RequestParam(required = false) String ids,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String date,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder) {

        try {
            List<RecognitionRecord> records = new ArrayList<>();

            // 1. 优先逻辑：按ID列表导出
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
                // 2. 后补逻辑：按搜索条件导出
                Sort sort = "asc".equalsIgnoreCase(sortOrder) ?
                        Sort.by(Sort.Direction.ASC, "createdAt") :
                        Sort.by(Sort.Direction.DESC, "createdAt");
                List<RecognitionRecord> allRecords = recordRepository.findAll(sort);

                for (RecognitionRecord record : allRecords) {
                    boolean match = true;

                    // 分类过滤
                    if (category != null && !category.isEmpty()) {
                        if (!category.equals(record.getCategory())) {
                            match = false;
                        }
                    }

                    // 物品名模糊匹配
                    if (match && keyword != null && !keyword.isEmpty()) {
                        if (record.getItemName() == null || !record.getItemName().toLowerCase().contains(keyword.toLowerCase())) {
                            match = false;
                        }
                    }

                    // 日期过滤
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

                    // 用户名过滤
                    if (match && username != null && !username.isEmpty()) {
                        Optional<User> userOpt = userRepository.findById(record.getUserId());
                        if (userOpt.isEmpty() || !userOpt.get().getUsername().toLowerCase().contains(username.toLowerCase())) {
                            match = false;
                        }
                    }

                    if (match) {
                        records.add(record);
                    }
                }
            }

            // 3. 构建 CSV
            StringBuilder csv = new StringBuilder();
            csv.append("\uFEFF");
            csv.append("用户名称,物品名称,分类,置信度,识别时间\n");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (RecognitionRecord record : records) {
                String usernameRes = userRepository.findById(record.getUserId())
                        .map(User::getUsername)
                        .orElse("未知用户");

                csv.append(usernameRes).append(",");
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
            headers.setContentDispositionFormData("attachment", "管理员_识别记录_" + LocalDate.now() + ".csv");

            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("管理员导出CSV失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ===== 收藏管理（已修改：增加 imageUrl 和 confidence） =====
    @GetMapping("/favorites")
    public Result<Page<Map<String, Object>>> getFavorites(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String date,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        Sort sort = "asc".equalsIgnoreCase(sortOrder) ?
                Sort.by(Sort.Direction.ASC, "createdAt") :
                Sort.by(Sort.Direction.DESC, "createdAt");

        Pageable pageable = PageRequest.of(page - 1, size, sort);
        Page<Favorite> favoritePage = favoriteRepository.findAll(pageable);
        List<Favorite> allFavs = favoritePage.getContent();

        List<Map<String, Object>> dataList = new ArrayList<>();

        for (Favorite fav : allFavs) {
            Optional<User> userOpt = userRepository.findById(fav.getUserId());
            Optional<RecognitionRecord> recordOpt = recordRepository.findById(fav.getRecordId());

            // 1. 分类过滤
            if (category != null && !category.isEmpty()) {
                if (recordOpt.isEmpty() || !category.equals(recordOpt.get().getCategory())) {
                    continue;
                }
            }

            // 2. 多条件模糊搜索
            if ((username != null && !username.isEmpty()) || (keyword != null && !keyword.isEmpty())) {
                boolean matchUser = true;
                boolean matchItem = true;

                if (username != null && !username.isEmpty()) {
                    matchUser = userOpt.map(u -> u.getUsername().toLowerCase().contains(username.toLowerCase())).orElse(false);
                }
                if (keyword != null && !keyword.isEmpty()) {
                    matchItem = recordOpt.map(r -> r.getItemName().toLowerCase().contains(keyword.toLowerCase())).orElse(false);
                }

                if (!matchUser || !matchItem) {
                    continue;
                }
            }

            // 3. 日期过滤
            if (date != null && !date.isEmpty()) {
                if (fav.getCreatedAt() == null) continue;
                try {
                    LocalDate targetDate = LocalDate.parse(date);
                    if (!fav.getCreatedAt().toLocalDate().equals(targetDate)) {
                        continue;
                    }
                } catch (Exception e) {
                    continue;
                }
            }

            // 4. 拼装返回数据（🆕 增加 imageUrl 和 confidence）
            Map<String, Object> item = new HashMap<>();
            item.put("id", fav.getId());
            item.put("userId", fav.getUserId());
            item.put("recordId", fav.getRecordId());
            item.put("createdAt", fav.getCreatedAt() != null ? fav.getCreatedAt().toString().replace("T", " ") : "");

            userOpt.ifPresent(user -> item.put("username", user.getUsername()));

            if (recordOpt.isPresent()) {
                RecognitionRecord record = recordOpt.get();
                item.put("itemName", record.getItemName());
                item.put("category", record.getCategory() != null ? record.getCategory() : "-");
                // 🆕 新增图片URL
                item.put("imageUrl", record.getImageUrl() != null ? record.getImageUrl() : "");
                // 🆕 新增置信度
                item.put("confidence", record.getConfidence() != null ? record.getConfidence() : 0);
            } else {
                item.put("itemName", "已删除");
                item.put("category", "-");
                item.put("imageUrl", "");
                item.put("confidence", 0);
            }

            dataList.add(item);
        }

        PageImpl<Map<String, Object>> resultPage = new PageImpl<>(dataList, pageable, favoritePage.getTotalElements());
        return Result.success(resultPage);
    }

    @PostMapping("/favorites/share")
    public Result<String> shareFavorites(@RequestBody ShareRequest request) {
        log.info("管理员分享收藏请求，记录数: {}", request.getIds().size());

        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User admin = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("管理员未找到"));

            if (admin.getEmail() == null || admin.getEmail().trim().isEmpty()) {
                return Result.error("请先绑定邮箱");
            }

            List<Favorite> favorites = favoriteRepository.findAllById(request.getIds());
            emailService.sendAdminShareFavoritesEmail(admin.getEmail(), admin.getUsername(), favorites);

            return Result.success("分享成功，邮件已发送至 " + admin.getEmail());

        } catch (Exception e) {
            log.error("管理员分享收藏失败: {}", e.getMessage());
            return Result.error("分享失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/favorites/batch")
    public Result<String> batchDeleteFavorites(@RequestBody ShareRequest request) {
        if (request.getIds() == null || request.getIds().isEmpty()) {
            return Result.error("请选择要删除的收藏");
        }
        favoriteRepository.deleteAllByIdInBatch(request.getIds());
        return Result.success("成功删除 " + request.getIds().size() + " 条收藏记录");
    }

    @DeleteMapping("/favorites/{id}")
    public Result<String> deleteFavorite(@PathVariable Long id) {
        favoriteRepository.deleteById(id);
        return Result.success("收藏删除成功");
    }

    @GetMapping("/favorites/export")
    public ResponseEntity<byte[]> exportFavorites(
            @RequestParam(required = false) String ids,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String date,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder) {

        try {
            List<Map<String, Object>> favoriteList = new ArrayList<>();

            if (ids != null && !ids.trim().isEmpty()) {
                String[] idArray = ids.split(",");
                List<Long> idList = new ArrayList<>();
                for (String s : idArray) {
                    try {
                        idList.add(Long.parseLong(s.trim()));
                    } catch (NumberFormatException ignored) {}
                }
                if (!idList.isEmpty()) {
                    List<Favorite> favs = favoriteRepository.findAllById(idList);
                    for (Favorite f : favs) {
                        Map<String, Object> item = new HashMap<>();
                        item.put("id", f.getId());
                        item.put("userId", f.getUserId());
                        item.put("createdAt", f.getCreatedAt() != null ? f.getCreatedAt().toString().replace("T", " ") : "");
                        userRepository.findById(f.getUserId()).ifPresent(u -> item.put("username", u.getUsername()));
                        recordRepository.findById(f.getRecordId()).ifPresent(r -> {
                            item.put("itemName", r.getItemName());
                            item.put("category", r.getCategory());
                            item.put("imageUrl", r.getImageUrl() != null ? r.getImageUrl() : "");
                            item.put("confidence", r.getConfidence() != null ? r.getConfidence() : 0);
                        });
                        favoriteList.add(item);
                    }
                }
            } else {
                Pageable pageable = PageRequest.of(0, 10000,
                        "asc".equalsIgnoreCase(sortOrder) ? Sort.by(Sort.Direction.ASC, "createdAt") : Sort.by(Sort.Direction.DESC, "createdAt"));

                Page<Favorite> favPage = favoriteRepository.findAll(pageable);
                for (Favorite f : favPage.getContent()) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", f.getId());
                    item.put("userId", f.getUserId());
                    item.put("createdAt", f.getCreatedAt() != null ? f.getCreatedAt().toString().replace("T", " ") : "");
                    userRepository.findById(f.getUserId()).ifPresent(u -> item.put("username", u.getUsername()));
                    recordRepository.findById(f.getRecordId()).ifPresent(r -> {
                        item.put("itemName", r.getItemName());
                        item.put("category", r.getCategory());
                        item.put("imageUrl", r.getImageUrl() != null ? r.getImageUrl() : "");
                        item.put("confidence", r.getConfidence() != null ? r.getConfidence() : 0);
                    });
                    favoriteList.add(item);
                }
            }

            StringBuilder csv = new StringBuilder();
            csv.append("\uFEFF");
            csv.append("收藏ID,用户ID,用户名,物品名称,分类,置信度,收藏时间\n");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (Map<String, Object> item : favoriteList) {
                csv.append(item.get("id")).append(",");
                csv.append(item.get("userId")).append(",");
                csv.append(item.getOrDefault("username", "")).append(",");
                csv.append(item.getOrDefault("itemName", "")).append(",");
                csv.append(item.getOrDefault("category", "")).append(",");
                // 🆕 导出置信度
                Object confidence = item.get("confidence");
                if (confidence instanceof Number) {
                    csv.append(String.format("%.2f%%", ((Number) confidence).doubleValue() * 100));
                } else {
                    csv.append("");
                }
                csv.append(",");
                Object timeObj = item.get("createdAt");
                String timeStr = timeObj != null ? timeObj.toString() : "";
                csv.append(timeStr).append("\n");
            }

            byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
            headers.setContentDispositionFormData("attachment", "收藏管理_" + LocalDate.now() + ".csv");

            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("导出收藏CSV失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/favorites/stats")
    public Result<Map<String, Object>> getFavoriteStats() {
        long totalFavorites = favoriteRepository.count();
        return Result.success(Map.of("totalFavorites", totalFavorites));
    }

    // ========================================================================
    // 知识库管理
    // ========================================================================

    @GetMapping("/knowledge/list")
    public Result<List<KnowledgeBase>> getKnowledgeList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "desc") String order) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            if ("asc".equalsIgnoreCase(order)) {
                return Result.success(knowledgeBaseService.searchAsc(keyword));
            }
            return Result.success(knowledgeBaseService.search(keyword));
        }
        if ("asc".equalsIgnoreCase(order)) {
            return Result.success(knowledgeBaseService.findAllAsc());
        }
        return Result.success(knowledgeBaseService.findAll());
    }

    @PostMapping("/knowledge/add")
    public Result<KnowledgeBase> addKnowledge(@RequestBody KnowledgeBase knowledge) {
        knowledge.setId(null);
        knowledge.setCreatedAt(LocalDateTime.now());
        KnowledgeBase saved = knowledgeBaseService.save(knowledge);
        return Result.success(saved);
    }

    @PutMapping("/knowledge/update")
    public Result<KnowledgeBase> updateKnowledge(@RequestBody KnowledgeBase knowledge) {
        if (knowledge.getId() == null) {
            return Result.error("ID不能为空");
        }
        KnowledgeBase updated = knowledgeBaseService.save(knowledge);
        return Result.success(updated);
    }

    @DeleteMapping("/knowledge/{id}")
    public Result<String> deleteKnowledge(@PathVariable Long id) {
        knowledgeBaseService.delete(id);
        return Result.success("知识条目删除成功");
    }

    // ---- 新增批量操作接口 ----

    @DeleteMapping("/knowledge/batch")
    public Result<String> batchDeleteKnowledge(@RequestBody ShareRequest request) {
        if (request.getIds() == null || request.getIds().isEmpty()) {
            return Result.error("请选择要删除的知识条目");
        }
        knowledgeBaseService.deleteAllById(request.getIds());
        return Result.success("成功删除 " + request.getIds().size() + " 条知识条目");
    }

    @PostMapping("/knowledge/share")
    public Result<String> shareKnowledge(@RequestBody ShareRequest request) {
        log.info("管理员分享知识条目请求，记录数: {}", request.getIds().size());

        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User admin = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("管理员未找到"));

            if (admin.getEmail() == null || admin.getEmail().trim().isEmpty()) {
                return Result.error("请先绑定邮箱");
            }

            List<KnowledgeBase> knowledgeList = knowledgeBaseService.findAllById(request.getIds());
            emailService.sendAdminShareKnowledgeEmail(admin.getEmail(), admin.getUsername(), knowledgeList);

            return Result.success("分享成功，邮件已发送至 " + admin.getEmail());

        } catch (Exception e) {
            log.error("管理员分享知识条目失败: {}", e.getMessage());
            return Result.error("分享失败: " + e.getMessage());
        }
    }

    @GetMapping("/knowledge/export")
    public ResponseEntity<byte[]> exportKnowledge(
            @RequestParam(required = false) String ids,
            @RequestParam(required = false) String keyword) {

        try {
            List<KnowledgeBase> list = new ArrayList<>();

            if (ids != null && !ids.trim().isEmpty()) {
                String[] idArray = ids.split(",");
                List<Long> idList = new ArrayList<>();
                for (String s : idArray) {
                    try {
                        idList.add(Long.parseLong(s.trim()));
                    } catch (NumberFormatException ignored) {}
                }
                if (!idList.isEmpty()) {
                    list = knowledgeBaseService.findAllById(idList);
                }
            } else {
                if (keyword != null && !keyword.trim().isEmpty()) {
                    list = knowledgeBaseService.search(keyword);
                } else {
                    list = knowledgeBaseService.findAll();
                }
            }

            StringBuilder csv = new StringBuilder();
            csv.append("\uFEFF");
            csv.append("ID,分类,物品名称,分类说明,投放建议,创建时间\n");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (KnowledgeBase item : list) {
                csv.append(item.getId()).append(",");
                csv.append(item.getCategory() != null ? item.getCategory() : "").append(",");
                csv.append(item.getName() != null ? item.getName() : "").append(",");
                csv.append(item.getDescription() != null ? item.getDescription() : "").append(",");
                csv.append(item.getSuggestion() != null ? item.getSuggestion() : "").append(",");

                String timeStr = "";
                if (item.getCreatedAt() != null) {
                    timeStr = formatter.format(item.getCreatedAt());
                }
                csv.append(timeStr).append("\n");
            }

            byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
            headers.setContentDispositionFormData("attachment", "知识库_" + LocalDate.now() + ".csv");

            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("导出知识库CSV失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/knowledge/batch-add")
    public Result<List<KnowledgeBase>> batchAddKnowledge(@RequestBody List<KnowledgeBase> list) {
        if (list == null || list.isEmpty()) {
            return Result.error("请至少添加一条知识条目");
        }
        for (KnowledgeBase item : list) {
            if (item.getCategory() == null || item.getCategory().trim().isEmpty()) {
                return Result.error("分类不能为空");
            }
            if (item.getName() == null || item.getName().trim().isEmpty()) {
                return Result.error("物品名称不能为空");
            }
        }
        List<KnowledgeBase> saved = knowledgeBaseService.saveAll(list);
        return Result.success(saved);
    }
}