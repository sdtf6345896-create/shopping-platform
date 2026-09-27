# 購物平台 Shopping Platform

[![GitHub repo](https://img.shields.io/badge/GitHub-shopping--platform-181717?logo=github)](https://github.com/sdtf6345896-create/shopping-platform)
[![CI](https://github.com/sdtf6345896-create/shopping-platform/actions/workflows/ci.yml/badge.svg)](https://github.com/sdtf6345896-create/shopping-platform/actions/workflows/ci.yml)
[![Last commit](https://img.shields.io/github/last-commit/sdtf6345896-create/shopping-platform)](https://github.com/sdtf6345896-create/shopping-platform/commits/master)
[![Stars](https://img.shields.io/github/stars/sdtf6345896-create/shopping-platform?style=flat)](https://github.com/sdtf6345896-create/shopping-platform/stargazers)

仿 momo 風格的購物網站,包含會員前台 + 管理後台,作為求職作品集專案。

## 技術棧

**後端**:Spring Boot 3、Spring Security + JWT、Spring Data JPA、MySQL、Flyway
**前端**:Vue 3(Vite)、Pinia、Vue Router、Element Plus、Axios

## 功能

**會員前台**
- 註冊(需 Email 驗證後才能登入)/ 登入(JWT + refresh token)、忘記密碼 / 重設密碼
- 登入防暴力破解:連續失敗 5 次鎖定 15 分鐘(會員、管理員皆適用)
- 首頁:輪播 banner(後台可管理)、分類導覽、熱銷推薦商品
- 商品列表:分類篩選、價格區間、搜尋、排序
- 商品詳情:規格選擇、評論與評分、相關商品推薦
- 購物車、願望清單、瀏覽紀錄
- 結帳流程:收件資訊 → 優惠券折抵 → 付款方式 → 訂單確認(狀態變更會寄 email 通知)
- 線上付款訂單有付款期限倒數,逾時未付款由排程自動取消並歸還庫存
- 會員中心:訂單查詢(含狀態歷程時間軸、物流單號、再買一次)、個人資料、收件地址管理

**後台管理**
- 商品管理:CRUD、上下架、庫存,低庫存警示並可直接補貨
- 分類管理、Banner 管理
- 訂單管理:狀態流轉(待付款 → 已付款 → 出貨中 → 已完成 / 取消),出貨填寫物流單號,完整操作歷程;關鍵字 / 日期搜尋、匯出 CSV
- 會員管理、優惠券管理
- 銷售報表

## 本機啟動

### 後端

需要本機有 MySQL,先建立資料庫:

```sql
CREATE DATABASE shopping_platform DEFAULT CHARACTER SET utf8mb4;
```

啟動時可用環境變數指定帳密(預設 `root` / `root`):

```bash
DB_USERNAME=root DB_PASSWORD=<your-password> mvn spring-boot:run
```

Flyway 會自動建表並灌入示範資料。啟動後:
- API:`http://localhost:8080`
- Swagger UI:`http://localhost:8080/swagger-ui.html`
- 預設管理員帳號:`admin` / `admin123`

### 前端

```bash
cd frontend
npm install
npm run dev
```

開發伺服器預設在 `http://localhost:5173`,已設定 Vite proxy 轉發 `/api` 到後端。

### 寄信設定(選用,真實 SMTP)

註冊驗證信、忘記密碼信、訂單狀態通知信都走同一套寄信機制。預設不設定 `MAIL_HOST` 時,信件內容只會輸出到後端 log(模擬寄信)——本機測試註冊流程時,驗證連結要從後端 console 的 `[模擬寄信]` 那行複製貼上瀏覽器。要改成真的寄出 email,啟動時加上:

```bash
MAIL_HOST=smtp.gmail.com MAIL_PORT=587 MAIL_USERNAME=you@gmail.com MAIL_PASSWORD=<app-password> mvn spring-boot:run
```

（Gmail 需要用「應用程式密碼」而非登入密碼;也可以換成 Mailtrap、SendGrid 等其他 SMTP 服務。）若寄信失敗(帳密錯誤、連不上等),會自動退回 log 模擬,不影響原本的 API 流程。

## 測試

```bash
mvn test              # 後端(JUnit + Mockito,另含 H2 上的 Spring context 啟動測試)
cd frontend && npm test   # 前端(Vitest)
```

`MysqlMigrationIntegrationTest` 會在真的 MySQL 上跑完整 Flyway migration + schema 驗證 + 示範資料初始化,
只有設定 `MIGRATION_TEST_DB_URL`(以及 `MIGRATION_TEST_DB_USERNAME` / `MIGRATION_TEST_DB_PASSWORD`)時才會執行,
CI 會自動用 MySQL service container 跑這一項。本機想跑的話先建一個**空的**測試資料庫:

```bash
MIGRATION_TEST_DB_URL="jdbc:mysql://localhost:3306/shopping_platform_it?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false" MIGRATION_TEST_DB_PASSWORD=<your-password> mvn test -Dtest=MysqlMigrationIntegrationTest
```

## 文件

- [API 清單](./API.md)
- [專案規格](./購物平台專案規格.md)
