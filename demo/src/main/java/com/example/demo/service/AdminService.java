package com.example.demo.service;

import com.example.demo.dto.CategoryStatsDTO;
import com.example.demo.dto.PageResult;
import com.example.demo.dto.RecordWithUserDTO;
import com.example.demo.entity.RecognitionRecord;
import com.example.demo.entity.User;
import com.example.demo.repository.RecognitionRecordRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final RecognitionRecordRepository recordRepository;
    private final UserRepository userRepository;

    public PageResult<RecordWithUserDTO> getRecordsWithPage(
            String category, String keyword, String username, String date, String sortOrder, int page, int size) {

        Sort sort = "asc".equalsIgnoreCase(sortOrder) ?
                Sort.by(Sort.Direction.ASC, "createdAt") :
                Sort.by(Sort.Direction.DESC, "createdAt");

        Pageable pageable = PageRequest.of(page - 1, size, sort);

        List<Long> targetUserIds = null;
        if (username != null && !username.trim().isEmpty()) {
            List<User> matchedUsers = userRepository.findByUsernameContainingIgnoreCase(username.trim());
            if (!matchedUsers.isEmpty()) {
                targetUserIds = matchedUsers.stream()
                        .map(User::getId)
                        .collect(Collectors.toList());
            } else {
                return new PageResult<>(new ArrayList<>(), 0, page, size);
            }
        }

        Page<RecognitionRecord> pageResult;

        if (date != null && !date.isEmpty()) {
            LocalDate targetDate = LocalDate.parse(date);
            LocalDateTime startOfDay = targetDate.atStartOfDay();
            LocalDateTime endOfDay = targetDate.atTime(LocalTime.MAX);

            if (targetUserIds != null) {
                pageResult = recordRepository.findByUserIdInAndCreatedAtBetween(targetUserIds, startOfDay, endOfDay, pageable);
            } else {
                pageResult = recordRepository.findByCreatedAtBetween(startOfDay, endOfDay, pageable);
            }
        } else if (category != null && !category.trim().isEmpty()) {
            if (targetUserIds != null) {
                pageResult = recordRepository.findByUserIdInAndCategory(targetUserIds, category.trim(), pageable);
            } else {
                pageResult = recordRepository.findByCategory(category.trim(), pageable);
            }
        } else if (keyword != null && !keyword.trim().isEmpty()) {
            if (targetUserIds != null) {
                pageResult = recordRepository.findByUserIdInAndItemNameContainingIgnoreCase(
                        targetUserIds, keyword.trim(), pageable);
            } else {
                pageResult = recordRepository.findByItemNameContainingIgnoreCase(keyword.trim(), pageable);
            }
        } else if (targetUserIds != null) {
            pageResult = recordRepository.findByUserIdIn(targetUserIds, pageable);
        } else {
            pageResult = recordRepository.findAll(pageable);
        }

        List<RecordWithUserDTO> dtoList = pageResult.getContent().stream()
                .map(record -> {
                    String usernameRes = userRepository.findById(record.getUserId())
                            .map(User::getUsername)
                            .orElse("未知用户");
                    return new RecordWithUserDTO(record, usernameRes);
                })
                .collect(Collectors.toList());

        return new PageResult<>(dtoList, pageResult.getTotalElements(), page, size);
    }

    public void batchDeleteRecords(List<Long> ids) {
        recordRepository.deleteAllById(ids);
        log.info("管理员批量删除记录: {} 条", ids.size());
    }

    public List<RecognitionRecord> getRecordsByIds(List<Long> ids) {
        return recordRepository.findAllById(ids);
    }

    // ===== 分类统计 =====
    public List<CategoryStatsDTO> getCategoryStats() {
        List<RecognitionRecord> allRecords = recordRepository.findAll();

        Map<String, Long> categoryCountMap = allRecords.stream()
                .collect(Collectors.groupingBy(
                        record -> record.getCategory() != null ? record.getCategory() : "未知",
                        Collectors.counting()
                ));

        List<CategoryStatsDTO> result = new ArrayList<>();
        String[] colors = {"#409EFF", "#E6A23C", "#67C23A", "#F56C6C", "#909399"};
        String[] categories = {"可回收物", "有害垃圾", "厨余垃圾", "其他垃圾", "未知"};

        int index = 0;
        for (String category : categories) {
            long count = categoryCountMap.getOrDefault(category, 0L);
            if (count > 0 || category.equals("未知")) {
                CategoryStatsDTO dto = new CategoryStatsDTO();
                dto.setName(category);
                dto.setValue(count);
                dto.setColor(colors[index % colors.length]);
                result.add(dto);
                index++;
            }
        }

        return result;
    }

    public List<Map<String, Object>> getTrendStats() {
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        List<RecognitionRecord> allRecords = recordRepository.findAll();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", date.toString());

            long count = allRecords.stream()
                    .filter(record -> {
                        if (record.getCreatedAt() == null) return false;
                        return record.getCreatedAt().toLocalDate().equals(date);
                    })
                    .count();

            dayData.put("count", count);
            result.add(dayData);
        }

        return result;
    }
}