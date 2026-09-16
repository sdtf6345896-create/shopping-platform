# API 清單

購物平台後端 API 文件。所有端點皆以 `http://localhost:8080` 為 base URL(前端開發時透過 Vite proxy 用相對路徑 `/api/...` 即可)。

也可以直接開 Swagger UI 互動測試:`http://localhost:8080/swagger-ui.html`

---

## 共用規則

### 回應格式

所有 API 一律回傳統一格式:

```json
{
  "success": true,
  "message": "OK",
  "data": { ... }
}
```

失敗時 `success: false`,`data: null`,`message` 為錯誤訊息,並搭配對應的 HTTP status code:

| Status | 情境 |
|---|---|
| 400 | 驗證失敗、業務邏輯錯誤(例:庫存不足、Email 重複)、參數格式錯誤 |
| 401 | 未登入 / token 失效或逾期 |
| 403 | 已登入但權限不足(例:會員 token 打後台 API) |
| 404 | 資源不存在 |
| 500 | 未預期的系統錯誤 |

### 分頁格式

所有分頁 API 回傳的 `data` 皆為:

```json
{
  "content": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 42,
  "totalPages": 3,
  "last": false
}
```

分頁請求共用參數:`page`(從 0 開始,預設 0)、`size`、`sort`(格式 `欄位,asc|desc`,例:`price,desc`,可重複帶多個)。

### 認證

- 除了登入/註冊與商品/分類的公開瀏覽端點,其餘皆需在 header 帶 `Authorization: Bearer <token>`
- **會員 token 與管理員 token 是兩套獨立系統**,不可互相使用。會員 token 打 `/api/admin/**` 會拿到 403。
- Token 由對應的登入端點簽發,預設有效期 24 小時(見 `application.yml` 的 `jwt.expiration-ms`)。

### 列舉值(Enum)

| Enum | 值 |
|---|---|
| `AccountStatus` | `ACTIVE`, `DISABLED` |
| `CategoryStatus` | `ACTIVE`, `DISABLED` |
| `ProductStatus` | `ON_SALE`, `OFF_SHELF` |
| `OrderStatus` | `PENDING_PAYMENT`, `PAID`, `SHIPPING`, `COMPLETED`, `CANCELLED` |
| `PaymentMethod` | `CREDIT_CARD`, `ATM`, `COD` |

訂單狀態合法轉換(後台變更狀態、會員取消訂單都受此限制):

```
PENDING_PAYMENT → PAID, CANCELLED
PAID            → SHIPPING, CANCELLED
SHIPPING        → COMPLETED
COMPLETED       → (終態)
CANCELLED       → (終態)
```

---

## 會員驗證(Auth)

### `POST /api/auth/register`
公開。註冊新會員。

請求:
```json
{ "email": "you@example.com", "password": "password123", "name": "王小明", "phone": "0912345678" }
```
- `password` 至少 8 碼;`phone` 選填

回應 `data`:`MemberResponse`
```json
{ "id": 1, "email": "you@example.com", "name": "王小明", "phone": "0912345678", "status": "ACTIVE", "createdAt": "2026-09-14T20:02:59" }
```

### `POST /api/auth/login`
公開。會員登入。帳號被停用(`DISABLED`)時會回 400「帳號已被停用,請聯繫客服」。

請求:`{ "email": "...", "password": "..." }`

回應 `data`:
```json
{ "token": "...", "tokenType": "Bearer", "memberId": 1, "name": "王小明", "email": "you@example.com" }
```

---

## 會員個人資料(Member)— 需會員登入

### `GET /api/members/me`
取得自己的個人資料 → `MemberResponse`(同上)

### `PUT /api/members/me`
更新自己的姓名/手機。請求:`{ "name": "...", "phone": "..." }` → `MemberResponse`

---

## 收件地址(Address)— 需會員登入

### `GET /api/members/addresses`
列出自己所有地址 → `List<AddressResponse>`

### `POST /api/members/addresses`
新增地址。請求:
```json
{
  "recipientName": "王小明", "phone": "0912345678", "postalCode": "106",
  "city": "台北市", "district": "大安區", "detailAddress": "復興南路一段1號",
  "defaultAddress": true
}
```
> 注意:JSON 欄位是 `defaultAddress`,不是 `isDefault`(Lombok + Jackson 對 `is` 前綴 boolean 欄位的命名慣例會導致 `isDefault` 被忽略)。

回應 `data`:`AddressResponse`(同請求欄位 + `id`)

### `PUT /api/members/addresses/{id}`
更新地址,body 同上。只能改自己的地址,否則 404。

### `DELETE /api/members/addresses/{id}`
刪除地址。若該地址已被歷史訂單引用,資料庫外鍵會擋下,回 400。

---

## 管理員登入(Admin Auth)

### `POST /api/admin/auth/login`
公開。管理員登入。開發環境預設帳號 `admin` / `admin123`(見 `DataInitializer`,只在 `dev` profile 生效)。

請求:`{ "username": "admin", "password": "admin123" }`

回應 `data`:
```json
{ "token": "...", "tokenType": "Bearer", "adminId": 1, "username": "admin", "name": "系統管理員" }
```

---

## 會員管理(Admin）— 需管理員登入

### `GET /api/admin/members`
分頁列表。Query:`status`(`ACTIVE`/`DISABLED`,選填)、`keyword`(比對 email 或姓名,選填)、`page`、`size`、`sort`

→ `PageResponse<MemberResponse>`

### `GET /api/admin/members/{id}`
會員詳情 → `MemberResponse`

### `PATCH /api/admin/members/{id}/status`
啟用/停用帳號。請求:`{ "status": "DISABLED" }` → `MemberResponse`

---

## 分類(Category)

### `GET /api/categories`
公開。回傳分類樹(只含 `ACTIVE`,巢狀 `children`)

```json
[
  { "id": 1, "name": "男裝", "parentId": null, "sortOrder": 1, "status": "ACTIVE",
    "children": [ { "id": 2, "name": "上衣", "parentId": 1, "sortOrder": 1, "status": "ACTIVE", "children": [] } ] }
]
```

### `GET /api/admin/categories`
需管理員登入。回傳完整分類樹(含 `DISABLED`)。

### `POST /api/admin/categories`
需管理員登入。新增分類。請求:`{ "name": "上衣", "parentId": null, "sortOrder": 1 }`(`parentId` 為 `null` 表示頂層)

### `PUT /api/admin/categories/{id}`
更新分類,body 同上。

### `PATCH /api/admin/categories/{id}/status`
請求:`{ "status": "DISABLED" }`

### `DELETE /api/admin/categories/{id}`
刪除分類。若底下還有子分類或商品會擋下,回 400。

---

## 商品(Product)

### `GET /api/products`
公開。只回傳 `ON_SALE` 的商品。Query:`categoryId`、`minPrice`、`maxPrice`、`keyword`、`page`、`size`、`sort`(例:`price,asc`、`salesCount,desc`)

→ `PageResponse<ProductListResponse>`,每筆:
```json
{ "id": 1, "name": "經典圓領T恤", "mainImage": "https://...", "price": 590.00, "status": "ON_SALE", "salesCount": 3 }
```

### `GET /api/products/{id}`
公開。商品詳情,若非 `ON_SALE` 回 404。

→ `ProductDetailResponse`:
```json
{
  "id": 1, "categoryId": 2, "categoryName": "上衣", "name": "經典圓領T恤",
  "description": "100% 純棉,舒適透氣", "price": 590.00, "mainImage": "https://...",
  "status": "ON_SALE", "salesCount": 3,
  "skus": [ { "id": 1, "skuCode": "TSHIRT-BLK-M", "specName": "黑色/M", "price": 590.00, "stock": 3 } ]
}
```

### `GET /api/admin/products`
需管理員登入。所有狀態商品皆可查。Query:`categoryId`、`status`、`keyword`、`page`、`size`、`sort`。回傳格式同 `GET /api/products`(輕量版,不含分類名稱與 SKU)。

### `GET /api/admin/products/{id}`
需管理員登入。回傳格式同 `GET /api/products/{id}`,不限狀態。

### `POST /api/admin/products`
新增商品。請求:
```json
{
  "categoryId": 2, "name": "經典圓領T恤", "description": "...", "price": 590,
  "mainImage": "https://...",
  "skus": [
    { "skuCode": "TSHIRT-BLK-M", "specName": "黑色/M", "price": 590, "stock": 50 },
    { "skuCode": "TSHIRT-BLK-L", "specName": "黑色/L", "price": 590, "stock": 30 }
  ]
}
```
`skus` 至少 1 筆。新商品預設 `OFF_SHELF`(下架)。

### `PUT /api/admin/products/{id}`
更新商品,body 同上。**SKU 為整批覆蓋**(用新的 `skus` 陣列取代全部舊資料,不是逐筆合併)。

### `PATCH /api/admin/products/{id}/status`
上下架。請求:`{ "status": "ON_SALE" }`

### `PATCH /api/admin/products/{productId}/skus/{skuId}/stock`
單獨調整某規格庫存。請求:`{ "stock": 100 }` → `SkuResponse`

### `DELETE /api/admin/products/{id}`
刪除商品(連同其 SKU)。

---

## 圖片上傳(Admin）— 需管理員登入

### `POST /api/admin/uploads/image`
`multipart/form-data`,欄位名 `file`。僅接受 `image/jpeg`、`image/png`、`image/webp`,大小上限 5MB(超過由 `MaxUploadSizeExceeded` handler 攔截,回 400 而非 500)。

檔案以 UUID 重新命名後存放於 `app.upload.dir`(預設 `./uploads`),不採用使用者原始檔名,避免路徑穿越風險。

回應 `data`:`UploadResponse`
```json
{ "url": "/uploads/3f2504e0-4f89-11d3-9a0c-0305e82c3301.jpg" }
```

上傳後的圖片透過 `/uploads/**` 靜態資源公開存取(無需認證),`url` 可直接填入商品的 `mainImage` 欄位。

> 本機磁碟儲存僅適合本專案規模的展示用途,多數雲端平台的檔案系統是暫時性的(重新部署會遺失),正式環境需改用 S3 相容的物件儲存。

---

## 購物車(Cart）— 需會員登入

### `GET /api/cart`
→ `CartSummaryResponse`:
```json
{
  "items": [
    { "id": 1, "skuId": 1, "productId": 1, "productName": "經典圓領T恤", "specName": "黑色/M",
      "mainImage": "https://...", "price": 590.00, "quantity": 2, "subtotal": 1180.00,
      "stock": 3, "productStatus": "ON_SALE" }
  ],
  "totalQuantity": 2, "totalAmount": 1180.00
}
```
> `price`/`stock`/`productStatus` 皆為即時值(非下單快照),商品下架後既有購物車項目仍會顯示,但 `productStatus` 會變成 `OFF_SHELF`,前端應提示使用者移除。

### `POST /api/cart/items`
加入商品。請求:`{ "skuId": 1, "quantity": 2 }`。同規格已存在於購物車時,數量會累加,而非覆蓋。超過庫存回 400。

### `PUT /api/cart/items/{itemId}`
修改數量。請求:`{ "quantity": 3 }`

### `DELETE /api/cart/items/{itemId}`
移除單一項目。

### `DELETE /api/cart`
清空購物車。

---

## 訂單(Order）— 會員端需登入

### `GET /api/orders`
自己的訂單列表。Query:`status`、`page`、`size` → `PageResponse<OrderResponse>`

`OrderResponse`:
```json
{
  "id": 1, "orderNo": "ORD202609142029305718", "status": "PAID", "paymentMethod": "CREDIT_CARD",
  "totalAmount": 1770.00, "receiverName": "王小明", "receiverPhone": "0912345678",
  "receiverAddress": "台北市大安區復興南路一段1號", "createdAt": "2026-09-14T20:29:30",
  "items": [ { "id": 1, "skuId": 2, "productName": "經典圓領T恤", "specName": "黑色/L",
                "unitPrice": 590.00, "quantity": 1, "subtotal": 590.00 } ]
}
```
> `receiverName`/`receiverPhone`/`receiverAddress` 與商品名稱/規格/單價皆為**下單當下的快照**,之後會員改地址或商家改商品都不影響歷史訂單。

### `GET /api/orders/{id}`
自己的訂單詳情,非本人訂單回 404。

### `POST /api/orders`
結帳。請求:
```json
{ "addressId": 2, "paymentMethod": "CREDIT_CARD", "cartItemIds": [1, 2] }
```
`cartItemIds` 選填,不帶則結帳購物車全部項目。下單當下就會扣庫存(非等付款);若購物車內有商品已下架或庫存不足,整筆交易失敗回 400,購物車項目不受影響。

### `POST /api/orders/{id}/pay`
模擬付款。只能對 `PENDING_PAYMENT` 的訂單執行,成功後狀態變 `PAID`,並累加商品 `salesCount`。

### `POST /api/orders/{id}/cancel`
取消訂單。只能對 `PENDING_PAYMENT` 或 `PAID` 的訂單執行,成功後歸還庫存。

---

## 訂單管理(Admin）— 需管理員登入

### `GET /api/admin/orders`
所有會員的訂單。Query:`status`、`page`、`size` → `PageResponse<OrderResponse>`

### `GET /api/admin/orders/{id}`
任意訂單詳情。

### `PATCH /api/admin/orders/{id}/status`
變更訂單狀態(出貨、標記完成、取消等)。請求:`{ "status": "SHIPPING" }`。不合法的狀態轉換回 400。取消訂單會歸還庫存。

---

## 銷售報表(Admin）— 需管理員登入

三支皆接受 Query:`startDate`、`endDate`(格式 `yyyy-MM-dd`,不帶則預設近 30 天,含今天)。統計只計入 `PAID`/`SHIPPING`/`COMPLETED` 三種狀態的訂單(排除待付款與已取消)。

### `GET /api/admin/reports/summary`
```json
{
  "startDate": "2026-08-16", "endDate": "2026-09-14",
  "totalOrders": 3, "paidOrders": 2, "cancelledOrders": 1,
  "totalRevenue": 2360.00, "averageOrderValue": 1180.00,
  "statusCounts": { "PENDING_PAYMENT": 0, "PAID": 0, "SHIPPING": 1, "COMPLETED": 1, "CANCELLED": 1 }
}
```

### `GET /api/admin/reports/top-products`
另接受 `limit`(預設 10)。依銷售數量排序:
```json
[ { "productId": 1, "productName": "經典圓領T恤", "mainImage": "https://...", "soldQuantity": 4, "revenue": 2360.00 } ]
```

### `GET /api/admin/reports/daily`
每日訂單數與營收:
```json
[ { "date": "2026-09-14", "orderCount": 2, "revenue": 2360.00 } ]
```

---

## 端點總覽表

| Method | Path | 權限 |
|---|---|---|
| POST | `/api/auth/register` | 公開 |
| POST | `/api/auth/login` | 公開 |
| GET / PUT | `/api/members/me` | 會員 |
| GET / POST | `/api/members/addresses` | 會員 |
| PUT / DELETE | `/api/members/addresses/{id}` | 會員 |
| POST | `/api/admin/auth/login` | 公開 |
| GET | `/api/admin/members` | 管理員 |
| GET | `/api/admin/members/{id}` | 管理員 |
| PATCH | `/api/admin/members/{id}/status` | 管理員 |
| GET | `/api/categories` | 公開 |
| GET / POST | `/api/admin/categories` | 管理員 |
| PUT / DELETE | `/api/admin/categories/{id}` | 管理員 |
| PATCH | `/api/admin/categories/{id}/status` | 管理員 |
| GET | `/api/products` | 公開 |
| GET | `/api/products/{id}` | 公開 |
| GET / POST | `/api/admin/products` | 管理員 |
| GET / PUT / DELETE | `/api/admin/products/{id}` | 管理員 |
| PATCH | `/api/admin/products/{id}/status` | 管理員 |
| PATCH | `/api/admin/products/{productId}/skus/{skuId}/stock` | 管理員 |
| POST | `/api/admin/uploads/image` | 管理員 |
| GET / DELETE | `/api/cart` | 會員 |
| POST | `/api/cart/items` | 會員 |
| PUT / DELETE | `/api/cart/items/{itemId}` | 會員 |
| GET / POST | `/api/orders` | 會員 |
| GET | `/api/orders/{id}` | 會員 |
| POST | `/api/orders/{id}/pay` | 會員 |
| POST | `/api/orders/{id}/cancel` | 會員 |
| GET | `/api/admin/orders` | 管理員 |
| GET | `/api/admin/orders/{id}` | 管理員 |
| PATCH | `/api/admin/orders/{id}/status` | 管理員 |
| GET | `/api/admin/reports/summary` | 管理員 |
| GET | `/api/admin/reports/top-products` | 管理員 |
| GET | `/api/admin/reports/daily` | 管理員 |
