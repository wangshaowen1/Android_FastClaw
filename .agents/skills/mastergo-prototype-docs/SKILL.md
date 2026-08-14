---
name: mastergo-prototype-docs
description: 读取和解释 MasterGo 原型事实。仅在用户提供 MasterGo 原型链接，要求提取页面、文案、状态、交互、跳转、资源或把真实原型资料合并到项目文档时使用；提供本地 Netscape Cookie Jar 时走只读快速链路。只按截图实现 UI、操作 MasterGo 画布、普通 Figma 还原、没有 MasterGo 链接的文档维护或仅修改源码时不要使用。
---

# MasterGo 原型读取

从真实 MasterGo 原型提取事实，不凭经验补页面、流程、文案或状态。本 Skill 负责原型读取和解释；项目文档仍由 `$write-project-docs` 治理，源码实现仍由当前项目规则和命中的实现 Skill 负责。

## 工作流程

1. 确认用户提供了 MasterGo 链接，并区分只读分析、写项目文档和源码实现。
2. 读取 [MasterGo 读取与分析](references/retrieval-and-analysis.md)，按其中的认证选择和停止条件执行。
3. 优先运行 `scripts/read_mastergo.py`，只把有限、结构化结果带入上下文，不直接输出整份二进制数据。
4. 根据 metadata、data、文本和图片的实际读取结果标记“完整可读”或“部分可读”；关键事实缺失时停止，不猜测补齐。
5. 需要写入项目文档时，再读取 [项目文档衔接](references/project-doc-integration.md)，并完整使用 `$write-project-docs`。
6. 用户同时要求实现时，先固定原型事实，再按仓库 `AGENTS.md` 重新路由；不要让本 Skill 直接决定平台实现方式。

## 安全边界

- Cookie Jar 只接受用户提供的本地 Netscape 文件路径，不要求用户粘贴内容。
- 不复制、改写、提交或展示 Cookie 文件；不在日志、文档和最终答复中记录 Cookie 值或本地路径。
- 只向 MasterGo 域名发送筛选后的 MasterGo Cookie。认证方式失败时停止，不自动换用其他凭据。
- 下载内容只放系统临时目录。完成分析后清理临时文件，不把原型数据包或批量图片带入仓库。
- 只读链接和整理需求时不调用画布写入能力。需要选中图层、组件库、变量或修改画布时，改用用户明确授权的官方 MasterGo MCP 流程。

## 输出

说明已读取范围、确认的原型事实、当前代码差异、仍需确认的内容和读取失败边界。不得把“metadata 可读”表述为“原型完整可读”。
