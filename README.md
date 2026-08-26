# Android 项目模板使用说明

## 一、概述

Android国内项目模板，此仓库仅维护模板代码。新建项目时在Codeup代码库
`新建代码库-模板新建-自定义模板-选择此模板`新建代码库，clone新代码库到本地，运行初始化脚本即可快速搭建一个完整的 Android 项目，无需从零配置。

## 二、模板包含内容

| 模块 | 说明 |
|------|------|
| `app/` | 主应用模块，包含完整的项目架构 |
| `matisse/` | 图片选择器库 |
| `magicindicator/` | 页面指示器库 |
| `datepicker/` | 日期选择器库 |
| `apisecurity/` | 原生安全库 |

### 项目架构（app 模块）

```
app/src/main/java/com/kgmedia/fastfilm/
├── App.kt                  # Application 入口
├── base/                   # 基础类（BaseActivity、BaseFragment 等）
├── common/                 # 公共工具类、适配器、常量
│   ├── adapter/            # 通用适配器
│   ├── ext/                # Kotlin 扩展函数
│   └── utils/              # 工具类
├── core/                   # 核心配置、管理器
├── feature/                # 功能模块（按页面划分）
│   ├── home/               # 首页
│   ├── login/              # 登录
│   ├── main/               # 主页面
│   ├── mine/               # 我的
│   ├── settings/           # 设置
│   └── ...                 # 其他功能页
├── kit/                    # 工具包
├── router/                 # 路由导航
├── service/                # 后台服务
├── widget/                 # 自定义控件
├── douyinapi/              # 抖音SDK集成
└── wxapi/                  # 微信支付集成
```

### 已集成的第三方库

| 库 | 用途 |
|----|------|
| Retrofit + OkHttp | 网络请求 |
| Room | 本地数据库 |
| ExoPlayer (Media3) | 视频播放 |
| Glide (imageloader) | 图片加载 |
| MMKV | 键值存储 |
| Lottie | 动画 |
| BaseRecyclerViewAdapter | RecyclerView 适配器 |
| SmartRefreshLayout | 下拉刷新 |
| Bugly | 崩溃上报 |
| 友盟 (Umeng) | 数据统计 |
| 字节 AppLog | 埋点 |
| GrowingIO | 增长分析 |
| FFmpeg | 视频处理 |
| PermissionX | 权限管理 |
| 闪验 | 一键登录 |

### 构建配置

| 配置项 | 值 |
|--------|-----|
| compileSdkVersion | 36 |
| minSdkVersion | 24 |
| targetSdkVersion | 35 |
| JVM Target | 17 |
| 环境维度 | develop / preview / production |
| 架构维度 | arch32 / arch64 |

---

## 三、新建项目步骤

### 第 1 步：复制模板

在Codeup代码库
`新建代码库-模板新建-自定义模板-选择此模板`新建代码库，clone新代码库到本地

### 第 2 步：运行初始化脚本

模板提供两个脚本，根据操作系统选择：

| 脚本 | 适用系统 |
|------|----------|
| `init_project.ps1` | Windows（PowerShell） |
| `init_project.sh` | Unix / macOS / Git Bash |

**Windows（PowerShell）：**
```powershell
.\init_project.ps1 <新包名> <新APP名称> <新项目名称> <keyAlias> <keyPassword> <storePassword>
```

**Unix / macOS / Git Bash：**
```bash
bash init_project.sh <新包名> <新APP名称> <新项目名称> <keyAlias> <keyPassword> <storePassword>
```

**参数说明：**

| 参数 | 说明 | 示例 |
|------|------|------|
| 新包名 | Android 应用包名，小写字母+数字+点号 | `com.example.myapp` |
| 新APP名称 | 显示在手机桌面上的应用名称 | `我的应用` |
| 新项目名称 | Gradle 项目名 | `MyApp` |
| keyAlias | 签名别名 | `myapp` |
| keyPassword | 签名密钥密码 | `abc123` |
| storePassword | 签名库密码 | `abc123` |

以上参数均为必填。

**示例：**

```bash
# Windows (PowerShell)
.\init_project.ps1 com.example.myapp 我的应用 MyApp myapp abc123 abc123

# Unix / macOS / Git Bash
bash init_project.sh com.example.myapp 我的应用 MyApp myapp abc123 abc123
```

### 第 3 步：用 Android Studio 打开项目

1. 打开 Android Studio → Open → 选择新项目目录
2. 等待 Gradle Sync 完成
3. 连接设备或启动模拟器，运行项目

---

## 四、初始化脚本自动处理的内容

脚本会自动完成以下 **14 项** 操作：

| 序号 | 操作 | 涉及文件 |
|------|------|----------|
| 1 | 更新 applicationId | `config.gradle` |
| 2 | 更新 rootProject.name | `settings.gradle` |
| 3 | 更新 namespace、keystore 路径、keyAlias、密码 | `app\build.gradle` |
| 4 | 清理第三方 SDK 密钥为占位符 | `app\build.gradle` |
| 5 | 更新 package 属性 | `app\src\main\AndroidManifest.xml` |
| 6 | 更新 app_name | `app\src\main\res\values\strings.xml` |
| 7 | 创建新签名文件（keytool） | `新包名.jks` |
| 8 | 提取签名信息并更新 | `gradle.properties` |
| 9 | 移动源码目录到新包路径 | `app\src\main\java\` 目录结构 |
| 10 | 替换所有 `.kt` / `.java` 文件中的包名 | 全部 Kotlin/Java 源文件（100+ 个） |
| 11 | 替换所有 XML 布局文件中的包名 | 全部 res 下 XML 文件（35+ 个） |
| 12 | 更新 apisecurity 原生代码（SHA1、signature、包名） | `apisecurity-lib.cpp` |
| 13 | API 密钥置为占位符 | `apisecurity-lib.cpp` |
| 14 | 清理构建缓存 | `.gradle/`、`build/`、`.idea/` |

---

## 五、初始化后必须手动完成的配置

### 1. 修改 keystore 密码（发布前必须）

初始化脚本已自动创建新的 keystore 文件，默认密码为 `123456`（或你在脚本参数中指定的密码）。正式发布前建议更换为更强的密码。

如需重新生成 keystore：
```
keytool -genkey -v -keystore 新包名.jks -alias 别名 -keyalg RSA -keysize 2048 -validity 36500
```

然后修改 `app\build.gradle` 中的签名配置：
```groovy
signingConfigs {
    release {
        keyAlias '你的别名'
        keyPassword '你的密码'
        storeFile file("../你的包名.jks")
        storePassword '你的密码'
    }
}
```

### 2. 更新第三方 SDK 密钥

初始化脚本会将 `app\build.gradle` 中的第三方 SDK 密钥替换为 `YOUR_XXX`，并清空 `gradle.properties` 中的友盟与厂商推送产品配置。按下表填写当前项目的真实值：

| 占位符 | SDK | 位置 |
|--------|-----|------|
| `YOUR_WX_APP_ID`、`YOUR_WX_APP_SECRET` | 微信 | defaultConfig |
| `YOUR_QQ_APP_ID`、`YOUR_QQ_APP_KEY` | QQ | buildTypes + manifestPlaceholders |
| `YOUR_WB_APP_KEY`、`YOUR_WB_APP_SECRET` | 微博 | buildTypes |
| `UMENG_APP_KEY`、`UMENG_MESSAGE_SECRET` | 友盟 | `gradle.properties` |
| `PUSH_ENABLE_CHANNEL_PUSH`、`PUSH_XIAOMI_*`、`PUSH_HUAWEI_APP_ID`、`PUSH_OPPO_*`、`PUSH_VIVO_*`、`PUSH_HONOR_APP_ID` | 厂商推送 | `gradle.properties` |
| `YOUR_SHANYAN_APPID`、`YOUR_SHANYAN_APP_KEY` | 闪验 | buildTypes |
| `YOUR_DY_CLIENT_KEY` | 抖音 | defaultConfig |
| `YOUR_GROWING_SCHEME` | GrowingIO | defaultConfig + manifestPlaceholders |
| `YOUR_KWAI_APP_ID` | 快手 | manifestPlaceholders |

厂商推送默认通过 `PUSH_ENABLE_CHANNEL_PUSH=false` 关闭，此时厂商 Key 可以为空；启用后必须填写全部 `PUSH_*`。这些配置会同时注入 `BuildConfig` 和友盟 `1.0.2` AAR 的 Manifest Placeholder，禁止再在 `AndroidManifest.xml` 或 `PushManager` 中重复维护。

### 3. 更新服务器地址

初始化脚本已将服务器地址替换为占位符。修改 `app\build.gradle` 中 `productFlavors` 下的 `SERVER_URL`：

```groovy
productFlavors {
    develop {
        buildConfigField 'String', 'SERVER_URL', '"https://你的测试服务器地址"'
    }
    preview {
        buildConfigField 'String', 'SERVER_URL', '"https://你的预发布服务器地址"'
    }
    production {
        buildConfigField 'String', 'SERVER_URL', '"https://你的正式服务器地址"'
    }
}
```

同时更新 `SERVICE_URL`（服务协议）、`PRIVACY_URL`（隐私政策）、`OFFICIAL_URL`（官方网站）、`WB_URL`（微博地址）。

### 4. 更新应用图标

替换以下目录中的图标文件：
```
app\src\main\res\mipmap-hdpi\
app\src\main\res\mipmap-mdpi\
app\src\main\res\mipmap-xhdpi\
app\src\main\res\mipmap-xxhdpi\
app\src\main\res\mipmap-xxxhdpi\
```

### 5. 配置原生安全库（apisecurity）

初始化脚本已自动完成：
- 包名替换（`APP_PKG`、`APPLICATION_NAME`）
- SHA1 和 signature 从新签名文件自动提取并写入
- API 密钥（`KEY`、`KEY_DEV`）置为 `YOUR_API_KEY` / `YOUR_API_KEY_DEV` 占位符

**仍需手动完成：** 与后端协商 API 签名密钥，替换 `apisecurity-lib.cpp` 中的 `KEY` 和 `KEY_DEV`。

详见 [apisecurity/README.md](apisecurity/README.md)。

### 6. 清理不需要的功能模块

根据新项目需求，删除 `app\src\main\java\新包名\feature\` 下不需要的功能模块目录，并同步删除对应的：
- `res\layout\` 中的布局文件
- `AndroidManifest.xml` 中的 Activity/Service 声明
- 路由注册代码

---

## 六、项目构建与运行

### 构建命令

```bash
# Debug 构建（开发环境 + 64位）
gradlew assembleDevelopArch64Debug

# Release 构建（生产环境 + 64位）
gradlew assembleProductionArch64Release

# 全量构建
gradlew assembleRelease
```

### APK 输出路径

APK 文件输出在 `app\build\outputs\apk\` 目录下，命名格式：
```
项目名_版本号_环境架构_时间_buildType.apk
```

### 版本号管理

修改 `config.gradle`：
```groovy
android = [
    versionCodeRelease: 100000,    // 6位版本号
    versionCodeDebug  : 100000,    // 8位Debug版本号
    versionName       : "1.0.0",   // 版本名称
]
```

版本号规则：
- **前两位**：重大版本更新
- **中间两位**：大版本更新
- **最后两位**：小版本更新
- Debug 版本额外两位：测试升级号

---

## 七、常见问题

### Q: PowerShell 脚本运行报错或中文乱码？
A: 确保 `init_project.ps1` 文件编码为 **UTF-8 with BOM**（PowerShell 5.x 需要 BOM 才能正确识别 UTF-8）。如果从 Git 拉取后 BOM 丢失，可用以下命令修复：
```powershell
$f = 'init_project.ps1'
$c = [IO.File]::ReadAllText($f, [Text.Encoding]::UTF8)
[IO.File]::WriteAllText($f, $c, [Text.UTF8Encoding]::new($true))
```
如遇到执行策略限制，先运行：`Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`

### Q: Gradle Sync 失败？
A: 检查以下几点：
1. JDK 版本是否为 17+
2. Gradle 版本是否匹配（查看 `gradle\wrapper\gradle-wrapper.properties`）
3. 网络代理设置（`gradle.properties` 中配置）

### Q: 如何启用/禁用子模块？
A: 编辑 `settings.gradle`，注释或取消注释对应的 `include` 行：
```groovy
include ':app'
include ':matisse'          // 图片选择器
include ':magicindicator'   // 页面指示器
//include ':apisecurity'    // 已注释=禁用
include ':datepicker'       // 日期选择器
```

### Q: 如何新增构建环境？
A: 在 `app\build.gradle` 的 `productFlavors` 中添加：
```groovy
staging {
    dimension 'env'
    buildConfigField 'String', 'SERVER_URL', '"https://staging.example.com"'
}
```
