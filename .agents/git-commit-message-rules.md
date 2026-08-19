# Git Commit 提交信息规范

## 1. 目的与适用范围

统一本项目 Git 提交信息格式，使提交记录清晰、易查，便于代码评审、问题定位和版本回溯。

本规范适用于本项目所有 Git 提交，包括开发者和代码智能体创建的提交。执行 `git commit` 前必须按本规范检查提交信息。

## 2. 提交格式

```text
<type>(<scope>): <description>
```

示例：

```text
feat: 新增用户登录功能
fix(order): 修复订单金额计算错误
```

- `type`：必填，表示本次提交的类型。
- `scope`：选填，表示本次修改涉及的模块。
- `description`：必填，简要说明做了什么。
- 冒号必须使用英文冒号 `:`，冒号后必须有一个空格。

## 3. 提交类型

| 类型 | 含义 | 使用场景 |
| --- | --- | --- |
| `feat` | 新功能 | 新增功能、接口、页面或业务能力 |
| `fix` | 修复 Bug | 修复功能异常、逻辑错误或兼容性问题 |
| `docs` | 文档变更 | 修改 README、接口文档或开发说明 |
| `style` | 代码格式调整 | 修改空格、分号、缩进等，不改变代码逻辑 |
| `refactor` | 代码重构 | 既不是新增功能，也不是修复 Bug，例如重命名、抽取方法 |
| `perf` | 性能优化 | 优化响应速度、内存占用、网络请求或数据库查询 |
| `chore` | 工程及配置变更 | 修改依赖、构建工具、流水线、辅助脚本、配置文件或测试 |
| `revert` | 回滚提交 | 回滚之前已经提交的代码 |

本规范不单独设置 `test`、`build` 和 `ci` 类型，相关修改统一归类为 `chore`：

```text
chore(test): 补充用户服务单元测试
chore(build): 更新项目构建脚本
chore(ci): 调整自动发布流水线
```

## 4. Scope 使用规范

`scope` 表示本次修改影响的模块，可以省略，建议使用小写英文。常见示例：

```text
user
login
order
payment
api
database
config
ui
```

同一个模块应保持统一命名，不要交替使用 `user`、`users`、`member` 等不同名称。提交前应优先参考本仓库已有提交中的稳定 scope。

## 5. Description 编写规范

- 简短、明确地说明本次提交做了什么，可以使用中文。
- 一次提交尽量只完成一件事。
- 不使用“修改一下”“调整代码”“修复 bug”等含义模糊的描述。
- 结尾不加句号。

推荐：

```text
feat(user): 新增用户注销功能
fix(payment): 修复订单重复扣款问题
refactor(api): 统一接口异常处理逻辑
```

不推荐：

```text
fix: 修改问题
fix: bug
chore: 更新代码
feat: 调整一下
```

## 6. Codeup 推送规则

Codeup 仓库可在“设置 → 推送规则 → 提交说明”中使用以下正则：

```text
^(feat|fix|docs|style|refactor|perf|chore|revert)(\([^)]+\))?: [^\r\n]+
```

正则末尾不添加 `$`，以允许首行标题后附带详细说明。服务端规则针对整个仓库生效，即使使用 `git commit --no-verify` 绕过本地 Hook，推送时仍会检查。

以下格式会被拒绝：

```text
新增登录功能
feat:新增登录功能
feature: 新增登录功能
fix(): 修复查询异常
fix(user) 修复查询异常
fix:
```

## 7. Merge Commit

Git 自动生成的 `Merge branch ...` 不符合严格规则，可能被 Codeup 拦截。默认优先使用 Squash 合并，并按本规范填写最终提交信息。

只有项目明确要求保留 Merge Commit 时，才将服务端规则调整为：

```text
^((feat|fix|docs|style|refactor|perf|chore|revert)(\([^)]+\))?: .+|Merge .+|Revert ".+")
```

## 8. 推荐实施方式

1. 配置本地 `commit-msg` Hook，提交时立即检查格式。
2. Codeup 配置服务端推送规则，防止绕过本地检查。
3. 将主分支设为保护分支，禁止直接推送。
4. 所有代码通过合并请求进入主分支。
5. 优先使用 Squash 合并，保持主分支提交历史简洁。
