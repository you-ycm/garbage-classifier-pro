package com.example.demo.service;

import com.example.demo.entity.OperationLog;
import com.example.demo.entity.User;
import com.example.demo.repository.OperationLogRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final OperationLogRepository operationLogRepository;
    private final UserRepository userRepository;

    /**
     * 记录操作日志
     */
    public void logOperation(String username, String operation, String detail, HttpServletRequest request) {
        try {
            User user = userRepository.findByUsername(username).orElse(null);
            if (user == null) {
                log.warn("记录操作日志失败: 用户不存在 - {}", username);
                return;
            }

            OperationLog log = new OperationLog();
            log.setUserId(user.getId());
            log.setUsername(username);
            log.setOperation(operation);
            log.setDetail(detail);

            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            log.setIp(ip);

            operationLogRepository.save(log);
        } catch (Exception e) {
            log.error("记录操作日志失败: {}", e.getMessage());
        }
    }

    /**
     * 多条件搜索操作日志
     */
    public Page<OperationLog> getLogsWithSearch(
            String username, String operation, String ip, String date, String sortOrder, int page, int size) {

        Sort sort = "asc".equalsIgnoreCase(sortOrder) ?
                Sort.by(Sort.Direction.ASC, "createdAt") :
                Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(page - 1, size, sort);

        LocalDateTime startTime = null;
        LocalDateTime endTime = null;
        if (date != null && !date.isEmpty()) {
            LocalDate targetDate = LocalDate.parse(date);
            startTime = targetDate.atStartOfDay();
            endTime = targetDate.atTime(LocalTime.MAX);
        }

        return operationLogRepository.searchLogs(
                username, operation, ip, startTime, endTime, pageable);
    }

    public Page<OperationLog> getLogs(int page, int size) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(page - 1, size, sort);
        return operationLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public Page<OperationLog> searchLogs(String keyword, int page, int size) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(page - 1, size, sort);
        return operationLogRepository.findByUsernameContainingIgnoreCase(keyword, pageable);
    }

    public List<OperationLog> getRecentLogs() {
        return operationLogRepository.findTop10ByOrderByCreatedAtDesc();
    }

    public List<OperationLog> getLogsByIds(List<Long> ids) {
        return operationLogRepository.findAllById(ids);
    }

    // 🟢【新增】批量删除操作日志
    public void batchDeleteLogs(List<Long> ids) {
        operationLogRepository.deleteAllByIdInBatch(ids);
        log.info("操作日志批量删除成功: {} 条", ids.size());
    }
}