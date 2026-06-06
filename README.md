# 闲置好物置换市集

> 聚焦社区邻里闲置物品流转，主打用户之间物品无偿互换，摒弃线上付费交易逻辑。让闲置物品流动起来，邻里互助，绿色环保。

## 项目简介

闲置好物置换市集是一个面向社区的闲置物品免费交换平台，用户可以发布自己的闲置物品，浏览他人发布的物品，发起互换邀约，实现闲置资源的重新利用。平台摒弃传统的买卖交易模式，以物换物，让闲置流动起来。

## 技术栈

### 前端

- **框架**: Vue 3.4 + Vite 5
- **UI 组件库**: Element Plus
- **状态管理**: Pinia
- **路由**: Vue Router 4
- **样式**: SCSS
- **图片处理**: Compressor.js
- **日期处理**: Day.js

### 后端

- **框架**: Spring Boot 3.3
- **JDK**: 17 Temurin
- **ORM**: MyBatis-Plus 3.5.5
- **缓存**: Redis
- **数据库**: MySQL 8.0
- **构建工具**: Maven

### 部署

- **容器化**: Docker
- **编排**: Docker Compose
- **反向代理**: Nginx

## 功能特性

### 核心功能

| 功能 | 说明 |
|------|------|
| 闲置物品发布 | 用户填写物品品类、成色、描述，上传本地图片（自动压缩），自定义期望互换物品类型 |
| 互换邀约管理 | 浏览闲置后发起互换申请，备注交换需求，发布方审核邀约（同意/驳回） |
| 个人闲置库房 | 已发布、置换成交、手动下架三类物品，支持编辑、下架、重新上架 |
| 精准筛选检索 | 按品类、成色、发布时间筛选，关键词全文检索物品名称与详情 |

### 页面一览

| 页面 | 路径 | 说明 |
|------|------|------|
| 首页 | `/` | 精选置顶物品、热门品类入口 |
| 物品市集 | `/market` | 物品列表、多维度筛选、关键词搜索 |
| 物品详情 | `/detail/:id` | 物品图片、描述、发布者信息、发起互换 |
| 发布闲置 | `/publish` | 表单发布、图片上传压缩 |
| 我的库房 | `/my` | 已发布/已成交/已下架物品管理 |
| 互换邀约 | `/offers` | 收到的邀约/发出的邀约 |

## 项目结构

```
.
├── frontend/                    # 前端项目
│   ├── src/
│   │   ├── views/              # 页面组件
│   │   │   ├── Home.vue        # 首页
│   │   │   ├── Market.vue      # 物品市集
│   │   │   ├── Detail.vue      # 物品详情
│   │   │   ├── Publish.vue     # 发布闲置
│   │   │   ├── My.vue          # 我的库房
│   │   │   └── Offers.vue      # 互换邀约
│   │   ├── stores/             # Pinia 状态管理
│   │   │   └── user.js         # 用户状态
│   │   ├── utils/              # 工具函数
│   │   │   ├── api.js          # Axios 封装
│   │   │   └── imageCompressor.js  # 图片压缩
│   │   ├── router/             # 路由配置
│   │   │   └── index.js
│   │   ├── styles/             # 全局样式
│   │   │   └── index.scss
│   │   ├── App.vue             # 根组件
│   │   └── main.js             # 入口文件
│   ├── Dockerfile              # 前端 Dockerfile
│   ├── nginx.conf              # Nginx 配置
│   ├── vite.config.js          # Vite 配置
│   ├── package.json            # 前端依赖
│   └── .npmrc                  # NPM 镜像源配置
│
├── backend/                     # 后端项目
│   ├── src/main/
│   │   ├── java/com/swapmarket/
│   │   │   ├── controller/     # 控制层
│   │   │   │   ├── UserController.java
│   │   │   │   ├── ItemController.java
│   │   │   │   ├── OfferController.java
│   │   │   │   └── CategoryController.java
│   │   │   ├── service/        # 业务逻辑层
│   │   │   │   ├── UserService.java
│   │   │   │   ├── ItemService.java
│   │   │   │   ├── SwapOfferService.java
│   │   │   │   ├── CategoryService.java
│   │   │   │   └── FileStorageService.java
│   │   │   ├── mapper/         # 数据访问层
│   │   │   │   ├── UserMapper.java
│   │   │   │   ├── ItemMapper.java
│   │   │   │   ├── ItemImageMapper.java
│   │   │   │   ├── SwapOfferMapper.java
│   │   │   │   └── CategoryMapper.java
│   │   │   ├── entity/         # 实体类
│   │   │   │   ├── User.java
│   │   │   │   ├── Item.java
│   │   │   │   ├── ItemImage.java
│   │   │   │   ├── SwapOffer.java
│   │   │   │   └── Category.java
│   │   │   ├── common/         # 公共类
│   │   │   │   ├── Result.java
│   │   │   │   └── PageResult.java
│   │   │   └── config/         # 配置类
│   │   │       ├── GlobalExceptionHandler.java
│   │   │       ├── MybatisPlusConfig.java
│   │   │       ├── RedisConfig.java
│   │   │       └── WebMvcConfig.java
│   │   └── resources/
│   │       ├── sql/             # 数据库初始化脚本
│   │       │   └── init.sql
│   │       └── application.yml  # 应用配置
│   ├── Dockerfile               # 后端 Dockerfile
│   ├── settings.xml             # Maven 镜像源配置
│   └── pom.xml                  # 后端依赖
│
├── data/                        # 数据挂载目录
│   ├── mysql/                   # MySQL 数据
│   ├── redis/                   # Redis 数据
│   └── uploads/                 # 上传文件
│
├── .env                         # 全局环境变量
├── docker-compose.yml           # Docker 编排配置
├── start.sh                     # 一键启动脚本
└── README.md
```

## 数据库设计

### user（用户表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| username | VARCHAR(50) | 用户名，唯一 |
| password | VARCHAR(100) | 密码 |
| nickname | VARCHAR(50) | 昵称 |
| avatar | VARCHAR(255) | 头像 |
| phone | VARCHAR(20) | 手机号 |
| email | VARCHAR(100) | 邮箱 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |
| deleted | TINYINT | 逻辑删除标记 |

### category（分类表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| name | VARCHAR(50) | 分类名称 |
| icon | VARCHAR(50) | 分类图标 |
| sort_order | INT | 排序值 |
| create_time | DATETIME | 创建时间 |
| deleted | TINYINT | 逻辑删除标记 |

### item（物品表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| user_id | BIGINT | 发布用户ID |
| title | VARCHAR(100) | 物品标题 |
| description | TEXT | 物品描述 |
| category_id | BIGINT | 分类ID |
| condition | VARCHAR(20) | 成色 |
| expected_swap | VARCHAR(255) | 期望互换 |
| status | VARCHAR(20) | 状态：published/offline/completed |
| is_top | TINYINT | 是否置顶 |
| view_count | INT | 浏览量 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |
| deleted | TINYINT | 逻辑删除标记 |

### item_image（物品图片表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| item_id | BIGINT | 物品ID |
| image_url | VARCHAR(255) | 图片地址 |
| sort_order | INT | 排序值 |
| create_time | DATETIME | 创建时间 |

### swap_offer（互换邀约表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| from_user_id | BIGINT | 发起方用户ID |
| to_user_id | BIGINT | 接收方用户ID |
| from_item_id | BIGINT | 发起方物品ID |
| to_item_id | BIGINT | 接收方物品ID |
| message | TEXT | 交换说明 |
| status | VARCHAR(20) | 状态：pending/accepted/rejected |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |
| deleted | TINYINT | 逻辑删除标记 |

## 快速开始

### 环境要求

- Docker & Docker Compose
- 或 Node.js 20+ / JDK 17 / MySQL 8.0 / Redis 7

### Docker 部署（推荐）

#### 一键启动

```bash
chmod +x start.sh
./start.sh
```

启动成功后访问：http://localhost:3008

#### 手动启动

```bash
# 构建并启动所有服务
docker compose up --build -d

# 查看服务状态
docker compose ps

# 查看日志
docker compose logs -f

# 停止服务
docker compose down

# 重启单个服务
docker compose restart backend
```

### 本地开发

#### 前端开发

```bash
cd frontend
npm install
npm run dev
```

#### 后端开发

```bash
cd backend
mvn clean package
java -jar target/swap-market.jar
```

> 本地开发需要自行配置 MySQL 和 Redis 连接。

## 端口配置

所有端口均使用自定义非默认端口，避免与其他服务冲突。统一在 `.env` 文件中配置管理。

| 服务 | 宿主机端口 | 容器内部端口 | 说明 |
|------|-----------|-------------|------|
| 前端 Nginx | 3008 | 80 | 前端静态资源访问 |
| 后端 SpringBoot | 8088 | 9024 | 后端 API 服务 |
| MySQL | 3309 | 3306 | 数据库服务 |
| Redis | 6380 | 6379 | 缓存服务 |

## 演示账号

| 用户名 | 密码 |
|--------|------|
| demo | 123456 |

## API 接口

### 用户接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/user/login` | 用户登录 |
| GET | `/api/user/{id}` | 获取用户信息 |

### 物品接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/item/top` | 获取置顶物品 |
| GET | `/api/item/list` | 物品列表（分页+筛选+搜索） |
| GET | `/api/item/my` | 我的物品列表 |
| GET | `/api/item/{id}` | 物品详情 |
| POST | `/api/item/publish` | 发布物品 |
| POST | `/api/item/offline/{id}` | 下架物品 |
| POST | `/api/item/publish/{id}` | 重新上架 |

### 互换邀约接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/offer/create` | 发起互换邀约 |
| GET | `/api/offer/received` | 收到的邀约列表 |
| GET | `/api/offer/sent` | 发出的邀约列表 |
| POST | `/api/offer/accept/{id}` | 接受邀约 |
| POST | `/api/offer/reject/{id}` | 驳回邀约 |

### 分类接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/category/list` | 分类列表 |

## 构建优化说明

### 国内镜像源加速

无需 VPN 即可正常拉取依赖，全链路国内加速：

| 依赖类型 | 镜像源 | 配置文件 |
|---------|-------|---------|
| Docker 基础镜像 | 道客镜像 | `.env` - `DOCKER_REGISTRY` |
| Maven 依赖 | 阿里云 Maven | `backend/settings.xml` |
| NPM 依赖 | npmmirror | `frontend/.npmrc` |

### Docker 分层缓存机制

采用 Docker 原生分层缓存，严禁使用 `# syntax=docker/dockerfile:*` 语法。

**缓存优化规则：**
- 首次构建：全量下载依赖包
- 后续构建（`pom.xml` / `package.json` 无变更）：复用构建缓存，跳过依赖下载
- 仅源代码修改：只执行重新编译，不触发依赖下载

### 服务健康检查

MySQL 和 Redis 配置了健康检查，后端服务会等待数据库和缓存就绪后再启动，避免启动时序问题。

## 常见问题

### Q: 构建速度很慢怎么办？
A: 项目已配置国内镜像源，如果仍然很慢，请检查网络连接。首次构建需要下载依赖，后续构建会复用缓存。

### Q: 如何修改端口？
A: 编辑 `.env` 文件中的端口配置，然后重新执行 `docker compose up -d` 即可。

### Q: 数据会丢失吗？
A: MySQL 和 Redis 的数据都挂载在 `./data/` 目录下，只要不删除该目录，数据就不会丢失。

### Q: 如何更换 Docker 镜像源？
A: 修改 `.env` 文件中的 `DOCKER_REGISTRY` 变量为你想用的镜像源地址。

### Q: 上传的图片保存在哪里？
A: 上传的图片保存在 `./data/uploads/` 目录下，按日期（yyyy/MM/dd）分文件夹存储。

## License

MIT
