#!/bin/bash
#
# Android 项目模板初始化脚本 (Unix/Mac/Git Bash)
# 用法: bash init_project.sh <新包名> <新APP名称> [新项目名称]
#
# 示例: bash init_project.sh com.example.myapp 我的应用 MyApp
#       bash init_project.sh com.company.newproject NewProject NewProject
#
# 说明:
#   - 新包名: 例如 com.example.myapp (必填)
#   - 新APP名称: 显示在手机上的名字 (必填)
#   - 新项目名称: Gradle项目名, 不填则从包名最后一段自动生成 (可选)
#

set -e

# ============================== 颜色输出 ==============================
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

info()  { echo -e "${BLUE}[INFO]${NC} $1"; }
ok()    { echo -e "${GREEN}[OK]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

# ============================== 参数校验 ==============================
OLD_PACKAGE="com.kgmedia.fastfilm"
OLD_APP_NAME="快出片"
OLD_PROJECT_NAME="FastFilm"
OLD_KEY_ALIAS="fastfilm"

if [ $# -lt 2 ]; then
    echo ""
    echo "用法: bash init_project.sh <新包名> <新APP名称> [新项目名称]"
    echo ""
    echo "参数:"
    echo "  新包名       必填, 例如 com.example.myapp"
    echo "  新APP名称    必填, 例如 我的应用"
    echo "  新项目名称   可选, 不填则从包名最后一段自动生成(首字母大写)"
    echo ""
    echo "示例:"
    echo "  bash init_project.sh com.example.myapp 我的应用 MyApp"
    echo "  bash init_project.sh com.company.news 新闻头条"
    echo ""
    exit 1
fi

NEW_PACKAGE="$1"
NEW_APP_NAME="$2"

# 验证包名格式
if ! echo "$NEW_PACKAGE" | grep -qE '^[a-z][a-z0-9]*(\.[a-z][a-z0-9]*)+$'; then
    error "包名格式不正确: $NEW_PACKAGE (应为 com.example.app 格式, 只含小写字母、数字、点号)"
fi

# 如果没提供项目名称, 从包名最后一段生成 (首字母大写)
if [ -n "$3" ]; then
    NEW_PROJECT_NAME="$3"
else
    LAST_SEGMENT=$(echo "$NEW_PACKAGE" | awk -F'.' '{print $NF}')
    NEW_PROJECT_NAME="$(echo "${LAST_SEGMENT:0:1}" | tr '[:lower:]' '[:upper:]')${LAST_SEGMENT:1}"
fi

# 路径转换: com.example.app -> com/example/app
OLD_PACKAGE_PATH=$(echo "$OLD_PACKAGE" | tr '.' '/')
NEW_PACKAGE_PATH=$(echo "$NEW_PACKAGE" | tr '.' '/')

# 新的 keyAlias (包名最后一段)
NEW_KEY_ALIAS=$(echo "$NEW_PACKAGE" | awk -F'.' '{print $NF}')

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo ""
echo "============================================"
echo "  Android 项目模板初始化"
echo "============================================"
echo ""
info "旧包名:     $OLD_PACKAGE"
info "新包名:     $NEW_PACKAGE"
info "旧APP名:    $OLD_APP_NAME"
info "新APP名:    $NEW_APP_NAME"
info "新项目名:   $NEW_PROJECT_NAME"
info "新keyAlias: $NEW_KEY_ALIAS"
echo ""

# ============================== 跨平台 sed ==============================
# macOS 的 sed -i 需要备份后缀, Linux 不需要
sedi() {
    if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i '' "$@"
    else
        sed -i "$@"
    fi
}

# ============================== 1. 更新 config.gradle ==============================
info "更新 config.gradle ..."
sedi "s|$OLD_PACKAGE|$NEW_PACKAGE|g" config.gradle
ok "config.gradle 已更新"

# ============================== 2. 更新 settings.gradle ==============================
info "更新 settings.gradle ..."
sedi "s|rootProject.name = \"$OLD_PROJECT_NAME\"|rootProject.name = \"$NEW_PROJECT_NAME\"|g" settings.gradle
ok "settings.gradle 已更新"

# ============================== 3. 更新 app/build.gradle ==============================
info "更新 app/build.gradle ..."
sedi "s|namespace '$OLD_PACKAGE'|namespace '$NEW_PACKAGE'|g" app/build.gradle
sedi "s|storeFile file(\"../$OLD_PACKAGE.jks\")|storeFile file(\"../$NEW_PACKAGE.jks\")|g" app/build.gradle
sedi "s|keyAlias '$OLD_KEY_ALIAS'|keyAlias '$NEW_KEY_ALIAS'|g" app/build.gradle
ok "app/build.gradle 已更新"

# ============================== 4. 更新 AndroidManifest.xml ==============================
info "更新 AndroidManifest.xml ..."
sedi "s|$OLD_PACKAGE|$NEW_PACKAGE|g" app/src/main/AndroidManifest.xml
ok "AndroidManifest.xml 已更新"

# ============================== 5. 更新 strings.xml (APP名称) ==============================
info "更新 strings.xml (APP名称) ..."
sedi "s|<string name=\"app_name\">$OLD_APP_NAME</string>|<string name=\"app_name\">$NEW_APP_NAME</string>|g" app/src/main/res/values/strings.xml
ok "strings.xml 已更新"

# ============================== 6. 重命名 keystore 文件 ==============================
info "重命名 keystore 文件 ..."
if [ -f "$OLD_PACKAGE.jks" ]; then
    mv "$OLD_PACKAGE.jks" "$NEW_PACKAGE.jks"
    ok "keystore 已重命名为 $NEW_PACKAGE.jks"
else
    warn "keystore 文件 $OLD_PACKAGE.jks 不存在, 跳过"
fi

# ============================== 7. 更新 gradle.properties ==============================
info "更新 gradle.properties ..."
sedi "s|#Alias: $OLD_KEY_ALIAS|#Alias: $NEW_KEY_ALIAS|g" gradle.properties
ok "gradle.properties 已更新"

# ============================== 8. 移动源码目录 ==============================
info "移动源码目录 ..."
OLD_SRC_DIR="app/src/main/java/$OLD_PACKAGE_PATH"
NEW_SRC_DIR="app/src/main/java/$NEW_PACKAGE_PATH"

if [ -d "$OLD_SRC_DIR" ]; then
    mkdir -p "$NEW_SRC_DIR"
    cp -r "$OLD_SRC_DIR"/* "$NEW_SRC_DIR"/
    rm -rf "$OLD_SRC_DIR"
    ok "源码目录已移动: $OLD_PACKAGE_PATH -> $NEW_PACKAGE_PATH"

    # 清理旧的空目录
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

# ============================== 9. 更新所有 Kotlin/Java 源文件中的包名 ==============================
info "更新 Kotlin/Java 源文件中的包名引用 ..."
COUNT=0
find app/src/main/java -type f \( -name "*.kt" -o -name "*.java" \) | while read -r file; do
    if grep -q "$OLD_PACKAGE" "$file"; then
        sedi "s|$OLD_PACKAGE|$NEW_PACKAGE|g" "$file"
        COUNT=$((COUNT + 1))
    fi
done
ok "Kotlin/Java 源文件已更新"

# ============================== 10. 更新所有 XML 布局文件中的包名 ==============================
info "更新 XML 布局文件中的包名引用 ..."
find app/src/main/res -type f -name "*.xml" | while read -r file; do
    if grep -q "$OLD_PACKAGE" "$file"; then
        sedi "s|$OLD_PACKAGE|$NEW_PACKAGE|g" "$file"
    fi
done
ok "XML 布局文件已更新"

# ============================== 11. 更新 apisecurity 原生代码 ==============================
info "更新 apisecurity 原生代码中的包名 ..."
CPP_FILE="apisecurity/src/main/cpp/apisecurity-lib.cpp"
if [ -f "$CPP_FILE" ]; then
    sedi "s|$OLD_PACKAGE|$NEW_PACKAGE|g" "$CPP_FILE"
    ok "apisecurity-lib.cpp 已更新 (APP_PKG + APPLICATION_NAME)"
    warn "请手动更新 apisecurity-lib.cpp 中的 SHA1、signature、KEY、KEY_DEV"
else
    warn "$CPP_FILE 不存在, 跳过"
fi

# ============================== 12. 清理构建缓存 ==============================
info "清理构建缓存 ..."
rm -rf .gradle/
rm -rf app/build/
rm -rf build/
rm -rf .idea/
ok "构建缓存已清理"

# ============================== 完成 ==============================
echo ""
echo "============================================"
echo -e "  ${GREEN}项目初始化完成!${NC}"
echo "============================================"
echo ""
info "项目名称: $NEW_PROJECT_NAME"
info "包名:     $NEW_PACKAGE"
info "APP名称:  $NEW_APP_NAME"
echo ""
warn "注意事项:"
echo "  1. 请用 Android Studio 打开项目, 等待 Gradle Sync 完成"
echo "  2. keystore 密码仍为默认值(111111), 请在正式发布前修改"
echo "  3. 请更新 app/build.gradle 中的第三方 SDK 密钥 (微信/QQ/友盟等)"
echo "  4. 请更新 app/build.gradle 中的 SERVER_URL 为你的服务器地址"
echo "  5. 如需启用 apisecurity 模块, 请手动修改 apisecurity-lib.cpp 中的:"
echo "     - SHA1 (APK签名哈希)"
echo "     - signature (签名证书十六进制内容)"
echo "     - KEY / KEY_DEV (API签名密钥)"
echo "     详见 README.md 第八章"
echo "  6. 如需生成新的 keystore, 请运行:"
echo "     keytool -genkey -v -keystore $NEW_PACKAGE.jks -alias $NEW_KEY_ALIAS -keyalg RSA -keysize 2048 -validity 36500"
echo ""
