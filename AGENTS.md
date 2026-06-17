# FastClaw Agent 工作流入口

本文件是跨工具薄入口，只承载红线、档位和路由。细则、skill、memory 按需读取 `.agents/`。

## 项目识别

- 项目名按 git remote 最后一级解析：当前仓库为 `FastClaw`。
- 版本号每次从 `config.gradle` 的 `versionName` 读取：当前为 `1.0.00`。
- applicationId / namespace / Manifest package 以当前分支为准：`com.shiyuan.fastclaw`。
- 真实源码、资源、依赖和 flavor 以当前分支文件为准，不照搬外部项目或旧包名。

## 关键禁令

- 只改本次任务要求的范围；不要顺手重构、格式化或清理历史代码。
- 未经用户要求，不跑 Gradle；用户说“不走 Gradle”时只做静态检查。
- 不新增 Hilt / di / UseCase / MVI / 多 Module，不生成 `@Inject`、`@HiltViewModel`、`@AndroidEntryPoint`、`@Module`、`@InstallIn`、`@Provides`、`@Binds`。
- 不改 `ApiResponse.java`，不把接口实体随意塞 UI 字段；确需 UI 派生状态时优先放 ViewModel / ItemVM / adapter 层。
- 不直接在 Activity / Fragment / Adapter 发网络请求；接口链路走 ViewModel + Repository。
- 不硬编码包名、scheme、SDK key、旧项目名或复制来源路径；看到旧包名先用 `rg` 回查当前分支是否真实存在。
- 不硬编码 hex 色值、裸 dp、字体兜底；新增 XML 优先复用当前项目已有 `?attr`、`@color`、`@dimen`、`@style`、`@drawable`。
- 不碰 `OssManager` 之类已有平台能力封装，除非用户明确要求且已读同类实现。
- 不做破坏性数据库迁移；Room 相关改动必须按当前项目已有迁移方式处理。
- 不把通用模板硬套到本项目；文档和代码冲突时，以当前分支同类代码为准。

## 执行档位

- 快速局部修改：读目标文件、直接相关代码和必要资源；不读完整规范，不读无关 skill。
- 标准代码生成：先读 `.agents/android-codegen-quick-rules.md`，再按实际场景读最小必要 skill。
- Figma / UI 还原：先读目标 Figma 节点或截图，再读 `.agents/android-figma-xml-quick.md` 和 XML 资源规则。
- 检查 / 不走 Gradle：读 `.agents/android-check-quick.md`，只做静态验证。
- 文档维护：读 `.agents/README.md`、`.agents/ARCHITECTURE.md` 和相关 memory，保持单一来源。

## 路由

- 项目基线、目录、Base 类：`.agents/android-codegen-project-rules.md`
- ViewModel / Repository / API / 异步错误处理：`.agents/android-codegen-architecture-rules.md`
- XML / token / drawable / 命名摘要：`.agents/android-codegen-xml-resource-rules.md`
- 场景到 skill 的完整路由：`.agents/android-codegen-scenarios.md`
- 完整审查清单：`.agents/android-codegen-checklist.md`
- 记忆索引：`.agents/memory/MEMORY.md`

## 输出

- 简洁说明改了什么、检查了什么、剩余风险。
- 未跑 Gradle 时明确说明。
- 代码审查先列问题，按严重程度排序；无问题也说明检查范围和剩余风险。
