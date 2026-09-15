package com.lingion.sleepy.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 数据库迁移清单 — 任何 schema 改动必须在此登记一条 Migration,
 * 然后在 [ALL_MIGRATIONS] 数组里挂上,再把 [AppDatabase] 的 version 推一档。
 *
 * 严禁 fallbackToDestructiveMigration 兜底 — 任意版本升级会清空用户课表。
 *
 * 版本变更记录:
 *   v3 → v4: 加 courses.colorMode 字段 (issue#22 同名课程多地点修复)
 *     - 列: INTEGER NOT NULL DEFAULT 0
 *     - 旧库所有行 colorMode=0 (GROUP 模式) → 渲染行为完全不变
 *   v4 → v5: 加 courses.isIrregularNode / isIrregularTime (issue#23 逐卡非常规重构)
 *     - 两列: INTEGER NOT NULL DEFAULT 0
 *     - ownTime=1 旧行 isIrregularTime=1 (旧「自定义时间」语义并入新标志)
 *     - isIrregularNode 由 startNode 是否为边缘编号推导, 保存时回写防漂移
 *   v5 → v6: 加 courses.alias (issue#26 课程别名)
 *     - 列: TEXT NOT NULL DEFAULT ''
 *     - 旧库所有行 alias='' → 处处显示原名, 行为不变
 *   v6 → v7: 加 import_drafts 导入草稿快照表
 *     - 草稿由显式 id 标识, payloadJson 保持导入预览数据的演进空间
 */
val MIGRATION_3_4: Migration = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            ALTER TABLE courses
            ADD COLUMN colorMode INTEGER NOT NULL DEFAULT 0
        """.trimIndent())
    }
}

val MIGRATION_4_5: Migration = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE courses ADD COLUMN isIrregularNode INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE courses ADD COLUMN isIrregularTime INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE courses SET isIrregularTime = 1 WHERE ownTime = 1")
    }
}

val MIGRATION_5_6: Migration = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        MIGRATION_5_6_STATEMENTS.forEach { db.execSQL(it) }
    }
}

val MIGRATION_6_7: Migration = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        MIGRATION_6_7_STATEMENTS.forEach { db.execSQL(it) }
    }
}

/** 当前已注册的全部 Migration — AppDatabase.Companion.get() 链入 */
val ALL_MIGRATIONS: Array<Migration> = arrayOf(
    MIGRATION_3_4,
    MIGRATION_4_5,
    MIGRATION_5_6,
    MIGRATION_6_7,
)

/** issue#26: v5→v6 逐条 SQL — 单一事实来源, CourseAliasMigrationTest 用 sqlite-jdbc 直接执行同一份 */
internal val MIGRATION_5_6_STATEMENTS: List<String> = listOf(
    "ALTER TABLE courses ADD COLUMN alias TEXT NOT NULL DEFAULT ''"
)

/** v6→v7 SQL is shared with the JVM migration contract test. */
internal val MIGRATION_6_7_STATEMENTS: List<String> = listOf(
    """
    CREATE TABLE IF NOT EXISTS import_drafts (
        id TEXT NOT NULL PRIMARY KEY,
        sourceType TEXT NOT NULL DEFAULT '',
        sourceUrl TEXT NOT NULL DEFAULT '',
        payloadJson TEXT NOT NULL,
        createdAt INTEGER NOT NULL,
        updatedAt INTEGER NOT NULL
    )
    """.trimIndent(),
)
