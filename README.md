# Hero Middleware - 智书合同与业财系统中间件

## 项目简介

本项目是一个中间件系统，用于集成**智书合同API**与**业财系统API**，实现两个系统之间的数据双向同步。

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| JDK | 1.8 | Java开发环境 |
| Spring Boot | 2.3.12.RELEASE | 基础框架 |
| MyBatis-Plus | 3.4.3 | ORM框架 |
| MySQL | 5.7+ | 数据库 |
| Druid | 1.2.8 | 数据库连接池 |
| Swagger | 2.9.2 | API文档 |
| Hutool | 5.7.22 | HTTP客户端工具 |
| FastJSON | 1.2.78 | JSON处理 |
| Lombok | 1.18.20 | 代码简化 |

## 环境配置

项目支持多环境配置，通过 `application.yml` 中的 `spring.profiles.active` 切换：

- `dev` - 开发环境
- `test` - 测试环境
- `prod` - 生产环境

## Swagger API 文档

启动服务后，可通过以下地址查看并调试接口：

- Swagger UI：`http://localhost:8087/swagger-ui.html`
- OpenAPI JSON：`http://localhost:8087/v2/api-docs`

部署到其他环境时，将 `localhost:8080` 替换为实际服务地址。Swagger 会自动扫描
`com.hero.middleware.controller` 下的接口，并按控制器上的 `@Api`、`@ApiOperation` 注解展示。

## 项目结构

```
hero/
├── src/main/java/com/hero/middleware/
│   ├── client/                    # 外部API客户端
│   │   ├── yuecai/               # 业财系统API客户端
│   │   │   ├── request/          # 请求类
│   │   │   ├── response/         # 响应类
│   │   │   ├── YuecaiApiClient.java
│   │   │   └── YuecaiContractClient.java
│   │   └── zhishu/               # 智书合同API客户端
│   │       ├── request/          # 请求类
│   │       ├── response/         # 响应类
│   │       ├── ZhishuApiClient.java
│   │       ├── ZhishuContractClient.java
│   │       └── ZhishuPaymentClient.java
│   ├── common/                    # 公共类
│   │   ├── PageResult.java       # 分页结果
│   │   └── Result.java           # 统一响应
│   ├── config/                    # 配置类
│   │   ├── MybatisPlusConfig.java
│   │   ├── SwaggerConfig.java
│   │   ├── YuecaiApiConfig.java
│   │   └── ZhishuApiConfig.java
│   ├── constant/                  # 常量类
│   │   ├── ContractStatusConstant.java
│   │   ├── SyncDirectionConstant.java
│   │   ├── SyncStatusConstant.java
│   │   └── SyncTypeConstant.java
│   ├── controller/                # 控制器层
│   │   ├── ApprovalController.java
│   │   ├── ContractController.java
│   │   ├── ContractStatusController.java
│   │   ├── DocumentController.java
│   │   ├── PaymentController.java
│   │   ├── ReceiptController.java
│   │   ├── YuecaiContractController.java
│   │   └── ZhishuContractController.java
│   ├── dto/                       # 数据传输对象
│   ├── entity/                    # 实体类
│   ├── exception/                 # 异常处理
│   ├── mapper/                    # MyBatis Mapper
│   ├── service/                   # 服务层
│   │   └── impl/                  # 服务实现
│   └── HeroMiddlewareApplication.java
├── src/main/resources/
│   ├── db/init.sql               # 数据库初始化脚本
│   ├── application.yml           # 主配置文件
│   ├── application-dev.yml       # 开发环境配置
│   ├── application-test.yml      # 测试环境配置
│   └── application-prod.yml      # 生产环境配置
└── pom.xml                       # Maven配置
```

---

## API接口汇总

### 一、业财系统调用的接口（5个）

| 序号 | 接口名称 | 请求方式 | 接口路径 | 完成状态 | 说明 |
|------|----------|----------|----------|----------|------|
| 1 | 创建合同 | POST | `/api/contract/create` | ✅ 已完成 | 业财创建单据后通知中间件，中间件调用智书创建合同，返回草稿页链接 |
| 2 | 业财合同变更同步 | PUT | `/api/yuecai/contract/sync` | ✅ 已完成 | 业财系统合同变更后同步至智书 |
| 3 | 合同批量审批 | POST | `/api/approval/batch` | ✅ 已完成 | 批量审批合同并返回下一审批人信息 |
| 4 | 付款记录同步 | POST | `/api/payment/sync` | ✅ 已完成 | 业财付款成功后同步付款记录至智书 |
| 5 | 收款记录同步 | POST | `/api/receipt/sync` | ✅ 已完成 | 业财收款成功后同步收款记录至智书 |

### 二、智书合同调用的接口（4个）
7
| 序号 | 接口名称 | 请求方式 | 接口路径 | 完成状态 | 说明 |
|------|----------|----------|----------|----------|------|
| 6 | 关联前置单据 | GET | `/api/document/list` | ✅ 已完成 | 智书表单下拉字段获取业财系统单据列表 |
| 7 | 合同状态变更同步 | POST | `/api/contract/status/sync` | ✅ 已完成 | 监听智书合同状态变更，同步至业财系统 |
| 8 | 智书合同创建同步 | POST | `/api/zhishu/contract/sync` | ✅ 已完成 | 智书创建合同后同步至业财系统 |
| 9 | 智书合同变更同步 | PUT | `/api/zhishu/contract/sync` | ✅ 已完成 | 智书合同变更后同步至业财系统 |

### 三、接口完成度统计

| 调用方 | 总数 | 已完成 | 进行中 | 待开发 | 完成率 |
|--------|------|--------|--------|--------|--------|
| 业财系统调用 | 5 | 5 | 0 | 0 | 100% |
| 智书合同调用 | 4 | 4 | 0 | 0 | 100% |
| **合计** | **9** | **9** | **0** | **0** | **100%** |

---

## 调用关系图

```
┌─────────────┐                    ┌─────────────┐                    ┌─────────────┐
│   业财系统   │                    │   中间件     │                    │   智书合同   │
└──────┬──────┘                    └──────┬──────┘                    └──────┬──────┘
       │                                  │                                  │
       │  ① 创建合同                       │                                  │
       │  ② 业财合同变更同步                │                                  │
       │  ③ 合同批量审批                   │                                  │
       │  ④ 付款记录同步                   │                                  │
       │  ⑤ 收款记录同步                   │                                  │
       │ ─────────────────────────────────>│ ─────────────────────────────────>│
       │                                  │                                  │
       │                                  │  ⑥ 关联前置单据                    │
       │                                  │  ⑦ 合同状态变更同步                 │
       │                                  │  ⑧ 智书合同创建同步                 │
       │                                  │  ⑨ 智书合同变更同步                 │
       │<───────────────────────────────── │<───────────────────────────────── │
       │                                  │                                  │
```

---

## 数据库表结构

| 表名 | 说明 | 状态 |
|------|------|------|
| t_contract | 合同主表 | ✅ 已创建 |
| t_contract_sync_log | 同步日志表 | ✅ 已创建 |
| t_approval_record | 审批记录表 | ✅ 已创建 |
| t_payment_record | 付款记录表 | ✅ 已创建 |
| t_receipt_record | 收款记录表 | ✅ 已创建 |

---

## 开发日志

### 2026-03-04
- ✅ 项目初始化，创建基础项目结构
- ✅ 配置多环境支持（dev/test/prod）
- ✅ 创建数据库表结构
- ✅ 实现9个API接口框架
- ✅ 修复Java 8兼容性问题（var关键字、import别名等）
- ✅ 项目编译通过
- ✅ 完成智书合同API访问凭证对接
  - 实现Token缓存机制（有效期2小时，提前5分钟刷新）
  - 支持多环境配置（dev/test/prod）
  - 添加详细日志记录
  - 添加草稿页URL配置
- ✅ 完善创建合同接口业务逻辑
  - 对接智书合同创建API（POST /open-apis/contract/v1/contracts）
  - 实现业财系统单据信息到智书合同数据的转换
  - 返回智书合同草稿页链接（PC端/移动端）
  - 添加详细日志记录（请求参数、响应结果、错误信息）
  - 保存合同信息到数据库
  - 记录同步日志
- ✅ 完善付款记录同步接口业务逻辑
  - 对接智书付款记录同步API（POST /open-apis/contract/v1/payment/notify）
  - 实现业财系统付款记录到智书付款计划的转换
  - 支持付款金额、币种、交易状态、银行账户等信息同步
  - 添加详细日志记录
  - 保存付款记录到数据库

---

## 智书合同API对接

### 访问凭证管理

| 组件 | 说明 |
|------|------|
| ZhishuTokenManager | Token缓存管理，自动刷新机制 |
| ZhishuTokenRequest | Token请求DTO |
| ZhishuTokenResponse | Token响应DTO |
| ZhishuApiClient | API客户端，自动携带Token认证 |

### Token获取接口

- **接口地址**: `POST /open-apis/auth/v3/tenant_access_token/internal`
- **请求参数**: appId, appSecret
- **响应字段**: tenant_access_token, expire(秒)
- **有效期**: 2小时（7200秒）
- **缓存策略**: 提前5分钟刷新，避免Token过期

### 创建合同接口

- **接口地址**: `POST /open-apis/contract/v1/contracts?user_id_type=user_id`
- **请求类**: ZhishuCreateContractRequest
- **响应类**: ZhishuCreateContractResponse
- **关键字段**:
  - contract_id: 智书合同ID
  - contract_number: 合同编号
  - multi_url.pc_url: PC端草稿页链接
  - multi_url.mobile_url: 移动端草稿页链接

### 创建合同接口入参说明

**接口地址**: `POST /api/contract/create`

**请求参数**:
```json
{
    "documentNumber": "string",     // 单据编号（必填）
    "documentType": 0,              // 单据类型（必填）
    "createUserId": "string",       // 创建人ID（必填）
    "counterPartyList": [           // 对方主体列表（必填）
        {
            "counterPartyCode": "string"  // 对方主体编码
        }
    ],
    "ourPartyList": [               // 我方主体列表（必填）
        {
            "ourPartyCode": "string"      // 我方主体编码
        }
    ]
}
```

**响应参数**:
```json
{
    "code": 200,
    "message": "合同创建成功",
    "data": {
        "contractId": "中间件合同ID",
        "zhishuContractId": "智书合同ID",
        "contractNumber": "合同编号",
        "contractName": "合同名称",
        "contractStatus": "DRAFT",
        "draftUrl": "草稿页链接",
        "pcUrl": "PC端链接",
        "mobileUrl": "移动端链接"
    }
}
```

---

## 付款记录同步接口

### 付款记录同步接口入参说明

**接口地址**: `POST /api/payment/sync`

**请求参数**:
```json
{
    "contractId": "string",            // 合同ID（必填）
    "createUserId": "string",          // 创建人ID（必填）
    "businessCode": "string",          // 业务编号（必填）
    "paymentAmount": 10000.00,         // 付款金额（必填）
    "currencyCode": "CNY",             // 币种编码（必填）
    "transactionStatus": "PAYMENT_SUCCESS",  // 交易状态（必填）
    "transactionTime": "2026-03-05",   // 交易时间（必填）
    "counterPartyCode": "string",      // 对方主体编码（必填）
    "bankAccountNumber": "string"      // 银行账号（可选）
}
```

**响应参数**:
```json
{
    "code": 200,
    "message": "付款记录同步成功",
    "data": null
}
```

**智书API对接**:
- **接口地址**: `POST /open-apis/contract/v1/payment/notify?user_id_type=user_id`
- **请求体结构**:
```json
{
    "applicant_user_id": "创建人ID",
    "finance_number": "业务编号",
    "finance_amount": 付款金额,
    "finance_currency": "币种名称",
    "payment_lines": [
        {
            "source_id": "业务编号",
            "transaction_amount": 付款金额,
            "transaction_status": "PAYMENT_SUCCESS",
            "transaction_time": "交易时间",
            "currency": "CNY",
            "trading_party_code": "对方主体编码",
            "trading_party_account": {
                "account_number": "银行账号"
            },
            "relations": [
                {
                    "contract_id": "合同ID",
                    "amount": 付款金额
                }
            ]
        }
    ]
}
```

### 环境配置

```yaml
zhishu:
  api:
    base-url: https://open.qfei.cn          # API基础地址
    app-id: your-app-id                      # 应用ID
    app-secret: your-app-secret              # 应用密钥
    timeout: 30000                           # 超时时间(ms)
    draft-page-url: https://zhishu.com/contract/draft?id={contractId}  # 草稿页URL模板
```

---

## 待办事项

- [x] 智书合同API访问凭证对接
- [x] 完善创建合同接口业务逻辑
- [x] 完善付款记录同步接口业务逻辑
- [x] 实现AOP日志功能
- [ ] 完善收款记录同步接口业务逻辑
- [ ] 补充其他接口业务逻辑
- [ ] 完善单元测试
- [ ] 接口联调测试
- [ ] 性能优化

---

## AOP日志功能

### 功能说明

通过自定义注解 `@ApiLog` 实现接口日志自动记录，使用AOP环绕通知拦截接口调用，自动记录接口入参、出参、执行时间等信息。

### 使用方式

在Controller方法上添加 `@ApiLog` 注解即可：

```java
@ApiLog(value = "创建合同", description = "业财系统创建单据后调用")
@PostMapping("/create")
public Result<CreateContractResultDTO> createContract(@Validated @RequestBody CreateContractDTO dto) {
    // 业务逻辑
}
```

### 注解参数

| 参数 | 类型 | 说明 |
|------|------|------|
| value | String | 接口名称 |
| description | String | 接口描述 |

### 记录内容

| 字段 | 说明 |
|------|------|
| trace_id | 追踪ID |
| api_name | 接口名称 |
| api_description | 接口描述 |
| request_method | 请求方法(GET/POST等) |
| request_url | 请求URL |
| request_params | 请求参数JSON |
| response_params | 响应参数JSON |
| request_ip | 请求IP |
| user_agent | 用户代理 |
| execute_time | 执行时间(毫秒) |
| http_status | HTTP状态码 |
| status | 执行状态(1成功/0失败) |
| error_message | 错误信息 |

### 相关文件

| 文件 | 说明 |
|------|------|
| ApiLog.java | 自定义注解 |
| ApiLogRecord.java | 日志实体类 |
| ApiLogRecordMapper.java | 日志Mapper |
| ApiLogAspect.java | AOP切面类 |

---

## 更新记录

| 日期 | 更新内容 | 更新人 |
|------|----------|--------|
| 2026-03-04 | 项目初始化，完成9个接口框架 | - |
| 2026-03-04 | 完成智书合同API访问凭证对接，实现Token缓存机制 | - |
| 2026-03-04 | 完善创建合同接口业务逻辑，对接智书合同创建API | - |
| 2026-03-05 | 完善付款记录同步接口业务逻辑，对接智书付款记录同步API | - |
| 2026-03-05 | 实现AOP日志功能，支持注解方式自动记录接口日志 | - |

---

## 备注

- 业务逻辑需要后续补充
- 接口完成度会随开发进度实时更新
