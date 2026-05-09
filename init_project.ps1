#Requires -Version 5.1
<#
.SYNOPSIS
    Android 项目模板初始化脚本 (PowerShell)

.DESCRIPTION
    将模板项目的包名、APP名称、项目名称替换为新项目的值，
    创建新签名文件，清理第三方SDK密钥为默认占位符。
    所有文件写入均使用 UTF-8 无 BOM 编码。

.PARAMETER NewPackage
    新包名, 例如 com.example.myapp

.PARAMETER NewAppName
    新APP名称, 例如 我的应用

.PARAMETER NewProjectName
    新项目名称, 例如 MyApp

.PARAMETER KeyAlias
    签名别名, 例如 myapp

.PARAMETER KeyPassword
    签名密钥密码

.PARAMETER StorePassword
    签名库密码

.EXAMPLE
    .\init_project.ps1 com.example.myapp 我的应用 MyApp myapp abc123 abc123
#>

param(
    [Parameter(Mandatory = $true, Position = 0)]
    [string]$NewPackage,

    [Parameter(Mandatory = $true, Position = 1)]
    [string]$NewAppName,

    [Parameter(Mandatory = $true, Position = 2)]
    [string]$NewProjectName,

    [Parameter(Mandatory = $true, Position = 3)]
    [string]$KeyAlias,

    [Parameter(Mandatory = $true, Position = 4)]
    [string]$KeyPassword,

    [Parameter(Mandatory = $true, Position = 5)]
    [string]$StorePassword
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

# ============================== Find JDK ==============================
$script:JAVA_HOME = $env:JAVA_HOME
$script:KeytoolCmd = 'keytool'

if (-not $script:JAVA_HOME) {
    Write-Host "[WARN] JAVA_HOME 未设置，尝试自动查找 JDK..." -ForegroundColor Yellow
    
    # Try common installation paths
    $jdkPaths = @(
        "D:\Java",
        "C:\Program Files\Java",
        "C:\Program Files (x86)\Java",
        "${env:LOCALAPPDATA}\Programs\Java"
    )
    
    foreach ($path in $jdkPaths) {
        if (Test-Path $path) {
            $jdkDir = Get-ChildItem $path -Directory | Where-Object { $_.Name -match '^jdk' } | Sort-Object LastWriteTime -Descending | Select-Object -First 1
            if ($jdkDir) {
                $script:JAVA_HOME = $jdkDir.FullName
                break
            }
        }
    }
}

if ($script:JAVA_HOME) {
    $keytoolPath = Join-Path $script:JAVA_HOME "bin\keytool.exe"
    if (Test-Path $keytoolPath) {
        $script:KeytoolCmd = $keytoolPath
        Write-Host "[INFO] 找到 JDK: $script:JAVA_HOME" -ForegroundColor Cyan
    } else {
        Write-Host "[WARN] JAVA_HOME 已设置但 keytool 不存在" -ForegroundColor Yellow
    }
} else {
    Write-Host "[WARN] 未找到 JDK，keytool 命令需要在 PATH 中" -ForegroundColor Yellow
}

# ============================== Constants ==============================
$OLD_PACKAGE      = 'com.kgmedia.fastfilm'
$OLD_APP_NAME     = '快出片'
$OLD_PROJECT_NAME = 'FastFilm'
$OLD_KEY_ALIAS    = 'fastfilm'

# UTF-8 without BOM encoder
$UTF8NoBOM = New-Object System.Text.UTF8Encoding $false

# ============================== Helpers ==============================

function Write-Info  { param([string]$msg) Write-Host "[INFO] $msg" -ForegroundColor Cyan }
function Write-Ok    { param([string]$msg) Write-Host "[OK]   $msg" -ForegroundColor Green }
function Write-Warn  { param([string]$msg) Write-Host "[WARN] $msg" -ForegroundColor Yellow }
function Write-Err   { param([string]$msg) Write-Host "[ERROR] $msg" -ForegroundColor Red }

function ReplaceInFile {
    param(
        [string]$FilePath,
        [string]$OldText,
        [string]$NewText
    )
    $fullPath = (Resolve-Path $FilePath).Path
    $content  = [IO.File]::ReadAllText($fullPath, $script:UTF8NoBOM)
    $content  = $content.Replace($OldText, $NewText)
    [IO.File]::WriteAllText($fullPath, $content, $script:UTF8NoBOM)
}

function ReplaceInDir {
    param(
        [string]$DirPath,
        [string[]]$Include,
        [string]$OldText,
        [string]$NewText
    )
    Get-ChildItem -Path $DirPath -Recurse -Include $Include | ForEach-Object {
        $content = [IO.File]::ReadAllText($_.FullName, $script:UTF8NoBOM)
        if ($content.Contains($OldText)) {
            $content = $content.Replace($OldText, $NewText)
            [IO.File]::WriteAllText($_.FullName, $content, $script:UTF8NoBOM)
        }
    }
}

# ============================== Validate ==============================

if ($NewPackage -notmatch '^[a-z][a-z0-9]*(\.[a-z][a-z0-9]*)+$') {
    Write-Err "包名格式不正确: $NewPackage"
    Write-Host "        应为 com.example.app 格式, 只含小写字母、数字、点号"
    exit 1
}

$OLD_PACKAGE_PATH = $OLD_PACKAGE.Replace('.', '\')
$NEW_PACKAGE_PATH = $NewPackage.Replace('.', '\')
$NEW_KEY_ALIAS    = $KeyAlias

Set-Location $PSScriptRoot

# ============================== Print info ==============================

Write-Host ""
Write-Host "============================================"
Write-Host "  Android 项目模板初始化"
Write-Host "============================================"
Write-Host ""
Write-Info "旧包名:       $OLD_PACKAGE"
Write-Info "新包名:       $NewPackage"
Write-Info "旧APP名:      $OLD_APP_NAME"
Write-Info "新APP名:      $NewAppName"
Write-Info "新项目名:     $NewProjectName"
Write-Info "keyAlias:     $NEW_KEY_ALIAS"
Write-Info "keyPassword:  $KeyPassword"
Write-Info "storePassword: $StorePassword"
Write-Host ""
Write-Host "按任意键开始初始化, 按 Ctrl+C 取消..."
$null = $Host.UI.RawUI.ReadKey('NoEcho,IncludeKeyDown')
Write-Host ""

# ============================== 1. config.gradle ==============================
Write-Info "更新 config.gradle ..."
ReplaceInFile 'config.gradle' $OLD_PACKAGE $NewPackage
Write-Ok "config.gradle"

# ============================== 2. settings.gradle ==============================
Write-Info "更新 settings.gradle ..."
ReplaceInFile 'settings.gradle' "rootProject.name = `"$OLD_PROJECT_NAME`"" "rootProject.name = `"$NewProjectName`""
Write-Ok "settings.gradle"

# ============================== 3. app\build.gradle (package) ==============================
Write-Info "更新 app\build.gradle (包名/签名) ..."
ReplaceInFile 'app\build.gradle' "namespace '$OLD_PACKAGE'" "namespace '$NewPackage'"
ReplaceInFile 'app\build.gradle' "storeFile file(`"../$OLD_PACKAGE.jks`")" "storeFile file(`"../$NewPackage.jks`")"
ReplaceInFile 'app\build.gradle' "keyAlias '$OLD_KEY_ALIAS'" "keyAlias '$NEW_KEY_ALIAS'"
ReplaceInFile 'app\build.gradle' "keyPassword '111111'" "keyPassword '$KeyPassword'"
ReplaceInFile 'app\build.gradle' "storePassword '111111'" "storePassword '$StorePassword'"
Write-Ok "app\build.gradle (包名/签名)"

# ============================== 4. app\build.gradle (SDK keys) ==============================
Write-Info "清理 app\build.gradle 第三方SDK密钥 ..."
$buildGradle = 'app\build.gradle'
# Douyin
ReplaceInFile $buildGradle 'awg154kyqmvhns2f' 'YOUR_DY_CLIENT_KEY'
# WeChat
ReplaceInFile $buildGradle 'wxc4c951d1cd1816fc' 'YOUR_WX_APP_ID'
ReplaceInFile $buildGradle '9d679c3bdc5531e794d7ab83214d40af' 'YOUR_WX_APP_SECRET'
# GrowingIO
ReplaceInFile $buildGradle '09ae0b3ae5497abe' 'YOUR_GROWING_SCHEME'
# Kuaishou
ReplaceInFile $buildGradle 'ks664562422307705809' 'YOUR_KWAI_APP_ID'
# QQ
ReplaceInFile $buildGradle '1112354170' 'YOUR_QQ_APP_ID'
ReplaceInFile $buildGradle 'JLx3P8RMZrnTQ2k0' 'YOUR_QQ_APP_KEY'
# Weibo
ReplaceInFile $buildGradle '4008287256' 'YOUR_WB_APP_KEY'
ReplaceInFile $buildGradle '9fe4864f3bfff59e241442ae0d645615' 'YOUR_WB_APP_SECRET'
ReplaceInFile $buildGradle 'https://m.helloaa.cn/' 'https://example.com'
# Service/Privacy/Official URLs
ReplaceInFile $buildGradle 'https://makemoneyoss.yiduizf.cn/data/front/taskdesc/%E8%B5%9A%E9%92%B1%E7%8C%AB%E7%94%A8%E6%88%B7%E6%9C%8D%E5%8A%A1%E5%8D%8F%E8%AE%AE.html' 'https://example.com/service'
ReplaceInFile $buildGradle 'https://makemoneyoss.yiduizf.cn/data/front/taskdesc/%E8%B5%9A%E9%92%B1%E7%8C%AB%E9%9A%90%E7%A7%81%E6%94%BF%E7%AD%96.html' 'https://example.com/privacy'
ReplaceInFile $buildGradle 'http://rmgf.wechatrenmai.cn' 'https://example.com'
# Umeng
ReplaceInFile $buildGradle 'a0d38408ddaceb5c935d511e71f1ac36' 'YOUR_UMENG_SECRET'
ReplaceInFile $buildGradle '698d31b96f259537c758f5a2' 'YOUR_UMENG_APP_KEY'
ReplaceInFile $buildGradle '69005d13644c9e2c20677a21' 'YOUR_UMENG_APP_KEY'
# Shanyan
ReplaceInFile $buildGradle 'KfNYHDRd' 'YOUR_SHANYAN_APPID'
ReplaceInFile $buildGradle 'VDE8Q8TY' 'YOUR_SHANYAN_APP_KEY'
# OpenInstall
ReplaceInFile $buildGradle 'dacw8u' 'YOUR_OPENINSTALL_KEY'
# Server URLs
ReplaceInFile $buildGradle 'https://kcp-test.kwaige.cn' 'https://your-dev-server.com'
ReplaceInFile $buildGradle 'http://120.24.240.28:9010' 'https://your-preview-server.com'
ReplaceInFile $buildGradle 'https://kuaichupianjk.kuaigeai.cn' 'https://your-production-server.com'
Write-Ok "app\build.gradle (第三方SDK密钥已清理)"

# ============================== 5. AndroidManifest.xml ==============================
Write-Info "更新 AndroidManifest.xml ..."
ReplaceInFile 'app\src\main\AndroidManifest.xml' $OLD_PACKAGE $NewPackage
Write-Ok "AndroidManifest.xml"

# ============================== 6. strings.xml ==============================
Write-Info "更新 strings.xml (APP名称) ..."
ReplaceInFile 'app\src\main\res\values\strings.xml' $OLD_APP_NAME $NewAppName
Write-Ok "strings.xml"

# ============================== 7. Create new keystore ==============================
Write-Info "创建新签名文件 ..."

# Remove old keystore
if (Test-Path "$OLD_PACKAGE.jks") {
    Remove-Item "$OLD_PACKAGE.jks" -Force
}

$jksFile = "$NewPackage.jks"
if (Test-Path $jksFile) {
    Remove-Item $jksFile -Force
}

$keytoolArgs = @(
    '-genkey', '-v',
    '-keystore', $jksFile,
    '-alias', $NEW_KEY_ALIAS,
    '-keyalg', 'RSA',
    '-keysize', '2048',
    '-validity', '36525',
    '-storepass', $StorePassword,
    '-keypass', $KeyPassword,
    '-dname', 'CN=kuaige, OU=kuaige, O=深圳市快歌智能有限公司, L=ShenZhen, ST=GuangDong, C=CN'
)

$ErrorActionPreference = 'Continue'
$keytoolResult = & $script:KeytoolCmd @keytoolArgs 2>&1
$ErrorActionPreference = 'Stop'
if ($LASTEXITCODE -ne 0) {
    Write-Err "keytool 执行失败: $keytoolResult"
    Write-Host "请确保 JDK 已安装且 keytool 在 PATH 中"
    exit 1
}
Write-Ok "签名文件已创建: $jksFile"

# ============================== 8. Extract cert info ==============================
Write-Info "提取签名信息 ..."

# Extract MD5/SHA1/SHA-256
$ErrorActionPreference = 'Continue'
$certInfo = & $script:KeytoolCmd -list -v -keystore $jksFile -alias $NEW_KEY_ALIAS -storepass $StorePassword 2>&1 | Out-String
$ErrorActionPreference = 'Stop'

$newMD5 = ''
$newSHA1 = ''
$newSHA256 = ''

if ($certInfo -match 'MD5:\s+([\dA-Fa-f:]+)') {
    $newMD5 = $Matches[1]
}
if ($certInfo -match 'SHA1:\s+([\dA-Fa-f:]+)') {
    $newSHA1 = $Matches[1]
}
if ($certInfo -match 'SHA-?256:\s+([\dA-Fa-f:]+)') {
    $newSHA256 = $Matches[1]
}

# SHA1 lowercase without colons (for apisecurity)
$newSHA1NoColon = $newSHA1.Replace(':', '').ToLower()

Write-Ok "MD5:    $newMD5"
Write-Ok "SHA1:   $newSHA1"
Write-Ok "SHA256: $newSHA256"

# Extract certificate DER hex (for apisecurity signature)
$ErrorActionPreference = 'Continue'
$certPem = & $script:KeytoolCmd -exportcert -keystore $jksFile -alias $NEW_KEY_ALIAS -storepass $StorePassword 2>$null
$certHex = ($certPem | ForEach-Object { '{0:x2}' -f $_ }) -join ''

# Extract public key PEM
$certPemText = & $script:KeytoolCmd -exportcert -rfc -keystore $jksFile -alias $NEW_KEY_ALIAS -storepass $StorePassword 2>$null | Out-String
$ErrorActionPreference = 'Stop'

Write-Ok "证书HEX长度: $($certHex.Length) 字符"

# ============================== 9. gradle.properties ==============================
Write-Info "更新 gradle.properties ..."
ReplaceInFile 'gradle.properties' "#Alias: $OLD_KEY_ALIAS" "#Alias: $NEW_KEY_ALIAS"
ReplaceInFile 'gradle.properties' '#MD5: 67:E7:63:39:E0:7A:4D:95:BC:7F:B9:24:C0:6B:C0:ED' "#MD5: $newMD5"
ReplaceInFile 'gradle.properties' '#SHA1: EC:82:59:D5:0D:00:62:32:6C:B4:80:34:32:A3:57:94:36:DF:38:76' "#SHA1: $newSHA1"
ReplaceInFile 'gradle.properties' '#SHA-256: FF:D7:E3:BC:E1:53:69:EF:1C:D5:19:8A:04:0E:7F:A7:D2:7E:39:32:BA:C1:3E:9A:A6:5C:94:C5:C0:55:A0:58' "#SHA-256: $newSHA256"

# Replace old public key PEM
$gradlePropPath = (Resolve-Path 'gradle.properties').Path
$gpContent = [IO.File]::ReadAllText($gradlePropPath, $UTF8NoBOM)
# Remove old public.key line(s) and replace with new
$gpContent = [regex]::Replace($gpContent, '#public\.key = .+', "#public.key = $($certPemText.Trim())")
[IO.File]::WriteAllText($gradlePropPath, $gpContent, $UTF8NoBOM)
Write-Ok "gradle.properties"

# ============================== 10. Move source directory ==============================
Write-Info "移动源码目录 ..."
$OLD_SRC = "app\src\main\java\$OLD_PACKAGE_PATH"
$NEW_SRC = "app\src\main\java\$NEW_PACKAGE_PATH"

if (Test-Path $OLD_SRC) {
    if (-not (Test-Path $NEW_SRC)) {
        New-Item -ItemType Directory -Path $NEW_SRC -Force | Out-Null
    }
    Copy-Item "$OLD_SRC\*" $NEW_SRC -Recurse -Force
    Remove-Item $OLD_SRC -Recurse -Force

    $parts = $OLD_PACKAGE.Split('.')
    $basePath = 'app\src\main\java'
    for ($i = $parts.Length - 1; $i -ge 0; $i--) {
        $dirPath = Join-Path $basePath ($parts[0..$i] -join '\')
        if ((Test-Path $dirPath) -and ((Get-ChildItem $dirPath -Force | Measure-Object).Count -eq 0)) {
            Remove-Item $dirPath -Force
        }
    }
    Write-Ok "源码目录已移动: $OLD_PACKAGE_PATH -> $NEW_PACKAGE_PATH"
} else {
    Write-Warn "源码目录 $OLD_SRC 不存在, 跳过"
}

# ============================== 11. Kotlin/Java source ==============================
Write-Info "更新 Kotlin/Java 源文件中的包名引用 ..."
ReplaceInDir 'app\src\main\java' @('*.kt', '*.java') $OLD_PACKAGE $NewPackage
Write-Ok "Kotlin/Java 源文件"

# ============================== 12. XML layouts ==============================
Write-Info "更新 XML 布局文件中的包名引用 ..."
ReplaceInDir 'app\src\main\res' @('*.xml') $OLD_PACKAGE $NewPackage
Write-Ok "XML 布局文件"

# ============================== 13. apisecurity native ==============================
Write-Info "更新 apisecurity 原生代码 ..."
$cppFile = 'apisecurity\src\main\cpp\apisecurity-lib.cpp'
if (Test-Path $cppFile) {
    # Package name (APP_PKG + APPLICATION_NAME)
    ReplaceInFile $cppFile $OLD_PACKAGE $NewPackage

    # SHA1
    ReplaceInFile $cppFile 'ec8259d50d0062326cb4803432a3579436df3876' $newSHA1NoColon

    # API keys -> placeholders
    $cppPath = (Resolve-Path $cppFile).Path
    $cppContent = [IO.File]::ReadAllText($cppPath, $UTF8NoBOM)

    # Replace KEY (production)
    $cppContent = [regex]::Replace($cppContent, '#define KEY ".*?"', '#define KEY "YOUR_API_KEY"')
    # Replace KEY_DEV
    $cppContent = [regex]::Replace($cppContent, '#define KEY_DEV ".*?"', '#define KEY_DEV "YOUR_API_KEY_DEV"')
    # Replace ERROR_KEY
    $cppContent = [regex]::Replace($cppContent, 'const std::string\s+ERROR_KEY\s*=\s*".*?"', 'const std::string  ERROR_KEY= "YOUR_ERROR_KEY"')
    # Replace signature
    $cppContent = [regex]::Replace($cppContent, 'const char \*signature = ".*?"', "const char *signature = `"$certHex`"")

    [IO.File]::WriteAllText($cppPath, $cppContent, $UTF8NoBOM)

    Write-Ok "apisecurity-lib.cpp (SHA1 + signature + 包名已更新)"
    Write-Warn "请手动更新 apisecurity-lib.cpp 中的 KEY、KEY_DEV (与后端协商)"
} else {
    Write-Warn "$cppFile 不存在, 跳过"
}

# ============================== 14. Clean build cache ==============================
Write-Info "清理构建缓存 ..."
@('.gradle', 'app\build', 'build', '.idea') | ForEach-Object {
    if (Test-Path $_) {
        try { Remove-Item $_ -Recurse -Force -ErrorAction Stop }
        catch { Write-Warn "无法完全删除 $_ (可能被占用), 请关闭 Android Studio 后手动清理" }
    }
}
Write-Ok "构建缓存已清理"

# ============================== 15. Git add ==============================
Write-Info "添加变更到 Git ..."
& git add -A 2>$null
if ($LASTEXITCODE -eq 0) {
    Write-Ok "所有变更已添加到 Git 暂存区"
} else {
    Write-Warn "Git add 失败, 请手动执行 git add"
}

# ============================== Done ==============================

Write-Host ""
Write-Host "============================================"
Write-Host "  项目初始化完成!" -ForegroundColor Green
Write-Host "============================================"
Write-Host ""
Write-Info "项目名称:     $NewProjectName"
Write-Info "包名:         $NewPackage"
Write-Info "APP名称:      $NewAppName"
Write-Info "keyAlias:     $NEW_KEY_ALIAS"
Write-Info "keystore:     $jksFile"
Write-Host ""
Write-Warn "注意事项:"
Write-Host "  1. 请用 Android Studio 打开项目, 等待 Gradle Sync 完成"
Write-Host "  2. 请更新 app\build.gradle 中的第三方 SDK 密钥 (微信/QQ/友盟等)"
Write-Host "     当前已替换为 YOUR_XXX 占位符, 需填入真实值"
Write-Host "  3. 请更新 app\build.gradle 中的 SERVER_URL 为你的服务器地址"
Write-Host "  4. 如需启用 apisecurity 模块, 请手动修改 apisecurity-lib.cpp 中的:"
Write-Host "     - KEY / KEY_DEV (API签名密钥, 与后端协商)"
Write-Host "     - SHA1 和 signature 已自动从新签名文件生成"
Write-Host "     详见 apisecurity\README.md"
Write-Host ""
