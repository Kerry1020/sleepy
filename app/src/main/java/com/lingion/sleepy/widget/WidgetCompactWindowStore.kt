package com.lingion.sleepy.widget

import android.content.Context
import com.lingion.sleepy.util.AppPrefs

/**
 * Android facade — [WidgetCompactWindowCore] 的 SharedPreferences 读写壳。
 * 与 [WidgetScrollStore] 同模式 (raw prefs map 进出, 业务逻辑归 Core)。
 *
 * 控制「· 小」变体 compact 脸的三天窗口方向: true = 今日居第一位
 * (今天/明天/后天), false = 今日居第二位 (昨天/今天/明天)。
 */
object WidgetCompactWindowStore {

    /** 本实例生效值 = 显式设置 ?: 旧全局值 (升级老用户行为不变)。 */
    fun isTodayFirst(context: Context, widgetId: Int): Boolean =
        WidgetCompactWindowCore.resolve(read(context, widgetId), AppPrefs.isCompactWindowTodayFirst(context))

    fun setTodayFirst(context: Context, widgetId: Int, todayFirst: Boolean) {
        synchronized(writeLock) {
            val raw = loadAll(context).toMutableMap()
            WidgetCompactWindowCore.write(raw, widgetId, todayFirst)
            saveAll(context, raw)
        }
    }

    /** onDeleted 清位 — 回收的 widget id 不得继承前任的窗口偏好。 */
    fun remove(context: Context, widgetId: Int) {
        synchronized(writeLock) {
            val raw = loadAll(context).toMutableMap()
            WidgetCompactWindowCore.delete(raw, widgetId)
            saveAll(context, raw)
        }
    }

    private val writeLock = Any()

    private fun read(context: Context, widgetId: Int): Boolean? =
        WidgetCompactWindowCore.read(loadAll(context), widgetId)

    private fun loadAll(context: Context): Map<String, Boolean> {
        val sp = context.getSharedPreferences(WidgetCompactWindowCore.PREFS_NAME, Context.MODE_PRIVATE)
        return sp.all.mapNotNull { (k, v) -> if (v is Boolean) k to v else null }.toMap()
    }

    private fun saveAll(context: Context, data: Map<String, Boolean>) {
        val sp = context.getSharedPreferences(WidgetCompactWindowCore.PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sp.edit().clear()
        data.forEach { (k, v) -> editor.putBoolean(k, v) }
        editor.apply()
    }
}
