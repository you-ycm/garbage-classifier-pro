package com.example.demo.controller;

import com.example.demo.dto.Result;
import com.example.demo.dto.ShareRequest;
import com.example.demo.entity.KnowledgeBase;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.EmailService;
import com.example.demo.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final UserRepository userRepository;
    private final EmailService emailService;

    // ===== 公开接口：获取知识库列表（无需登录） =====
    @GetMapping("/public/list")
    public List<KnowledgeBase> getPublicList(@RequestParam(required = false) String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return knowledgeBaseService.search(keyword);
        }
        return knowledgeBaseService.findAll();
    }

    // ===== 🆕 邮箱分享知识条目（需要登录） =====
    @PostMapping("/share")
    public Result<String> shareKnowledge(@RequestBody ShareRequest request) {
        List<Long> ids = request.getIds();
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要分享的知识条目");
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户未找到"));

        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            return Result.error("请先绑定邮箱");
        }

        List<KnowledgeBase> knowledgeList = knowledgeBaseService.findAllById(ids);
        if (knowledgeList.isEmpty()) {
            return Result.error("未找到要分享的知识条目");
        }

        try {
            emailService.sendAdminShareKnowledgeEmail(user.getEmail(), user.getUsername(), knowledgeList);
            log.info("知识库分享邮件已发送至: {}", user.getEmail());
            return Result.success("分享成功，邮件已发送至 " + user.getEmail());
        } catch (Exception e) {
            log.error("发送分享邮件失败: {}", e.getMessage());
            return Result.error("邮件发送失败: " + e.getMessage());
        }
    }

    // ===== 🆕 导出知识库 CSV（支持搜索/勾选，无需登录） =====
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportKnowledge(
            @RequestParam(required = false) String ids,
            @RequestParam(required = false) String keyword) {

        try {
            List<KnowledgeBase> list = new ArrayList<>();

            // 1. 如果传入了 ids，按 ID 列表导出（勾选导出）
            if (ids != null && !ids.trim().isEmpty()) {
                String[] idArray = ids.split(",");
                List<Long> idList = new ArrayList<>();
                for (String s : idArray) {
                    try {
                        idList.add(Long.parseLong(s.trim()));
                    } catch (NumberFormatException ignored) {}
                }
                if (!idList.isEmpty()) {
                    list = knowledgeBaseService.findAllById(idList);
                }
            } else {
                // 2. 否则按搜索条件导出
                if (keyword != null && !keyword.trim().isEmpty()) {
                    list = knowledgeBaseService.search(keyword);
                } else {
                    list = knowledgeBaseService.findAll();
                }
            }

            // 3. 构建 CSV
            StringBuilder csv = new StringBuilder();
            csv.append("\uFEFF"); // BOM for Excel
            csv.append("物品名称,分类,分类说明,投放建议\n");

            for (KnowledgeBase item : list) {
                csv.append(item.getName() != null ? item.getName() : "").append(",");
                csv.append(item.getCategory() != null ? item.getCategory() : "").append(",");
                csv.append(item.getDescription() != null ? item.getDescription() : "").append(",");
                csv.append(item.getSuggestion() != null ? item.getSuggestion() : "").append("\n");
            }

            byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
            headers.setContentDispositionFormData("attachment", "垃圾分类知识库_" + LocalDate.now() + ".csv");

            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("导出知识库CSV失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}