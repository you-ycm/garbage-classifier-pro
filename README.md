# 🚀 垃圾分类助手 Pro

> 基于图像识别的垃圾分类助手 - 功能完善版

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7-brightgreen)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3-blue)](https://vuejs.org/)
[![Element Plus](https://img.shields.io/badge/Element%20Plus-2.14-blueviolet)](https://element-plus.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)
[![JDK](https://img.shields.io/badge/JDK-8-orange)](https://www.oracle.com/java/technologies/downloads/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-lightblue)](https://www.mysql.com/)

## 📌 项目简介

本项目是在原「垃圾分类助手」基础上全面增强的**功能完善版**。用户上传垃圾图片，系统调用百度 AI 通用物体识别 API 识别物品并自动判断所属分类，帮助用户正确进行垃圾分类。

原项目已实现基础的用户认证和管理后台，在此基础上**新增了**个人信息管理、我的收藏、垃圾分类知识库、操作日志、系统概览统计看板等完整功能模块，并对现有模块进行了全面升级。

## ✨ 功能概览

### 🔐 用户认证
- 支持 **用户名/邮箱** 登录
- 注册可选填邮箱
- **JWT Token** 认证，**BCrypt** 密码加密
- 修改密码 + 修改用户名 + 绑定邮箱

### 📷 垃圾分类
- 图片压缩质量选择（高清/推荐/快速）
- 单张上传 + 批量上传（最多 10 张）
- 最近识别记录（图片预览 + 置信度进度条）
- 批量删除 + 点击跳转历史

### 📋 识别历史
- 多条件筛选：物品名称 / 类别 / 识别日期
- 正倒序排序（最新优先/最早优先）
- 批量操作：邮箱分享 / 导出 CSV / 批量删除 / 收藏/取消收藏
- 分页优化（5/10/20/50 条切换）

### ⚙️ 管理员后台
- **系统概览**：统计卡片 + ECharts 分类扇形图 + 近 7 天趋势图
- **用户管理**：多条件搜索（用户名/邮箱/角色/日期）+ 批量导出/删除
- **识别记录管理**：多条件筛选 + 批量操作（分享/导出/删除）
- **操作日志**（新增）：自动记录用户操作 + 多条件搜索 + 批量导出/删除
- **收藏管理**（新增）：多条件搜索 + 批量操作（分享/导出/删除）
- **知识库管理**（新增）：搜索 + 批量操作 + 批量新增 + 编辑/删除

### 👤 个人信息
- 修改用户名（实时校验唯一性）
- 显示用户身份（管理员/普通用户）
- 修改/更换邮箱

### ⭐ 我的收藏
- 多条件筛选 + 正倒序排序
- 批量操作：邮箱分享 / 导出 CSV / 批量取消收藏
- 分页优化

### 📚 垃圾分类知识
- 搜索 + 批量导出/分享
- 统计卡片展示各类别知识条目数量

## 🛠️ 技术栈

| 层级 | 技术 |
|------|------|
| **后端框架** | Spring Boot 2.7 |
| **数据访问** | Spring Data JPA / Hibernate |
| **数据库** | MySQL 8.0 |
| **安全框架** | Spring Security + JWT + BCrypt |
| **前端框架** | Vue 3 + Vite |
| **UI 组件库** | Element Plus |
| **状态管理** | Pinia |
| **HTTP 请求** | Axios |
| **数据可视化** | ECharts |
| **部署环境** | Ubuntu 22.04 + Nginx |

## 📁 项目结构
```text
oro/
├── demo/                                    # 后端 Spring Boot 项目
│   └── src/main/java/com/example/demo/
│       ├── config/                          # 配置类（JWT、Security、CORS）
│       ├── controller/                      # 控制器（API 接口）
│       │   ├── AdminController.java         # 管理员后台接口
│       │   ├── AuthController.java          # 认证接口（登录/注册/密码）
│       │   ├── FavoriteController.java      # 收藏接口
│       │   ├── GarbageController.java       # 垃圾分类识别接口
│       │   └── KnowledgeController.java     # 知识库接口
│       ├── service/                         # 业务逻辑层
│       ├── repository/                      # 数据访问层（JPA）
│       ├── entity/                          # 实体类
│       ├── dto/                             # 数据传输对象
│       └── util/                            # 工具类（JWT工具）
│
└── frontend/                                # 前端 Vue 3 项目
    ├── src/
    │   ├── views/                           # 页面组件
    │   │   ├── Login.vue                    # 登录页
    │   │   ├── Register.vue                 # 注册页
    │   │   ├── History.vue                  # 识别历史
    │   │   ├── Favorites.vue                # 我的收藏
    │   │   ├── KnowledgeBase.vue            # 垃圾分类知识
    │   │   ├── Profile.vue                  # 个人信息
    │   │   └── AdminPanel.vue               # 管理员后台
    │   ├── components/                      # 公共组件
    │   ├── store/                           # Pinia 状态管理
    │   ├── router/                          # 路由配置
    │   └── utils/                           # 工具函数（axios 封装）
    ├── package.json
    └── vite.config.js
```


## 📊 数据库设计

| 表名 | 说明 |
|------|------|
| `user` | 用户表（id、username、password、role、email、created_at） |
| `recognition_record` | 识别记录表（id、image_url、item_name、category、confidence、user_id、created_at） |
| `favorite` | 收藏表（id、user_id、record_id、created_at）**【新增】** |
| `knowledge_base` | 知识库表（id、category、name、description、suggestion、sort_order、created_at）**【新增】** |
| `operation_log` | 操作日志表（id、user_id、username、operation、detail、ip、created_at）**【新增】** |

## 🚀 快速启动

### 环境要求
- JDK 8+
- MySQL 8.0+
- Node.js 18+
- Maven 3.6+

### 1️⃣ 克隆项目
```bash
git clone https://github.com/you-ycm/garbage-classifier-pro.git
cd garbage-classifier-pro

2️⃣ 数据库配置
-- 创建数据库
CREATE DATABASE garbage_classifier DEFAULT CHARACTER SET utf8mb4;

3️⃣ 后端启动
cd demo
mvn clean install
mvn spring-boot:run

4️⃣ 前端启动
cd frontend
npm install
npm run dev

5️⃣ 默认账号
角色	    用户名	    密码
管理员	    youwu	    123456
普通用户	  user1	    123456
```

## 🎯 核心功能演示
垃圾分类识别
- 上传图片 → 百度 AI 识别 → 展示物品名称、分类、置信度

- 支持压缩质量选择、批量上传

识别历史
- 多条件筛选 + 批量操作（分享/导出/删除/收藏）

管理员后台
- 系统概览 + ECharts 统计图表

- 用户管理 / 识别记录管理 / 操作日志 / 收藏管理 / 知识库管理

## 👥 团队成员
成员	  职责	                                 占比
- 吴嘉兴	  项目架构、百度 AI 集成、部署运维         	60%
- 游昌民	  全栈功能开发、数据库设计、项目测试	        40%

## 📄 许可证
本项目采用 MIT License 开源许可证。
