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
| 429 | 登入失敗次數過多,帳號暫時鎖定 |
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
- 會員登入額外會拿到 `refreshToken`,預設 14 天內有效(見 `jwt.refresh-expiration-ms`),過期前可用 `POST /api/auth/refresh` 換發新 token,不需要重新輸入密碼。管理員沒有 refresh token,token 過期需重新登入。

### 列舉值(Enum)

| Enum | 值 |
|---|---|
| `AccountStatus` | `ACTIVE`, `DISABLED` |
| `CategoryStatus` | `ACTIVE`, `DISABLED` |
| `ProductStatus` | `ON_SALE`, `OFF_SHELF` |
| `OrderStatus` | `PENDING_PAYMENT`, `PAID`, `SHIPPING`, `COMPLETED`, `CANCELLED`, `REFUNDED` |
| `ReturnStatus` | `PENDING`(審核中), `APPROVED`(已核准退款), `REJECTED`(未通過) |
| `PaymentMethod` | `CREDIT_CARD`, `ATM`, `COD` |
| `CouponStatus` | `ACTIVE`, `DISABLED` |
| `DiscountType` | `FIXED_AMOUNT`(固定金額折抵), `PERCENTAGE`(百分比折扣) |
| `PointTransactionType` | `EARN`(訂單完成回饋), `REDEEM`(結帳折抵), `REFUND`(取消退還), `ADJUST`(活動贈送/調整) |
| `InvoiceType` | `MEMBER_CARRIER`, `MOBILE_BARCODE`, `COMPANY`, `DONATION` |
| `OrderActor` | `MEMBER`, `ADMIN`, `SYSTEM`(訂單歷程中觸發狀態變更的角色) |

訂單狀態合法轉換(後台變更狀態、會員取消訂單都受此限制):

```
PENDING_PAYMENT → PAID, CANCELLED
PAID            → SHIPPING, CANCELLED
SHIPPING        → COMPLETED(管理員、會員確認收貨,或出貨 7 天後系統自動完成)
COMPLETED       → (終態;僅能透過退貨核准變成 REFUNDED)
CANCELLED       → (終態)
REFUNDED        → (終態)
```

---

## 會員驗證(Auth)

### `POST /api/auth/register`
公開。註冊新會員。新帳號預設**未驗證 Email**,會寄出驗證信,驗證前無法登入。

請求:
```json
{ "email": "you@example.com", "password": "password123", "name": "王小明", "phone": "0912345678" }
```
- `password` 至少 8 碼;`phone` 選填
- `referralCode` 選填,好友的邀請碼(不分大小寫);填了但找不到或邀請人已停用回 400

回應 `data`:`MemberResponse`
```json
{ "id": 1, "email": "you@example.com", "name": "王小明", "phone": "0912345678", "status": "ACTIVE", "points": 0, "createdAt": "2026-09-14T20:02:59" }
```

### `POST /api/auth/verify-email`
公開。用註冊信中的 token 完成 Email 驗證,驗證成功後才能登入。

請求:`{ "token": "..." }`
- token 不存在、已過期(24 小時)或已使用過都會回 400「驗證連結無效或已過期」

### `POST /api/auth/resend-verification`
公開。重新寄送驗證信。不論 Email 是否存在、是否已驗證,都回傳相同成功訊息(避免帳號列舉)。

請求:`{ "email": "..." }`

> 與忘記密碼分開計算,同一個 email 每小時最多寄 3 封,超過時仍回成功但不寄信。
### `POST /api/auth/login`
公開。會員登入。帳號被停用(`DISABLED`)時回 400「帳號已被停用,請聯繫客服」;Email 尚未驗證時回 400「請先完成 Email 驗證,請查看您的收件匣」。

> 防暴力破解:同一 Email 連續登入失敗 5 次後鎖定 15 分鐘(`app.security.login.*` 可調),鎖定期間即使密碼正確也回 429;Email 不存在同樣計數,避免藉此探測帳號是否存在。登入成功會清除失敗次數。管理員登入套用相同規則(以 username 計數)。

請求:`{ "email": "...", "password": "..." }`

回應 `data`:
```json
{ "token": "...", "tokenType": "Bearer", "refreshToken": "...", "memberId": 1, "name": "王小明", "email": "you@example.com" }
```

### `POST /api/auth/refresh`
公開。用 refresh token 換發新的 access token(access token 過期前,前端可用這支 API 換新,不必重新登入)。

請求:`{ "refreshToken": "..." }`

回應 `data`:同登入回應,`token`、`refreshToken` 皆會換新(refresh token 為一次性,換發後舊的即失效)。

- refresh token 不存在、已過期、已被撤銷,或對應帳號已被停用,都回 401,前端應導回登入頁

### `POST /api/auth/logout`
公開。撤銷指定的 refresh token。找不到對應 token 也視為成功(冪等)。

請求:`{ "refreshToken": "..." }`

### `POST /api/auth/forgot-password`
公開。申請重設密碼信。不論 Email 是否存在都回傳相同成功訊息(避免帳號列舉)。

請求:`{ "email": "..." }`

若 Email 存在,會產生一組 30 分鐘內有效的一次性 token,並寄出重設連結信件(格式:`{前端網址}/reset-password?token=...`)。實際寄信方式視 SMTP 設定而定(見 README「忘記密碼信」一節),未設定 `MAIL_HOST` 或寄送失敗時會退回以 log 模擬寄信,不影響 API 回應。

> 同一個 email 每小時最多觸發 3 封重設密碼信(`app.mail.max-per-email-per-hour`),超過時仍回成功但不寄信,避免被用來轟炸信箱,也不洩漏帳號是否存在。
### `POST /api/auth/reset-password`
公開。使用 token 重設密碼。

請求:`{ "token": "...", "newPassword": "..." }`
- `newPassword` 至少 8 碼
- token 不存在、已過期或已使用過都會回 400「重設密碼連結無效或已過期」

---

## 會員個人資料(Member)— 需會員登入

### `GET /api/members/me/referral`
我的邀請碼與成果(第一次呼叫時產生 8 碼邀請碼):
```json
{ "code": "K7M2QX9A", "invitedCount": 3, "rewardedCount": 1,
  "referrerPoints": 100, "refereePoints": 50, "minOrderAmount": 300 }
```
被邀請的新會員**第一筆**商品金額(不含運費)達 `minOrderAmount` 的訂單完成(確認收貨/自動完成)時,邀請人得 `referrerPoints`、新會員得 `refereePoints` 點購物金,每位新會員只發一次。可用 `app.referral.*` 調整。前端邀請連結格式:`/register?ref=CODE`。

### `GET /api/members/me`
取得自己的個人資料 → `MemberResponse`(同上)

### `PUT /api/members/me`
更新自己的姓名/手機/生日。請求:`{ "name": "...", "phone": "...", "birthday": "1990-05-20" }` → `MemberResponse`

- `birthday` 選填,只能設定一次;已設定後送不同的值回 400(送 null 或相同值不變)
- 生日禮:每年生日當月自動發放 `app.birthday.reward-points`(預設 100)點購物金並發站內通知;在生日當月才設定生日的,當年不發、隔年起發放


### `DELETE /api/members/me`
刪除帳號。請求:`{ "password": "..." }`。密碼錯誤、仍有處理中的訂單(待付款 / 已付款 / 出貨中)或審核中的退貨時回 400。成功後:
- 帳號匿名化並停用(Email 改為 `deleted-{id}@deleted.invalid`、姓名「已刪除會員」、清除電話與收件地址個資),所有 refresh token 撤銷
- 購物車、收藏、瀏覽紀錄、站內通知、貨到通知訂閱一併刪除
- 訂單、評論、購物金明細等交易紀錄保留(帳務需求),評論顯示名稱變為「已**」

### `PUT /api/members/me/password`
登入狀態下修改密碼。請求:`{ "currentPassword": "...", "newPassword": "..." }`(新密碼至少 8 碼)。目前密碼錯誤、或新舊密碼相同回 400。成功後撤銷此會員**所有** refresh token(其他裝置須重新登入),並寄出「密碼已變更」安全通知信。
> `POST /api/auth/reset-password`(忘記密碼)成功後同樣會撤銷所有 refresh token。
---

## 會員等級(Member Tier)— 需會員登入

### `GET /api/members/me/tier`
依近 12 個月**已完成**訂單的商品金額(不含運費)計算:一般會員(回饋 1 倍)、銀卡 ≥ NT$5,000(1.5 倍)、金卡 ≥ NT$20,000(2 倍)。等級即時計算,不另外儲存;訂單完成時以「這筆之前」的等級決定購物金回饋倍率。
```json
{ "tier": "SILVER", "label": "銀卡會員", "pointsMultiplier": 1.5, "spending": 8000.00,
  "nextTier": "GOLD", "nextLabel": "金卡會員", "amountToNext": 12000.00 }
```

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

## 商品瀏覽紀錄(Browsing History)— 需會員登入

同一會員瀏覽同一商品只會保留一筆紀錄,再次瀏覽會更新時間並排到最前面。

### `GET /api/browsing-history`
分頁列出瀏覽紀錄,依 `viewedAt` 由新到舊排序 → 分頁格式,`content` 為 `BrowsingHistoryItemResponse[]`
```json
{ "id": 1, "productId": 10, "productName": "...", "mainImage": "...", "price": 990, "productStatus": "ON_SALE", "viewedAt": "2026-09-27T20:00:00" }
```

### `POST /api/browsing-history/{productId}`
記錄一次瀏覽(前端在商品詳情頁載入時呼叫)。商品不存在回 404。

### `DELETE /api/browsing-history/{productId}`
刪除單筆瀏覽紀錄。找不到也視為成功(冪等)。

### `DELETE /api/browsing-history`
清空自己所有的瀏覽紀錄。

---

## 為你推薦(Recommendations)— 需會員登入

### `GET /api/recommendations`
依會員最近 20 筆瀏覽紀錄中最常看的(最多 3 個)分類,推薦這些分類裡**還沒看過**的熱銷上架商品,不足時以全站熱銷補齊。Query:`limit`(預設 8,最多 20)。
```json
{ "personalized": true, "products": [ /* ProductListResponse */ ] }
```
沒有瀏覽紀錄時 `personalized` 為 `false`,`products` 只是全站熱銷(前台首頁此時不顯示「為你推薦」區塊)。

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
公開。只回傳 `ON_SALE` 的商品。Query:`categoryId`、`minPrice`、`maxPrice`、`keyword`、`inStock`(`true` 只看至少一個規格有庫存的商品)、`page`、`size`、`sort`(例:`price,asc`、`salesCount,desc`、`ratingAverage,desc`)

→ `PageResponse<ProductListResponse>`,每筆:
```json
{ "id": 1, "name": "經典圓領T恤", "mainImage": "https://...", "price": 590.00, "status": "ON_SALE", "salesCount": 3,
  "ratingAverage": 4.5, "reviewCount": 12 }
```
> `ratingAverage`(平均星等,小數一位)與 `reviewCount` 存在商品上,評論新增 / 修改 / 刪除時即時更新,列表可直接顯示與排序。

### `GET /api/products/{id}`
公開。商品詳情,若非 `ON_SALE` 回 404。

→ `ProductDetailResponse`:
```json
{
  "id": 1, "categoryId": 2, "categoryName": "上衣", "name": "經典圓領T恤",
  "description": "100% 純棉,舒適透氣", "price": 590.00, "mainImage": "https://...",
  "images": ["https://.../2.jpg", "https://.../3.jpg"],
  "status": "ON_SALE", "salesCount": 3,
  "skus": [ { "id": 1, "skuCode": "TSHIRT-BLK-M", "specName": "黑色/M", "price": 590.00, "stock": 3 } ]
}
```

### `GET /api/products/suggestions`
公開。搜尋框即時建議:名稱包含 `keyword`(不分大小寫)的上架商品,熱銷優先。Query:`keyword`、`limit`(預設 8,最多 10)。`keyword` 空白時回傳空陣列;`%`、`_` 視為一般字元。回傳 `ProductListResponse` 陣列。

### `GET /api/products/hot-searches`
公開。近 30 天熱門搜尋關鍵字(依次數排序)。Query:`limit`(預設 8,最多 20)。回傳字串陣列,例如 `["t恤", "耳機"]`。
`GET /api/products` 帶 `keyword` 且是第一頁、有搜尋結果時才計入(關鍵字會去空白、轉小寫;查無結果的字不會上榜);記錄失敗不會影響搜尋。

### `GET /api/products/flash-sale`
公開。目前限時特價中的上架商品,最快結束的排前面。Query:`limit`(預設 8,最多 20)。回傳 `ProductListResponse` 陣列。

> **限時特價**:商品可設定 `saleDiscountPercent`(1~90)與 `saleStartAt` / `saleEndAt`,期間內所有規格依比例打折(四捨五入到整數元)。商品列表、詳情與 SKU 回應保留原價 `price`,另外提供 `salePrice`(非特價期間為 `null`);列表另有 `saleDiscountPercent`、`saleEndAt`。購物車 `price` 為實際售價並附 `originalPrice`,結帳與優惠券門檻都以特價計算,訂單明細的 `unitPrice` 會記錄下單當下的特價。

### `GET /api/products/{id}/bought-together`
公開。「買了這個的人也買了」:統計和此商品出現在同一筆**已付款 / 出貨中 / 已完成**訂單中的其他上架商品,依共同出現的訂單數排序。Query:`limit`(預設 6,最多 12)。
```json
[ { "product": { /* ProductListResponse */ }, "orderCount": 3 } ]
```

### `GET /api/products/{id}/related`
公開。相關商品推薦:同分類的其他上架商品依銷量排序,不足 `limit` 筆時以全站熱銷商品補齊(不含本商品)。Query:`limit`(預設 6,最多 20)。回傳格式同商品列表項目(`ProductListResponse` 陣列)。

### `GET /api/admin/products`
需管理員登入。所有狀態商品皆可查。Query:`categoryId`、`status`、`keyword`、`page`、`size`、`sort`。回傳格式同 `GET /api/products`(輕量版,不含分類名稱與 SKU)。

### `PATCH /api/admin/products/status`
批次上 / 下架。請求:`{ "ids": [1, 2, 3], "status": "OFF_SHELF" }`(最多 100 個,重複 id 只算一次,不存在的 id 忽略)。回傳 `{ "updated": 3 }`,並記入管理員操作紀錄。

### `POST /api/admin/products/stock-import`
以 CSV 批次設定庫存。`multipart/form-data`,欄位名 `file`,UTF-8(可含 BOM),每列 `SKU 編號,庫存`,第一列可為標題列。最多 5,000 列、1MB。
**全有或全無**:先驗證所有資料列(SKU 不存在、庫存不是 0~1,000,000 的整數、SKU 重複、欄位不足),任何一列有問題就完全不更新。
```json
{ "applied": false, "totalRows": 3, "updated": 0,
  "errors": [ { "line": 3, "message": "找不到 SKU:NO-SUCH-SKU" } ] }
```
會記入管理員操作紀錄(檔名與結果)。

### `GET /api/admin/products/{productId}/skus/{skuId}/stock-movements`
需管理員登入。規格的庫存異動紀錄(新到舊,分頁):
```json
{ "id": 12, "changeQty": -2, "stockAfter": 18, "reason": "ORDER", "reference": "ORD202609281030001234",
  "createdAt": "2026-09-28T10:30:00" }
```
`reason`:`INITIAL`(新增商品 / 規格)、`ORDER`(下單扣庫存)、`ORDER_CANCEL`(取消歸還)、`RETURN`(退貨歸還)、`MANUAL`(後台手動調整)、`IMPORT`(CSV 匯入,`reference` 為檔名)、`PRODUCT_EDIT`(編輯商品改庫存)。`stockAfter` 為該筆異動寫入後資料庫中的實際庫存。規格或商品刪除時紀錄一併刪除。

### `GET /api/admin/products/low-stock`
需管理員登入。庫存警示:列出**上架中**商品裡庫存小於等於門檻的規格,庫存最少的排前面。Query:`threshold`(預設 10)、`limit`(預設 50,最多 200)。
```json
[ { "productId": 1, "productName": "經典圓領T恤", "mainImage": "https://...", "skuId": 2,
    "skuCode": "TSHIRT-BLK-M", "specName": "黑色/M", "stock": 0 } ]
```
後台「銷售報表」頁的庫存警示區塊可直接修改庫存(呼叫下方的 `PATCH .../stock`)。

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
`saleDiscountPercent`(1~90)、`saleStartAt`、`saleEndAt` 選填,設定限時特價時三者都要填且結束須晚於開始;不帶 `saleDiscountPercent` 表示取消特價。`publishAt` / `unpublishAt` 選填,排程上架 / 下架時間:新設定的時間須晚於現在、下架須晚於上架(原樣送回既有值不受限);排程約每分鐘檢查一次,時間到自動切換 `status` 並清空該欄位(兩者都已過期時結果為下架)。不帶表示取消排程。詳情與列表回應都帶這兩個欄位。更新商品時規格以 `skuCode` 比對:同編號就地更新(id 不變),新編號新增,移除的規格若已有訂單會回 400。
`images` 選填,為主圖以外的商品圖片網址(依陣列順序顯示,最多 8 張);更新時整批取代,不帶或空陣列表示清空圖庫。
`skus` 至少 1 筆。新商品預設 `OFF_SHELF`(下架)。

### `PUT /api/admin/products/{id}`
更新商品,body 同上。**SKU 為整批覆蓋**(用新的 `skus` 陣列取代全部舊資料,不是逐筆合併)。

### `PATCH /api/admin/products/{id}/status`
上下架。請求:`{ "status": "ON_SALE" }`

### `PATCH /api/admin/products/{productId}/skus/{skuId}/stock`
單獨調整某規格庫存。請求:`{ "stock": 100 }` → `SkuResponse`

### `DELETE /api/admin/products/{id}`
刪除商品(連同其 SKU、圖庫)。已有訂單的商品無法刪除(回 400,請改為下架);沒有訂單的商品會一併清除購物車、收藏、瀏覽紀錄、提問與評論中的相關資料。

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

### `POST /api/uploads/image` — 需會員登入
會員上傳圖片(評論照片用),參數同上。每位會員 10 分鐘內最多 20 張(`app.upload.member-*` 可調),超過回 429。

> 兩個上傳 API 除了檢查 `Content-Type`,還會檢查檔頭(magic bytes)是否真的是 JPG / PNG / WEBP,偽裝成圖片的檔案回 400;檔名一律改為隨機 UUID。

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

## SEO

### `GET /sitemap.xml`
公開,`application/xml`。首頁、商品列表、每個啟用分類(`/products?categoryId=`)與每個上架商品(`/products/{id}`,附 `lastmod`),網址以 `app.site.base-url`(環境變數 `SITE_BASE_URL`)組成絕對網址,最多 50,000 筆。

### `GET /robots.txt`
公開,`text/plain`。排除後台、會員中心、購物車、結帳、訂單與 `/api/`,並指向 sitemap。

## 運費(Shipping)

### `GET /api/shipping/policy`
公開。運費規則:`{ "fee": 60, "cvsFee": 45, "freeThreshold": 999 }`。商品金額(套用優惠券後、折抵購物金前)達 `freeThreshold` 免運,否則結帳時依配送方式加收 `fee`(宅配)或 `cvsFee`(超商取貨),記在訂單的 `shippingFee` 並包含在 `totalAmount`。購物金回饋只計算商品金額,不含運費。可用 `app.shipping.fee`、`app.shipping.cvs-fee`、`app.shipping.free-threshold` 調整。

---

## 購物車提醒

系統每小時檢查一次:啟用中的會員購物車最後異動超過 `app.cart.reminder-idle-hours`(預設 24 小時)且尚未結帳,發一則站內通知(連結 `/cart`)。同一個購物車狀態只提醒一次;會員之後再加入 / 修改商品、又放著超過門檻,才會再提醒。

## 訂單(Order）— 會員端需登入

### `GET /api/orders`
自己的訂單列表。Query:`status`、`page`、`size` → `PageResponse<OrderResponse>`

`OrderResponse`:
```json
{
  "id": 1, "orderNo": "ORD202609142029305718", "status": "PAID", "paymentMethod": "CREDIT_CARD",
  "subtotalAmount": 1870.00, "discountAmount": 100.00, "totalAmount": 1770.00, "couponCode": "SAVE100", "pointsUsed": 0, "shippingFee": 0,
  "receiverName": "王小明", "receiverPhone": "0912345678",
  "receiverAddress": "台北市大安區復興南路一段1號",
  "shippingCarrier": null, "trackingNumber": null, "shippedAt": null,
  "paymentDeadline": "2026-09-14T20:59:30",
  "createdAt": "2026-09-14T20:29:30",
  "items": [ { "id": 1, "skuId": 2, "skuCode": "TSHIRT-BLK-L", "productName": "經典圓領T恤", "specName": "黑色/L",
                "unitPrice": 590.00, "quantity": 1, "subtotal": 590.00 } ],
  "statusLogs": [
    { "fromStatus": null, "toStatus": "PENDING_PAYMENT", "actor": "MEMBER", "note": "訂單成立", "createdAt": "2026-09-14T20:29:30" },
    { "fromStatus": "PENDING_PAYMENT", "toStatus": "PAID", "actor": "MEMBER", "note": "會員完成付款", "createdAt": "2026-09-14T20:30:02" }
  ]
}
```
> `statusLogs` 為訂單狀態歷程,依時間由舊到新排列,每一次狀態變更(會員付款/取消、管理員出貨/取消、系統自動處理)都會新增一筆。
> `shippingCarrier`/`trackingNumber`/`shippedAt` 在管理員出貨後才有值。
> `pointsEarned` 為訂單完成時回饋的購物金;`returnDeadline` 為可申請退貨的期限(完成後 7 天,非已完成訂單為 `null`);`returnRequest` 為退貨申請摘要(`{ id, status, reason, adminNote, createdAt, processedAt }`),沒申請過為 `null`。
> `paymentDeadline` 為付款期限:線上付款(`CREDIT_CARD`/`ATM`)為下單後 30 分鐘(`app.order.payment-timeout-minutes` 可調),貨到付款(`COD`)為 `null`。逾期仍未付款的訂單會被背景排程(每分鐘掃描一次)自動取消,歸還庫存與優惠券名額,訂單歷程記為 `SYSTEM`。
> `receiverName`/`receiverPhone`/`receiverAddress` 與商品名稱/規格/單價皆為**下單當下的快照**,之後會員改地址或商家改商品都不影響歷史訂單。
> `subtotalAmount` 為套用優惠券前的商品原價小計,`totalAmount`(= `subtotalAmount` − `discountAmount` − `pointsUsed` + `shippingFee`)才是實付金額;未使用優惠券時 `discountAmount` 為 0、`couponCode` 為 `null`。

### `GET /api/orders/{id}`
自己的訂單詳情,非本人訂單回 404。

### `POST /api/orders`
結帳。請求:
```json
{ "addressId": 2, "paymentMethod": "CREDIT_CARD", "cartItemIds": [1, 2], "couponCode": "SAVE100", "pointsToUse": 100, "note": "請於平日配送" }
```
`shippingMethod` 選填,`HOME_DELIVERY`(預設,宅配,`addressId` 必填)或 `CVS_PICKUP`(超商取貨,改帶 `cvsPickup`、不需 `addressId`):
```json
{ "shippingMethod": "CVS_PICKUP",
  "cvsPickup": { "brand": "SEVEN_ELEVEN", "storeName": "信義門市", "storeCode": "123456",
                 "recipientName": "王小明", "recipientPhone": "0912345678" } }
```
`brand` 為 `SEVEN_ELEVEN` / `FAMILY_MART` / `HI_LIFE` / `OK_MART`;`storeCode` 選填(最多 8 位數字);`recipientPhone` 需為 09 開頭 10 碼手機。未串接超商電子地圖,門市由會員自填。訂單的 `receiverAddress` 會是門市描述(例如「7-ELEVEN 信義門市(店號 123456)」),回應帶 `shippingMethod`。

`invoice` 選填,電子發票開立方式(不帶為會員載具):
- `{ "type": "MEMBER_CARRIER" }` 會員載具
- `{ "type": "MOBILE_BARCODE", "carrierCode": "/ABC1234" }` 手機條碼(斜線 + 7 碼,自動轉大寫)
- `{ "type": "COMPANY", "taxId": "04595257", "companyTitle": "範例股份有限公司" }` 公司戶,統一編號以財政部檢查碼規則驗證(2023 年起被 5 整除即有效)
- `{ "type": "DONATION", "donationCode": "919" }` 捐贈,愛心碼 3~7 位數字

格式錯誤回 400;與開立方式無關的欄位會被忽略。訂單回應帶 `invoice` 物件。
`note` 選填,給賣家的備註(最多 200 字),會顯示在訂單詳情、後台與揀貨單,回傳於 `OrderResponse.buyerNote`。
`pointsToUse` 選填,使用購物金折抵(1 點 = NT$1),最多為「套用優惠券後應付金額的 50%」且不超過餘額,超過回 400。
`cartItemIds` 選填,不帶則結帳購物車全部項目。`couponCode` 選填,不帶則不使用優惠券;若代碼無效、已停用/過期/兌換完畢,或未達最低消費門檻,回 400 且不會建立訂單。下單當下就會扣庫存與優惠券使用名額(非等付款);若購物車內有商品已下架或庫存不足,整筆交易失敗回 400,購物車項目不受影響。

### `POST /api/orders/{id}/pay`
模擬付款。只能對 `PENDING_PAYMENT` 且尚未超過 `paymentDeadline` 的訂單執行(逾期回 400),成功後狀態變 `PAID`,並累加商品 `salesCount`。

### `POST /api/orders/{id}/complete`
會員確認收貨:只能對 `SHIPPING` 的訂單執行,改為 `COMPLETED` 並依會員等級回饋購物金(訂單歷程記為 `MEMBER`)。出貨後 7 天(`app.order.auto-complete-days`)仍未確認的訂單,會由背景排程自動完成(記為 `SYSTEM`)。

### `POST /api/orders/{id}/reorder`
再買一次:把該訂單的商品依原數量重新加入購物車(與購物車既有數量合併)。已下架的品項略過;庫存不足時只加入可購買的數量。任何狀態的訂單都可以執行。
```json
{ "addedCount": 1, "notices": ["「經典圓領T恤 黑色/M」庫存不足,僅加入 1 件", "「碎花洋裝 S」已下架"] }
```

### `POST /api/orders/{id}/cancel`
取消訂單。只能對 `PENDING_PAYMENT` 或 `PAID` 的訂單執行,成功後歸還庫存;若該訂單有使用優惠券,也會歸還一次使用名額。
請求 body 選填:`{ "reason": "重複下單" }`(最多 100 字),會寫入訂單歷程(「會員取消訂單:重複下單」),後台訂單詳情可看到。

### `GET /api/orders/{id}/messages`
訂單留言串(舊到新):`[ { "id": 1, "sender": "MEMBER", "content": "可以週六送嗎?", "createdAt": "..." } ]`,`sender` 為 `MEMBER` 或 `ADMIN`。非本人訂單回 404。

### `POST /api/orders/{id}/messages`
會員對自己的訂單留言。請求:`{ "content": "..." }`(1~500 字),回傳新留言。每張訂單最多 100 則(含賣家回覆),超過回 400。

> 訂單成立、付款成功、出貨、完成、取消時,都會寄一封通知信給下單會員(標題依狀態而異,例如「商品出貨通知」)。實際寄送方式與忘記密碼信共用同一套 SMTP 設定(見 README),未設定時以 log 模擬,寄送失敗也不會讓 API request 失敗。

---

## 退貨(Return)

### `POST /api/orders/{orderId}/return` — 需會員登入
申請退貨。請求:`{ "reason": "尺寸不合" }`(最多 500 字)。只有 `COMPLETED` 且在完成後 7 天鑑賞期內的訂單可申請,每筆訂單只能申請一次,否則回 400。回傳更新後的 `OrderResponse`(含 `returnRequest`)。

### `GET /api/admin/returns` — 需管理員登入
退貨申請列表(新到舊)。Query:`status`(選填)、`page`、`size`。
```json
{ "id": 1, "orderId": 12, "orderNo": "ORD2026...", "orderTotalAmount": 1080.00, "memberEmail": "you@example.com",
  "receiverName": "王小明", "reason": "尺寸不合", "status": "PENDING", "adminNote": null,
  "createdAt": "2026-09-27T18:00:00", "processedAt": null }
```

### `POST /api/admin/returns/{id}/approve` — 需管理員登入
核准退貨。請求(選填):`{ "note": "已收到退回商品" }`。訂單狀態變為 `REFUNDED`(寫入訂單歷程)、商品數量加回庫存並扣回銷量、退還該訂單使用的購物金、收回該訂單完成時回饋的購物金(若會員已花掉,只收回剩餘餘額,不會變負數),並寄送「退貨退款完成通知」。優惠券名額不退還。

### `POST /api/admin/returns/{id}/reject` — 需管理員登入
拒絕退貨。請求(選填):`{ "note": "商品已拆封使用" }`,說明會寄給會員。訂單維持 `COMPLETED`。已處理過的申請不能再核准/拒絕(400)。

---

## 管理員操作紀錄(Audit Log)— 需管理員登入

後台所有寫入操作(商品、分類、Banner、優惠券、訂單狀態、退貨審核、會員停用、問答、圖片上傳)以及訂單 CSV 匯出,都會由 AOP(`@AdminAudit` + `AdminAuditAspect`)自動記錄「哪位管理員、何時、對哪個對象、做了什麼、成功或失敗」。失敗的操作(例如不合法的狀態轉換)也會記錄錯誤訊息;紀錄以獨立交易寫入,寫入失敗不影響原本的 API。

### `GET /api/admin/audit-logs`
Query(皆選填):`adminUsername`、`targetType`(`PRODUCT`/`CATEGORY`/`BANNER`/`COUPON`/`ORDER`/`RETURN`/`MEMBER`/`QUESTION`/`UPLOAD`)、`targetId`、`page`、`size`。新到舊排序。
```json
{ "id": 12, "adminId": 1, "adminUsername": "admin", "action": "更新訂單狀態", "targetType": "ORDER",
  "targetId": "5", "detail": "SHIPPING 黑貓宅急便 TRK123", "success": true, "errorMessage": null,
  "createdAt": "2026-09-27T18:00:00" }
```

---

## 貨到通知(Stock Alerts)— 需會員登入

### `POST /api/stock-alerts/{skuId}`
訂閱缺貨規格的貨到通知(只有庫存為 0 的規格可以訂閱,重複訂閱不會重複建立)。背景排程每分鐘檢查,規格一補貨(且商品上架中)就寄站內通知與 email,並刪除訂閱。

### `DELETE /api/stock-alerts/{skuId}`
取消訂閱。

### `GET /api/stock-alerts?productId=`
我在該商品已訂閱的規格 id 陣列,例如 `[3, 5]`。

---

## 站內通知(Notifications)— 需會員登入

訂單狀態變更(成立、付款、出貨、完成、取消、退款)、退貨申請未通過、商品提問獲得回覆時、收藏的商品開始限時特價時(每 5 分鐘檢查,同一檔特價只通知一次),除了寄 email,也會建立一則站內通知(`type`:`ORDER`/`RETURN`/`QUESTION`/`SYSTEM`)。

### `GET /api/notifications`
通知列表(新到舊)。Query:`page`、`size`
```json
{ "id": 8, "type": "ORDER", "title": "商品出貨通知", "content": "訂單 ORD2026...:您的訂單已出貨...",
  "link": "/orders/12", "read": false, "createdAt": "2026-09-27T18:00:00" }
```
`link` 為前台路徑,點擊通知時導向。

### `GET /api/notifications/unread-count`
未讀數量:`{ "count": 3 }`(導覽列小紅點用)。

### `POST /api/notifications/{id}/read`
標記單則已讀(只能標記自己的通知,否則 404)。

### `POST /api/notifications/read-all`
全部標為已讀:`{ "updated": 3 }`。

---

## 購物金(Points)— 需會員登入

### `GET /api/points`
目前餘額與規則。
```json
{ "balance": 300, "earnRate": 0.01, "maxRedeemRatio": 0.5 }
```
- 訂單被管理員標記為 `COMPLETED` 時,回饋實付金額 × `earnRate` 點(無條件捨去)。
- 結帳時可用 `pointsToUse` 折抵,上限為套用優惠券後應付金額 × `maxRedeemRatio`(無條件捨去)。
- 訂單取消(會員取消、管理員取消、逾期自動取消)時,已使用的購物金會全數退回。
- 規則可用 `app.points.earn-rate`、`app.points.max-redeem-ratio` 調整。

### `GET /api/points/transactions`
購物金異動明細(新到舊)。Query:`page`、`size` → `PageResponse`
```json
{ "id": 3, "orderId": 12, "amount": -100, "type": "REDEEM", "description": "訂單 ORD2026... 折抵",
  "balanceAfter": 200, "createdAt": "2026-09-27T18:00:00" }
```

### `POST /api/admin/members/{id}/points` — 需管理員登入
手動調整會員購物金。請求:`{ "amount": 100, "reason": "客服補償" }`(`amount` 正數發放、負數扣除,單次上限 ±100000,不可為 0;扣除超過餘額回 400)。以 `ADJUST` 類型記入異動明細、寄送站內通知給會員,並記錄在管理員操作紀錄。回傳調整後的 `{ balance, earnRate, maxRedeemRatio }`。

### `GET /api/admin/members/{id}/tier` — 需管理員登入
指定會員的等級資訊,格式同 `GET /api/members/me/tier`。

### `GET /api/admin/members/{id}/points/transactions` — 需管理員登入
指定會員的購物金異動明細,格式同 `GET /api/points/transactions`。

---

## 商品評論(Reviews)

### `GET /api/products/{productId}/reviews`
公開。評論列表(新到舊)。Query:`withImages`(`true` 只看有照片的評論)、`page`、`size`。
```json
{ "id": 1, "memberName": "陳**", "rating": 5, "content": "布料厚度剛好", "images": ["/uploads/xxx.jpg"],
  "createdAt": "2026-09-27T18:00:00", "updatedAt": "2026-09-27T18:00:00",
  "sellerReply": "謝謝支持!", "repliedAt": "2026-09-28T09:00:00", "hidden": false }
```
被管理員隱藏的評論不會出現在列表,也不列入 summary 與商品評分。

### `GET /api/products/{productId}/reviews/summary`
公開。`{ "averageRating": 4.5, "reviewCount": 12 }`

### `GET /api/products/{productId}/reviews/me` — 需會員登入
自己對此商品的評論,沒有則 `data` 為 `null`。被隱藏時仍會回傳,`hidden` 為 `true`。

### `GET /api/admin/reviews` — 需管理員登入
評價管理列表(新到舊)。Query 皆選填:`rating`(1~5)、`replied`(`true`/`false`)、`hidden`(`true`/`false`)、`keyword`(商品名稱)、`page`、`size`。每筆含 `productId`、`productName`、未遮蔽的 `memberName`、`memberEmail`,以及 `sellerReply`、`repliedAt`、`hidden`。

### `PUT /api/admin/reviews/{id}/reply` — 需管理員登入
公開回覆評價:`{ "reply": "謝謝支持!" }`(最多 500 字,空白表示刪除回覆)。第一次回覆時以站內通知提醒評論者。

### `PATCH /api/admin/reviews/{id}/hidden` — 需管理員登入
`{ "hidden": true }` 隱藏 / `false` 取消隱藏,並重新計算商品評分。以上兩支都會記入管理員操作紀錄。

### `PUT /api/products/{productId}/reviews/me` — 需會員登入
新增或修改自己的評論(每人每商品一則)。需購買過此商品且訂單為已付款 / 出貨中 / 已完成才能評論。
```json
{ "rating": 5, "content": "布料厚度剛好", "images": ["/uploads/xxx.jpg"] }
```
`images` 選填,最多 5 張,只接受 `POST /api/uploads/image` 回傳的 `/uploads/...` 路徑,外部網址會回 400。

### `DELETE /api/products/{productId}/reviews/me` — 需會員登入
刪除自己的評論。評論新增 / 修改 / 刪除後,商品的 `ratingAverage` / `reviewCount` 會即時更新。

---

## 商品問答(Q&A)

### `GET /api/products/{productId}/questions`
公開。商品的提問列表(新到舊),含未回覆的提問。Query:`page`、`size` → `PageResponse<QuestionResponse>`
```json
{ "id": 1, "memberName": "王**", "content": "請問有其他顏色嗎?", "answer": "目前只有黑色",
  "answeredAt": "2026-09-27T18:00:00", "createdAt": "2026-09-27T17:30:00" }
```
> `memberName` 會遮罩成「姓氏 + **」;`answer`/`answeredAt` 尚未回覆時為 `null`。

### `POST /api/products/{productId}/questions` — 需會員登入
提問。請求:`{ "content": "請問有其他顏色嗎?" }`(最多 500 字)。只能對上架中商品提問;同一會員對同一商品一小時內最多 5 則,超過回 400。

### `GET /api/admin/questions` — 需管理員登入
所有提問。Query:`answered`(`true` 已回覆 / `false` 待回覆 / 不帶為全部)、`page`、`size`。回傳額外包含 `productId`、`productName`、`memberId`、`memberEmail` 與未遮罩的 `memberName`。

### `PUT /api/admin/questions/{id}/answer` — 需管理員登入
回覆(或修改回覆)。請求:`{ "answer": "目前只有黑色" }`(最多 1000 字)。第一次回覆時會寄 email 通知提問會員,修改回覆不會重複寄信。

### `DELETE /api/admin/questions/{id}` — 需管理員登入
刪除提問(例如廣告、不當內容)。

---

## 滿件折扣(Promotion)

結帳時自動套用、不需輸入代碼。活動範圍(指定分類含子分類,或全站)內的商品合計達 `minQuantity` 件時,這些商品打 `discountPercent`% 折扣(以實際售價含限時特價計算,無條件捨去到整數元)。同時符合多個活動只套用折扣金額最大的一個。**先套用滿件折扣,再以折扣後金額計算優惠券門檻與折抵、購物金上限與免運門檻**。訂單回應帶 `promotionDiscount` 與 `promotionName`(活動名稱快照),`totalAmount = subtotalAmount - promotionDiscount - discountAmount - pointsUsed + shippingFee`。

### `GET /api/promotions`
公開。進行中的活動:`[ { "id", "name", "categoryId", "categoryName", "minQuantity", "discountPercent", "startAt", "endAt", "active", "running" } ]`(`categoryId` 為 `null` 表示全站)。

### `GET /api/cart/promotion` — 需會員登入
以購物車試算。Query:`cartItemIds`(選填,逗號分隔,不帶為整個購物車)。
```json
{ "promotionId": 3, "promotionName": "女裝任選 2 件 9 折", "discount": 85,
  "hints": [ { "promotionId": 1, "name": "全站任選 3 件 95 折", "missingQuantity": 1, "discountPercent": 5 } ] }
```
`hints` 為已有商品在範圍內、但還差幾件才達標的活動(差最少的排前面)。`POST /api/coupons/apply` 試算的優惠券也以扣掉滿件折扣後的金額計算。

### 後台 `/api/admin/promotions` — 需管理員登入
`GET` 列表;`POST` 新增、`PUT /{id}` 修改,請求:
```json
{ "name": "女裝任選 2 件 9 折", "categoryId": 1, "minQuantity": 2, "discountPercent": 10,
  "startAt": null, "endAt": "2026-10-31T23:59:59", "active": true }
```
`minQuantity` 2~99、`discountPercent` 1~90,`startAt` / `endAt` 選填(兩者都填時結束須晚於開始)。`PATCH /{id}/active` `{ "active": false }` 啟用 / 停用;`DELETE /{id}` 刪除(已成立訂單只存快照,不受影響)。被活動使用的分類無法刪除。以上變更皆記入管理員操作紀錄。

## 優惠券(Coupon）

### `GET /api/coupons/center` — 需會員登入
領券中心:後台設為 `claimable` 且目前可用(啟用中、在期間內、未發完)的優惠券,附 `claimed` 表示自己是否已領。
```json
[ { "id": 5, "code": "SAVE100", "name": "滿千折百", "discountType": "FIXED_AMOUNT", "discountValue": 100,
    "maxDiscountAmount": null, "minSpendAmount": 1000, "endAt": "2026-12-31T23:59:59", "claimed": false } ]
```

### `POST /api/coupons/{id}/claim` — 需會員登入
領取優惠券(重複領取不會重複建立);非公開或目前不可用的券回 400。

### `GET /api/coupons/mine` — 需會員登入
我的優惠券:已領取且目前仍可使用的(排除過期、停用、發完、已用滿個人次數)。結帳時仍以代碼套用;直接輸入代碼的方式不受影響。

### `POST /api/coupons/apply` — 需會員登入
依購物車目前選取的項目試算優惠券折扣,**不會**消耗使用名額(僅供結帳頁預覽,實際扣抵在 `POST /api/orders` 結帳時才會發生)。請求:
```json
{ "code": "SAVE100", "cartItemIds": [1, 2] }
```
`cartItemIds` 選填,不帶則以購物車全部項目計算小計。回應 `CouponApplyResponse`:
```json
{
  "couponId": 1, "code": "SAVE100", "name": "新會員折抵 100 元",
  "discountType": "FIXED_AMOUNT", "discountValue": 100.00,
  "discountAmount": 100.00, "payableAmount": 1080.00
}
```
代碼不存在、已停用、未到/已過有效期間、兌換名額已滿,或購物車小計未達 `minSpendAmount` 門檻,皆回 400 並附原因訊息。

### `GET /api/admin/coupons` — 需管理員登入
Query:`keyword`(比對代碼或名稱)、`status`、`page`、`size` → `PageResponse<CouponResponse>`

`CouponResponse`:
```json
{
  "id": 1, "code": "SAVE100", "name": "新會員折抵 100 元",
  "discountType": "FIXED_AMOUNT", "discountValue": 100.00, "maxDiscountAmount": null,
  "minSpendAmount": 500.00, "totalQuantity": null, "usedQuantity": 3,
  "startAt": null, "endAt": null, "status": "ACTIVE", "createdAt": "2026-09-01T00:00:00"
}
```
> `maxDiscountAmount` 只在 `discountType` 為 `PERCENTAGE` 時有意義,作為折扣金額上限;`totalQuantity` 為 `null` 代表發放數量不限。

### `GET /api/admin/coupons/{id}` — 需管理員登入
單張優惠券詳情。

### `POST /api/admin/coupons` — 需管理員登入
新增優惠券。請求:
```json
{
  "code": "SAVE10PCT", "name": "全館 9 折", "discountType": "PERCENTAGE", "discountValue": 10,
  "maxDiscountAmount": 300, "minSpendAmount": 1000, "totalQuantity": 200,
  "startAt": "2026-09-01T00:00:00", "endAt": "2026-09-30T23:59:59"
}
```
代碼重複回 400;`startAt`/`endAt`/`totalQuantity` 皆選填,不帶代表不限。代碼會統一轉大寫儲存。

`claimable`(布林,預設 false)表示是否公開於領券中心。`perMemberLimit` 選填:每位會員最多可使用幾次(不含已取消的訂單),不填為不限;超過時試算與結帳都會回 400「此優惠券每人限用 N 次」。
### `PUT /api/admin/coupons/{id}` — 需管理員登入
更新優惠券,body 同上(代碼仍可修改,但不可與其他優惠券重複)。

### `PATCH /api/admin/coupons/{id}/status` — 需管理員登入
請求:`{ "status": "DISABLED" }`

### `POST /api/admin/coupons/{id}/issue` — 需管理員登入
把優惠券直接放進會員的「我的優惠券」並發站內通知(不需要是領券中心的券)。請求:
```json
{ "target": "TIER", "minTier": "GOLD" }
{ "target": "EMAILS", "emails": ["a@example.com", "b@example.com"] }
{ "target": "ALL" }
```
- `TIER`:等級至少為 `minTier`(`SILVER` / `GOLD`,依近 12 個月完成訂單計算)的啟用會員
- `EMAILS`:最多 1,000 個,不分大小寫;找不到或已停用的列在 `unmatchedEmails`
- 已持有的會員略過(不重複發、不重複通知);停用、過期或已兌換完畢的券回 400

回應:`{ "targeted": 12, "issued": 10, "alreadyHeld": 2, "unmatchedEmails": [] }`。會記入管理員操作紀錄。

### `DELETE /api/admin/coupons/{id}` — 需管理員登入
刪除優惠券。已被使用過(`usedQuantity > 0`)的優惠券無法刪除,回 400,請改用停用。

---

## 訂單管理(Admin）— 需管理員登入

### `GET /api/admin/orders`
所有會員的訂單。Query(皆選填):`status`、`memberId`(只看某會員)、`keyword`(比對訂單編號、收件人、收件電話、會員 Email)、`startDate` / `endDate`(`YYYY-MM-DD`,依建立日期,含頭尾)、`page`、`size` → `PageResponse<OrderResponse>`

### `GET /api/admin/orders/export`
依與列表相同的查詢條件匯出 CSV(新到舊,最多 10,000 筆),回應為檔案下載(`Content-Disposition: attachment; filename="orders-YYYYMMDD.csv"`),不是 JSON。檔案為 UTF-8 含 BOM,Excel 可直接開啟;以 `=`、`+`、`-`、`@` 開頭的文字欄位會加上 `'` 前綴,避免被 Excel 當成公式執行(CSV injection)。

### 後台內部備註 `/api/admin/notes` — 需管理員登入
管理員對訂單 / 會員留下的內部筆記,會員端看不到。
- `GET ?targetType=ORDER&targetId=5`:新到舊 `[ { "id", "adminUsername", "content", "createdAt" } ]`
- `POST` `{ "targetType": "MEMBER", "targetId": 3, "content": "客人偏好週末到貨" }`(最多 500 字)
- `DELETE /{id}`:只能刪自己寫的,刪別人的回 400
`targetType` 為 `ORDER` 或 `MEMBER`,對象不存在回 404。

### `GET /api/admin/order-messages/awaiting`
待回覆的訂單留言(每張訂單最後一則是買家留言者,舊到新),分頁。每筆:`{ "orderId", "orderNo", "memberName", "memberEmail", "lastMessage", "lastMessageAt" }`。

### `GET /api/admin/orders/{id}/messages` / `POST /api/admin/orders/{id}/messages`
後台查看 / 回覆訂單留言,格式同會員端。回覆後以站內通知提醒買家,並記入管理員操作紀錄。

### `GET /api/admin/orders/ship-template`
下載所有**已付款(待出貨)**訂單的出貨單號範本 CSV(舊到新,UTF-8 含 BOM)。欄位:`訂單編號,物流業者,物流單號,收件人,收件電話,收件地址,商品明細,訂單備註`,物流兩欄留空讓管理員填寫。

### `POST /api/admin/orders/ship-import`
批次出貨。`multipart/form-data`,欄位名 `file`;每列前三欄為 `訂單編號,物流業者,物流單號`(其餘欄位忽略,可直接上傳填好的範本),第一列可為標題列,儲存格可用雙引號包住。最多 1,000 列、512KB。
**全有或全無**:訂單不存在、不是已付款、物流欄位空白或過長、訂單重複任何一列有問題就一筆都不出貨;回應格式同 `stock-import`。成功時每筆訂單都會改為出貨中並寄出出貨通知,並記入管理員操作紀錄。

### `GET /api/admin/orders/{id}`
任意訂單詳情。

### `PATCH /api/admin/orders/{id}/status`
變更訂單狀態(出貨、標記完成、取消等)。請求:
```json
{ "status": "SHIPPING", "shippingCarrier": "黑貓宅急便", "trackingNumber": "TRK123456", "note": "選填備註" }
```
改為 `SHIPPING` 時 `shippingCarrier`、`trackingNumber` 必填(缺少回 400),會一併記錄出貨時間,出貨通知信也會附上物流單號。`note` 選填,會寫入訂單歷程(例如取消原因)。不合法的狀態轉換回 400。取消訂單會歸還庫存與優惠券使用名額。

---

## 銷售報表(Admin）— 需管理員登入

三支皆接受 Query:`startDate`、`endDate`(格式 `yyyy-MM-dd`,不帶則預設近 30 天,含今天)。統計只計入 `PAID`/`SHIPPING`/`COMPLETED` 三種狀態的訂單(排除待付款與已取消),金額皆以套用優惠券後的實付金額(`total_amount`)計算。

### `GET /api/admin/reports/dashboard`
後台首頁總覽。
```json
{ "todayOrders": 4, "todayRevenue": 2500.00, "todayNewMembers": 2,
  "pendingPaymentOrders": 1, "ordersToShip": 5, "pendingReturns": 1,
  "unansweredQuestions": 3, "awaitingOrderMessages": 2, "lowStockSkus": 6, "lowStockThreshold": 10 }
```
`awaitingOrderMessages` 為最後一則是買家留言、等待回覆的訂單數。
`ordersToShip` 為已付款待出貨(`PAID`)的訂單數;`lowStockSkus` 為上架商品中庫存 ≤ `lowStockThreshold` 的規格數。

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

### `GET /api/admin/reports/categories`
各**頂層分類**的銷售彙總(子分類的銷售併入其頂層分類),營收高的排前面。Query:`startDate`、`endDate`(同其他報表,預設近 30 天)。只計算已付款 / 出貨中 / 已完成的訂單。
```json
[ { "categoryId": 1, "categoryName": "服飾", "soldQuantity": 12, "revenue": 8900.00, "share": 62.5 } ]
```
`share` 為營收佔比(百分比,小數一位)。

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
| PUT | `/api/members/me/password` | 會員 |
| DELETE | `/api/members/me` | 會員 |
| GET / POST | `/api/members/addresses` | 會員 |
| PUT / DELETE | `/api/members/addresses/{id}` | 會員 |
| POST | `/api/admin/auth/login` | 公開 |
| GET | `/api/admin/members` | 管理員 |
| GET | `/api/admin/members/{id}` | 管理員 |
| PATCH | `/api/admin/members/{id}/status` | 管理員 |
| GET | `/api/admin/members/{id}/tier` | 管理員 |
| POST | `/api/admin/members/{id}/points` | 管理員 |
| GET | `/api/admin/members/{id}/points/transactions` | 管理員 |
| GET | `/api/categories` | 公開 |
| GET / POST | `/api/admin/categories` | 管理員 |
| PUT / DELETE | `/api/admin/categories/{id}` | 管理員 |
| PATCH | `/api/admin/categories/{id}/status` | 管理員 |
| GET | `/api/products` | 公開 |
| GET | `/api/products/{id}` | 公開 |
| GET | `/api/products/suggestions` | 公開 |
| GET | `/api/products/hot-searches` | 公開 |
| GET | `/api/products/flash-sale` | 公開 |
| GET | `/api/shipping/policy` | 公開 |
| GET | `/api/promotions` | 公開 |
| GET | `/api/cart/promotion` | 會員 |
| GET | `/api/products/{id}/bought-together` | 公開 |
| GET | `/api/products/{id}/related` | 公開 |
| GET | `/api/recommendations` | 會員 |
| GET | `/api/products/{productId}/reviews` | 公開 |
| GET | `/api/products/{productId}/reviews/summary` | 公開 |
| GET / PUT / DELETE | `/api/products/{productId}/reviews/me` | 會員 |
| GET | `/api/products/{productId}/questions` | 公開 |
| POST | `/api/products/{productId}/questions` | 會員 |
| GET | `/api/admin/questions` | 管理員 |
| GET | `/api/admin/reviews` | 管理員 |
| PUT | `/api/admin/reviews/{id}/reply` | 管理員 |
| PATCH | `/api/admin/reviews/{id}/hidden` | 管理員 |
| PUT | `/api/admin/questions/{id}/answer` | 管理員 |
| DELETE | `/api/admin/questions/{id}` | 管理員 |
| GET / POST | `/api/admin/products` | 管理員 |
| GET / PUT / DELETE | `/api/admin/products/{id}` | 管理員 |
| PATCH | `/api/admin/products/{id}/status` | 管理員 |
| PATCH | `/api/admin/products/{productId}/skus/{skuId}/stock` | 管理員 |
| POST | `/api/admin/uploads/image` | 管理員 |
| POST | `/api/uploads/image` | 會員 |
| GET / DELETE | `/api/cart` | 會員 |
| POST | `/api/cart/items` | 會員 |
| PUT / DELETE | `/api/cart/items/{itemId}` | 會員 |
| GET / POST | `/api/orders` | 會員 |
| GET | `/api/orders/{id}` | 會員 |
| POST | `/api/orders/{id}/pay` | 會員 |
| POST | `/api/orders/{id}/complete` | 會員 |
| POST | `/api/orders/{id}/reorder` | 會員 |
| POST | `/api/orders/{orderId}/return` | 會員 |
| GET | `/api/admin/returns` | 管理員 |
| POST | `/api/admin/returns/{id}/approve` | 管理員 |
| POST | `/api/admin/returns/{id}/reject` | 管理員 |
| GET | `/api/members/me/tier` | 會員 |
| GET | `/api/points` | 會員 |
| GET | `/api/stock-alerts` | 會員 |
| POST / DELETE | `/api/stock-alerts/{skuId}` | 會員 |
| GET | `/api/notifications` | 會員 |
| GET | `/api/notifications/unread-count` | 會員 |
| POST | `/api/notifications/{id}/read` | 會員 |
| POST | `/api/notifications/read-all` | 會員 |
| GET | `/api/points/transactions` | 會員 |
| POST | `/api/coupons/apply` | 會員 |
| GET | `/api/coupons/center` | 會員 |
| POST | `/api/coupons/{id}/claim` | 會員 |
| GET | `/api/coupons/mine` | 會員 |
| GET / POST | `/api/admin/promotions` | 管理員 |
| PUT / DELETE | `/api/admin/promotions/{id}` | 管理員 |
| PATCH | `/api/admin/promotions/{id}/active` | 管理員 |
| GET / POST | `/api/admin/coupons` | 管理員 |
| GET / PUT / DELETE | `/api/admin/coupons/{id}` | 管理員 |
| PATCH | `/api/admin/coupons/{id}/status` | 管理員 |
| POST | `/api/admin/coupons/{id}/issue` | 管理員 |
| POST | `/api/orders/{id}/cancel` | 會員 |
| GET / POST | `/api/orders/{id}/messages` | 會員 |
| GET | `/api/admin/order-messages/awaiting` | 管理員 |
| GET / POST | `/api/admin/notes` | 管理員 |
| DELETE | `/api/admin/notes/{id}` | 管理員 |
| GET / POST | `/api/admin/orders/{id}/messages` | 管理員 |
| PATCH | `/api/admin/products/status` | 管理員 |
| POST | `/api/admin/products/stock-import` | 管理員 |
| GET | `/api/admin/products/low-stock` | 管理員 |
| GET | `/api/admin/products/{productId}/skus/{skuId}/stock-movements` | 管理員 |
| GET | `/api/admin/orders` | 管理員 |
| GET | `/api/admin/orders/export` | 管理員 |
| GET | `/api/admin/orders/ship-template` | 管理員 |
| POST | `/api/admin/orders/ship-import` | 管理員 |
| GET | `/api/admin/orders/{id}` | 管理員 |
| PATCH | `/api/admin/orders/{id}/status` | 管理員 |
| GET | `/api/admin/reports/dashboard` | 管理員 |
| GET | `/api/admin/audit-logs` | 管理員 |
| GET | `/api/admin/reports/summary` | 管理員 |
| GET | `/api/admin/reports/top-products` | 管理員 |
| GET | `/api/admin/reports/categories` | 管理員 |
| GET | `/api/admin/reports/daily` | 管理員 |
