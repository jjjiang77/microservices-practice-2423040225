#!/bin/sh
# 迁移时暂停业务写入，保留原数据卷与旧库。
set -eu
cd "$(dirname "$0")/.."
docker compose build
docker compose stop product-service user-service purchase-service
docker compose up -d --wait mysql
docker compose exec -T mysql sh /opt/shangxiaotao-db/bootstrap.sh
docker compose up -d
