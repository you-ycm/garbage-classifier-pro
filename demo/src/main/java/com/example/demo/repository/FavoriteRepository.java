package com.example.demo.repository;

import com.example.demo.entity.Favorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    Page<Favorite> findByUserId(Long userId, Pageable pageable);

    Optional<Favorite> findByUserIdAndRecordId(Long userId, Long recordId);

    void deleteByUserIdAndRecordId(Long userId, Long recordId);

    boolean existsByUserIdAndRecordId(Long userId, Long recordId);

    List<Favorite> findByUserId(Long userId);

    // 🆕 批量查询：根据用户ID和记录ID列表查询收藏
    @Query("SELECT f FROM Favorite f WHERE f.userId = :userId AND f.recordId IN :recordIds")
    List<Favorite> findByUserIdAndRecordIdIn(@Param("userId") Long userId, @Param("recordIds") List<Long> recordIds);

    // 检查用户是否收藏了某条记录（批量）
    @Query("SELECT f.recordId FROM Favorite f WHERE f.userId = :userId AND f.recordId IN :recordIds")
    List<Long> findFavoritedRecordIds(@Param("userId") Long userId, @Param("recordIds") List<Long> recordIds);

    // ===== 管理员查询 =====
    Page<Favorite> findAll(Pageable pageable);

    @Query("SELECT f FROM Favorite f JOIN RecognitionRecord r ON f.recordId = r.id WHERE r.itemName LIKE %:keyword%")
    Page<Favorite> findByItemNameContaining(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT f FROM Favorite f WHERE f.userId IN (SELECT u.id FROM User u WHERE u.username LIKE %:username%)")
    Page<Favorite> findByUsernameContaining(@Param("username") String username, Pageable pageable);

    long count();
}