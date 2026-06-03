package com.memoriabox.database

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.memoriabox.data.dao.*
import com.memoriabox.data.model.*
import net.sqlcipher.database.SupportFactory
import java.security.SecureRandom
import javax.crypto.spec.PBEKeySpec
import javax.crypto.SecretKeyFactory

@Database(
    entities = [
        Box::class, Event::class, Friend::class, Label::class, 
        FriendRelation::class, EventLabel::class, LogEntry::class
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun boxDao(): BoxDao
    abstract fun eventDao(): EventDao
    abstract fun friendDao(): FriendDao
    abstract fun labelDao(): LabelDao
    abstract fun logDao(): LogDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(
            context: Context,
            passphrase: ByteArray? = null,
            migrationCallback: () -> Unit = {}
        ): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val dbBuilder = Room.databaseBuilder(
                    context,
                    AppDatabase::class.java,
                    "memoriabox.db"
                )
                
                dbBuilder.addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5
                )
                
                dbBuilder.addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Insert default box after database is created
                        // Use execSQL directly since INSTANCE is not yet available
                        db.execSQL("""
                            INSERT INTO boxes (id, name, icon, bg_type, bg_value, sort_order, is_archived, created_at)
                            VALUES ('default_1', '我的日子', '*', 'COLOR', '#7C4DFF', 0, 0, strftime('%s', 'now') * 1000)
                        """.trimIndent())
                    }

                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        repairLegacyData(db)
                        migrationCallback()
                    }
                })

                if (passphrase != null) {
                    val passphraseOrNull: ByteArray? = passphrase
                    val factory = SupportFactory(passphraseOrNull)
                    dbBuilder.openHelperFactory(factory)
                }
                
                val instance = dbBuilder.build()
                INSTANCE = instance
                instance
            }
        }

        fun getOrCreatePassphrase(context: Context, keyAlias: String?): ByteArray {
            val prefs = context.getSharedPreferences("db_key", Context.MODE_PRIVATE)
            var encodedKey = prefs.getString("encoded_key", null)
            
            if (encodedKey == null) {
                val salt = ByteArray(16)
                SecureRandom().nextBytes(salt)
                val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                val spec = PBEKeySpec(keyAlias?.toCharArray() ?: "default".toCharArray(), salt, 10000, 256)
                val secret = factory.generateSecret(spec)
                encodedKey = android.util.Base64.encodeToString(secret.encoded, android.util.Base64.NO_WRAP)
                prefs.edit().putString("encoded_key", encodedKey).apply()
            }
            
            return android.util.Base64.decode(encodedKey, android.util.Base64.NO_WRAP)
        }

        private fun repairLegacyData(db: SupportSQLiteDatabase) {
            db.execSQL("UPDATE boxes SET bg_type = 'COLOR' WHERE bg_type NOT IN ('COLOR', 'IMAGE')")
            db.execSQL("UPDATE events SET type = 'COUNTDOWN' WHERE type NOT IN ('COUNTDOWN', 'ANNIVERSARY', 'ELAPSED', 'BIRTHDAY', 'TODO')")
            db.execSQL("UPDATE events SET todo_status = 'PENDING' WHERE todo_status NOT IN ('PENDING', 'COMPLETED', 'CANCELLED')")
            db.execSQL("UPDATE boxes SET sort_order = 0 WHERE sort_order IS NULL")
            db.execSQL("UPDATE boxes SET is_archived = 0 WHERE is_archived IS NULL")
            ensureColumnExists(db, "events", "is_pinned", "INTEGER NOT NULL DEFAULT 0")
            ensureColumnExists(db, "events", "pushplus_enabled", "INTEGER NOT NULL DEFAULT 0")
            ensureEventStyleColumns(db)
            ensureEventRepeatColumns(db)
            db.execSQL("UPDATE events SET repeat_mode = 'NONE' WHERE repeat_mode NOT IN ('NONE', 'YEARLY', 'MONTHLY', 'CUSTOM_DAYS', 'CUSTOM_WEEKS', 'CUSTOM_MONTHS')")
            db.execSQL("""
                INSERT OR IGNORE INTO boxes (id, name, icon, bg_type, bg_value, sort_order, is_archived, created_at)
                VALUES ('default_1', '我的日子', '*', 'COLOR', '#7C4DFF', 0, 0, strftime('%s', 'now') * 1000)
            """.trimIndent())
        }

    }
}

private fun ensureColumnExists(db: SupportSQLiteDatabase, table: String, column: String, definition: String) {
    db.query("PRAGMA table_info($table)").use { cursor ->
        while (cursor.moveToNext()) {
            if (cursor.getString(cursor.getColumnIndexOrThrow("name")) == column) {
                return
            }
        }
    }
    db.execSQL("ALTER TABLE $table ADD COLUMN $column $definition")
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE events ADD COLUMN note TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE events ADD COLUMN reminder_enabled INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE events ADD COLUMN reminder_days INTEGER NOT NULL DEFAULT 1")
        database.execSQL("ALTER TABLE events ADD COLUMN alarm_enabled INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE events ADD COLUMN alarm_time TEXT NOT NULL DEFAULT '09:00'")
        database.execSQL("ALTER TABLE events ADD COLUMN card_style_json TEXT")
        database.execSQL("ALTER TABLE events ADD COLUMN avatar_uri TEXT")
        database.execSQL("ALTER TABLE events ADD COLUMN is_birthday INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE events ADD COLUMN repeat_yearly INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE events ADD COLUMN todo_status TEXT NOT NULL DEFAULT 'PENDING'")
        database.execSQL("ALTER TABLE events ADD COLUMN due_date INTEGER")
        
        database.execSQL("CREATE TABLE IF NOT EXISTS friends (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, avatar_uri TEXT, birthday_date INTEGER, created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000))")
        
        database.execSQL("CREATE TABLE IF NOT EXISTS labels (name TEXT NOT NULL PRIMARY KEY, color TEXT NOT NULL DEFAULT '#7C4DFF', created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000))")
        
        database.execSQL("CREATE TABLE IF NOT EXISTS friend_relations (friend_id TEXT NOT NULL, label TEXT NOT NULL, PRIMARY KEY (friend_id, label), FOREIGN KEY (friend_id) REFERENCES friends(id) ON DELETE CASCADE)")
        
        database.execSQL("CREATE TABLE IF NOT EXISTS event_labels (event_id TEXT NOT NULL, label TEXT NOT NULL, PRIMARY KEY (event_id, label), FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE)")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        ensureColumnExists(database, "events", "is_pinned", "INTEGER NOT NULL DEFAULT 0")
        ensureColumnExists(database, "events", "pushplus_enabled", "INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        ensureEventStyleColumns(database)
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        ensureEventRepeatColumns(database)
    }
}

private fun ensureEventStyleColumns(database: SupportSQLiteDatabase) {
    ensureColumnExists(database, "events", "repeat_mode", "TEXT NOT NULL DEFAULT 'NONE'")
    ensureColumnExists(database, "events", "repeat_interval", "INTEGER NOT NULL DEFAULT 1")
    ensureColumnExists(database, "events", "gradient_start", "TEXT NOT NULL DEFAULT '#7C4DFF'")
    ensureColumnExists(database, "events", "gradient_end", "TEXT NOT NULL DEFAULT '#FF8A80'")
    ensureColumnExists(database, "events", "text_color", "TEXT NOT NULL DEFAULT '#FFFFFF'")
    ensureColumnExists(database, "events", "card_template", "TEXT NOT NULL DEFAULT 'HERO'")
    ensureColumnExists(database, "events", "display_fields", "TEXT NOT NULL DEFAULT 'date,note,lunar,reminder'")
}

private fun ensureEventRepeatColumns(database: SupportSQLiteDatabase) {
    ensureColumnExists(database, "events", "repeat_end_date", "INTEGER")
    ensureColumnExists(database, "events", "repeat_count", "INTEGER NOT NULL DEFAULT 0")
    ensureColumnExists(database, "events", "reminder_offsets", "TEXT NOT NULL DEFAULT '1'")
}
