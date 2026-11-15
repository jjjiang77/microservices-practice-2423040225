#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

user_url=${USER_URL:-http://127.0.0.1:8081}
product_url=${PRODUCT_URL:-http://127.0.0.1:8080}
purchase_url=${PURCHASE_URL:-http://127.0.0.1:8083}
suffix="$(date +%s)$RANDOM"
post() { curl -fsS --max-time 12 -H 'Content-Type: application/json' -d "$2" "$1"; }
seller=$(post "$user_url/api/users" "{\"studentId\":\"S$suffix\",\"nickname\":\"week08 seller\"}" | jq -er .id)
buyer=$(post "$user_url/api/users" "{\"studentId\":\"B$suffix\",\"nickname\":\"week08 buyer\"}" | jq -er .id)
for i in 1 2 3 4; do
  product=$(post "$product_url/api/products" "{\"title\":\"Week 08 $suffix-$i\",\"price\":10,\"category\":\"TEXTBOOK\",\"conditionLevel\":\"GOOD\",\"sellerId\":$seller}" | jq -er .id)
  post "$product_url/api/products/$product/approve" '{}' >/dev/null
  printf 'POST /api/purchases productId=%s\n' "$product"
  post "$purchase_url/api/purchases" "{\"productId\":$product,\"buyerId\":$buyer}" |
    jq -ec --argjson id "$product" 'select(.id == $id and .status == "SOLD") | {id, status}'
done

if [[ ${VERIFY_FAILURE:-0} == 1 ]]; then
  failed_product=$(post "$product_url/api/products" "{\"title\":\"Week 08 failure $suffix\",\"price\":10,\"category\":\"TEXTBOOK\",\"conditionLevel\":\"GOOD\",\"sellerId\":$seller}" | jq -er .id)
  post "$product_url/api/products/$failed_product/approve" '{}' >/dev/null
  body=$(mktemp)
  trap 'rm -f "$body"; docker compose start product-service product-service-replica >/dev/null' EXIT
  docker compose stop product-service product-service-replica
  status=$(curl -sS --max-time 15 -o "$body" -w '%{http_code}' -H 'Content-Type: application/json' \
    -d "{\"productId\":$failed_product,\"buyerId\":$buyer}" "$purchase_url/api/purchases")
  printf 'Both product instances stopped: HTTP %s\n' "$status"
  jq -c '{status, error}' "$body"
  test "$status" = 503
fi
