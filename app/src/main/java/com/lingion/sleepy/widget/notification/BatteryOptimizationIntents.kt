package com.lingion.sleepy.widget.notification

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri

/** 电池/后台管理页候选入口: component 为显式组件(am/ai), action 为隐式意图; 二者取其一。 */
data class BatteryIntentSpec(
    val component: String? = null,
    val action: String? = null,
)

/**
 * 候选顺序: 厂商耗电/自启动管理页 → 标准"忽略电池优化"弹窗 → 原生电池优化应用列表页。
 * 原生列表页没有"无限制后台活动"选项 — 该选项只存在于厂商定制省电页, 所以厂商页必须排最前。
 * 厂商组件名来自社区逆向(证据等级 B, 随 ROM 版本漂移), 逐个 try-launch 吞异常, 全败才落原生。
 */
fun vendorBatterySpecs(vendor: LiveCardVendor): List<BatteryIntentSpec> = buildList {
    when (vendor) {
        // MIUI/HyperOS 省电策略-省电策略页(内含"无限制"档位)
        LiveCardVendor.XIAOMI ->
            add(BatteryIntentSpec(component = "com.miui.powerkeeper/com.miui.powerkeeper.ui.HiddenAppsConfigActivity"))
        LiveCardVendor.HUAWEI ->
            add(BatteryIntentSpec(component = "com.huawei.systemmanager/com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"))
        LiveCardVendor.HONOR ->
            add(BatteryIntentSpec(component = "com.hihonor.systemmanager/com.hihonor.systemmanager.appcontrol.activity.StartupAppControlActivity"))
        // ColorOS/OxygenOS 13+ 应用耗电详情(内含"允许后台活动")
        LiveCardVendor.OPPO, LiveCardVendor.ONEPLUS, LiveCardVendor.REALME ->
            add(BatteryIntentSpec(component = "com.oplus.battery/com.oplus.pantanal.ums.ui.appdetail.AppDetailActivity"))
        LiveCardVendor.VIVO, LiveCardVendor.IQOO ->
            add(BatteryIntentSpec(component = "com.iqoo.secure/com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"))
        LiveCardVendor.MEIZU, LiveCardVendor.SAMSUNG, LiveCardVendor.GENERIC -> Unit
    }
    add(BatteryIntentSpec(action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS))
    add(BatteryIntentSpec(action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
}

/** 逐个尝试启动候选, 命中返回 true; 全部不可用返回 false(调用方静默放弃)。 */
fun launchBatterySettings(
    context: Context,
    vendor: LiveCardVendor = detectLiveCardVendor(),
): Boolean = vendorBatterySpecs(vendor).any { spec ->
    try {
        val pkg = context.packageName
        val intent = when {
            spec.component != null -> Intent().setComponent(ComponentName.unflattenFromString(spec.component))
            else -> Intent(spec.action)
        }
        when (spec.action) {
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS ->
                intent.setData("package:$pkg".toUri())
            else -> {
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, pkg)
                // MIUI PowerKeeper 读这两个 extra 定位到本应用的省电策略页
                intent.putExtra("package_name", pkg)
                intent.putExtra("package_label", context.applicationInfo.loadLabel(context.packageManager).toString())
            }
        }
        context.startActivity(intent)
        true
    } catch (_: Exception) {
        false
    }
}
