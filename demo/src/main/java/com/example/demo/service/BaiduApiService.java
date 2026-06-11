package com.example.demo.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class BaiduApiService {

    private static final String API_KEY = "S4jEfpnTjtrE0ofYtvP1pp1g";
    private static final String SECRET_KEY = "lgSEyTRKRXufHyydraL3vQmpJ460gqJv";
    private static final String API_URL = "https://aip.baidubce.com/rest/2.0/image-classify/v2/advanced_general";

    private String accessToken = null;
    private long tokenExpireTime = 0;

    private synchronized String getAccessToken() throws Exception {
        if (accessToken != null && System.currentTimeMillis() < tokenExpireTime) {
            return accessToken;
        }

        String url = "https://aip.baidubce.com/oauth/2.0/token?" +
                "grant_type=client_credentials&" +
                "client_id=" + API_KEY + "&" +
                "client_secret=" + SECRET_KEY;

        log.info("获取Access Token...");

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(10000);

        int code = connection.getResponseCode();
        if (code == 200) {
            String response = new String(connection.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            JSONObject json = JSON.parseObject(response);
            accessToken = json.getString("access_token");
            tokenExpireTime = System.currentTimeMillis() + 28L * 24 * 3600 * 1000;
            log.info("Token获取成功");
            return accessToken;
        }
        throw new RuntimeException("获取Token失败: " + code);
    }

    public JSONObject classifyGarbage(String imageBase64) throws Exception {
        String token = getAccessToken();
        String url = API_URL + "?access_token=" + token;

        // URL编码图片Base64
        String encodedImage = URLEncoder.encode(imageBase64, "UTF-8");
        String params = "image=" + encodedImage;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setDoOutput(true);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(params.getBytes(StandardCharsets.UTF_8));
        }

        int code = connection.getResponseCode();
        String response;
        if (code == 200) {
            response = new String(connection.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } else {
            response = new String(connection.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        }

        log.info("API响应码: {}, 响应: {}", code, response);

        JSONObject json = JSON.parseObject(response);
        if (json.containsKey("error_code")) {
            throw new RuntimeException("百度API错误 [" + json.getInteger("error_code") + "]: " + json.getString("error_msg"));
        }

        return json;
    }
}