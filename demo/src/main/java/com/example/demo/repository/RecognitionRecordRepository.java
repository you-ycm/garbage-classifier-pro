package com.example.demo.repository;

import com.example.demo.entity.RecognitionRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RecognitionRecordRepository extends JpaRepository<RecognitionRecord, Long> {

    // ===== 原有方法 =====
    List<RecognitionRecord> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<RecognitionRecord> findByUserIdAndCategoryOrderByCreatedAtDesc(Long userId, String category);

    List<RecognitionRecord> findTop5ByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<RecognitionRecord> findByCategory(String category);

    List<RecognitionRecord> findByUserId(Long userId, Sort sort);

    List<RecognitionRecord> findByUserIdAndCategory(Long userId, String category, Sort sort);

    List<RecognitionRecord> findByCategory(String category, Sort sort);

    // ===== 用户分页 =====
    Page<RecognitionRecord> findByUserId(Long userId, Pageable pageable);

    Page<RecognitionRecord> findByUserIdAndCategory(Long userId, String category, Pageable pageable);

    Page<RecognitionRecord> findByUserIdAndItemNameContainingIgnoreCase(Long userId, String itemName, Pageable pageable);

    Page<RecognitionRecord> findByUserIdAndCategoryAndItemNameContainingIgnoreCase(
            Long userId, String category, String itemName, Pageable pageable);

    // ===== 管理员分页 =====
    Page<RecognitionRecord> findAll(Pageable pageable);

    Page<RecognitionRecord> findByCategory(String category, Pageable pageable);

    Page<RecognitionRecord> findByItemNameContainingIgnoreCase(String itemName, Pageable pageable);

    Page<RecognitionRecord> findByCategoryAndItemNameContainingIgnoreCase(String category, String itemName, Pageable pageable);

    // ===== 使用 @Query 实现日期范围查询 =====
    @Query("SELECT r FROM RecognitionRecord r WHERE r.userId = :userId AND r.createdAt BETWEEN :startDate AND :endDate")
    Page<RecognitionRecord> findRecordsByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    @Query("SELECT r FROM RecognitionRecord r WHERE r.userId = :userId AND r.category = :category AND r.createdAt BETWEEN :startDate AND :endDate")
    Page<RecognitionRecord> findRecordsByUserIdAndCategoryAndDateRange(
            @Param("userId") Long userId,
            @Param("category") String category,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    @Query("SELECT r FROM RecognitionRecord r WHERE r.createdAt BETWEEN :startDate AND :endDate")
    Page<RecognitionRecord> findByCreatedAtBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    // ===== 新增：管理员按分类和日期范围查询 =====
    @Query("SELECT r FROM RecognitionRecord r WHERE r.category = :category AND r.createdAt BETWEEN :startDate AND :endDate")
    Page<RecognitionRecord> findByCategoryAndDateRange(
            @Param("category") String category,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    // ============================================================
    // 🟢 【新增】以下 4 个方法专门用于后台管理员“按用户名/物品名混合搜索”功能
    // ============================================================

    // 1. 按「用户ID列表」直接查询记录
    Page<RecognitionRecord> findByUserIdIn(List<Long> userIds, Pageable pageable);

    // 2. 按「用户ID列表 + 物品名称」模糊查询
    Page<RecognitionRecord> findByUserIdInAndItemNameContainingIgnoreCase(List<Long> userIds, String itemName, Pageable pageable);

    // 3. 按「用户ID列表 + 分类」查询
    Page<RecognitionRecord> findByUserIdInAndCategory(List<Long> userIds, String category, Pageable pageable);

    // 4. 按「用户ID列表 + 日期区间」查询
    Page<RecognitionRecord> findByUserIdInAndCreatedAtBetween(List<Long> userIds, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
}