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
| compileSdkVersion | 35 |
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
| `init_project.bat` | Windows（CMD） |
| `init_project.sh` | Unix / macOS / Git Bash |

**Windows（CMD）：**
```
init_project.bat <新包名> <新APP名称> [新项目名称]
```

**Unix / macOS / Git Bash：**
```bash
bash init_project.sh <新包名> <新APP名称> [新项目名称]
```

**参数说明：**

| 参数 | 是否必填 | 说明 | 示例 |
|------|---------|------|------|
| 新包名 | 必填 | Android 应用包名，小写字母+数字+点号 | `com.example.myapp` |
| 新APP名称 | 必填 | 显示在手机桌面上的应用名称 | `我的应用` |
| 新项目名称 | 可选 | Gradle 项目名，不填则自动从包名最后一段生成（首字母大写） | `MyApp` |

**示例：**

```bash
# Windows
init_project.bat com.example.myapp 我的应用 MyApp

# Unix / macOS / Git Bash
bash init_project.sh com.example.myapp 我的应用 MyApp

# 省略项目名（自动生成为 Myapp）
init_project.bat com.example.myapp 我的应用

# 中文APP名称
init_project.bat com.company.news 新闻头条 News
```

### 第 3 步：用 Android Studio 打开项目

1. 打开 Android Studio → Open → 选择新项目目录
2. 等待 Gradle Sync 完成
3. 连接设备或启动模拟器，运行项目

---

## 四、初始化脚本自动处理的内容

脚本会自动完成以下 **11 项** 操作：

| 序号 | 操作 | 涉及文件 |
|------|------|----------|
| 1 | 更新 applicationId | `config.gradle` |
| 2 | 更新 rootProject.name | `settings.gradle` |
| 3 | 更新 namespace、keystore 路径、keyAlias | `app\build.gradle` |
| 4 | 更新 package 属性 | `app\src\main\AndroidManifest.xml` |
| 5 | 更新 app_name | `app\src\main\res\values\strings.xml` |
| 6 | 重命名 keystore 文件 | `com.kgmedia.fastfilm.jks` → `新包名.jks` |
| 7 | 更新 Alias 注释 | `gradle.properties` |
| 8 | 移动源码目录到新包路径 | `app\src\main\java\` 目录结构 |
| 9 | 替换所有 `.kt` / `.java` 文件中的包名 | 全部 Kotlin/Java 源文件（100+ 个） |
| 10 | 替换所有 XML 布局文件中的包名 | 全部 res 下 XML 文件（35+ 个） |
| 11 | 清理构建缓存 | `.gradle/`、`build/`、`.idea/` |

---

## 五、初始化后必须手动完成的配置

### 1. 修改 keystore 密码（发布前必须）

当前 keystore 使用默认密码 `111111`，正式发布前必须更换。

生成全新 keystore：
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

以下密钥在 `app\build.gradle` 中，需替换为新项目自己的：

| SDK | 配置项 | 位置 |
|-----|--------|------|
| 微信 | `WX_APP_ID`、`WX_APP_SECRET` | defaultConfig |
| QQ | `QQ_APP_ID`、`QQ_APP_KEY` | buildTypes |
| 微博 | `WB_APP_KEY`、`WB_APP_SECRET` | buildTypes |
| 友盟 | `UMENG_APP_KEY`、`UMENG_SECRET` | buildTypes |
| 闪验 | `SHANYAN_APPID`、`SHANYAN_APP_KEY` | buildTypes |
| 抖音 | `DY_CLIENT_KEY` | defaultConfig |
| GrowingIO | `GROWING_URL_SCHEME` | defaultConfig |
| 快手 | `KWAI_APP_ID` | manifestPlaceholders |

### 3. 更新服务器地址

修改 `app\build.gradle` 中 `productFlavors` 下的 `SERVER_URL`：

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
    versionCodeRelease: 100400,    // 6位版本号
    versionCodeDebug  : 100400,    // 8位Debug版本号
    versionName       : "1.4.0",   // 版本名称
]
```

版本号规则：
- **前两位**：重大版本更新
- **中间两位**：大版本更新
- **最后两位**：小版本更新
- Debug 版本额外两位：测试升级号

---

## 七、常见问题

### Q: 脚本运行后中文乱码？
A: 确保命令提示符使用 UTF-8 编码。脚本开头已设置 `chcp 65001`，如仍有问题，手动在 CMD 中先执行 `chcp 65001`。

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
