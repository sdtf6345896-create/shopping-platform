# 購物平台 Shopping Platform

仿 momo 風格的購物網站,包含會員前台 + 管理後台,作為求職作品集專案。

**Repo:** https://github.com/sdtf6345896-create/shopping-platform

## 技術棧

**後端**:Spring Boot 3、Spring Security + JWT、Spring Data JPA、MySQL、Flyway
**前端**:Vue 3(Vite)、Pinia、Vue Router、Element Plus、Axios

## 功能

**會員前台**
- 註冊 / 登入(JWT)
- 首頁:輪播 banner(後台可管理)、分類導覽、熱銷推薦商品
- 商品列表:分類篩選、價格區間、搜尋、排序
- 商品詳情:規格選擇、評論與評分
- 購物車、願望清單
- 結帳流程:收件資訊 → 優惠券折抵 → 付款方式 → 訂單確認
- 會員中心:訂單查詢、個人資料、收件地址管理

**後台管理**
- 商品管理:CRUD、上下架、庫存
- 分類管理、Banner 管理
- 訂單管理:狀態流轉(待付款 → 已付款 → 出貨中 → 已完成 / 取消)
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

## 測試

```bash
mvn test              # 後端(JUnit + Mockito)
cd frontend && npm test   # 前端(Vitest)
```

## 文件

- [API 清單](./API.md)
- [專案規格](./購物平台專案規格.md)
