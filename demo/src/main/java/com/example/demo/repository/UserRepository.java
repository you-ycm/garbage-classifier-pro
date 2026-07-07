package com.example.demo.repository;

import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    // 🟢 【新增】模糊搜索用户名 (忽略大小写)，用于管理员查找用户记录
    List<User> findByUsernameContainingIgnoreCase(String username);
}