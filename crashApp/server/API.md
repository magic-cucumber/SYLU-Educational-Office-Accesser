# POST /test

验证客户端持有的 RSA 公钥与服务端私钥是否匹配。

## Header

`Content-Type: application/json`

## 请求参数

| 名字       | 简介           | 限制                                                   |
|----------|--------------|------------------------------------------------------|
| `first`  | `Pair` 的首个字段 | 固定为 `payload`                                        |
| `second` | RSA 加密数据     | 原文必须为 128 字节随机数据；使用 RSA PKCS#1 v1.5 加密后再进行 Base64 编码 |

服务端解密 `second` 后，将原文按 Base64 编码写入 `data`。

## 响应示例

```json
{
  "success": true,
  "message": null,
  "data": "Base64 编码的 128 字节原文"
}
```

# PUT /report

提交 AES 密钥并申请一次性上传令牌。

## Header

`Content-Type: application/json`

## 请求参数

| 名字       | 简介             | 限制                                                             |
|----------|----------------|----------------------------------------------------------------|
| `first`  | `Pair` 的首个字段   | 固定为 `payload`                                                  |
| `second` | RSA 加密的 AES 密钥 | 原文必须为 32 字节 AES-256 RAW 密钥；使用 RSA PKCS#1 v1.5 加密后再进行 Base64 编码 |

## 响应示例

```json
{
  "success": true,
  "message": null,
  "data": "一次性上传 token"
}
```

# POST /report

上传使用申请令牌时所提交 AES 密钥加密的崩溃报告。

## Header

`Content-Type: multipart/form-data; boundary=<自动生成>`

## 请求参数

| 名字      | 简介       | 限制                                                                           |
|---------|----------|------------------------------------------------------------------------------|
| `token` | 上传令牌     | 必须来自 `PUT /report`，且仅供本次上传使用                                                 |
| `file`  | 加密后的崩溃报告 | 文件名为 `<UUID>.bin`，分片类型为 `application/octet-stream`；明文是 ZIP，使用 AES-256-CBC 加密 |

## 响应示例

```json
{
  "success": true,
  "message": null,
  "data": null
}
```

# 通用响应

所有接口均返回 JSON。HTTP 状态码必须为 2xx，且 `success` 必须为 `true`；否则客户端终止自动上报。

| 名字        | 简介      | 限制            |
|-----------|---------|---------------|
| `success` | 业务处理结果  | 布尔值           |
| `message` | 提示或错误信息 | 可为 `null`     |
| `data`    | 接口返回数据  | 可为字符串或 `null` |

失败响应示例：

```json
{
  "success": false,
  "message": "错误原因",
  "data": null
}
```

> 当前客户端使用 Kotlin `Pair` 作为两个 JSON 请求体，因此实际字段名为 `first`、`second`。AES-CBC 的 IV、填充及密文封装格式由当前加密库生成，`AppModel.kt` 未单独声明协议格式，服务端实现必须与该库的输出保持一致。
