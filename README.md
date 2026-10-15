# appsettings-kmp

> The business-agnostic settings layer of an app — appearance, about, what's new, app language, the settings screen — wired up with a single config object. Compose Multiplatform, Android / iOS.

English | [简体中文](README.zh-CN.md)

[![Maven Central](https://img.shields.io/maven-central/v/wang.harlon/appsettings-kmp)](https://central.sonatype.com/artifact/wang.harlon/appsettings-kmp) [![license](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

## Quick start

**1. Add the dependency.**

```kotlin
dependencies { implementation("wang.harlon:appsettings-kmp:<version>") }
```

**2. One config object and one Root at the top of the tree.** It runs with no configuration at all: appearance is fully enabled by default, the About screen shows the app name and version.

```kotlin
@Composable
fun Root() {
    val appSettings = rememberAppSettings {
        about { privacy = PRIVACY_URL; terms = TERMS_URL }
        whatsNew { bundled = { Res.readBytes("files/whatsnew.json").decodeToString() } }
    }
    AppSettingsRoot(appSettings) { App() }   // = theme + system bars + what's-new dialog
}
```

**3. The settings screen and three routes.** You lay out the groups and rows of the settings screen; the library supplies the common rows. The secondary screens come whole from the library, top bar and back button included.

```kotlin
is Settings   -> SettingsScreen(onBack = { nav.pop() }, onOpen = { nav.push(it.route()) }) {
    SettingsGroup(title = "Account") { settingsItem(icon = …, title = { Text("Sign in") }, onClick = …) }
    GeneralSettingsGroup()          // appearance / about; use appearanceItem() / aboutItem() to place them in your own group
}
is Appearance -> AppearanceScreen(onBack = { nav.pop() }, onOpenColorLab = { nav.push(ColorLab) })
is ColorLab   -> ColorLabScreen(onBack = { nav.pop() })
is About      -> AboutScreen(onBack = { nav.pop() })
```

That's it. Users now have light / dark / AMOLED modes, 13 color presets, a color lab, wallpaper-based color, an About screen, and a what's-new dialog on the first launch after an upgrade.

## What the host provides

| | Required? | When omitted |
|---|---|---|
| `AppSettingsRoot` at the top of the tree | Yes | Screens and rows throw with a message about wiring order |
| The settings screen and three routes | Yes | The library never takes over navigation; all four screens are your routes |
| `whatsNew { bundled }` | Only for what's new | No what's-new dialog |
| `language { languages }` | Only for the app-language row | No app language; library strings follow the system |
| `about { privacy / terms / … }` | Only for link rows | About shows just the app name and version |
| `settings = ` your existing multiplatform-settings instance | No | The library creates one and writes only the `appsettings.*` namespace |
| `appVersion` | No | Read from the platform (PackageInfo / CFBundleShortVersionString) |
| `onEvent` | No | Events are dropped |
| `topBar { containerColor }` | No | Material 3 default |
| `appearance { … }` | No | Full preset palette + color lab + wallpaper color; see below for fixed brand colors |

## Going deeper

### Appearance

Fixed brand colors, no user-selectable palette:

```kotlin
appearance { colorSource = ColorSource.Fixed(light = BrandLight, dark = BrandDark) }
```

The palette and color lab disappear; users can still pick light / dark / AMOLED. Other knobs: `modes` (pass a subset to drop AMOLED), `palette`, `allowCustom`, `allowSystemDynamic`.

**Dynamic app icons**:

```kotlin
appearance {
    appIcons = listOf(
        AppIconOption(id = "default", androidAlias = "com.example.MainActivityDefault", iosIconName = null,
            label = { "Default" }, preview = { Box(Modifier.fillMaxSize().background(Color(0xFF6750A4))) }),
        // …
    )
}
```

Switching, reading the real state, iOS alternate icons and the picker UI are the library's job. Each `androidAlias` needs a matching `<activity-alias>` in your manifest (with a MAIN/LAUNCHER intent filter and `targetActivity` pointing at your real Activity), exactly one of them `android:enabled="true"`, and the real Activity no longer appears on the launcher itself. Debug builds validate this on first use and throw with instructions. Set the Android Studio run configuration to Specified Activity pointing at the real Activity, otherwise devices that have switched icons report "Activity does not exist".

### About

```kotlin
about {
    changelog = …; website = …; privacy = …; terms = …; feedback = …; licenses = …   // each url you give becomes a row
    checkUpdates(status = { myUpdateStatus() }) { checker.manualCheck() }        // the checking logic stays on your side
    onLinkClick = { link -> if (link is AboutLink.Privacy) openWebView(link.url) else openBrowser(link.url) }
}
```

Append your own rows (donations, debug switches) on the screen; hidden entries hang off `onVersionClick`:

```kotlin
AboutScreen(onBack, onVersionClick = { taps++ }) {
    item(icon = …, title = { Text("Donate") }) { showDonate = true }
    if (taps >= 7) item(title = { Text("Debug channel") }, onClick = …)
}
```

### What's new

A JSON file shipped with the app: `{"version":"1.2.0","zh":["…"],"en":["…"]}` — every key other than `version` is a language code. The dialog shows once on the first launch after an upgrade, when `version` equals the current app version and lastSeen is below it; a fresh install records silently, and a placeholder file (`0.0.0`) never shows. The display language defaults to the system language (or the app language once `language { }` is enabled); `whatsNew { language = { … } }` overrides it. When a higher-priority full-screen state such as a force-update gate is up, `whatsNew { enabled = { !blocked } }` holds the dialog back without losing the pending state.

### App language

```kotlin
language { languages = listOf("en", "zh") }      // only you know which languages you translated; order is menu order, the first must be your default resource language
SettingsGroup(title = "General") { languageItem(); appearanceItem(); aboutItem() }
```

An inline dropdown of "Follow system" plus your list. Options show each language's own name (English / 中文) by default; `label = { tag -> … }` overrides. The choice is stored as a BCP 47 tag; stale values outside the list count as follow-system. Once enabled, the what's-new dialog follows the app language automatically. Read the current language (for a locale query parameter, an Accept-Language header) with `appSettings.language.resolved()`; outside Compose (view models, networking) read `APP_LANGUAGE_STORAGE_KEY` from your own Settings and go through `AppLanguage.fromStorage(stored, languages).resolve(languages)` for the same answer. Follow-system clamps the system language to your list by primary subtag; when the system language is not in the list, the first entry wins — so **the first entry must match your `values/` / development language**, otherwise it and the platform's actual fallback diverge.

**Android**: the library calls `AppCompatDelegate.setApplicationLocales` from the Root. Your Activity must extend `AppCompatActivity` with a `Theme.AppCompat.*` or `Theme.Material3.*` theme (debug builds validate the Activity on first use and throw; a wrong theme makes AppCompat itself throw at startup). On API 33+ the system persists the choice and users can also change the app's language in system settings; the library adopts the system's choice on startup rather than overwriting it — keep your list in sync with `locales_config.xml`. For API 33 and below to cold-start directly in the chosen language, add to the manifest:

```xml
<service android:name="androidx.appcompat.app.AppLocalesMetadataHolderService" android:enabled="false" android:exported="false">
    <meta-data android:name="autoStoreLocales" android:value="true" />
</service>
```

Without it things still work; below API 33 the first frame of a cold start renders in the system language and recreates once.

**iOS**: there is no per-app language switch without a restart, so the row opens this app's page in system Settings and echoes the effective language in its trailing text. `Info.plist` needs `CFBundleLocalizations` listing the same languages, otherwise system Settings has no Language entry for the app. Changing the language in system Settings emits no `LanguageEvent` (it is not an in-app choice); log `resolved()` once at startup if you need the number.

### Analytics

```kotlin
rememberAppSettings {
    onEvent = { e -> when (e) {
        is AppearanceEvent.ModeChanged -> track("theme", e.mode.name)
        is AppearanceEvent.ColorSelected -> track("seed_color", e.choice.key())
        is WhatsNewEvent.Shown -> track("whats_new_shown", e.version)
        is LanguageEvent.Changed -> track("app_language", e.tag ?: "system")
        else -> Unit
    } }
}
```

Events report settled outcomes (an icon switch is reported only once it succeeded).

### Top bar color

The library's screens use the M3 default top bar. An app with its own style changes one line and all four screens follow:

```kotlin
topBar { containerColor = { MaterialTheme.colorScheme.surfaceContainer } }
```

### Your own settings rows

`SettingsGroup { settingsItem(…) }` is the same card group the library's common rows use; `InfoDialog`, `SettingsBottomSheet` and `SettingsDropdownMenu` are public too.

## Not in this library

Accounts, subscriptions, feedback, notifications, navigation, analytics transport, where `minVersion` comes from, how update checks work. The rule: anything that needs to know what the app sells, who it signs in, or which backend it talks to stays out.

## Building

- Consuming the library from local source: add `appsettings-kmp.dir=<path>` to the app's `local.properties` (the app needs the composite build from `gradle/composite-substitutions`). CI always resolves the Maven coordinate — **a library change must be released and bumped**.
- `./gradlew :library:testAndroidHostTest :sample:assembleDebug`; the iOS targets compile with `assemble` on macOS.

## License

MIT.
