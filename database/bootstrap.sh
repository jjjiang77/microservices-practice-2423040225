#!/bin/sh
# 空卷首次初始化自动执行。已有数据卷通过 scripts/migrate-databases.sh 执行。
set -eu
export MYSQL_PWD="$MYSQL_ROOT_PASSWORD"

mysql_root() {
    mysql --user=root --default-character-set=utf8mb4 "$@"
}

mysql_root < /opt/shangxiaotao-db/schema.sql

provision_account() {
    account=$1
    # 以十六进制传递密码，避免引号等字符进入 SQL 语法。
    password_hex=$(printf '%s' "$2" | od -An -tx1 | tr -d ' \n')
    mysql_root <<SQL
CREATE USER IF NOT EXISTS '${account}'@'%';
SET @password_sql = CONCAT('ALTER USER ''${account}''@''%'' IDENTIFIED BY ', QUOTE(CONVERT(0x${password_hex} USING utf8mb4)));
PREPARE account_stmt FROM @password_sql;
EXECUTE account_stmt;
DEALLOCATE PREPARE account_stmt;
GRANT SELECT, INSERT, UPDATE, DELETE ON \`${account}\`.* TO '${account}'@'%';
SQL
}
provision_account shangxiaotao_user "$USER_DB_PASSWORD"
provision_account shangxiaotao_product "$PRODUCT_DB_PASSWORD"
provision_account shangxiaotao_purchase "$PURCHASE_DB_PASSWORD"

if [ "$(mysql_root -Nse "SELECT COUNT(*) FROM shangxiaotao_product.schema_migration WHERE version='split-v1'")" = 1 ]; then
    exit 0
fi

legacy=${LEGACY_MYSQL_DATABASE:-shangxiaotao}
case "$legacy" in
    ''|*[!a-zA-Z0-9_]*|shangxiaotao_user|shangxiaotao_product|shangxiaotao_purchase)
        printf 'Invalid legacy database name\n' >&2
        exit 1 ;;
esac

table_count=$(mysql_root -Nse "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$legacy' AND table_name IN ('t_user','t_product')")
case "$table_count" in
    0)
        { printf 'START TRANSACTION;\n'; cat /opt/shangxiaotao-db/seed.sql
          printf "\nINSERT INTO shangxiaotao_product.schema_migration VALUES ('split-v1');\nCOMMIT;\n"
        } | mysql_root ;;
    2)
        nickname="u.nickname"
        if [ "$(mysql_root -Nse "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='$legacy' AND table_name='t_product' AND column_name='seller_nickname'")" = 1 ]; then
            nickname="COALESCE(p.seller_nickname, u.nickname)"
        fi
        mysql_root <<SQL
START TRANSACTION;
INSERT INTO shangxiaotao_user.t_user
    (id, student_id, nickname, department, role, credit_score, created_at, updated_at)
SELECT id, student_id, nickname, department, role, credit_score, created_at, updated_at
FROM \`$legacy\`.t_user;
INSERT INTO shangxiaotao_product.t_product
    (id, title, description, price, category, condition_level, status, seller_id, seller_nickname, created_at, updated_at)
SELECT p.id, p.title, p.description, p.price, p.category, p.condition_level, p.status,
       p.seller_id, $nickname, p.created_at, p.updated_at
FROM \`$legacy\`.t_product p LEFT JOIN \`$legacy\`.t_user u ON u.id=p.seller_id;
INSERT INTO shangxiaotao_product.schema_migration VALUES ('split-v1');
COMMIT;
SQL
        ;;
    *)
        printf 'Legacy database has only one expected table. Migration stopped.\n' >&2
        exit 1 ;;
esac
