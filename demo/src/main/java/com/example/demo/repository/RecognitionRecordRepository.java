package com.example.demo.repository;
import com.example.demo.entity.RecognitionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecognitionRecordRepository extends JpaRepository<RecognitionRecord, Long> {
    List<RecognitionRecord> findAllByOrderByCreatedAtDesc();
    // 继承JPA自带方法即可
}