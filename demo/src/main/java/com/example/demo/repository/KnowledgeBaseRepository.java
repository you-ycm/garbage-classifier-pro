package com.example.demo.repository;

import com.example.demo.entity.KnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long> {
    // 按分类和物品名称模糊搜索，用于前台搜索
    List<KnowledgeBase> findByCategoryContainingOrNameContaining(String category, String name);

    // 按排序顺序展示
    List<KnowledgeBase> findAllByOrderBySortOrderAsc();
}