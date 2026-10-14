# appsettings-kmp 设计

App 里与业务无关的那层「设置」：外观、关于、更新说明、设置页 UI 基元，以及 v1.x 的强更门与界面语言。
Compose Multiplatform（Android / iOS）库，坐标 `wang.harlon:appsettings-kmp`，包根 `wang.harlon.appsettings`。

首批宿主：TrendingAI、Tono。第三方宿主：chatbase-kmp（只用 `ui` 包）。

2026-10-14 成文。源自 TrendingAI 的两次竖切讨论（关于 SDK、外观 SDK），本文把它们收成一个整体。

## 1. 决策记录

| 日期 | 决定 | 理由 |
|---|---|---|
| 2026-10-14 | 一个仓、一个 artifact，按能力分包 | 卡片组件是所有能力的底座，分仓会造成版本矩阵；双轨构建下每多一个仓就多一份「改了必须发版并 bump」的税 |
| 2026-10-14 | 仓名 `appsettings-kmp`，不用 `*base` | 没有服务端半边，`*base` 系列的命名含义是「有服务端配套」 |
| 2026-10-14 | Tono 接受卡片组风格 | 接 `ui` 包即视觉向 TrendingAI 对齐，产品侧已同意 |
| 2026-10-14 | 强更门、界面语言放 v1.x | Tono 现在都不需要，不为第一个用户提前设计 |
| 2026-10-14 | chatbase-kmp 反向依赖本库的 `ui` 包 | 删掉 `ModelPicker` 里镜像的卡片规格，三份变一份 |
| 之前 | 外观不拆 core / ui 两个 artifact | 同上一条「一个 artifact」 |
| 之前 | 壁纸取色（`SystemDynamic`）进 v1 | 通用外观库最常被问的能力 |
| 之前 | TrendingAI 的外观旧存储不做兼容 | 库不该为第一个用户背历史包袱；接受老用户升级后主题重置一次 |
| 2026-10-14 | 更新说明同样不做兼容，库里不留任何迁移入口 | 代价只是老用户升级那一次少弹一回 |
| 之前 | 动态图标进库，作为 appearance 下的独立能力 | 因 App 而异的只有 alias 声明与图标资源；切换机制、真实状态读取、iOS 备用图标、选择 UI 都通用，且坑多值得封装 |
| 之前 | 图标与主题色不联动 | Android 切图标有副作用（快捷方式失效、少数机型杀进程），跟着主题色自动切会让用户频繁遇到 |

## 2. 仓与包结构

```
appsettings-kmp/
├── library/                      唯一发布模块
│   └── src/commonMain/kotlin/wang/harlon/appsettings/
│       ├── ui/                   SettingsGroup、InfoDialog、SettingsBottomSheet、SettingsDropdownMenu、SettingsDefaults
│       ├── whatsnew/             解析、弹出条件、lastSeen、弹窗
│       ├── about/                AboutContent、AboutLink、UpdateStatus 行
│       ├── appearance/           模型、store、AppearanceTheme、Picker、ColorLab、动态图标
│       ├── update/               v1.x：版本比较、ForceUpdateGate
│       └── language/             v1.x：AppLanguage、per-app locale
├── sample/                       不接任何后端的 demo 宿主，五个包全接一遍
├── gradle/composite-substitutions  `wang.harlon:appsettings-kmp = :library`
└── docs/                         本文、api.md
```

**包间依赖只允许向下**：`appearance / about / whatsnew / update / language → ui`，彼此之间不依赖。
这是以后万一要按依赖重量拆 artifact 的保险，拆的时候是机械搬运。

**外部依赖**（全部已在两个宿主里）：

| 依赖 | 用途 |
|---|---|
| compose material3、foundation、runtime | 全部 |
| compose components-resources | 库内中英文兜底文案 |
| material-icons-core | 返回键一类的基础图标；**不依赖 extended**，用到的其余图标以 `ImageVector` 内联 |
| materialkolor | appearance 的 seed 取色、SPEC_2025 |
| multiplatform-settings | 持久化 |
| kotlinx-serialization-json | appearance 的 JSON 存储、whatsnew 解析 |

不依赖 eventbase / loginbase / chatbase / ktor。

## 3. 能力清单与归属

| 能力 | 进库 | 不进库的部分 |
|---|---|---|
| 卡片组、说明弹窗、底部浮层、下拉菜单 | v1 | 页面骨架（Scaffold / 顶栏 / 返回键） |
| 更新说明 | v1 | whatsnew.json 怎么生成（CI 的事）、弹出时机的编排（宿主决定与强更门、登录浮层的先后） |
| 关于 | v1 | 更新检查的实现（渠道相关）、赞助弹窗、调试开关之类的 App 私有行 |
| 外观（模式、配色、调色台、壁纸取色、动态图标） | v1 | alias 声明与图标资源、图标预览怎么画 |
| 强更门 | v1.x | `minVersion` 从哪来、怎么拉 |
| 界面语言 | v1.x | 摘要语言这类业务语言 |
| 账户区、反馈、通知、数据来源、订阅 | 不进 | 判断标准：需要知道「这个 App 卖什么、登什么、连哪个服务端」的都不进 |

**设置页本身不进库。** 哪些组、什么顺序、放哪些业务行是产品决策。库给的是件，页在 App。

## 4. 宿主契约（六条，对每个包都成立）

1. **三件套：`Options` 进、`State` 出、`Event` 回。** `Options` 收所有因 App 而异的静态差异；`State` 由 `rememberXxxState(settings, options, onEvent)` 创建、挂事件出口；composable 只吃 `State`。
   唯一例外是 about：没有可变状态，只有 `onLinkClick` 这一个出口。
2. **存储：宿主传 `Settings` 实例，库只认命名空间。** 库不自己建实例。每个包的 key 都在 `appsettings.<包名>.` 下；多字段状态存一个带 `schemaVersion` 的 JSON key，单值直接存。
   **不提供任何旧数据兼容或迁移入口**：从旧实现迁过来的 App 接受一次性退化（外观重置一次、更新说明少弹一回）。
3. **事件：一个事件根、每包一棵 sealed 子树，记落定后的结果。** 根 `AppSettingsEvent` 是普通接口（Kotlin 的 sealed 子类必须同包），`WhatsNewEvent`、`AppearanceEvent` 各自 sealed。宿主用一个函数映射到自己的埋点词汇表，库零埋点依赖。
   事件记的是落定状态（例如权限被拒后的 false），不是用户意图。
4. **只给内容，不给页面。** 每个包导出 `XxxContent(state, modifier)` 与更细的件；不带 Scaffold、顶栏、返回键、导航。
   `*Content` 是整页内容，自带滚动；更细的件不滚动。页面间跳转是回调。
5. **文案与图标：库内中英文只是兜底，图标全部内联。** 所有可见文字都可覆盖，标签类型是 `@Composable () -> String`，不是 `StringResource`。
6. **平台差异收在库内 expect/actual。** 系统深色、壁纸取色是否可用、activity-alias 切换与真实状态、iOS 备用图标、系统栏样式、打开系统应用设置。宿主 commonMain 不写平台分支。

另外两条实现约束，来自 TrendingAI 的教训：

- **初值必须同步可读。** 全部 `State` 基于 `StateFlow` / Compose `State`，宿主不需要再写 `collectAsState(remember { current() })` 防首帧闪烁。
- **加载指示一律 M3 Expressive `LoadingIndicator`。** 两个宿主的 CLAUDE.md 都有这条，库内同样遵守。

## 5. 稳定契约

改动即破坏性变更，升主版本并在 CHANGELOG 写迁移：

- 各包 JSON 的 `schemaVersion` 与字段名
- `ThemeMode` 的名称（按名存，不按 ordinal）
- 内置预设配色的 `id`（色值可以随时调，`id` 不能动）
- `AboutLink`、`AppSettingsEvent` 的子类集合（新增子类不算破坏，删除或改名算）
- 存储命名空间 `appsettings.<包名>.`

## 6. 测试归属

库自带：版本号比较、更新说明弹出条件、外观 JSON 读写与快照撤销、预设 `id` 唯一、`ThemeStyle` 覆盖全部预设用到的 `PaletteStyle`、动态图标「有且只有一个 alias 启用」。
两个宿主删掉各自那份同类测试。

## 7. 与其他基建的关系

- **chatbase-kmp → appsettings-kmp（`ui` 包）**：`ModelPicker` 改用 `SettingsGroup`，删镜像。chatbase-kmp 的 bump 跟着本库发版走。
- **eventbase-kmp**：无代码依赖。宿主把 `AppSettingsEvent` 映射成自己的 `AppEvent`。
- **双轨构建**：与 loginbase-kmp / chatbase-kmp 同一套。本机 `local.properties` 加 `appsettings-kmp.dir=<路径>`，CI 与 F-Droid 走 `libs.versions.toml` 坐标。改了库必须发版并 bump。

## 8. 落地顺序

每步独立可发版、独立可回退：

1. 建 Gradle 骨架（参照 chatbase-kmp），先搬 `ui` 包。TrendingAI 的 `ui/common` 四个文件换成 import，跑 release 冒烟，截图对比。验证双轨构建与发版流程。
2. 搬 `whatsnew`。两个宿主各删一份重复代码（两边 `WhatsNew.kt` 函数签名逐字相同）。
3. 搬 `about`。TrendingAI 的赞助、TinyUI 通道改成自定义行；Tono 新增关于页，设置页只留入口。
4. 搬 `appearance`，含动态图标。唯一有存储破坏性变更的一步，单独发版。
5. Tono 接外观：`ColorSource.Fixed` + 深浅模式，设置页换卡片组。
6. chatbase-kmp 的 `ModelPicker` 改依赖 `ui` 包。
7. 更新 brain 的 atlas 共享基建表。
8. v1.x：`update`、`language`。

## 9. 已知的坑（搬运时带进 KDoc / README）

- **动态图标与 Android Studio 的 Run**：AS 的 Default Activity 策略固定挑 merged manifest 里的 `MainActivityDefault`，撞上被禁用的 alias 就报「Activity 不存在」。库解决不了，README 写明：Launch Options 改 Specified Activity，用本体 `MainActivity`。不要用 adb 强行 enable alias。
- **alias 声明错误要尽早暴露**：debug 包首次使用时校验每个 `androidAlias` 存在、且有且只有一个默认启用，不满足直接抛异常并在信息里写 manifest 该怎么写；release 只打日志。
- **图标真实状态只有一份**：Android 读 `getComponentEnabledSetting`，iOS 读 `alternateIconName`，库不另存回显值。
- **切换图标的平台副作用**：固定的快捷方式失效、部分桌面延迟刷新、少数机型即使 `DONT_KILL_APP` 也杀进程。库发 `AppIconChanged` 事件，由宿主决定是否提示。
- **色规**：materialkolor 显式用 `SPEC_2025`，不作为可调项暴露。
- **纯黑档**：`Seed` 源走 materialkolor 的 `isAmoled`；`Fixed` 源由库把 `background / surface*` 压成黑，变换是通用的。
- **`isDark` 只算一次**：TrendingAI 曾在 `MainActivity` 为系统栏样式重抄一遍 `when`，且初值不一致导致首帧可能闪。库提供 `state.isDark()` 与 Android 的 `SystemBarsEffect(state)`。
