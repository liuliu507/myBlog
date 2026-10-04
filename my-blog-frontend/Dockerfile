# ================= 第一阶段：构建 Vue =================
FROM node:20-alpine AS builder

WORKDIR /app

# 先复制依赖清单，利用缓存；npm ci 严格按 lock 文件安装，构建可复现
COPY package*.json ./
RUN npm ci

# 复制源码并构建
COPY . .
RUN npm run build

# ================= 第二阶段：Nginx 托管 =================
FROM nginx:alpine

# 复制自定义 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 把构建产物复制到 Nginx 静态目录
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]