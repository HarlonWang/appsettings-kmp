package wang.harlon.appsettings

/**
 * 所有包的事件根。库零埋点依赖：宿主写一个 `when` 把事件映射到自己的埋点词汇表。
 * 事件记的是落定后的结果，不是用户意图。
 *
 * 根是普通接口而不是 sealed：Kotlin 要求 sealed 的子类与它同包，而各包的子树
 * （`whatsnew.WhatsNewEvent` 等）各自 sealed。按根 `when` 时加 `else`，按某包的子树 `when` 可穷尽。
 */
interface AppSettingsEvent
