@echo off
chcp 65001 >nul 2>&1
setlocal EnableDelayedExpansion

:: ============================================================
::  Android 项目模板初始化脚本 (Windows)
::  用法: init_project.bat 新包名 新APP名称 [新项目名称]
::  示例: init_project.bat com.example.myapp 我的应用 MyApp
:: ============================================================

:: ============================== 参数校验 ==============================
set "OLD_PACKAGE=com.kgmedia.fastfilm"
set "OLD_APP_NAME=快出片"
set "OLD_PROJECT_NAME=FastFilm"
set "OLD_KEY_ALIAS=fastfilm"

if "%~1"=="" goto :usage
if "%~2"=="" goto :usage

set "NEW_PACKAGE=%~1"
set "NEW_APP_NAME=%~2"

:: 验证包名格式 (至少两段, 只含小写字母数字和点)
echo %NEW_PACKAGE% | findstr /r "^[a-z][a-z0-9]*\.[a-z][a-z0-9]*" >nul 2>&1
if errorlevel 1 (
    echo [ERROR] 包名格式不正确: %NEW_PACKAGE%
    echo         应为 com.example.app 格式 ^(小写字母、数字、点号^)
    goto :eof
)

:: 项目名称: 有第三个参数就用, 没有就从包名最后一段生成
if not "%~3"=="" (
    set "NEW_PROJECT_NAME=%~3"
) else (
    for /f "delims=" %%a in ('powershell -NoProfile -Command "('%NEW_PACKAGE%'.Split('.')[-1].Substring(0,1).ToUpper() + '%NEW_PACKAGE%'.Split('.')[-1].Substring(1))"') do set "NEW_PROJECT_NAME=%%a"
)

:: 路径转换: com.example.app -> com\example\app
set "OLD_PACKAGE_PATH=%OLD_PACKAGE:.=\%"
set "NEW_PACKAGE_PATH=%NEW_PACKAGE:.=\%"

:: keyAlias: 包名最后一段
for /f "delims=" %%a in ('powershell -NoProfile -Command "'%NEW_PACKAGE%'.Split('.')[-1]"') do set "NEW_KEY_ALIAS=%%a"

:: 获取脚本所在目录并切换
set "SCRIPT_DIR=%~dp0"
cd /d "%SCRIPT_DIR%"

echo.
echo ============================================
echo   Android 项目模板初始化
echo ============================================
echo.
echo [INFO] 旧包名:     %OLD_PACKAGE%
echo [INFO] 新包名:     %NEW_PACKAGE%
echo [INFO] 旧APP名:    %OLD_APP_NAME%
echo [INFO] 新APP名:    %NEW_APP_NAME%
echo [INFO] 新项目名:   %NEW_PROJECT_NAME%
echo [INFO] 新keyAlias: %NEW_KEY_ALIAS%
echo.
echo 按任意键开始初始化, 按 Ctrl+C 取消...
pause >nul

:: ============================== 1. config.gradle ==============================
echo [INFO] 更新 config.gradle ...
powershell -NoProfile -Command ^
    "(Get-Content 'config.gradle' -Encoding UTF8) -replace '%OLD_PACKAGE%', '%NEW_PACKAGE%' | Set-Content 'config.gradle' -Encoding UTF8"
echo [OK]   config.gradle 已更新

:: ============================== 2. settings.gradle ==============================
echo [INFO] 更新 settings.gradle ...
powershell -NoProfile -Command ^
    "(Get-Content 'settings.gradle' -Encoding UTF8) -replace 'rootProject\.name = \"%OLD_PROJECT_NAME%\"', 'rootProject.name = \"%NEW_PROJECT_NAME%\"' | Set-Content 'settings.gradle' -Encoding UTF8"
echo [OK]   settings.gradle 已更新

:: ============================== 3. app\build.gradle ==============================
echo [INFO] 更新 app\build.gradle ...
powershell -NoProfile -Command ^
    "$c = Get-Content 'app\build.gradle' -Encoding UTF8;" ^
    "$c = $c -replace [regex]::Escape('namespace ''%OLD_PACKAGE%'''), 'namespace ''%NEW_PACKAGE%''';" ^
    "$c = $c -replace [regex]::Escape('storeFile file(\"../%OLD_PACKAGE%.jks\")'), 'storeFile file(\"../%NEW_PACKAGE%.jks\")';" ^
    "$c = $c -replace [regex]::Escape('keyAlias ''%OLD_KEY_ALIAS%'''), 'keyAlias ''%NEW_KEY_ALIAS%''';" ^
    "$c | Set-Content 'app\build.gradle' -Encoding UTF8"
echo [OK]   app\build.gradle 已更新

:: ============================== 4. AndroidManifest.xml ==============================
echo [INFO] 更新 AndroidManifest.xml ...
powershell -NoProfile -Command ^
    "(Get-Content 'app\src\main\AndroidManifest.xml' -Encoding UTF8) -replace '%OLD_PACKAGE%', '%NEW_PACKAGE%' | Set-Content 'app\src\main\AndroidManifest.xml' -Encoding UTF8"
echo [OK]   AndroidManifest.xml 已更新

:: ============================== 5. strings.xml ==============================
echo [INFO] 更新 strings.xml (APP名称) ...
powershell -NoProfile -Command ^
    "(Get-Content 'app\src\main\res\values\strings.xml' -Encoding UTF8) -replace [regex]::Escape('<string name=\"app_name\">%OLD_APP_NAME%</string>'), '<string name=\"app_name\">%NEW_APP_NAME%</string>' | Set-Content 'app\src\main\res\values\strings.xml' -Encoding UTF8"
echo [OK]   strings.xml 已更新

:: ============================== 6. keystore 重命名 ==============================
echo [INFO] 重命名 keystore 文件 ...
if exist "%OLD_PACKAGE%.jks" (
    move /Y "%OLD_PACKAGE%.jks" "%NEW_PACKAGE%.jks" >nul
    echo [OK]   keystore 已重命名为 %NEW_PACKAGE%.jks
) else (
    echo [WARN] keystore 文件 %OLD_PACKAGE%.jks 不存在, 跳过
)

:: ============================== 7. gradle.properties ==============================
echo [INFO] 更新 gradle.properties ...
powershell -NoProfile -Command ^
    "(Get-Content 'gradle.properties' -Encoding UTF8) -replace '#Alias: %OLD_KEY_ALIAS%', '#Alias: %NEW_KEY_ALIAS%' | Set-Content 'gradle.properties' -Encoding UTF8"
echo [OK]   gradle.properties 已更新

:: ============================== 8. 移动源码目录 ==============================
echo [INFO] 移动源码目录 ...
set "OLD_SRC=app\src\main\java\%OLD_PACKAGE_PATH%"
set "NEW_SRC=app\src\main\java\%NEW_PACKAGE_PATH%"

if exist "%OLD_SRC%" (
    if not exist "%NEW_SRC%" mkdir "%NEW_SRC%"
    xcopy "%OLD_SRC%\*" "%NEW_SRC%\" /E /Q /Y >nul
    rmdir /S /Q "%OLD_SRC%" >nul 2>&1

    :: 清理旧的空父目录
    powershell -NoProfile -Command ^
        "$parts = '%OLD_PACKAGE%'.Split('.');" ^
        "$basePath = 'app\src\main\java';" ^
        "for ($i = $parts.Length - 1; $i -ge 0; $i--) {" ^
        "  $dir = Join-Path $basePath ($parts[0..$i] -join '\');" ^
        "  if ((Test-Path $dir) -and ((Get-ChildItem $dir -Force | Measure-Object).Count -eq 0)) {" ^
        "    Remove-Item $dir -Force" ^
        "  }" ^
        "}"
    echo [OK]   源码目录已移动: %OLD_PACKAGE_PATH% -^> %NEW_PACKAGE_PATH%
) else (
    echo [WARN] 源码目录 %OLD_SRC% 不存在, 跳过
)

:: ============================== 9. 更新 Kotlin/Java 源文件 ==============================
echo [INFO] 更新 Kotlin/Java 源文件中的包名引用 ...
powershell -NoProfile -Command ^
    "Get-ChildItem -Path 'app\src\main\java' -Recurse -Include '*.kt','*.java' | ForEach-Object {" ^
    "  $content = Get-Content $_.FullName -Raw -Encoding UTF8;" ^
    "  if ($content -match '%OLD_PACKAGE%') {" ^
    "    $content -replace '%OLD_PACKAGE%', '%NEW_PACKAGE%' | Set-Content $_.FullName -Encoding UTF8 -NoNewline" ^
    "  }" ^
    "}"
echo [OK]   Kotlin/Java 源文件已更新

:: ============================== 10. 更新 XML 布局文件 ==============================
echo [INFO] 更新 XML 布局文件中的包名引用 ...
powershell -NoProfile -Command ^
    "Get-ChildItem -Path 'app\src\main\res' -Recurse -Include '*.xml' | ForEach-Object {" ^
    "  $content = Get-Content $_.FullName -Raw -Encoding UTF8;" ^
    "  if ($content -match '%OLD_PACKAGE%') {" ^
    "    $content -replace '%OLD_PACKAGE%', '%NEW_PACKAGE%' | Set-Content $_.FullName -Encoding UTF8 -NoNewline" ^
    "  }" ^
    "}"
echo [OK]   XML 布局文件已更新

:: ============================== 11. 更新 apisecurity 原生代码 ==============================
echo [INFO] 更新 apisecurity 原生代码中的包名 ...
set "CPP_FILE=apisecurity\src\main\cpp\apisecurity-lib.cpp"
if exist "%CPP_FILE%" (
    powershell -NoProfile -Command ^
        "$c = Get-Content '%CPP_FILE%' -Raw -Encoding UTF8;" ^
        "$c = $c -replace [regex]::Escape('%OLD_PACKAGE%'), '%NEW_PACKAGE%';" ^
        "$c | Set-Content '%CPP_FILE%' -Encoding UTF8 -NoNewline"
    echo [OK]   apisecurity-lib.cpp 已更新 (APP_PKG + APPLICATION_NAME)
    echo [WARN] 请手动更新 apisecurity-lib.cpp 中的 SHA1、signature、KEY、KEY_DEV
) else (
    echo [WARN] %CPP_FILE% 不存在, 跳过
)

:: ============================== 12. 清理构建缓存 ==============================
echo [INFO] 清理构建缓存 ...
if exist ".gradle" rmdir /S /Q ".gradle" >nul 2>&1
if exist "app\build" rmdir /S /Q "app\build" >nul 2>&1
if exist "build" rmdir /S /Q "build" >nul 2>&1
if exist ".idea" rmdir /S /Q ".idea" >nul 2>&1
echo [OK]   构建缓存已清理

:: ============================== 完成 ==============================
echo.
echo ============================================
echo   项目初始化完成!
echo ============================================
echo.
echo [INFO] 项目名称: %NEW_PROJECT_NAME%
echo [INFO] 包名:     %NEW_PACKAGE%
echo [INFO] APP名称:  %NEW_APP_NAME%
echo.
echo [注意事项]
echo   1. 请用 Android Studio 打开项目, 等待 Gradle Sync 完成
echo   2. keystore 密码仍为默认值(111111), 请在正式发布前修改
echo   3. 请更新 app\build.gradle 中的第三方 SDK 密钥 (微信/QQ/友盟等)
echo   4. 请更新 app\build.gradle 中的 SERVER_URL 为你的服务器地址
echo   5. 如需启用 apisecurity 模块, 请手动修改 apisecurity-lib.cpp 中的:
echo      - SHA1 (APK签名哈希)
echo      - signature (签名证书十六进制内容)
echo      - KEY / KEY_DEV (API签名密钥)
echo      详见 README.md 第八章
echo   6. 如需生成新的 keystore, 请运行:
echo      keytool -genkey -v -keystore %NEW_PACKAGE%.jks -alias %NEW_KEY_ALIAS% -keyalg RSA -keysize 2048 -validity 36500
echo.
pause
goto :eof

:: ============================== 帮助信息 ==============================
:usage
echo.
echo  Android 项目模板初始化脚本
echo.
echo  用法: init_project.bat ^<新包名^> ^<新APP名称^> [新项目名称]
echo.
echo  参数:
echo    新包名       必填, 例如 com.example.myapp
echo    新APP名称    必填, 例如 我的应用
echo    新项目名称   可选, 不填则从包名最后一段自动生成(首字母大写)
echo.
echo  示例:
echo    init_project.bat com.example.myapp 我的应用 MyApp
echo    init_project.bat com.company.news 新闻头条
echo.
pause
goto :eof
