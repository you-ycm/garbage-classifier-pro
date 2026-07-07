// ===== GarbageService.java（完整版） =====

package com.example.demo.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.demo.dto.BatchClassifyResponse;
import com.example.demo.dto.ClassifyResponse;
import com.example.demo.dto.PageResult;
import com.example.demo.entity.RecognitionRecord;
import com.example.demo.entity.User;
import com.example.demo.repository.RecognitionRecordRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GarbageService {

    private final BaiduApiService baiduApiService;
    private final RecognitionRecordRepository recordRepository;
    private final UserRepository userRepository;

    private static final String IMAGE_STORAGE_PATH = "/home/ycm/images/";

    // 压缩质量枚举
    public enum CompressionQuality {
        HIGH(90, "高"),
        MEDIUM(70, "中"),
        LOW(50, "低");

        public final int quality;
        public final String label;

        CompressionQuality(int quality, String label) {
            this.quality = quality;
            this.label = label;
        }
    }

    private static final java.util.Map<String, String> CATEGORY_MAP = new java.util.HashMap<>();

    static {
        String[] recyclable = {"纸", "报纸", "纸箱", "塑料瓶", "易拉罐", "玻璃瓶", "金属", "衣服", "矿泉水瓶", "饮料瓶", "苏打水"};
        String[] kitchen = {"香蕉", "苹果", "橙子", "蔬菜", "米饭", "面包", "鸡蛋", "果皮", "水果"};
        String[] hazardous = {"电池", "灯管", "药品", "温度计", "杀虫剂", "油漆"};
        String[] other = {"纸巾", "卫生纸", "一次性餐具", "塑料袋", "尘土", "烟蒂", "塑料袋"};

        for (String item : recyclable) CATEGORY_MAP.put(item, "可回收物");
        for (String item : kitchen) CATEGORY_MAP.put(item, "厨余垃圾");
        for (String item : hazardous) CATEGORY_MAP.put(item, "有害垃圾");
        for (String item : other) CATEGORY_MAP.put(item, "其他垃圾");
    }

    private String getCategory(String keyword) {
        for (java.util.Map.Entry<String, String> entry : CATEGORY_MAP.entrySet()) {
            if (keyword.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "其他垃圾";
    }

    private String saveImage(MultipartFile file, Long userId, String itemName) throws IOException {
        String userDir = IMAGE_STORAGE_PATH + userId;
        File dir = new File(userDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String timestamp = String.valueOf(System.currentTimeMillis());
        String safeItemName = itemName.replaceAll("[\\\\/:*?\"<>|]", "_");
        String filename = timestamp + "_" + safeItemName + ".jpg";
        File destFile = new File(dir, filename);

        file.transferTo(destFile);
        log.info("图片已保存: {}", destFile.getAbsolutePath());
        return "/api/images/" + userId + "/" + filename;
    }

    // ===== 图片压缩方法（支持质量参数） =====
    private String convertImageToJpgBase64(MultipartFile file, int quality) throws IOException {
        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        if (originalImage == null) {
            throw new RuntimeException("无法解析图片，请确保是JPG或PNG格式");
        }

        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        log.info("原始图片: {} x {}, 类型: {}", width, height, file.getContentType());

        int maxWidth = 1024;
        BufferedImage processedImage = originalImage;

        if (width > maxWidth) {
            int newWidth = maxWidth;
            int newHeight = (int) ((double) maxWidth / width * height);
            processedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = processedImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, newWidth, newHeight);
            g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
            g2d.dispose();
            log.info("压缩后: {} x {}", newWidth, newHeight);
        } else if (originalImage.getType() != BufferedImage.TYPE_INT_RGB) {
            BufferedImage rgbImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = rgbImage.createGraphics();
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, width, height);
            g2d.drawImage(originalImage, 0, 0, null);
            g2d.dispose();
            processedImage = rgbImage;
            log.info("转换RGB格式: {} x {}", width, height);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        javax.imageio.ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        javax.imageio.stream.ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
        writer.setOutput(ios);
        javax.imageio.IIOImage image = new javax.imageio.IIOImage(processedImage, null, null);
        javax.imageio.plugins.jpeg.JPEGImageWriteParam param = new javax.imageio.plugins.jpeg.JPEGImageWriteParam(writer.getLocale());
        param.setCompressionMode(javax.imageio.plugins.jpeg.JPEGImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality / 100.0f);
        writer.write(null, image, param);
        writer.dispose();
        ios.close();

        String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
        log.info("压缩质量: {}%, Base64长度: {} 字符", quality, base64.length());
        return base64;
    }

    // ===== 单张识别（支持压缩质量） =====
    public ClassifyResponse classifyImageWithQuality(MultipartFile file, String qualityStr) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("请选择要识别的图片");
        }

        int quality = 70;
        if (qualityStr != null) {
            switch (qualityStr.toLowerCase()) {
                case "high":
                    quality = 90;
                    break;
                case "low":
                    quality = 50;
                    break;
                default:
                    quality = 70;
                    break;
            }
        }

        String imageBase64 = convertImageToJpgBase64(file, quality);
        log.info("压缩质量: {}%, 图片大小: {} bytes", quality, imageBase64.length());

        try {
            JSONObject response = baiduApiService.classifyGarbage(imageBase64);
            log.info("百度API返回: {}", response.toJSONString());

            if (response.containsKey("result")) {
                JSONArray results = response.getJSONArray("result");
                if (results != null && !results.isEmpty()) {
                    JSONObject firstResult = results.getJSONObject(0);
                    String itemName = firstResult.getString("keyword");
                    Double confidence = firstResult.getDouble("score");
                    String category = getCategory(itemName);

                    log.info("识别成功: {} (置信度: {}%) -> {}", itemName, confidence * 100, category);

                    String username = SecurityContextHolder.getContext().getAuthentication().getName();
                    User user = userRepository.findByUsername(username)
                            .orElseThrow(() -> new RuntimeException("用户未找到"));

                    String imageUrl = saveImage(file, user.getId(), itemName);

                    RecognitionRecord record = new RecognitionRecord();
                    record.setItemName(itemName);
                    record.setCategory(category);
                    record.setConfidence(confidence);
                    record.setUserId(user.getId());
                    record.setImageUrl(imageUrl);
                    recordRepository.save(record);

                    return ClassifyResponse.builder()
                            .id(record.getId())
                            .itemName(itemName)
                            .category(category)
                            .confidence(confidence)
                            .build();
                }
            }
            throw new RuntimeException("未能识别出图片中的物体");
        } catch (Exception e) {
            log.error("识别异常", e);
            throw new RuntimeException("识别失败: " + e.getMessage());
        } finally {
            log.info("========================================");
        }
    }

    // ===== 批量识别 =====
    public BatchClassifyResponse classifyBatch(List<MultipartFile> files) {
        List<BatchClassifyResponse.ClassifyResult> results = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户未找到"));

        for (MultipartFile file : files) {
            try {
                String imageBase64 = convertImageToJpgBase64(file, 70);
                JSONObject response = baiduApiService.classifyGarbage(imageBase64);

                if (response.containsKey("result")) {
                    JSONArray resultArray = response.getJSONArray("result");
                    if (resultArray != null && !resultArray.isEmpty()) {
                        JSONObject firstResult = resultArray.getJSONObject(0);
                        String itemName = firstResult.getString("keyword");
                        Double confidence = firstResult.getDouble("score");
                        String category = getCategory(itemName);

                        // 保存记录
                        String imageUrl = saveImage(file, user.getId(), itemName);
                        RecognitionRecord record = new RecognitionRecord();
                        record.setItemName(itemName);
                        record.setCategory(category);
                        record.setConfidence(confidence);
                        record.setUserId(user.getId());
                        record.setImageUrl(imageUrl);
                        recordRepository.save(record);

                        results.add(BatchClassifyResponse.ClassifyResult.builder()
                                .fileName(file.getOriginalFilename())
                                .itemName(itemName)
                                .category(category)
                                .confidence(confidence)
                                .success(true)
                                .build());
                        successCount++;
                    } else {
                        results.add(BatchClassifyResponse.ClassifyResult.builder()
                                .fileName(file.getOriginalFilename())
                                .success(false)
                                .errorMsg("未能识别出图片中的物体")
                                .build());
                        failCount++;
                    }
                } else {
                    results.add(BatchClassifyResponse.ClassifyResult.builder()
                            .fileName(file.getOriginalFilename())
                            .success(false)
                            .errorMsg("识别失败")
                            .build());
                    failCount++;
                }
            } catch (Exception e) {
                log.error("批量识别异常: {}", e.getMessage());
                results.add(BatchClassifyResponse.ClassifyResult.builder()
                        .fileName(file.getOriginalFilename())
                        .success(false)
                        .errorMsg(e.getMessage())
                        .build());
                failCount++;
            }
        }

        return BatchClassifyResponse.builder()
                .total(files.size())
                .successCount(successCount)
                .failCount(failCount)
                .results(results)
                .build();
    }

    // ===== 原有分类方法（保持兼容） =====
    public ClassifyResponse classifyImage(MultipartFile file) throws IOException {
        return classifyImageWithQuality(file, "medium");
    }

    public List<RecognitionRecord> getHistory() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户未找到"));
        return recordRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    public List<RecognitionRecord> getHistoryByCategory(String category) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户未找到"));
        return recordRepository.findByUserIdAndCategoryOrderByCreatedAtDesc(user.getId(), category);
    }

    public List<RecognitionRecord> getRecentHistory(int limit) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户未找到"));
        return recordRepository.findTop5ByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(0, limit));
    }

    public List<RecognitionRecord> getRecordsByIds(List<Long> ids) {
        return recordRepository.findAllById(ids);
    }

    public PageResult<RecognitionRecord> getHistoryWithPage(
            String category, String sortOrder, String date, int page, int size) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户未找到"));

        Sort sort = "asc".equalsIgnoreCase(sortOrder) ?
                Sort.by(Sort.Direction.ASC, "createdAt") :
                Sort.by(Sort.Direction.DESC, "createdAt");

        Pageable pageable = PageRequest.of(page - 1, size, sort);

        Page<RecognitionRecord> pageResult;

        if (category != null && !category.trim().isEmpty() && date != null && !date.isEmpty()) {
            LocalDate targetDate = LocalDate.parse(date);
            LocalDateTime startOfDay = targetDate.atStartOfDay();
            LocalDateTime endOfDay = targetDate.atTime(LocalTime.MAX);
            pageResult = recordRepository.findRecordsByUserIdAndCategoryAndDateRange(
                    user.getId(), category.trim(), startOfDay, endOfDay, pageable);
        } else if (date != null && !date.isEmpty()) {
            LocalDate targetDate = LocalDate.parse(date);
            LocalDateTime startOfDay = targetDate.atStartOfDay();
            LocalDateTime endOfDay = targetDate.atTime(LocalTime.MAX);
            pageResult = recordRepository.findRecordsByUserIdAndDateRange(
                    user.getId(), startOfDay, endOfDay, pageable);
        } else if (category != null && !category.trim().isEmpty()) {
            pageResult = recordRepository.findByUserIdAndCategory(user.getId(), category.trim(), pageable);
        } else {
            pageResult = recordRepository.findByUserId(user.getId(), pageable);
        }

        return new PageResult<>(pageResult.getContent(), pageResult.getTotalElements(), page, size);
    }

    public PageResult<RecognitionRecord> searchHistory(
            String category, String keyword, String date, int page, int size) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户未找到"));

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(page - 1, size, sort);

        Page<RecognitionRecord> pageResult;

        if (category != null && !category.trim().isEmpty() && keyword != null && !keyword.trim().isEmpty()) {
            pageResult = recordRepository.findByUserIdAndCategoryAndItemNameContainingIgnoreCase(
                    user.getId(), category.trim(), keyword.trim(), pageable);
        } else if (keyword != null && !keyword.trim().isEmpty()) {
            pageResult = recordRepository.findByUserIdAndItemNameContainingIgnoreCase(
                    user.getId(), keyword.trim(), pageable);
        } else if (category != null && !category.trim().isEmpty()) {
            pageResult = recordRepository.findByUserIdAndCategory(user.getId(), category.trim(), pageable);
        } else {
            pageResult = recordRepository.findByUserId(user.getId(), pageable);
        }

        return new PageResult<>(pageResult.getContent(), pageResult.getTotalElements(), page, size);
    }

    public void batchDeleteRecords(List<Long> ids) {
        recordRepository.deleteAllById(ids);
    }

    public List<RecognitionRecord> exportHistory(String category, String keyword, String date, String sortOrder, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户未找到"));

        List<RecognitionRecord> records;

        if (category != null && !category.trim().isEmpty() && keyword != null && !keyword.trim().isEmpty()) {
            records = recordRepository.findByUserIdAndCategoryAndItemNameContainingIgnoreCase(
                    user.getId(), category.trim(), keyword.trim(), PageRequest.of(0, 10000)).getContent();
        } else if (keyword != null && !keyword.trim().isEmpty()) {
            records = recordRepository.findByUserIdAndItemNameContainingIgnoreCase(
                    user.getId(), keyword.trim(), PageRequest.of(0, 10000)).getContent();
        } else if (category != null && !category.trim().isEmpty()) {
            records = recordRepository.findByUserIdAndCategory(user.getId(), category.trim(),
                    Sort.by(Sort.Direction.DESC, "createdAt"));
        } else {
            records = recordRepository.findByUserId(user.getId(),
                    Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        if ("asc".equalsIgnoreCase(sortOrder)) {
            records.sort((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()));
        } else {
            records.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        }

        return records;
    }
}