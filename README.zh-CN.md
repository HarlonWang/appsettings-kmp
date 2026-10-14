# appsettings-kmp

> App 里与业务无关的那层设置：外观、关于、更新说明，以及一套设置页 UI 基元。Compose Multiplatform，Android / iOS。

**简体中文** | English（待写）

[![license](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

> 状态：设计阶段。本 README 的快速开始是 API 的试金石——写不短就说明 API 还要收。签名见 [docs/api.md](docs/api.md)，设计与决策见 [docs/design.md](docs/design.md)。

## 能力

- **外观一行接入。** `AppearanceTheme(state) { App() }` 给你深浅 / 纯黑模式、预设配色、调色台、壁纸取色、切换动画。品牌色写死的 App 传两套 `ColorScheme`，只拿深浅切换。
- **动态 App 图标。** 声明 alias 和图标资源是你的事，切换、读真实状态、iOS 备用图标、选择 UI 是库的事。
- **关于页是声明出来的。** 隐私、协议、官网、更新日志、检查更新行都是内置类型；赞助、调试开关这类自家的行照样能插。
- **更新说明不用再抄一遍。** 随包 JSON 的解析、升级后首启只弹一次、语言挑选都在库里。
- **设置页长一个样。** 连体卡片组、说明弹窗、底部浮层、下拉菜单，一套规格，多个 App 共用。
- **不认识你的 App。** 不带 Scaffold、不带导航、不埋点、不建存储。宿主传一个 `Settings`，接一个事件回调，套自己的壳。

## 快速开始

**1. 加依赖。**

```kotlin
dependencies {
    implementation("wang.harlon:appsettings-kmp:<version>")
}
```

**2. 根上挂主题与更新说明。** `settings` 是你已有的 multiplatform-settings 实例。

```kotlin
@Composable
fun Root() {
    val appearance = rememberAppearanceState(settings, onEvent = ::onSettingsEvent)
    val whatsNew = rememberWhatsNewState(
        settings,
        currentVersion = appVersion,
        load = { Res.readBytes("files/whatsnew.json").decodeToString() },
        onEvent = ::onSettingsEvent,
    )

    AppearanceTheme(appearance, typography = MyTypography) {
        SystemBarsEffect(appearance)          // Android；iOS 不需要
        App()
        WhatsNewHost(whatsNew, language = currentLanguage)
    }
}
```

品牌色写死、不想让用户选配色的 App，只改一处：

```kotlin
rememberAppearanceState(settings, AppearanceOptions(
    colorSource = ColorSource.Fixed(light = BrandLight, dark = BrandDark),
    modes = listOf(ThemeMode.FollowSystem, ThemeMode.Light, ThemeMode.Dark),
))
```

**3. 设置页：你的壳，库的件。**

```kotlin
Scaffold(topBar = { MyTopBar("设置") }) { padding ->
    LazyColumn(Modifier.padding(padding)) {
        item {
            SettingsGroup(title = "个性化", modifier = Modifier.padding(horizontal = 16.dp)) {
                settingsItem(
                    icon = Icons.Default.Palette,
                    title = { Text("外观") },
                    trailing = { Text(appearance.appearance.mode.label()) },
                    onClick = { nav.push(AppearanceRoute) },
                )
            }
        }
        item {
            SettingsGroup(title = "通用", modifier = Modifier.padding(horizontal = 16.dp)) {
                settingsItem(icon = Icons.Default.Info, title = { Text("关于") }, onClick = { nav.push(AboutRoute) })
            }
        }
    }
}
```

**4. 外观页与关于页：整页内容由库给，顶栏和返回键还是你的。**

```kotlin
// 外观
Scaffold(topBar = { MyTopBar("外观", onBack) }) { padding ->
    AppearanceContent(appearance, Modifier.padding(padding), onOpenColorLab = { nav.push(ColorLabRoute) })
}

// 关于
Scaffold(topBar = { MyTopBar("关于", onBack) }) { padding ->
    AboutContent(name = "My App", version = appVersion, modifier = Modifier.padding(padding)) {
        group {
            link(AboutLink.Changelog(CHANGELOG_URL))
            link(AboutLink.Website(WEBSITE_URL))
            link(AboutLink.Privacy(PRIVACY_URL))
            link(AboutLink.Terms(TERMS_URL))
        }
    }
}
```

**5. 事件映射到你的埋点。** 库不依赖任何埋点 SDK。

```kotlin
fun onSettingsEvent(e: AppSettingsEvent) = when (e) {
    is AppearanceEvent.ModeChanged   -> track(SettingChanged("theme_mode", e.mode.name))
    is AppearanceEvent.ColorSelected -> track(SettingChanged("theme_color", e.choice.key()))
    is WhatsNewEvent.Shown           -> track(WhatsNewShown(e.version))
    else -> Unit
}
```

到这里，三个 App 的设置页长得一样，外观和关于的代码只剩上面这些。

## 进阶

| | |
|---|---|
| 检查更新行 | 在 `AboutContent` 的 group 里 `updateCheck(status, onClick)`；检查逻辑在你这边，库只渲染 `UpdateStatus` |
| 隐藏开发者入口 | `AboutContent(onVersionClick = { taps++ })`，计数和阈值在你这边 |
| App 内打开隐私页 | `AboutContent(onLinkClick = { link -> if (link is AboutLink.Privacy) openWebView(link.url) else openBrowser(link.url) })` |
| 动态图标 | `rememberAppIconState(options)` 传给 `AppearanceContent(icons = ...)`；manifest 里的 alias 写法与 Android Studio 的 Run 配置见 [docs/design.md §9](docs/design.md) |
| 调色台 | `rememberColorLabSession(appearance)` + `ColorLabContent(session)`；`session.undo()` 接顶栏按钮，`session.commit()` 接离开页面 |
| 只拿配色、主题自己写 | `rememberAppearanceColorScheme(appearance)` 与 `appearance.isDark()` |
| 自家的设置行 | 直接用 `SettingsGroup { settingsItem(...) }`，与库内的行同一套规格 |

## 不在库里的东西

账户、订阅、反馈、通知、页面骨架、导航、埋点上报、`minVersion` 从哪来。判断标准：需要知道「这个 App 卖什么、登什么、连哪个服务端」的，都不在。

## 构建

- App 吃本地源码：在 App 的 `local.properties` 加 `appsettings-kmp.dir=<路径>`（App 需支持 `gradle/composite-substitutions` 的 composite build）。CI 一律走 Maven 坐标，**改了库必须发版并 bump**。

## 许可

MIT。
