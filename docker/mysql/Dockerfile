# 薄镜像层：仅加入客户端字符集配置。
# 不使用 bind mount —— Windows Docker Desktop 的挂载文件为 0777，
# 会被 MySQL 以 "World-writable config file is ignored" 拒绝。
FROM mysql:8.4
COPY charset.cnf /etc/mysql/conf.d/charset.cnf
