# 原生安全库（apisecurity）使用说明

## 一、模块概述

`apisecurity` 是一个基于 JNI/NDK 的 **API 请求签名 + 应用防篡改** 安全库。核心安全逻辑在 C++ 原生代码中实现，防止被 Java 层逆向绕过。

**核心能力：**

| 能力 | 说明 |
|------|------|
| API 请求签名 | 对请求体进行 Base64 编码 + HMAC-SHA256 签名，防止接口被伪造调用 |
| APK 签名验证 | 三层校验（PackageManager / Java反射 / APK文件），防止二次打包 |
| Application 防替换 | 校验 Application 类名未被篡改 |
| PackageManager 反代理 | 检测并重置被 Hook 的 PackageManager |
| 网络代理/VPN 检测 | 检测 HTTP 代理和 VPN 连接 |
| 反调试 | 检测 IDA 调试端口（23946）和异常线程数 |

---

## 二、模块目录结构

```
apisecurity/
├── build.gradle                          # 模块构建配置（NDK 25.1, CMake 3.10.2）
├── proguard-rules.pro
├── README.md                             # 本文档
└── src/main/
    ├── AndroidManifest.xml               # 注册 InitProvider（早于 Application 初始化）
    ├── cpp/                              # C/C++ 原生代码
    │   ├── CMakeLists.txt                # CMake 构建配置
    │   ├── apisecurity-lib.cpp           # 主 JNI 实现（签名验证 + API签名）
    │   ├── base64.h / base64.cpp         # Base64 编解码
    │   ├── hmac_sha256.h / hmac_sha256.c # HMAC-SHA256 签名算法
    │   └── sha256.h / sha256.c           # SHA256 哈希算法
    └── java/cn/android/security/
        ├── APISecurity.java              # JNI 桥接类（对外暴露的 API）
        ├── AppSigning.java               # 签名校验工具类
        ├── InitProvider.java             # ContentProvider 早期初始化
        └── NetProxy.java                 # 网络代理/VPN检测
```

### 构建配置

| 配置项 | 值 |
|--------|-----|
| NDK 版本 | 25.1.8937393 |
| CMake 版本 | 3.10.2 |
| 输出 SO 名称 | libapisecurity-lib.so |
| 命名空间 | cn.android.security |
| 页面大小 | 16KB（适配新版 Android） |

---

## 三、工作原理

```
┌──────────────────────────────────────────────────────────────┐
│                       应用启动流程                              │
│                                                              │
│  1. InitProvider.onCreate()          ← 早于 Application       │
│     ├── 检测 PackageManager 是否被代理                          │
│     ├── 被代理则重置 PM（防 Hook）                              │
│     ├── 校验 APK 签名                                         │
│     ├── 校验 Application 类名                                  │
│     └── 调用 native verifyApp() / verifyApplication()         │
│                                                              │
│  2. App.onCreate()                                           │
│     └── APISecurity.init(context, isProduction)               │
│         ├── [Native] 第1层: PackageManager 获取签名 → 比对SHA1  │
│         ├── [Native] 第2层: 调用Java反射获取APK签名 → 比对SHA1   │
│         ├── [Native] 第3层: 从APK安装路径读取文件签名 → 比对SHA1  │
│         ├── [Native] 校验包名是否匹配 APP_PKG                   │
│         └── 通过后加载 API 密钥（production → KEY / dev → KEY_DEV）│
│                                                              │
│  3. 网络请求时                                                 │
│     └── APISecurity.sign(context, userId, requestBody)        │
│         ├── Base64 编码请求体                                   │
│         ├── 根据 userId 动态派生签名密钥                         │
│         ├── HMAC-SHA256(派生密钥, Base64数据)                    │
│         └── 返回 {"data":"base64内容","sign":"hmac_hex签名"}     │
└──────────────────────────────────────────────────────────────┘
```

### 签名验证三层防护

| 层级 | 方式 | 防护对象 |
|------|------|----------|
| 第1层 | PackageManager.getPackageInfo() | 检测基本签名篡改 |
| 第2层 | Java 反射调用 APK 签名解析 | 防 PackageManager 被 Hook 后返回伪造签名 |
| 第3层 | 从 APK 安装路径直接读取文件签名 | 防应用内存中签名被替换 |

### API 签名密钥派生

当提供 `userId` 时，签名密钥不直接使用原始 KEY，而是基于 userId 动态派生：
1. 将 userId 各位数字进行位运算求和
2. 根据 userId 头部和尾部计算索引，从字符表中取混淆字符
3. 将混淆字符插入原始 KEY 的特定位置
4. 用派生后的密钥进行 HMAC-SHA256 签名

这使得每个用户的签名密钥不同，即使泄露单个用户的密钥也无法伪造其他用户的请求。

---

## 四、对外 API

### APISecurity 类

```java
import cn.android.security.APISecurity;

// ===================== 初始化 =====================

/**
 * 初始化安全模块（应用启动时调用一次）
 * @param context     应用上下文
 * @param production  true=使用生产环境密钥, false=使用开发环境密钥
 * @return true=验证通过, false=签名或包名不匹配（init失败后 sign() 将返回空结果）
 */
boolean success = APISecurity.init(context, isProduction);

// ===================== 请求签名 =====================

/**
 * 对 API 请求体进行签名
 * @param context  应用上下文
 * @param userId   当前用户ID（用于密钥派生，可为null）
 * @param body     请求体 JSON 字符串
 * @return JSON 字符串: {"data":"base64编码的body","sign":"hmac-sha256十六进制签名"}
 *         如果 init 未调用或失败，返回 {"data":null,"sign":null}
 */
String signedBody = APISecurity.sign(context, userId, requestBodyJson);

// ===================== 防篡改校验 =====================

/**
 * 校验 Application 是否被替换（传入反射获取的 Application 实例）
 * 如果类名不匹配，内部会将 isInit 置为 false，后续 sign() 失效
 */
APISecurity.verifyApp(application);

/**
 * 通过反射获取 ActivityThread 中的 Application 并校验类名
 * @return true=Application未被替换, false=被篡改
 */
boolean valid = APISecurity.verifyApplication();

/**
 * 获取 Native 层硬编码的 Application 类全名
 * 用于 Java 层辅助校验
 */
String expectedName = APISecurity.getRealyAppName();
```

### NetProxy 类

```java
import cn.android.security.NetProxy;

/**
 * 检测是否存在网络代理或 VPN
 * 内部会检测：
 *   - HTTP 代理（http.proxyHost / http.proxyPort）
 *   - VPN（tun0 / ppp0 网络接口）
 * @return true=检测到代理/VPN, false=网络正常
 */
boolean hooked = NetProxy.isHookNet(context);
```

### AppSigning 类

```java
import cn.android.security.AppSigning;

// 重置被代理的 PackageManager（恢复真实签名）
AppSigning.resetPackageManager(context);

// 检测 PackageManager 是否被代理
boolean proxied = AppSigning.checkPMProxy(context);

// 校验 Application 类名是否一致
boolean same = AppSigning.sameApplication();

// 计算 APK 文件的 MD5（用于完整性校验）
String md5 = AppSigning.apkMD5(context);

// 通过反射获取当前 Application 实例
Application app = AppSigning.getApplicationByReflect();
```

---

## 五、在 App 中的集成位置

模板中已集成在以下 3 处：

### 1. Application 初始化 — `App.kt`

```kotlin
// SDK初始化中调用，传入是否为生产环境
APISecurity.init(App, isProduction)
```

### 2. 网络请求签名 — `RetrofitClient.kt`

```kotlin
// 在 encryptRequestBody() 中对请求体签名
val signedBody = APISecurity.sign(App.instance, userId, dataString)
```

### 3. SSE 请求签名 — `SseCallAdapter.kt`

```kotlin
// Server-Sent Events 推送请求的签名
val signedBody = APISecurity.sign(App.instance, User.get().id, bodyString)
```

---

## 六、新项目适配（重要）

### 第 1 步：启用模块

在 `settings.gradle` 中取消注释：
```groovy
include ':apisecurity'
```

在 `app\build.gradle` 的 dependencies 中添加：
```groovy
implementation project(':apisecurity')
```

### 第 2 步：修改 C++ 原生硬编码（必须）

打开 `src\main\cpp\apisecurity-lib.cpp`，修改文件顶部的常量定义：

```cpp
// ============ 以下内容必须修改 ============

// 第30行 - 新项目的 APK 签名 SHA1（小写，无冒号分隔）
#define SHA1 "你的APK签名SHA1值"

// 第32行 - 新项目的包名
#define APP_PKG "com.你的.包名"

// 第34行 - 新项目的 Application 类全名
#define APPLICATION_NAME "com.你的.包名.App"

// 第36行 - 生产环境 API 签名密钥（Base64编码）
#define KEY "你的生产环境API密钥"

// 第37行 - 开发环境 API 签名密钥（Base64编码）
#define KEY_DEV "你的开发环境API密钥"

// 第39行 - APK 签名证书的十六进制完整内容
const char *signature = "你的APK签名证书十六进制内容";
```

> **注意：** 初始化脚本（`init_project.bat` / `init_project.sh`）会自动替换 `APP_PKG` 和 `APPLICATION_NAME` 中的包名。但 **SHA1、signature、KEY、KEY_DEV 需要手动修改**。

### 第 3 步：获取 APK 签名信息

#### 获取 SHA1

**方法一：从 keystore 获取**
```bash
keytool -list -v -keystore 你的keystore.jks -alias 你的别名
```
输出示例：
```
SHA1: EC:82:59:D5:0D:00:62:32:6C:B4:80:34:32:A3:57:94:36:DF:38:76
```
去掉冒号并转小写 → `ec8259d50d0062326cb4803432a3579436df3876`

**方法二：从已签名 APK 获取**
```bash
apksigner verify --print-certs your-app.apk
```

**方法三：从 Logcat 获取**

先临时注释掉签名校验（或使用旧签名先构建一次），安装运行后在 Logcat 搜索 `APISECURITY` 或 `非法调用`，日志中会打印实际的 SHA1 值。

#### 获取签名证书十六进制内容

```bash
keytool -printcert -rfc -keystore 你的keystore.jks -alias 你的别名
```

或查看项目根目录 `gradle.properties` 中已记录的证书信息。

### 第 4 步：配置 API 密钥

API 密钥（KEY / KEY_DEV）是服务端和客户端协商的 HMAC 签名盐值：
1. 与后端团队协商生成一对密钥（生产/开发各一个）
2. 将密钥进行 Base64 编码后填入 `#define KEY` 和 `#define KEY_DEV`
3. 确保服务端使用相同密钥验签

### 第 5 步：确认 NDK 环境

`build.gradle` 中指定了 `ndkVersion '25.1.8937393'`，请确保 Android Studio → SDK Manager → SDK Tools 中已安装该版本 NDK。

---

## 七、不启用安全库的情况

如果新项目不需要 API 签名和防篡改功能：

**1.** `settings.gradle` 中保持注释状态：
```groovy
//include ':apisecurity'
```

**2.** 删除 `app` 模块中对 `APISecurity` 的引用：
- `App.kt` 中的 `APISecurity.init()` 调用
- `RetrofitClient.kt` 中的 `APISecurity.sign()` 调用
- `SseCallAdapter.kt` 中的 `APISecurity.sign()` 调用

**3.** 移除 `app\build.gradle` 中的依赖：
```groovy
// 删除这行
implementation project(':apisecurity')
```

---

## 八、安全注意事项

| 事项 | 说明 |
|------|------|
| 密钥保密 | API 密钥虽在 Native 层，但仍可被逆向提取，建议定期轮换 |
| 签名同步 | 更换 keystore 后**必须同步更新** C++ 中的 SHA1 和 signature，否则 init 失败 |
| SO 加固 | 发布 Release 版本时建议配合 SO 加固（梆梆、爱加密等），防止 native 代码被 IDA 分析 |
| 调试日志 | 开发阶段如签名校验失败，在 Logcat 中过滤 `APISECURITY` tag 查看详细日志 |
| 360加固兼容 | 使用 360 等加固方案会替换 Application 类，导致 APPLICATION_NAME 校验失败，需注意兼容处理 |
| ProGuard | `APISecurity` 类已标注 `@Keep`，不会被混淆；JNI 方法名通过 native 关键字绑定，不可改变 |
| VPN 检测 | `NetProxy.isHookNet()` 会检测 VPN，如果产品需要支持 VPN 用户，请勿用此方法阻断请求 |

---

## 九、Logcat 日志参考

| Tag | 日志内容 | 含义 |
|-----|---------|------|
| `APISECURITY` | `初始化成功！` | init 通过，签名和包名均匹配 |
| `APISECURITY` | `非法调用1，SHA1: xxx` | 第1层 PackageManager 获取的签名不匹配 |
| `APISECURITY` | `非法调用2，Package: xxx` | 包名不匹配 |
| `APISECURITY` | `非法调用3，SHA1: xxx` | 第2层 Java 反射获取的签名不匹配 |
| `APISECURITY` | `非法调用4，SHA1: xxx` | 第3层 APK 文件签名不匹配 |
| `APISECURITY` | `非法调用5，NAME: xxx` | Application 类名被替换（verifyApp） |
| `APISECURITY` | `非法调用6，NAME: xxx` | Application 类名被替换（verifyApplication） |
| `APISECURITY` | `请先初始化！` | 调用 sign() 前未调用 init() 或 init() 失败 |
| `mhyLog_PM代理` | `truePMName==nowPMName` | PackageManager 代理检测结果 |

---

## 十、打包为 AAR 使用

apisecurity 模块以源码形式集成在模板中，方便新项目修改 C++ 硬编码并调试。**测试通过后**，建议将其打包为 AAR，后续项目直接引用 AAR 即可，无需保留源码和 NDK 编译环境。

### 10.1 构建 AAR

在项目根目录执行：

```bash
# Windows
gradlew :apisecurity:assembleRelease

# Unix / macOS
./gradlew :apisecurity:assembleRelease
```

构建完成后，AAR 文件位于：
```
apisecurity\build\outputs\aar\apisecurity-release.aar
```

### 10.2 将 AAR 集成到新项目

**第 1 步：** 将 AAR 文件复制到 `app\libs\` 目录下：
```
app\libs\apisecurity-release.aar
```

**第 2 步：** 确认 `app\build.gradle` 的 dependencies 中已有 fileTree 引用（模板默认已包含）：
```groovy
dependencies {
    implementation fileTree(dir: 'libs', include: ['*.jar', '*.aar'])
    // ...
}
```

**第 3 步：** 在 `settings.gradle` 中注释掉源码模块：
```groovy
//include ':apisecurity'
```

**第 4 步：** 删除 `app\build.gradle` 中的源码依赖（如果有）：
```groovy
// 删除这行
implementation project(':apisecurity')
```

**第 5 步：** AAR 的传递依赖不会自动引入，需在 `app\build.gradle` 中手动添加 apisecurity 的依赖：
```groovy
implementation 'androidx.annotation:annotation:1.9.1'
```

> 模板中已通过 `libs.androidx.appcompat` 间接引入了 annotation，通常无需额外添加。仅当编译报缺少 `@Keep` 等注解时才需要手动加。

### 10.3 AAR 模式 vs 源码模式对比

| 对比项 | 源码模式 (`project(':apisecurity')`) | AAR 模式 (`libs/*.aar`) |
|--------|--------------------------------------|--------------------------|
| 需要 NDK | 是（编译 C++ 代码） | 否（SO 已包含在 AAR 中） |
| 编译速度 | 慢（每次构建需编译 native） | 快（直接引用预编译产物） |
| 可修改 C++ | 是 | 否（需重新构建 AAR） |
| 适用场景 | 初始适配、调试签名校验 | 测试通过后的正式使用 |
| 团队协作 | 每人需配置 NDK 环境 | 无 NDK 要求 |

### 10.4 推荐工作流

```
1. 新建项目 → 运行 init_project 脚本
                ↓
2. 启用 apisecurity 源码模块
   修改 apisecurity-lib.cpp 中的硬编码（SHA1、包名、密钥等）
                ↓
3. 构建 Debug APK → 安装到设备
   查看 Logcat 确认 "初始化成功！"
                ↓
4. 验证 API 签名：抓包确认请求体已加密、sign 字段正确
   验证防篡改：使用 MT 管理器等工具尝试去签名，确认被拦截
                ↓
5. 测试通过 → gradlew :apisecurity:assembleRelease
   将 AAR 复制到 app\libs\
                ↓
6. 注释掉 settings.gradle 中的 include ':apisecurity'
   删除 implementation project(':apisecurity')
                ↓
7. 可选：删除 apisecurity\ 源码目录，仅保留 AAR
```

### 10.5 更新 AAR

当需要修改安全配置（如更换 keystore、轮换 API 密钥）时：

1. 重新检出 apisecurity 源码（或从模板中复制）
2. 修改 `apisecurity-lib.cpp` 中的对应常量
3. 重新执行 `gradlew :apisecurity:assembleRelease`
4. 用新的 AAR 替换 `app\libs\` 中的旧文件
5. Clean 后重新构建项目：`gradlew clean assembleDebug`
