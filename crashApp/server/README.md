# Crash Report Server

## 启动参数

服务默认监听 `8080` 端口：

```text
server --cert-path <RSA私钥PEM文件> [--port 8080] [--save-dir ./output]
        [--max-transport-size 5MB] [--debug-mode]
```

| 参数                     | 必填 | 默认值        | 说明                                         |
|------------------------|----|------------|--------------------------------------------|
| `--port`               | 否  | `8080`     | HTTP 监听端口，范围 `1-65535`                     |
| `--save-dir`           | 否  | `./output` | 解密后 ZIP 的保存目录                              |
| `--cert-path`          | 是  | 无          | 未加密的 RSA PKCS#1 或 PKCS#8 私钥 PEM 文件         |
| `--max-transport-size` | 否  | `5MB`      | 加密报告文件的最大大小；支持字节数以及 `KB/KiB/MB/MiB/GB/GiB` |
| `--debug-mode`         | 否  | `false`    | 每个请求额外延迟 3 秒                               |

## 通用约定

- 以下路径均相对于服务地址，例如 `http://127.0.0.1:8080`。
- 成功和失败响应均为 JSON：

  ```json
  {
    "success": true,
    "message": null,
    "data": "..."
  }
  ```

- 失败时 `success` 为 `false`，`message` 包含原因，`data` 为 `null`。
- RSA 接口的请求体来自 Kotlin `Pair`，实际字段名固定为 `first`、`second`：

  ```json
  {
    "first": "payload",
    "second": "<Base64 编码的 RSA 密文>"
  }
  ```

- `first` 必须为 `payload`；`second` 必须是使用服务端 RSA 公钥加密后再进行标准 Base64 编码的密文。

## `POST /test`

用于测试客户端 RSA 公钥与服务端私钥是否匹配。

请求头：`Content-Type: application/json`

请求参数：

| 字段       | 类型     | 限制                                                  |
|----------|--------|-----------------------------------------------------|
| `first`  | string | 固定为 `payload`                                       |
| `second` | string | RSA PKCS#1 v1.5 密文；解密后的原文必须正好是 128 字节，再进行 Base64 编码 |

成功响应中的 `data` 是那 128 字节原文的 Base64 字符串：

```json
{
  "success": true,
  "message": null,
  "data": "<Base64 编码的 128 字节原文>"
}
```

## `PUT /report`

使用 RSA 加密 AES 密钥，申请一次性上传 token。

请求头：`Content-Type: application/json`

请求参数：

| 字段       | 类型     | 限制                                                                |
|----------|--------|-------------------------------------------------------------------|
| `first`  | string | 固定为 `payload`                                                     |
| `second` | string | RSA PKCS#1 v1.5 密文；解密后的原文必须正好是 32 字节 AES-256 RAW 密钥，再进行 Base64 编码 |

成功响应中的 `data` 是上传 token：

```json
{
  "success": true,
  "message": null,
  "data": "<一次性上传 token>"
}
```

token 有效期为 1 分钟，且只能使用一次。申请后应立即调用 `POST /report`。

## `POST /report`

上传使用上一步 AES-256 密钥加密的崩溃报告。

请求头：`Content-Type: multipart/form-data; boundary=<自动生成>`

表单参数：

| 字段      | 类型      | 限制                                                          |
|---------|---------|-------------------------------------------------------------|
| `token` | form 字段 | 必须是 `PUT /report` 返回的有效 token；token 在校验上传内容前即被消费            |
| `file`  | 文件字段    | 必须且只能有一个，文件分片 `Content-Type` 必须为 `application/octet-stream` |

`file` 还必须满足：

- 文件名为 `<UUID>.bin`；支持 32 位十六进制 UUID，或带连字符的标准 UUID。
- 加密文件大小不得超过 `--max-transport-size`，默认 `5MB`。
- 文件内容格式为：`16 字节随机 IV + AES-256-CBC 密文`。
- AES 使用 PKCS#5/PKCS#7 填充；解密后的明文必须是有效 ZIP 文件。

服务端解密成功后，会以同一 UUID 命名并保存为 `<UUID>.zip`，保存位置由 `--save-dir` 决定。

成功响应：

```json
{
  "success": true,
  "message": null,
  "data": null
}
```

## 常见 HTTP 状态码

| 状态码   | 含义                            |
|-------|-------------------------------|
| `200` | 请求成功                          |
| `400` | JSON、RSA 密文、token、文件参数或加密内容无效 |
| `413` | 上传超过 `--max-transport-size`   |
| `500` | 服务端生成 token 或保存 ZIP 失败        |
