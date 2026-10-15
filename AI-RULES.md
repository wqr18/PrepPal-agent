# AI-RULES.md — PrepPal 全局开发规则（核心约束）

> 本文件是 AI 参与 PrepPal 开发的总约束。所有代码生成、修改、重构必须遵守本规则。

---

## 1. 项目简介与技术栈

### 1.1 项目简介
PrepPal = 高校教师智能备课助手。基于大语言模型 + RAG，帮助教师快速生成结构化教案。
后端 Spring Boot + 微信小程序双端架构。

### 1.2 完整技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 3.5.16 |
| 语言 | Java | 17 |
| 构建 | Maven（mvnw） | — |
| 数据库 | MySQL | 8.x |
| ORM | MyBatis-Plus（构件名 `mybatis-plus-spring-boot3-starter`，Boot 3 专用） | 3.5.17 |
| AI 框架 | Spring AI（OpenAI 兼容接口，BOM `spring-ai-bom:1.0.6`） | 1.0.6 |
| AI 服务 | 硅基流动 SiliconFlow（base-url: https://api.siliconflow.cn/v1） | — |
| 嵌入模型 | BAAI/bge-m3（1024 维，多语言多粒度） | 智源研究院 |
| 配置加载 | spring-dotenv（自动读取项目根 `.env`） | 4.0.0 |
| JWT | jjwt | 0.12.6 |
| 工具 | Lombok | 随 Spring Boot |
| 前端 | 微信小程序原生 | 基础库 3.17.3 |
| UI 库 | Vant Weapp | 已引入 miniprogram_npm |

> **版本选型说明**：
> - Spring Boot **3.5.16**（非 4.x）：MyBatis-Plus 对 Boot 4 的支持尚不完善，3.5.x 可获得稳定的 ORM 支持
> - Spring AI **1.0.6**（非 2.0）：2.0 的 starter 传递依赖 Spring Boot 4 模块，与 Boot 3.5.16 产生 `JacksonAutoConfiguration` Bean 冲突；1.0.6 原生适配 Boot 3.5.x，无此问题
> - Spring AI 1.0.x 模型配置用 `spring.ai.openai.chat.options.model` / `spring.ai.openai.embedding.options.model`（2.0 才改为直接 `.model`）

### 1.3 目录结构

> **包结构按业务模块组织**，每个模块自带 controller/service/mapper/entity/dto，不使用全局的 controller/service 目录。

```
PrepPal-agent/
├── PrepPal/                    # 后端（Spring Boot）
│   ├── src/main/java/com/preppal/
│   │   ├── PrepPalApplication.java
│   │   ├── common/             # 通用：统一响应 Result、异常、JWT、工具类
│   │   ├── config/             # 配置类（MyBatis-Plus、Spring AI、跨域等）
│   │   └── module/             # 业务模块
│   │       ├── user/           # 用户模块（controller/service/mapper/entity/dto）
│   │       ├── course/         # 课程模块
│   │       ├── lesson/         # 教案模块
│   │       ├── rag/            # RAG 模块（资料上传、解析、检索）
│   │       └── ai/             # AI 模块（LLM 调用封装）
│   ├── src/main/resources/
│   │   ├── application.properties
│   │   └── mapper/             # MyBatis XML（按模块分子目录）
│   └── pom.xml
├── preppal-miniprogram/        # 微信小程序
│   ├── pages/                  # 页面
│   ├── components/             # 自定义组件
│   ├── utils/                  # 工具函数（request 封装等）
│   ├── app.js / app.json / app.wxss
│   └── project.config.json
├── requirement.md              # 产品需求
├── AI-RULES.md                 # 本文件（AI 开发规则）
└── AI-CONTEXT.md               # 业务上下文（实体/表结构/接口）
```

---

## 2. 启动 / 打包 / 测试命令

### 后端

```bash
# 开发启动（在 PrepPal/ 目录下）
./mvnw spring-boot:run

# 打包
./mvnw clean package -DskipTests

# 运行测试
./mvnw test

# 仅编译
./mvnw compile
```

> Windows 环境使用 `mvnw.cmd`，Linux/Mac 使用 `./mvnw`。

### 前端

使用微信开发者工具打开 `preppal-miniprogram/` 目录，点击编译运行。

---

## 3. 代码规范

### 3.1 Java 后端

- **包结构**：严格按 `controller / service / mapper / entity / dto / config / common` 分层
- **命名**：
  - 类名大驼峰：`LessonPlanController`、`UserService`
  - 方法名小驼峰：`generateLessonPlan()`
  - 常量全大写下划线：`DEFAULT_PAGE_SIZE`
  - 数据库表名小写下划线：`lesson_plan`
- **Lombok**：实体类用 `@Data`、`@Builder`；Controller 不写 getter/setter
- **响应格式**：统一用 `Result<T>` 封装，格式 `{ code, message, data }`
- **异常处理**：全局异常处理器 `@RestControllerAdvice`，业务异常抛 `BusinessException`
- **接口前缀**：所有 API 统一 `/api` 前缀

### 3.2 小程序前端

- **页面文件**：`pages/xxx/xxx.js` + `xxx.wxml` + `xxx.wxss` + `xxx.json`
- **请求封装**：统一使用 `utils/request.js`，baseURL 从配置读取，不硬编码
- **UI 组件**：优先使用 Vant Weapp，避免重复造轮子
- **样式单位**：统一用 `rpx`
- **命名**：页面文件名小写，方法名小驼峰

### 3.3 注释与格式

- **禁止**写无意义注释（如 `// 设置 name`）
- 复杂业务逻辑必须写注释说明意图
- Java 用 4 空格缩进；小程序用 2 空格缩进
- 行宽不超过 120 字符

---

## 4. 禁止修改目录 / 文件黑名单

以下文件/目录未经明确指令不得修改：

| 路径 | 原因 |
|------|------|
| `preppal-miniprogram/miniprogram_npm/` | 第三方依赖包，禁止手改 |
| `PrepPal/target/` | Maven 构建产物 |
| `PrepPal/.env` | 本地环境变量（密钥等），禁止提交或修改格式 |
| `preppal-miniprogram/project.config.json` | 小程序项目配置，appid 等不可乱改 |
| `AI-RULES.md` / `AI-CONTEXT.md` / `requirement.md` | 项目规则文档，仅可追加不可删除 |

---

## 5. AI 工作流程

### 5.1 新增功能标准步骤

1. **读文档**：先读 `requirement.md` 确认需求 → 读 `AI-CONTEXT.md` 确认现有实体/接口 → 读 `AI-RULES.md` 确认规范
2. **拆分任务**：将功能拆为「后端接口 → 前端页面 → 联调」三段
3. **写后端**：entity → mapper → service → controller → 统一响应
4. **写前端**：页面 → 调用接口 → 展示
5. **校验**：`mvnw compile` 通过 → 接口能返回数据

### 5.2 代码修改原则

- **最小改动**：只改与任务相关的代码，不重构无关部分
- **保持可运行**：每一步修改后项目必须能编译通过
- **不引入未使用依赖**：新增依赖前先确认 pom.xml 中没有

---

## 6. 输出规范

- 代码必须可直接运行，不写伪代码
- 涉及数据库的改动需同时给出建表 SQL
- 涉及 LLM 调用需说明模型、Prompt 模板
- 单元测试：核心 Service 逻辑必须写单元测试，Controller 用 MockMvc