package com.example.demo.repository;

import com.example.demo.entity.OperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {

    // 原有的分页查询
    Page<OperationLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // 原有的最近查询
    List<OperationLog> findTop10ByOrderByCreatedAtDesc();

    // 原有的搜索
    Page<OperationLog> findByUsernameContainingIgnoreCase(String username, Pageable pageable);

    // 🟢【核心修复】使用原生 SQL 查询，避免 JPA 方法名解析失败
    @Query("SELECT l FROM OperationLog l WHERE " +
            "(:username IS NULL OR l.username LIKE %:username%) AND " +
            "(:operation IS NULL OR l.operation LIKE %:operation%) AND " +
            "(:ip IS NULL OR l.ip LIKE %:ip%) AND " +
            "(:startTime IS NULL OR l.createdAt >= :startTime) AND " +
            "(:endTime IS NULL OR l.createdAt <= :endTime)")
    Page<OperationLog> searchLogs(
            @Param("username") String username,
            @Param("operation") String operation,
            @Param("ip") String ip,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable
    );
}