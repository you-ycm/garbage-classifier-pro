package com.example.demo.service;

import com.example.demo.entity.Favorite;
import com.example.demo.entity.KnowledgeBase;
import com.example.demo.entity.RecognitionRecord;
import com.example.demo.entity.User;
import com.example.demo.repository.RecognitionRecordRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    private final RecognitionRecordRepository recordRepository;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * 发送邮箱验证邮件
     */
    public void sendVerificationEmail(String toEmail, String username, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("垃圾分类助手 - 邮箱验证");

            String content = "<html><body>" +
                    "<h2>🌍 垃圾分类助手</h2>" +
                    "<p>您好，" + username + "！</p>" +
                    "<p>感谢您注册垃圾分类助手，请点击下方链接完成邮箱验证：</p>" +
                    "<a href='http://192.168.58.128:8080/api/auth/verify?token=" + token + "'>点击验证邮箱</a>" +
                    "<p>如果链接无法点击，请复制以下地址到浏览器打开：</p>" +
                    "<p>http://192.168.58.128:8080/api/auth/verify?token=" + token + "</p>" +
                    "<p>此链接有效期为 24 小时。</p>" +
                    "<p>垃圾分类助手，共建美好环境 🌍</p>" +
                    "</body></html>";

            helper.setText(content, true);
            mailSender.send(message);
            log.info("验证邮件已发送至: {}", toEmail);
        } catch (Exception e) {
            log.error("发送验证邮件失败: {}", e.getMessage());
            throw new RuntimeException("邮件发送失败: " + e.getMessage());
        }
    }

    /**
     * 发送重置密码邮件
     */
    public void sendResetPasswordEmail(String toEmail, String newPassword) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("垃圾分类助手 - 密码重置");

            String content = "<html><body>" +
                    "<h2>🌍 垃圾分类助手</h2>" +
                    "<p>您好！</p>" +
                    "<p>您的密码已重置成功，新密码为：</p>" +
                    "<h3 style='color: #409EFF;'>" + newPassword + "</h3>" +
                    "<p>请使用新密码登录后，及时修改为您习惯的密码。</p>" +
                    "<p>垃圾分类助手，共建美好环境 🌍</p>" +
                    "</body></html>";

            helper.setText(content, true);
            mailSender.send(message);
            log.info("重置密码邮件已发送至: {}", toEmail);
        } catch (Exception e) {
            log.error("发送重置密码邮件失败: {}", e.getMessage());
            throw new RuntimeException("邮件发送失败: " + e.getMessage());
        }
    }

    /**
     * 发送识别记录分享邮件（普通用户）
     */
    public void sendShareRecordsEmail(String toEmail, String username, List<RecognitionRecord> records) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("垃圾分类助手 - 识别记录分享");

            StringBuilder content = new StringBuilder();
            content.append("<html><body>");
            content.append("<h2>🌍 垃圾分类助手</h2>");
            content.append("<p>您好，").append(username).append("！</p>");
            content.append("<p>您请求分享的识别记录如下：</p>");
            content.append("<table border='1' cellpadding='5' style='border-collapse: collapse; width: 100%;'>");
            content.append("<tr style='background-color: #f2f2f2;'>");
            content.append("<th>物品名称</th><th>分类</th><th>置信度</th><th>识别时间</th>");
            content.append("</tr>");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (RecognitionRecord record : records) {
                content.append("<tr>");
                content.append("<td>").append(record.getItemName()).append("</td>");
                content.append("<td>").append(record.getCategory()).append("</td>");
                content.append("<td>").append(String.format("%.2f%%", record.getConfidence() * 100)).append("</td>");
                content.append("<td>").append(record.getCreatedAt() != null ? formatter.format(record.getCreatedAt()) : "").append("</td>");
                content.append("</tr>");
            }

            content.append("</table>");
            content.append("<p>垃圾分类助手，共建美好环境 🌍</p>");
            content.append("</body></html>");

            helper.setText(content.toString(), true);
            mailSender.send(message);
            log.info("识别记录分享邮件已发送至: {}", toEmail);
        } catch (Exception e) {
            log.error("发送分享邮件失败: {}", e.getMessage());
            throw new RuntimeException("邮件发送失败: " + e.getMessage());
        }
    }

    /**
     * 发送管理员识别记录分享邮件
     */
    public void sendAdminShareEmail(String toEmail, String username, List<RecognitionRecord> records) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("管理员后台 - 识别记录分享");

            StringBuilder content = new StringBuilder();
            content.append("<html><body>");
            content.append("<h2>⚙️ 管理员后台</h2>");
            content.append("<p>您好，管理员 ").append(username).append("！</p>");
            content.append("<p>您请求分享的识别记录如下：</p>");
            content.append("<table border='1' cellpadding='5' style='border-collapse: collapse; width: 100%;'>");
            content.append("<tr style='background-color: #f2f2f2;'>");
            content.append("<th>用户名称</th><th>物品名称</th><th>分类</th><th>置信度</th><th>识别时间</th>");
            content.append("</tr>");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (RecognitionRecord record : records) {
                String usernameRecord = userRepository.findById(record.getUserId())
                        .map(User::getUsername)
                        .orElse("未知用户");

                content.append("<tr>");
                content.append("<td>").append(usernameRecord).append("</td>");
                content.append("<td>").append(record.getItemName()).append("</td>");
                content.append("<td>").append(record.getCategory()).append("</td>");
                content.append("<td>").append(String.format("%.2f%%", record.getConfidence() * 100)).append("</td>");
                content.append("<td>").append(record.getCreatedAt() != null ? formatter.format(record.getCreatedAt()) : "").append("</td>");
                content.append("</tr>");
            }

            content.append("</table>");
            content.append("<p>垃圾分类助手，共建美好环境 🌍</p>");
            content.append("</body></html>");

            helper.setText(content.toString(), true);
            mailSender.send(message);
            log.info("管理员识别记录分享邮件已发送至: {}", toEmail);
        } catch (Exception e) {
            log.error("发送管理员分享邮件失败: {}", e.getMessage());
            throw new RuntimeException("邮件发送失败: " + e.getMessage());
        }
    }

    /**
     * 发送管理员收藏列表分享邮件
     */
    public void sendAdminShareFavoritesEmail(String toEmail, String username, List<Favorite> favorites) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("管理员后台 - 收藏列表分享");

            StringBuilder content = new StringBuilder();
            content.append("<html><body>");
            content.append("<h2>⚙️ 管理员后台</h2>");
            content.append("<p>您好，管理员 ").append(username).append("！</p>");
            content.append("<p>您请求分享的收藏列表如下：</p>");
            content.append("<table border='1' cellpadding='5' style='border-collapse: collapse; width: 100%;'>");
            content.append("<tr style='background-color: #f2f2f2;'>");
            content.append("<th>用户名</th><th>物品名称</th><th>分类</th><th>收藏时间</th>");
            content.append("</tr>");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (Favorite fav : favorites) {
                String userName = userRepository.findById(fav.getUserId())
                        .map(User::getUsername)
                        .orElse("未知用户");

                String itemName = "已删除";
                String category = "已删除";
                Optional<RecognitionRecord> recordOpt = recordRepository.findById(fav.getRecordId());
                if (recordOpt.isPresent()) {
                    itemName = recordOpt.get().getItemName();
                    category = recordOpt.get().getCategory();
                }

                content.append("<tr>");
                content.append("<td>").append(userName).append("</td>");
                content.append("<td>").append(itemName).append("</td>");
                content.append("<td>").append(category).append("</td>");
                content.append("<td>").append(fav.getCreatedAt() != null ? formatter.format(fav.getCreatedAt()) : "").append("</td>");
                content.append("</tr>");
            }

            content.append("</table>");
            content.append("<p>垃圾分类助手，共建美好环境 🌍</p>");
            content.append("</body></html>");

            helper.setText(content.toString(), true);
            mailSender.send(message);
            log.info("管理员收藏列表分享邮件已发送至: {}", toEmail);
        } catch (Exception e) {
            log.error("发送收藏列表分享邮件失败: {}", e.getMessage());
            throw new RuntimeException("邮件发送失败: " + e.getMessage());
        }
    }

    // ========================================================================
    // 新增：知识库分享邮件
    // ========================================================================

    /**
     * 发送管理员知识库分享邮件
     */
    public void sendAdminShareKnowledgeEmail(String toEmail, String username, List<KnowledgeBase> knowledgeList) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("管理员后台 - 垃圾分类知识库分享");

            StringBuilder content = new StringBuilder();
            content.append("<html><body>");
            content.append("<h2>📚 垃圾分类知识库分享</h2>");
            content.append("<p>您好，管理员 <b>").append(username).append("</b>！</p>");
            content.append("<p>您请求分享的知识库条目如下：</p>");
            content.append("<table border='1' cellpadding='5' style='border-collapse: collapse; width: 100%;'>");
            content.append("<tr style='background-color: #f2f2f2;'>");
            content.append("<th>ID</th><th>分类</th><th>物品名称</th><th>分类说明</th><th>投放建议</th><th>创建时间</th>");
            content.append("</tr>");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (KnowledgeBase item : knowledgeList) {
                content.append("<tr>");
                content.append("<td>").append(item.getId()).append("</td>");
                content.append("<td>").append(item.getCategory() != null ? item.getCategory() : "").append("</td>");
                content.append("<td>").append(item.getName() != null ? item.getName() : "").append("</td>");
                content.append("<td>").append(item.getDescription() != null ? item.getDescription() : "").append("</td>");
                content.append("<td>").append(item.getSuggestion() != null ? item.getSuggestion() : "").append("</td>");
                content.append("<td>").append(item.getCreatedAt() != null ? formatter.format(item.getCreatedAt()) : "").append("</td>");
                content.append("</tr>");
            }

            content.append("</table>");
            content.append("<p>共 <b>").append(knowledgeList.size()).append("</b> 条记录。</p>");
            content.append("<p>垃圾分类助手，共建美好环境 🌍</p>");
            content.append("</body></html>");

            helper.setText(content.toString(), true);
            mailSender.send(message);
            log.info("管理员知识库分享邮件已发送至: {}", toEmail);
        } catch (Exception e) {
            log.error("发送知识库分享邮件失败: {}", e.getMessage());
            throw new RuntimeException("邮件发送失败: " + e.getMessage());
        }
    }
}