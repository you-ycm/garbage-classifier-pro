package com.example.demo.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.demo.dto.ClassifyResponse;
import com.example.demo.entity.RecognitionRecord;
import com.example.demo.repository.RecognitionRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GarbageService {

    private final BaiduApiService baiduApiService;
    private final RecognitionRecordRepository recordRepository;

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

    /**
     * 将图片转换为JPG格式的Base64（解决PNG兼容性问题）
     */
    private String convertImageToJpgBase64(MultipartFile file) throws IOException {
        // 读取原始图片
        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        if (originalImage == null) {
            throw new RuntimeException("无法解析图片，请确保是JPG或PNG格式");
        }

        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        log.info("原始图片: {} x {}, 类型: {}", width, height, file.getContentType());

        // 压缩图片（最大宽度 1024px）
        int maxWidth = 1024;
        BufferedImage processedImage = originalImage;

        if (width > maxWidth) {
            int newWidth = maxWidth;
            int newHeight = (int) ((double) maxWidth / width * height);

            // 创建新的RGB图片（去除透明通道）
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
            // 如果不是RGB格式，转换为RGB（处理PNG透明背景）
            BufferedImage rgbImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = rgbImage.createGraphics();
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, width, height);
            g2d.drawImage(originalImage, 0, 0, null);
            g2d.dispose();
            processedImage = rgbImage;
            log.info("转换RGB格式: {} x {}", width, height);
        }

        // 压缩质量（循环压缩直到小于1MB）
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int quality = 85;
        boolean compressed = false;

        while (!compressed && quality >= 40) {
            baos.reset();

            // 使用JPEG压缩
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

            if (baos.size() < 1024 * 1024) { // 小于1MB
                compressed = true;
                log.info("压缩完成: 质量={}%, 大小={} bytes", quality, baos.size());
            } else {
                quality -= 10;
                log.info("图片太大({} bytes)，降低质量到{}%", baos.size(), quality);
            }
        }

        // 转为Base64（无换行）
        String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
        log.info("Base64长度: {} 字符", base64.length());

        return base64;
    }

    public ClassifyResponse classifyImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("请选择要识别的图片");
        }

        log.info("========================================");
        log.info("处理图片: {}", file.getOriginalFilename());
        log.info("文件大小: {} bytes", file.getSize());
        log.info("文件类型: {}", file.getContentType());

        // 转换图片为Base64（统一转成JPG格式）
        String imageBase64 = convertImageToJpgBase64(file);

        try {
            // 调用百度API
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

                    RecognitionRecord record = new RecognitionRecord();
                    record.setItemName(itemName);
                    record.setCategory(category);
                    record.setConfidence(confidence);
                    recordRepository.save(record);

                    return ClassifyResponse.builder()
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

    public List<RecognitionRecord> getHistory() {
        return recordRepository.findAllByOrderByCreatedAtDesc();
    }
}