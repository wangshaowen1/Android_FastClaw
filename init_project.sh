#!/bin/bash
#
# Android 项目模板初始化脚本 (Unix/Mac/Git Bash)
# 用法: bash init_project.sh <新包名> <新APP名称> <新项目名称> <keyAlias> <keyPassword> <storePassword>
#
# 示例: bash init_project.sh com.example.myapp 我的应用 MyApp myapp abc123 abc123
#

set -e

# ============================== Color output ==============================
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

info()  { echo -e "${BLUE}[INFO]${NC} $1"; }
ok()    { echo -e "${GREEN}[OK]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

# ============================== Constants ==============================
OLD_PACKAGE="com.kgmedia.fastfilm"
OLD_APP_NAME="快出片"
OLD_PROJECT_NAME="FastFilm"
OLD_KEY_ALIAS="fastfilm"

if [ $# -lt 6 ]; then
    echo ""
    echo "用法: bash init_project.sh <新包名> <新APP名称> <新项目名称> <keyAlias> <keyPassword> <storePassword>"
    echo ""
    echo "参数:"
    echo "  新包名         必填, 例如 com.example.myapp"
    echo "  新APP名称      必填, 例如 我的应用"
    echo "  新项目名称     必填, 例如 MyApp"
    echo "  keyAlias       必填, 签名别名, 例如 myapp"
    echo "  keyPassword    必填, 签名密钥密码"
    echo "  storePassword  必填, 签名库密码"
    echo ""
    echo "示例:"
    echo "  bash init_project.sh com.example.myapp 我的应用 MyApp myapp abc123 abc123"
    echo ""
    exit 1
fi

NEW_PACKAGE="$1"
NEW_APP_NAME="$2"

# Validate package name
if ! echo "$NEW_PACKAGE" | grep -qE '^[a-z][a-z0-9]*(\.[a-z][a-z0-9]*)+$'; then
    error "包名格式不正确: $NEW_PACKAGE (应为 com.example.app 格式, 只含小写字母、数字、点号)"
fi

NEW_PROJECT_NAME="$3"
NEW_KEY_ALIAS="$4"
KEY_PASSWORD="$5"
STORE_PASSWORD="$6"

# Path conversion
OLD_PACKAGE_PATH=$(echo "$OLD_PACKAGE" | tr '.' '/')
NEW_PACKAGE_PATH=$(echo "$NEW_PACKAGE" | tr '.' '/')

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo ""
echo "============================================"
echo "  Android 项目模板初始化"
echo "============================================"
echo ""
info "旧包名:       $OLD_PACKAGE"
info "新包名:       $NEW_PACKAGE"
info "旧APP名:      $OLD_APP_NAME"
info "新APP名:      $NEW_APP_NAME"
info "新项目名:     $NEW_PROJECT_NAME"
info "keyAlias:     $NEW_KEY_ALIAS"
info "keyPassword:  $KEY_PASSWORD"
info "storePassword: $STORE_PASSWORD"
echo ""

# ============================== Cross-platform sed ==============================
sedi() {
    if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i '' "$@"
    else
        sed -i "$@"
    fi
}

set_gradle_property() {
    local file="$1"
    local key="$2"
    local value="$3"
    if ! grep -q "^${key}=" "$file"; then
        error "Gradle property not found: $key"
    fi
    sedi "s|^${key}=.*$|${key}=${value}|g" "$file"
}

# ============================== 1. config.gradle ==============================
info "更新 config.gradle ..."
sedi "s|$OLD_PACKAGE|$NEW_PACKAGE|g" config.gradle
ok "config.gradle"

# ============================== 2. settings.gradle ==============================
info "更新 settings.gradle ..."
sedi "s|rootProject.name = \"$OLD_PROJECT_NAME\"|rootProject.name = \"$NEW_PROJECT_NAME\"|g" settings.gradle
ok "settings.gradle"

# ============================== 3. app/build.gradle (package/signing) ==============================
info "更新 app/build.gradle (包名/签名) ..."
sedi "s|namespace '$OLD_PACKAGE'|namespace '$NEW_PACKAGE'|g" app/build.gradle
sedi "s|storeFile file(\"../$OLD_PACKAGE.jks\")|storeFile file(\"../$NEW_PACKAGE.jks\")|g" app/build.gradle
sedi "s|keyAlias '$OLD_KEY_ALIAS'|keyAlias '$NEW_KEY_ALIAS'|g" app/build.gradle
sedi "s|keyPassword '111111'|keyPassword '$KEY_PASSWORD'|g" app/build.gradle
sedi "s|storePassword '111111'|storePassword '$STORE_PASSWORD'|g" app/build.gradle
ok "app/build.gradle (包名/签名)"

# ============================== 4. app/build.gradle (SDK keys) ==============================
info "清理 app/build.gradle 第三方SDK密钥 ..."
BG="app/build.gradle"
# Douyin
sedi "s|awg154kyqmvhns2f|YOUR_DY_CLIENT_KEY|g" "$BG"
# WeChat
sedi "s|wxc4c951d1cd1816fc|YOUR_WX_APP_ID|g" "$BG"
sedi "s|9d679c3bdc5531e794d7ab83214d40af|YOUR_WX_APP_SECRET|g" "$BG"
# GrowingIO
sedi "s|09ae0b3ae5497abe|YOUR_GROWING_SCHEME|g" "$BG"
# Kuaishou
sedi "s|ks664562422307705809|YOUR_KWAI_APP_ID|g" "$BG"
# QQ
sedi "s|1112354170|YOUR_QQ_APP_ID|g" "$BG"
sedi "s|JLx3P8RMZrnTQ2k0|YOUR_QQ_APP_KEY|g" "$BG"
# Weibo
sedi "s|4008287256|YOUR_WB_APP_KEY|g" "$BG"
sedi "s|9fe4864f3bfff59e241442ae0d645615|YOUR_WB_APP_SECRET|g" "$BG"
sedi "s|https://m.helloaa.cn/|https://example.com|g" "$BG"
# Service/Privacy/Official URLs
sedi "s|https://makemoneyoss.yiduizf.cn/data/front/taskdesc/%E8%B5%9A%E9%92%B1%E7%8C%AB%E7%94%A8%E6%88%B7%E6%9C%8D%E5%8A%A1%E5%8D%8F%E8%AE%AE.html|https://example.com/service|g" "$BG"
sedi "s|https://makemoneyoss.yiduizf.cn/data/front/taskdesc/%E8%B5%9A%E9%92%B1%E7%8C%AB%E9%9A%90%E7%A7%81%E6%94%BF%E7%AD%96.html|https://example.com/privacy|g" "$BG"
sedi "s|http://rmgf.wechatrenmai.cn|https://example.com|g" "$BG"
# Shanyan
sedi "s|KfNYHDRd|YOUR_SHANYAN_APPID|g" "$BG"
sedi "s|VDE8Q8TY|YOUR_SHANYAN_APP_KEY|g" "$BG"
# Server URLs
sedi "s|https://kcp-test.kwaige.cn|https://your-dev-server.com|g" "$BG"
sedi "s|http://120.24.240.28:9010|https://your-preview-server.com|g" "$BG"
sedi "s|https://kuaichupianjk.kuaigeai.cn|https://your-production-server.com|g" "$BG"
ok "app/build.gradle (第三方SDK密钥已清理)"

info "清理 gradle.properties 友盟与厂商推送配置 ..."
GP="gradle.properties"
set_gradle_property "$GP" "UMENG_APP_KEY" ""
set_gradle_property "$GP" "UMENG_MESSAGE_SECRET" ""
set_gradle_property "$GP" "PUSH_ENABLE_CHANNEL_PUSH" "false"
set_gradle_property "$GP" "PUSH_XIAOMI_APP_ID" ""
set_gradle_property "$GP" "PUSH_XIAOMI_APP_KEY" ""
set_gradle_property "$GP" "PUSH_HUAWEI_APP_ID" ""
set_gradle_property "$GP" "PUSH_OPPO_APP_KEY" ""
set_gradle_property "$GP" "PUSH_OPPO_APP_SECRET" ""
set_gradle_property "$GP" "PUSH_VIVO_API_KEY" ""
set_gradle_property "$GP" "PUSH_VIVO_APP_ID" ""
set_gradle_property "$GP" "PUSH_HONOR_APP_ID" ""
ok "gradle.properties (友盟与厂商推送配置已清理)"

# ============================== 5. AndroidManifest.xml ==============================
info "更新 AndroidManifest.xml ..."
sedi "s|$OLD_PACKAGE|$NEW_PACKAGE|g" app/src/main/AndroidManifest.xml
ok "AndroidManifest.xml"

# ============================== 6. strings.xml ==============================
info "更新 strings.xml (APP名称) ..."
sedi "s|<string name=\"app_name\">$OLD_APP_NAME</string>|<string name=\"app_name\">$NEW_APP_NAME</string>|g" app/src/main/res/values/strings.xml
ok "strings.xml"

# ============================== 7. Create new keystore ==============================
info "创建新签名文件 ..."

rm -f "$OLD_PACKAGE.jks"
rm -f "$NEW_PACKAGE.jks"

JKS_FILE="$NEW_PACKAGE.jks"
keytool -genkey -v \
    -keystore "$JKS_FILE" \
    -alias "$NEW_KEY_ALIAS" \
    -keyalg RSA \
    -keysize 2048 \
    -validity 36525 \
    -storepass "$STORE_PASSWORD" \
    -keypass "$KEY_PASSWORD" \
    -dname "CN=kuaige, OU=kuaige, O=深圳市快歌智能有限公司, L=ShenZhen, ST=GuangDong, C=CN"

if [ ! -f "$JKS_FILE" ]; then
    error "keytool 执行失败, 请确保 JDK 已安装且 keytool 在 PATH 中"
fi
ok "签名文件已创建: $JKS_FILE"

# ============================== 8. Extract cert info ==============================
info "提取签名信息 ..."

CERT_INFO=$(keytool -list -v -keystore "$JKS_FILE" -alias "$NEW_KEY_ALIAS" -storepass "$STORE_PASSWORD" 2>&1)

NEW_MD5=$(echo "$CERT_INFO" | grep "MD5:" | head -1 | sed 's/.*MD5: *//')
NEW_SHA1=$(echo "$CERT_INFO" | grep "SHA1:" | head -1 | sed 's/.*SHA1: *//')
NEW_SHA256=$(echo "$CERT_INFO" | grep -i "SHA-\?256:" | head -1 | sed 's/.*SHA-\?256: *//')

# SHA1 lowercase without colons (for apisecurity)
NEW_SHA1_NOCOLON=$(echo "$NEW_SHA1" | tr -d ':' | tr '[:upper:]' '[:lower:]')

ok "MD5:    $NEW_MD5"
ok "SHA1:   $NEW_SHA1"
ok "SHA256: $NEW_SHA256"

# Extract certificate DER hex (for apisecurity signature)
CERT_HEX=$(keytool -exportcert -keystore "$JKS_FILE" -alias "$NEW_KEY_ALIAS" -storepass "$STORE_PASSWORD" 2>/dev/null | xxd -p | tr -d '\n')

# Extract public key PEM
CERT_PEM=$(keytool -exportcert -rfc -keystore "$JKS_FILE" -alias "$NEW_KEY_ALIAS" -storepass "$STORE_PASSWORD" 2>/dev/null)

ok "证书HEX长度: ${#CERT_HEX} 字符"

# ============================== 9. gradle.properties ==============================
info "更新 gradle.properties ..."
sedi "s|#Alias: $OLD_KEY_ALIAS|#Alias: $NEW_KEY_ALIAS|g" gradle.properties
sedi "s|#MD5: 67:E7:63:39:E0:7A:4D:95:BC:7F:B9:24:C0:6B:C0:ED|#MD5: $NEW_MD5|g" gradle.properties
sedi "s|#SHA1: EC:82:59:D5:0D:00:62:32:6C:B4:80:34:32:A3:57:94:36:DF:38:76|#SHA1: $NEW_SHA1|g" gradle.properties
sedi "s|#SHA-256: FF:D7:E3:BC:E1:53:69:EF:1C:D5:19:8A:04:0E:7F:A7:D2:7E:39:32:BA:C1:3E:9A:A6:5C:94:C5:C0:55:A0:58|#SHA-256: $NEW_SHA256|g" gradle.properties

# Replace old public.key
sedi "/#public.key = /d" gradle.properties
echo "" >> gradle.properties
echo "#public.key = $CERT_PEM" >> gradle.properties
ok "gradle.properties"

# ============================== 10. Move source directory ==============================
info "移动源码目录 ..."
OLD_SRC_DIR="app/src/main/java/$OLD_PACKAGE_PATH"
NEW_SRC_DIR="app/src/main/java/$NEW_PACKAGE_PATH"

if [ -d "$OLD_SRC_DIR" ]; then
    mkdir -p "$NEW_SRC_DIR"
    cp -r "$OLD_SRC_DIR"/* "$NEW_SRC_DIR"/
    rm -rf "$OLD_SRC_DIR"
    ok "源码目录已移动: $OLD_PACKAGE_PATH -> $NEW_PACKAGE_PATH"

    OLD_BASE=$(echo "$OLD_PACKAGE" | awk -F'.' '{print $1}')
    NEW_BASE=$(echo "$NEW_PACKAGE" | awk -F'.' '{print $1}')
    if [ "$OLD_BASE" != "$NEW_BASE" ]; then
        OLD_TOP_DIR="app/src/main/java/$OLD_BASE"
        if [ -d "$OLD_TOP_DIR" ]; then
            find "$OLD_TOP_DIR" -type d -empty -delete 2>/dev/null || true
            rmdir "$OLD_TOP_DIR" 2>/dev/null || true
        fi
    else
        find "app/src/main/java/$OLD_BASE" -type d -empty -delete 2>/dev/null || true
    fi
    ok "旧目录已清理"
else
    warn "源码目录 $OLD_SRC_DIR 不存在, 跳过"
fi

# ============================== 11. Kotlin/Java source ==============================
info "更新 Kotlin/Java 源文件中的包名引用 ..."
find app/src/main/java -type f \( -name "*.kt" -o -name "*.java" \) | while read -r file; do
    if grep -q "$OLD_PACKAGE" "$file"; then
        sedi "s|$OLD_PACKAGE|$NEW_PACKAGE|g" "$file"
    fi
done
ok "Kotlin/Java 源文件"

# ============================== 12. XML layouts ==============================
info "更新 XML 布局文件中的包名引用 ..."
find app/src/main/res -type f -name "*.xml" | while read -r file; do
    if grep -q "$OLD_PACKAGE" "$file"; then
        sedi "s|$OLD_PACKAGE|$NEW_PACKAGE|g" "$file"
    fi
done
ok "XML 布局文件"

# ============================== 13. apisecurity native ==============================
info "更新 apisecurity 原生代码 ..."
CPP_FILE="apisecurity/src/main/cpp/apisecurity-lib.cpp"
if [ -f "$CPP_FILE" ]; then
    # Package name (APP_PKG + APPLICATION_NAME)
    sedi "s|$OLD_PACKAGE|$NEW_PACKAGE|g" "$CPP_FILE"

    # SHA1
    sedi "s|ec8259d50d0062326cb4803432a3579436df3876|$NEW_SHA1_NOCOLON|g" "$CPP_FILE"

    # API keys -> placeholders
    sedi 's|#define KEY ".*"|#define KEY "YOUR_API_KEY"|g' "$CPP_FILE"
    sedi 's|#define KEY_DEV ".*"|#define KEY_DEV "YOUR_API_KEY_DEV"|g' "$CPP_FILE"
    sedi 's|const std::string  ERROR_KEY= ".*"|const std::string  ERROR_KEY= "YOUR_ERROR_KEY"|g' "$CPP_FILE"

    # Replace signature with new cert hex
    sedi "s|const char \*signature = \".*\"|const char *signature = \"$CERT_HEX\"|g" "$CPP_FILE"

    ok "apisecurity-lib.cpp (SHA1 + signature + 包名已更新)"
    warn "请手动更新 apisecurity-lib.cpp 中的 KEY、KEY_DEV (与后端协商)"
else
    warn "$CPP_FILE 不存在, 跳过"
fi

# ============================== 14. Clean build cache ==============================
info "清理构建缓存 ..."
rm -rf .gradle/
rm -rf app/build/
rm -rf build/
rm -rf .idea/
ok "构建缓存已清理"

# ============================== 15. Git add ==============================
info "添加变更到 Git ..."
if git add -A 2>/dev/null; then
    ok "所有变更已添加到 Git 暂存区"
else
    warn "Git add 失败, 请手动执行 git add"
fi

# ============================== Done ==============================
echo ""
echo "============================================"
echo -e "  ${GREEN}项目初始化完成!${NC}"
echo "============================================"
echo ""
info "项目名称:     $NEW_PROJECT_NAME"
info "包名:         $NEW_PACKAGE"
info "APP名称:      $NEW_APP_NAME"
info "keyAlias:     $NEW_KEY_ALIAS"
info "keystore:     $JKS_FILE"
echo ""
warn "注意事项:"
echo "  1. 请用 Android Studio 打开项目, 等待 Gradle Sync 完成"
echo "  2. 请更新 app/build.gradle 中的第三方 SDK 密钥 (微信/QQ/友盟等)"
echo "     当前已替换为 YOUR_XXX 占位符, 需填入真实值"
echo "  3. 请更新 app/build.gradle 中的 SERVER_URL 为你的服务器地址"
echo "  4. 如需启用 apisecurity 模块, 请手动修改 apisecurity-lib.cpp 中的:"
echo "     - KEY / KEY_DEV (API签名密钥, 与后端协商)"
echo "     - SHA1 和 signature 已自动从新签名文件生成"
echo "     详见 apisecurity/README.md"
echo ""
