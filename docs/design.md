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
| 2026-10-15 | **接入面 v2**：对外一个配置对象 `rememberAppSettings { }` + 一个 `AppSettingsRoot` + 整页屏；状态对象、`*Content`、Options、Session 全部 internal | 第一版要宿主认识十来个概念、Tono 接三项能力写了 150 行；新接入方照 README 不知道从哪进 |
| 2026-10-15 | 设置页框架进库（壳、滚动、通用行），编排仍在宿主；二级页导航归宿主，库只发 `onOpen(page)` | 两个 App 的业务行都多，但骨架与通用行一样；库内导航会和宿主的返回、`screen_viewed`、状态恢复打架 |
| 2026-10-15 | 不做 `*Content` 孪生体，也不做壳槽位；顶栏颜色一个配置项，默认 M3 | TrendingAI 的「换壳」拆开看只是顶栏颜色恒定 `surfaceContainer`，`TrendingScaffold` 其余接线在 pinned + 恒定色下无可见效果；库默认跟平台，house style 放配置 |
| 2026-10-15 | v1.x 先做 `update` 再做 `language`，各发一个 minor；强更门与更新说明的让路、界面语言与更新说明语言的联动都收进库 | Tono 的 app-config 已带 `min_version` 只是没消费，强更门有两个用户；两处联动是库内两个包的关系，v1 让 TrendingAI 在根上写了 8 行只为接这两条线。详见第 11 节 |
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
| 卡片组、说明弹窗、底部浮层、下拉菜单、设置页框架与整页屏 | v1 | 导航、业务行 |
| 更新说明 | v1 | whatsnew.json 怎么生成（CI 的事）、弹出时机的编排（宿主决定与强更门、登录浮层的先后） |
| 关于 | v1 | 更新检查的实现（渠道相关）、赞助弹窗、调试开关之类的 App 私有行 |
| 外观（模式、配色、调色台、壁纸取色、动态图标） | v1 | alias 声明与图标资源、图标预览怎么画 |
| 强更门 | v1.x | `minVersion` 从哪来、怎么拉 |
| 界面语言 | v1.x | 摘要语言这类业务语言 |
| 账户区、反馈、通知、数据来源、订阅 | 不进 | 判断标准：需要知道「这个 App 卖什么、登什么、连哪个服务端」的都不进 |

**设置页的框架进库，编排在 App。** 壳、滚动容器、页内间距、通用行由库给；哪些组、什么顺序、放哪些业务行仍是产品决策，宿主在 `SettingsScreen { }` 里摆。

## 4. 宿主契约（六条，对每个包都成立）

1. **一个配置对象。** `rememberAppSettings { appearance { } ; about { } ; whatsNew { } ; topBar { } }` 是宿主唯一要写的配置，每块有默认值，不写即默认或不启用；
   `Options` / `State` / `Event` 三件套是库内部的组织方式，宿主只看到配置与事件。
2. **存储：宿主传 `Settings` 实例，库只认命名空间。** 库不自己建实例。每个包的 key 都在 `appsettings.<包名>.` 下；多字段状态存一个带 `schemaVersion` 的 JSON key，单值直接存。
   **不提供任何旧数据兼容或迁移入口**：从旧实现迁过来的 App 接受一次性退化（外观重置一次、更新说明少弹一回）。
3. **事件：一个事件根、每包一棵 sealed 子树，记落定后的结果。** 根 `AppSettingsEvent` 是普通接口（Kotlin 的 sealed 子类必须同包），`WhatsNewEvent`、`AppearanceEvent` 各自 sealed。宿主用一个函数映射到自己的埋点词汇表，库零埋点依赖。
   事件记的是落定状态（例如权限被拒后的 false），不是用户意图。
4. **页面由库给，导航归宿主。** 四个整页屏（设置页框架、外观、调色台、关于）自带 M3 壳；通用行 `appearanceItem()` / `aboutItem()` 可插进宿主任意组；
   二级页之间的跳转全部经回调（`onOpen` / `onOpenColorLab`）交给宿主路由。`*Content` 只是实现细节，不公开。
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

## 10. 接入面 v2 的形状（2026-10-15）

公开面：`rememberAppSettings { }`、`AppSettingsRoot`、`SettingsScreen` / `AppearanceScreen` / `ColorLabScreen` / `AboutScreen`、`appearanceItem()` / `aboutItem()` / `GeneralSettingsGroup`、`ui` 包的基元、模型与事件类型。
其余（`AppearanceState`、`AppearanceStore`、`*Content`、`ColorLabSession`、`AppIconState`、`WhatsNewState`、CompositionLocal）全部 internal。

成功标准：Tono 接三项能力 1 个文件、40 行以内；没看过本仓的人照 README 10 分钟接上外观与关于。

顺带的结论（不阻塞本库）：TrendingAI 的 `TrendingScaffold` 规则把「顶栏颜色恒定 `surfaceContainer`」编码成了页面骨架，
其 nestedScroll / CompositionLocal 接线在 pinned + 恒定色下无可见效果；更合适的形态是只保留 `TrendingTopAppBar` 的颜色默认值、删掉 scaffold 包装。

## 11. v1.x 规划：`update` 与 `language`（2026-10-15）

两包都是增量，不动已有稳定契约，各发一个 minor：`0.2.0` 出 `update`，`0.3.0` 出 `language`。先做 `update`：Tono 的 `AppConfigResponse` 已经带 `min_version` 但客户端没消费，是现成的第二个用户；`language` 只有 TrendingAI 用，且 Android 侧要多一个 appcompat 依赖。

源实现都在 TrendingAI：`update/MinVersion.kt`（含测试）、`ui/common/ForceUpdateGate.kt`、`data/local/SettingsManager.kt` 的 `AppLanguage`、`MainActivity` 的 `setApplicationLocales`、`SettingsScreen` 的语言行。搬完两边各删一份。

### 11.1 `update`：强更门

**进库**：版本比较、整屏强更门、`minVersion` 缓存与首帧同步判定、与更新说明的让路联动、事件。
**不进库**：`minVersion` 从哪来、怎么拉、其他 app-config 字段的落地。

| 决定 | 理由 |
|---|---|
| 宿主只给一个 `suspend () -> String?`，库冷启动调一次 | TrendingAI 的 `refreshAppConfig` 顺带落地五六个字段，这些仍在宿主；lambda 里宿主做完自己的事把 `minVersion` 返回即可。返回 null 或抛异常都不改缓存（fail-open） |
| 缓存写 `appsettings.update.minVersion`，初值同步读缓存判定 | 强更一旦下发，离线重启也拦得住；首帧不闪。沿用 v1 的「初值必须同步可读」约束 |
| 门在 `AppSettingsRoot` 里、`content()` 外层 | 宿主不再自己包 `ForceUpdateGate`；整屏替换无 dismiss 语义，不能绕过 |
| 门亮着时库内自动压住更新说明 | v1 让 TrendingAI 写 `enabled = { !forceUpdateBlocked }` 外加一个同步初值的 `remember`，共 6 行只为避免双弹窗；这是库里两个包的关系，库自己管。`whatsNew.enabled` 保留给别的全屏（登录浮层等） |
| 版本比较只比 `-` / `+` 前的数值段，任一侧解析失败不拦 | 原样搬 TrendingAI 的规则：git describe 构建与 prerelease 都按数值段比；兜底版本号或服务端误配不能把用户锁死 |
| 按钮默认 `openUrl(storeUrl)`，可换 `onUpdateClick` | iOS 跳 App Store 页、Android apk 渠道跳官网、play 渠道跳商店，都是一个 url 的事；要自建下载器的给回调 |
| 文案三段可覆盖，库内中英兜底 | 同 v1 契约 5 |

配置形状：

```kotlin
update {
    minVersion = { api.fetchAppConfig()?.also { cacheMyOtherFields(it) }?.minVersion }
    storeUrl = OFFICIAL_WEBSITE_URL        // 或 onUpdateClick = { … }
    // title / message / button: (@Composable () -> String)? 可覆盖
}
```

事件 `UpdateEvent.ForceUpdateShown(current, min)`、`ForceUpdateClicked`，记落定结果。存储 key `appsettings.update.minVersion`，单值 string。

测试归库：版本比较全部用例（从 `MinVersionTest` 搬）、「缓存有值时首帧即阻断」、「拉取失败不改缓存」、「门亮着时 `isWhatsNewPending` 为 true 但不弹、门灭后补弹」。

### 11.2 `language`：界面语言

**进库**：语言模型、存储、Android 的 per-app locale 应用、设置页一行、「跟随系统」解析、事件。
**不进库**：可选语言清单（宿主的 compose resources 支持哪些语言只有宿主知道）、摘要语言这类业务语言。

| 决定 | 理由 |
|---|---|
| 存 BCP 47 tag 字符串，不存 ordinal | TrendingAI 存 `prefs_language` 的 Int ordinal，清单一变就错位；契约 5 要求按名存 |
| 可选清单由宿主传 `languages = listOf("zh", "en")`，库不枚举 | 库不知道宿主翻译了哪些语言；清单外的旧值当作跟随系统 |
| Android 用 `AppCompatDelegate.setApplicationLocales`，库在 Root 里起 effect 应用 | 宿主 Activity 不再写 collect；API 33+ 走系统 per-app language，以下走 appcompat。要求宿主 Activity 是 `AppCompatActivity`——debug 包首次使用校验，错了抛异常写明怎么改（同动态图标的做法） |
| iOS 不做应用内切换，行点按跳系统设置 | iOS 没有不重启的 per-app locale 切换；TrendingAI 现状就是跳系统设置，用户在系统「App 语言」里改。宿主 `Info.plist` 要有 `CFBundleLocalizations`，否则系统设置没这一项——README 写明 |
| 只给 `languageItem()` 一行（Android 下拉、iOS 跳转），不做整页屏 | 清单一般不超过五项，下拉够用；要整页的时候再加 |
| 选项标签默认用该语言自己的显示名 | `Locale("zh").getDisplayName(Locale("zh"))` → 「中文」，`en` → 「English」；跟随系统显示库内兜底文案。可用 `label = { tag -> … }` 覆盖 |
| 启用后更新说明的展示语言自动跟界面语言 | TrendingAI 现在要写 `whatsNew { language = { appLanguage.isoCode ?: getSystemLanguage() } }`，这是库里两个包的关系。显式 `whatsNew.language` 仍优先 |
| TrendingAI 的旧 key 不迁移 | 同 v1：老用户升级后语言回到跟随系统一次 |

配置形状：

```kotlin
language {
    languages = listOf("zh", "en")       // 给了才启用；顺序即菜单顺序，「跟随系统」固定第一
    // label: (@Composable (tag: String) -> String)? 可覆盖
}
```

对外读：`appSettings.language.current: AppLanguage`（`tag: String?`，null 为跟随系统）与 `resolved(): String`（跟随系统时取系统语言，钳到清单，清单外回落清单第一项）。TrendingAI 拼外链 `ui_locales` 参数、摘要语言首次初始化都读这个。
事件 `LanguageEvent.Changed(tag)`。存储 key `appsettings.language.tag`，单值 string，`"system"` 表示跟随系统。

测试归库：`resolved()` 的钳制与回落、清单外旧值视为跟随系统、`languages` 为空时行不显示。

### 11.3 落地顺序

1. `update` 包 + 测试，`AppSettingsRoot` 接门与让路联动，README 加「强更门」一节；发 `0.2.0`。
2. TrendingAI 接 `update { }`，删 `ForceUpdateGate.kt`、`MinVersion.kt` 与两处 `forceUpdateBlocked`；Tono 接 `update { }`，消费已有的 `min_version`。
3. `language` 包 + 测试，`languageItem()`，Android appcompat 依赖与 Activity 校验，README 加「界面语言」一节（含 iOS 的 `CFBundleLocalizations`）；发 `0.3.0`。
4. TrendingAI 接 `language { }`，删 `AppLanguage` 枚举、`MainActivity` 的 locale collect、设置页语言行；`whatsNew.language` 一行去掉。
5. 更新 `docs/api.md` 的稳定契约（`UpdateEvent` / `LanguageEvent` 子类集合、两个新 key）与 brain atlas 的能力描述。
