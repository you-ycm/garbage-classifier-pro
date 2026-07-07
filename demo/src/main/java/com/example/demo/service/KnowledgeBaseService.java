package com.example.demo.service;

import com.example.demo.entity.KnowledgeBase;
import com.example.demo.repository.KnowledgeBaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class KnowledgeBaseService {
    @Autowired
    private KnowledgeBaseRepository repository;

    // ---- 原有方法 ----

    /**
     * 获取所有知识条目，按 ID 降序排列（最新的在前，ID 从大到小）
     */
    public List<KnowledgeBase> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    /**
     * 获取所有知识条目，按 ID 升序排列（最早的在前，ID 从小到大）
     */
    public List<KnowledgeBase> findAllAsc() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    /**
     * 搜索知识条目，按 ID 降序排列（最新的在前）
     */
    public List<KnowledgeBase> search(String keyword) {
        List<KnowledgeBase> result = repository.findByCategoryContainingOrNameContaining(keyword, keyword);
        result.sort((a, b) -> b.getId().compareTo(a.getId()));
        return result;
    }

    /**
     * 搜索知识条目，按 ID 升序排列（最早的在前）
     */
    public List<KnowledgeBase> searchAsc(String keyword) {
        List<KnowledgeBase> result = repository.findByCategoryContainingOrNameContaining(keyword, keyword);
        result.sort((a, b) -> a.getId().compareTo(b.getId()));
        return result;
    }

    public KnowledgeBase save(KnowledgeBase knowledge) {
        if (knowledge.getId() == null) {
            knowledge.setCreatedAt(LocalDateTime.now());
        }
        return repository.save(knowledge);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    // ---- 新增方法 ----

    public List<KnowledgeBase> findAllById(List<Long> ids) {
        return repository.findAllById(ids);
    }

    public void deleteAllById(List<Long> ids) {
        repository.deleteAllById(ids);
    }

    public List<KnowledgeBase> saveAll(List<KnowledgeBase> list) {
        for (KnowledgeBase item : list) {
            if (item.getId() == null) {
                item.setCreatedAt(LocalDateTime.now());
            }
        }
        return repository.saveAll(list);
    }
}