# 校园活动管理系统 V1.0 - 前端

> 勇闯天涯组 · Vue 3 + Vite + Pinia

## 技术栈

- Vue 3 (Composition API)
- Vite 5 (开发端口 5173, 与后端 CORS 配置一致)
- Pinia (登录态管理, localStorage 持久化)
- vue-router 4 (history 模式, 路由守卫做角色校验)
- Element Plus (UI 组件库)
- axios (HTTP 客户端, 拦截器统一处理 JWT 与错误码)
- dayjs (时间格式化)

## 命名规范遵循 (对应 命名规范.xlsx)

| 变量名 | 含义 |
|---|---|
| `createRouter` | vue-router 内置函数, 创建路由实例 |
| `createWebHistory` | 路由模式函数, history 模式 |
| `routes` | 路由配置数组 |
| `path` | 路由访问路径 |
| `name` | 路由唯一名称 |
| `component` | 路由绑定的页面组件 |
| `router` | 全局路由实例 |
| `Home` | 首页组件 |
| `Login` | 登录页 |
| `Student` | 学生端主页 |
| `Teacher` | 教师端主页 |
| `StudentRecord` | 学生报名记录 |
| `NotFound` | 404 页面 |

## 目录结构

```
frontend/
├── package.json
├── vite.config.js          # Vite 配置 + /api 代理到 8080
├── index.html
├── public/favicon.svg
└── src/
    ├── main.js             # 应用入口
    ├── App.vue             # 根组件
    ├── assets/main.css     # 全局样式
    ├── router/index.js     # 路由配置 + 守卫
    ├── stores/user.js      # Pinia 用户 store
    ├── api/
    │   ├── http.js         # axios 实例 + 拦截器
    │   ├── auth.js         # 登录接口
    │   ├── student.js      # 学生端接口
    │   └── teacher.js      # 教师端接口
    └── views/
        ├── Home.vue        # 首页 (活动总览 + 角色入口)
        ├── Login.vue       # 登录页
        ├── Student.vue     # 学生端 (浏览/搜索/报名)
        ├── StudentRecord.vue # 我的报名 (退选)
        ├── Teacher.vue     # 教师端 (发布/编辑/删除/统计)
        └── NotFound.vue    # 404
```

## 启动方式

```bash
# 进入 frontend 目录
cd frontend

# 安装依赖 (使用 npm 或 pnpm)
npm install

# 启动开发服务器 (默认 5173)
npm run dev
```

启动后访问 http://localhost:5173 , 后端需同时运行在 http://localhost:8080 (Vite 已配置 /api 代理)。

## 登录账号

所有账号初始密码 `123456`:

- 教师: T2024001 / T2024002
- 学生: S2024001 ~ S2024006

## 路由守卫说明

- 未登录访问受保护页面 → 跳 `/login?redirect=原路径`
- 已登录访问 `/login` → 跳首页
- 学生访问教师页 / 教师访问学生页 → 跳首页
- 401 (Token 失效) → 清登录态并跳 `/login`

## 后端接口对接

| 接口 | 方法/路径 | 页面调用 |
|---|---|---|
| 登录 | POST /api/auth/login | Login.vue |
| 活动分页搜索 | GET /api/student/activities | Student.vue |
| 活动详情 | GET /api/student/activities/{id} | Student.vue 弹窗 |
| 报名 | POST /api/student/activities/{id}/register | Student.vue |
| 退选 | POST /api/student/activities/{id}/withdraw | StudentRecord.vue |
| 我的报名记录 | GET /api/student/registrations | StudentRecord.vue |
| 发布活动 | POST /api/teacher/activities | Teacher.vue |
| 本人活动列表 | GET /api/teacher/activities | Teacher.vue |
| 修改活动 | PUT /api/teacher/activities/{id} | Teacher.vue |
| 删除活动 | DELETE /api/teacher/activities/{id} | Teacher.vue |
| 报名统计 | GET /api/teacher/activities/{id}/stats | Teacher.vue |
