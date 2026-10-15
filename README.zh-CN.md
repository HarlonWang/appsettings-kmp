# appsettings-kmp

> App 里与业务无关的那层设置——外观、关于、更新说明、设置页——一个配置对象接完。Compose Multiplatform，Android / iOS。

**简体中文** | English（待写）

[![license](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

> 状态：0.1.0 已发 Maven Central（ui / whatsnew / about / appearance 四包，接入面 v2）。TrendingAI、Tono、chatbase-kmp 已接入。v1.x 规划：强更门、界面语言。

## 3 分钟接入

**1. 加依赖。**

```kotlin
dependencies { implementation("wang.harlon:appsettings-kmp:<version>") }
```

**2. 根上一个配置对象、一个 Root。** 什么都不配也能跑：外观全套默认开，关于页显示应用名与版本号。

```kotlin
@Composable
fun Root() {
    val appSettings = rememberAppSettings {
        about { privacy = PRIVACY_URL; terms = TERMS_URL }
        whatsNew { bundled = { Res.readBytes("files/whatsnew.json").decodeToString() } }
    }
    AppSettingsRoot(appSettings) { App() }   // = 主题 + 系统栏 + 更新说明弹窗
}
```

**3. 设置页与三条路由。** 设置页的组和行你摆，通用行库给；二级页整页由库给，含顶栏与返回键。

```kotlin
is Settings   -> SettingsScreen(onBack = { nav.pop() }, onOpen = { nav.push(it.route()) }) {
    SettingsGroup(title = "账户") { settingsItem(icon = …, title = { Text("登录") }, onClick = …) }
    GeneralSettingsGroup()          // 外观 / 关于；想插到自己的组里就用 appearanceItem() / aboutItem()
}
is Appearance -> AppearanceScreen(onBack = { nav.pop() }, onOpenColorLab = { nav.push(ColorLab) })
is ColorLab   -> ColorLabScreen(onBack = { nav.pop() })
is About      -> AboutScreen(onBack = { nav.pop() })
```

到这里结束。用户已经有了深浅 / 纯黑模式、13 色预设、调色台、壁纸取色、关于页，升级后首启会弹更新说明。

## 宿主要提供什么

| | 必填？ | 不给时 |
|---|---|---|
| `AppSettingsRoot` 挂在根上 | 是 | 页面与行会直接报错，提示接线顺序 |
| 设置页与三条路由 | 是 | 库不接管导航，这四个页面都是你的路由 |
| `whatsNew { bundled }` | 要更新说明功能才给 | 没有更新说明 |
| `about { privacy / terms / … }` | 要链接行才给 | 关于页只有应用名与版本号 |
| `settings = ` 你已有的 multiplatform-settings 实例 | 否 | 库自建一个，只写 `appsettings.*` 命名空间 |
| `appVersion` | 否 | 读平台（PackageInfo / CFBundleShortVersionString） |
| `onEvent` | 否 | 事件丢弃 |
| `topBar { containerColor }` | 否 | Material 3 默认 |
| `appearance { … }` | 否 | 全套预设 + 调色台 + 壁纸取色；品牌色写死的 App 见下 |

## 按需深入

### 外观

品牌色写死、不想让用户选配色：

```kotlin
appearance { colorSource = ColorSource.Fixed(light = BrandLight, dark = BrandDark) }
```

色板与调色台随之隐藏，用户仍能选深浅 / 纯黑。其余可调项：`modes`（去掉纯黑就传子集）、`palette`、`allowCustom`、`allowSystemDynamic`。

**动态 App 图标**：

```kotlin
appearance {
    appIcons = listOf(
        AppIconOption(id = "default", androidAlias = "com.example.MainActivityDefault", iosIconName = null,
            label = { "默认" }, preview = { Box(Modifier.fillMaxSize().background(Color(0xFF6750A4))) }),
        // …
    )
}
```

切换、读真实状态、iOS 备用图标、选择 UI 是库的事；`androidAlias` 要在你的 manifest 里有同名 `<activity-alias>`（带 MAIN/LAUNCHER intent-filter、`targetActivity` 指向本体 Activity），且恰好一个 `android:enabled="true"`，本体 Activity 不再直接出现在桌面。debug 包首次使用会校验这些，错了直接抛异常并写明怎么改。Android Studio 的 Run 配置要改成 Specified Activity 指向本体，否则在切换过图标的设备上会报「Activity 不存在」。

### 关于

```kotlin
about {
    changelog = …; website = …; privacy = …; terms = …; feedback = …; licenses = …   // 给了哪个出哪一行
    checkUpdates(status = { myUpdateStatus() }) { checker.manualCheck() }        // 检查逻辑在你这边
    onLinkClick = { link -> if (link is AboutLink.Privacy) openWebView(link.url) else openBrowser(link.url) }
}
```

自家的行（赞助、调试开关）追加在页面上，隐藏入口靠 `onVersionClick`：

```kotlin
AboutScreen(onBack, onVersionClick = { taps++ }) {
    item(icon = …, title = { Text("捐助") }) { showDonate = true }
    if (taps >= 7) item(title = { Text("调试通道") }, onClick = …)
}
```

### 更新说明

随包的 JSON：`{"version":"1.2.0","zh":["…"],"en":["…"]}`，`version` 之外的每个 key 都是语言码。`version` 等于当前版本号且 lastSeen 低于它时，升级后首启弹一次；首装静默、占位文件（`0.0.0`）不弹。展示语言默认系统语言，`whatsNew { language = { appLanguage } }` 可覆盖；有强更门之类更高优先级的全屏时，`whatsNew { enabled = { !blocked } }` 让它先别弹，待弹状态不会丢。

### 埋点

```kotlin
rememberAppSettings {
    onEvent = { e -> when (e) {
        is AppearanceEvent.ModeChanged -> track("theme", e.mode.name)
        is AppearanceEvent.ColorSelected -> track("seed_color", e.choice.key())
        is WhatsNewEvent.Shown -> track("whats_new_shown", e.version)
        else -> Unit
    } }
}
```

事件记的是落定后的结果（图标切换成功才报）。

### 顶栏颜色

库自带页面用 M3 默认顶栏。有自己风格的 App 改一行，四个页面一起生效：

```kotlin
topBar { containerColor = { MaterialTheme.colorScheme.surfaceContainer } }
```

### 自己的设置行

`SettingsGroup { settingsItem(…) }` 就是库里通用行用的那套卡片，直接用；`InfoDialog`、`SettingsBottomSheet`、`SettingsDropdownMenu` 同样公开。

## 不在库里的东西

账户、订阅、反馈、通知、导航、埋点上报、`minVersion` 从哪来、更新检查怎么做。判断标准：需要知道「这个 App 卖什么、登什么、连哪个服务端」的，都不在。

## 构建

- App 吃本地源码：在 App 的 `local.properties` 加 `appsettings-kmp.dir=<路径>`（App 需支持 `gradle/composite-substitutions` 的 composite build）。CI 一律走 Maven 坐标，**改了库必须发版并 bump**。
- `./gradlew :library:testAndroidHostTest :sample:assembleDebug`；iOS target 在 macOS 上随 `assemble` 编译。

## 许可

MIT。`library/src/commonMain/kotlin/wang/harlon/appsettings/icons/` 下的少数图标取自 material-icons-extended / core（Apache License 2.0），文件头保留原版权声明。
