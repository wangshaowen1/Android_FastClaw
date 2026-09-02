---
title: Feature 包结构与边界
type: architecture
status: active
last_verified: 2026-09-02
---

# Feature 包结构与边界

## 用途

`feature/` 按用户可识别的业务能力组织页面、状态和流程。

查看包名时，应该能直接判断代码属于哪项功能，而不是先判断它是 Activity、Dialog、Adapter 还是 ViewModel。

## 官方建议

Android 官方没有规定统一的包名模板，但建议按以下原则划分功能：

- 一个 feature 对应一项独立功能、一个页面或一组紧密相关的页面；
- 同一 feature 只放同一业务范围内的代码，保持高内聚，减少与其他 feature 的直接依赖；
- UI、ViewModel 和仅供该功能使用的代码留在 feature 内，多处复用的代码放入 core；
- feature 之间通过明确的入口或共享数据交互，不直接依赖对方的内部实现。

Now in Android 也以 `foryou`、`interests`、`search`、`settings`、`topic` 等功能命名 feature，而不是按 Adapter、Dialog 或 ViewModel 分类。

本项目沿用这些划分原则，但 `feature/` 只是 `app` 内的源码目录，不是独立 Gradle Module。

## 当前目录

```text
feature/
├── clawagent/
│   └── message/
├── home/
│   └── data/
├── hr/
├── interview/
├── invite/
├── login/
│   ├── data/
│   ├── flash/
│   ├── phone/
│   ├── phonebinding/
│   └── unified/
├── main/
├── message/
│   ├── chat/
│   └── data/
├── mine/
├── opportunity/
├── order/
├── payment/
├── report/
│   ├── data/
│   ├── generation/
│   ├── history/
│   └── interview/
├── resume/
│   ├── experience/
│   ├── preference/
│   ├── profile/
│   └── upload/
├── settings/
├── shrimp/
├── splash/
├── upgrade/
└── web/
    └── data/
```

各顶层包的职责如下：

| 包 | 职责 |
| --- | --- |
| `clawagent` | 小虾智能体会话、动作编排和消息卡片 |
| `home` | 既有首页内容和视频流 |
| `hr` | HR 入口 |
| `interview` | 模拟面试房间和面试交互 |
| `invite` | 邀请好友流程 |
| `login` | 登录、隐私确认和手机号相关流程 |
| `main` | 主界面、主 Tab 和应用壳 |
| `message` | 消息列表和招聘沟通 |
| `mine` | “我的”聚合页及其状态 |
| `opportunity` | 机会、职位和宝典入口 |
| `order` | 订单及订单记录 |
| `payment` | 支付方式选择和支付状态处理 |
| `report` | 报告首页、生成、历史和面试报告 |
| `resume` | 在线简历、求职偏好和附件简历 |
| `settings` | 设置、账号和备案信息 |
| `shrimp` | 虾分余额、明细和充值 |
| `splash` | 启动和隐私同意流程 |
| `upgrade` | 版本升级交互 |
| `web` | 带应用业务桥接的 Web 容器 |

`main` 负责页面组装，不作为普通业务包参考。`hr` 虽然文件较少，但具有独立用户入口，因此不并回 `mine`。

## 新代码放在哪里

| 代码职责 | 建议位置 |
| --- | --- |
| 一个明确的用户功能或页面流程 | `feature/<business>/` |
| 某项功能内部的稳定子流程 | `feature/<business>/<subfeature>/` |
| 仅供某项功能使用的数据访问代码 | `feature/<business>/data/` |
| 多个功能共用的 UI 能力 | `core/ui/`、`common/` 或已有 `widget/` |
| 网络、数据库、用户和全局状态 | `core/` |
| 厂商 SDK 的 Android 入口与回调 | `platform/<integration>/` |
| 公共链接和跨功能跳转 | `router/` |

先按业务归类。不要因为某个类是 Adapter、Dialog 或 ViewModel，就为它单独创建目录。

## Feature 内部的划分

顶层包应对应明确的用户目的、导航目的地或连续业务流程。文件少不是合并业务包的理由，只要业务边界清楚，Activity 和 ViewModel 两个文件也可以构成独立 feature。

只有下列情况适合继续增加子包：

- 已经形成可单独描述的子流程；
- 存在功能专用的数据访问层；
- 子功能会持续增长，继续放在父目录会影响定位。

feature 下不要单独创建以下目录：

```text
adapter/
dialog/
viewholder/
viewModel/
vm/
bind/
```

这些文件直接放在对应页面或子流程的包中。

## 共享能力和平台入口

与具体业务无关、会被多个功能复用的代码不放在 `feature/`。

当前约定：

- 图片预览位于 `core/ui/imagepreview/`；
- 厂商推送入口位于 `platform/push/`；
- 首页视频流的数据访问代码位于 `feature/home/data/`；
- 手机号绑定位于 `feature/login/phonebinding/`。

名称通用不代表职责通用。例如 `web/` 仍包含报告等业务动作，因此继续保留在 `feature/`。如果以后要下沉为公共 Web 容器，需要先拆开容器能力和业务处理。

## 跨 Feature 调用

跨功能跳转优先通过 `router/`、事件或共享数据能力完成。

避免：

- 一个页面直接使用另一个页面的 ViewModel；
- 公共层依赖具体页面；
- 两个 feature 互相持有实现类；
- 仅靠移动目录掩盖原有依赖。

当前双向引用集中在以下业务关系：

- 应用壳和聚合页面：`login ↔ settings`、`main ↔ mine`、`main ↔ opportunity`；
- 消息与机会：`message ↔ opportunity`；
- 个人中心、订单和虾分：`mine ↔ order`、`mine ↔ shrimp`、`order ↔ shrimp`；
- 报告相关流程：`report ↔ resume`、`report ↔ shrimp`、`report ↔ web`。

这些引用不影响当前包结构，但在拆分 Gradle Module 前需要先处理。`mine`、`order` 和 `shrimp` 之间共用页面 ViewModel 的做法也不作为新代码参考。

`core` 和 `kit` 目前还保留少量对登录页、升级弹窗、Web 容器及 feature 数据模型的直接引用。这些都是现有依赖；后续修改对应能力时，再结合路由契约和数据所有权逐步收敛。

## 修改目录时检查什么

移动或新增 feature 后，至少检查：

- 目录与 `package` 声明一致；
- Manifest 中的 Activity、Service 路径有效；
- DataBinding XML 中的完整类型名有效；
- 不再保留旧包引用和空目录；
- 没有新增 `adapter`、`dialog`、`viewModel` 等按代码名称划分的目录；
- 共享层没有新增对具体页面的依赖。

是否拆分 Gradle Module，需要结合复用范围、依赖隔离和构建收益单独判断，不能只根据包数量决定。

## 参考资料

- [Android Developers：Guide to Android app modularization](https://developer.android.com/topic/modularization)
- [Android Developers：Common modularization patterns](https://developer.android.com/topic/modularization/patterns)
- [Now in Android：Modularization learning journey](https://github.com/android/nowinandroid/blob/main/docs/ModularizationLearningJourney.md)
- [Android Architecture Samples](https://github.com/android/architecture-samples)
