#!/usr/bin/env bash
# Week09 网关鉴权自动测试。
# 依次验证：未登录 401、登录签发 JWT、携带 Token 访问成功、
# 低权限访问管理接口 403、错误密码 401、伪造身份头无效、绕过网关直连被拒绝。
#
# 用法：./scripts/auth-test.sh
# 可用环境变量覆盖：GATEWAY_URL、USER_DIRECT_URL、STUDENT_ID 等。
set -uo pipefail
cd "$(dirname "$0")/.."

gateway_url=${GATEWAY_URL:-http://127.0.0.1:8080}
user_direct_url=${USER_DIRECT_URL:-http://127.0.0.1:8081}
student_id=${STUDENT_ID:-2423040225}
student_password=${STUDENT_PASSWORD:-Student@123}
admin_id=${ADMIN_ID:-admin}
admin_password=${ADMIN_PASSWORD:-Admin@123}

if ! command -v jq >/dev/null 2>&1; then
  printf '缺少 jq，请先安装（brew install jq）\n' >&2
  exit 2
fi

pass=0
fail=0
body=$(mktemp)
trap 'rm -f "$body"' EXIT

# 发起请求，输出 HTTP 状态码，响应体写入 $body
request() {
  local method=$1 url=$2
  shift 2
  curl -sS --max-time 15 -o "$body" -w '%{http_code}' -X "$method" "$@" "$url" 2>/dev/null || true
}

check() {
  local name=$1 expected=$2 actual=$3
  if [[ "$expected" == "$actual" ]]; then
    pass=$((pass + 1))
    printf 'PASS  %-44s 期望 %-3s 实际 %s\n' "$name" "$expected" "$actual"
  else
    fail=$((fail + 1))
    printf 'FAIL  %-44s 期望 %-3s 实际 %s\n' "$name" "$expected" "$actual"
    printf '      响应: %s\n' "$(tr -d '\n' <"$body" | cut -c1-240)"
  fi
}

login() {
  local account=$1 password=$2
  request POST "$gateway_url/api/auth/login" \
    -H 'Content-Type: application/json' \
    -d "{\"studentId\":\"$account\",\"password\":\"$password\"}" >/dev/null
  jq -er '.accessToken // empty' "$body" 2>/dev/null || true
}

printf '== 网关鉴权测试 ==\n'
printf '网关: %s\n\n' "$gateway_url"

# 1. 未登录访问受保护接口
status=$(request GET "$gateway_url/api/users/1")
check '未登录访问 /api/users/1' 401 "$status"

# 2. 学生登录获取 Token
student_token=$(login "$student_id" "$student_password")
if [[ -n "$student_token" && "$(awk -F. '{print NF}' <<<"$student_token")" == 3 ]]; then
  pass=$((pass + 1))
  printf 'PASS  %-44s 已获取 JWT（%s...）\n' '学生登录签发 JWT' "${student_token:0:24}"
  printf '      角色: %s\n' "$(jq -r '.role // "?"' "$body")"
else
  fail=$((fail + 1))
  printf 'FAIL  %-44s 未获取到三段式 JWT\n' '学生登录签发 JWT'
fi

# 3. 携带 Token 访问受保护接口
status=$(request GET "$gateway_url/api/users/1" -H "Authorization: Bearer $student_token")
check '携带 Token 访问 /api/users/1' 200 "$status"

# 4. 低权限访问管理接口
status=$(request GET "$gateway_url/api/admin/users" -H "Authorization: Bearer $student_token")
check '学生 Token 访问 /api/admin/users' 403 "$status"

# 5. 错误密码
status=$(request POST "$gateway_url/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"studentId\":\"$student_id\",\"password\":\"wrong-password\"}")
check '错误密码登录' 401 "$status"

# 6. 管理员登录并访问管理接口
admin_token=$(login "$admin_id" "$admin_password")
status=$(request GET "$gateway_url/api/admin/users" -H "Authorization: Bearer $admin_token")
check '管理员访问 /api/admin/users' 200 "$status"

# 7. 客户端伪造身份头（无 Token）应被网关拦截
status=$(request GET "$gateway_url/api/admin/users" \
  -H 'X-User-Id: 4' -H 'X-User-Role: ADMIN' -H 'X-Gateway-Secret: forged')
check '伪造 X-User-Role 请求头' 401 "$status"

# 8. 绕过网关直连 user-service，缺少内部密钥应被拒绝
status=$(request GET "$user_direct_url/api/admin/users" -H 'X-User-Role: ADMIN')
check '绕过网关直连管理接口' 401 "$status"

# 9. 绕过网关读取用户资料，缺少内部密钥应被拒绝
status=$(request GET "$user_direct_url/api/users/1")
check '绕过网关直连用户资料' 401 "$status"

printf '\n== 结果: %d 通过, %d 失败 ==\n' "$pass" "$fail"
[[ "$fail" -eq 0 ]]
