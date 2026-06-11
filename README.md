# 垃圾分类助手 - 部署说明

## 项目简介
基于图像识别的垃圾分类助手，用户上传垃圾图片，系统调用百度AI识别物品并告知所属分类。

## 技术栈
- 后端：Spring Boot 2.7.15 + MySQL 8.0
- 前端：Vue 3 + Element Plus + Axios
- AI：百度通用物体识别API

## 本地部署步骤

### 一、环境要求
- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Maven 3.6+

### 二、数据库配置
1. 创建数据库
```sql
CREATE DATABASE garbage_classifier;