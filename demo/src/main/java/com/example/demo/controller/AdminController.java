package com.example.demo.controller;

import com.example.demo.dto.Result;
import com.example.demo.entity.RecognitionRecord;
import com.example.demo.entity.User;
import com.example.demo.repository.RecognitionRecordRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") // 只有管理员能访问
public class AdminController {

    private final UserRepository userRepository;
    private final RecognitionRecordRepository recordRepository;

    // 1. 获取所有用户
    @GetMapping("/users")
    public Result<List<User>> getAllUsers() {
        return Result.success(userRepository.findAll());
    }

    // 2. 删除用户
    @DeleteMapping("/users/{id}")
    public Result<String> deleteUser(@PathVariable Long id) {
        userRepository.deleteById(id);
        return Result.success("用户删除成功");
    }

    // 3. 获取所有识别记录
    @GetMapping("/records")
    public Result<List<RecognitionRecord>> getAllRecords() {
        return Result.success(recordRepository.findAll());
    }

    // 4. 删除识别记录
    @DeleteMapping("/records/{id}")
    public Result<String> deleteRecord(@PathVariable Long id) {
        recordRepository.deleteById(id);
        return Result.success("记录删除成功");
    }

    // 5. 获取统计信息
    @GetMapping("/stats")
    public Result<StatsVO> getStats() {
        long userCount = userRepository.count();
        long recordCount = recordRepository.count();
        return Result.success(new StatsVO(userCount, recordCount));
    }
}

// 统计信息实体类
class StatsVO {
    public long userCount;
    public long recordCount;

    public StatsVO(long userCount, long recordCount) {
        this.userCount = userCount;
        this.recordCount = recordCount;
    }
}