package com.memoriabox.database

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.memoriabox.data.dao.*
import com.memoriabox.data.model.*
import com.memoriabox.utils.ColorUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.sqlcipher.database.SupportFactory
import java.security.SecureRandom
import javax.crypto.spec.PBEKeySpec
import javax.crypto.SecretKeyFactory

@Database(
    entities = [
        Box::class, Event::class, Friend::class, Label::class, 
        FriendRelation::class, EventLabel::class, LogEntry::class
    ],
    version = 2,
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
                    MIGRATION_1_2
                )
                
                dbBuilder.addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Insert default box after database is created
                        // Use execSQL directly since INSTANCE is not yet available
                        db.execSQL("INSERT INTO boxes (id, name, icon, bg_type, bg_value, created_at) VALUES ('default_1', '我的盒子', '📦', 0, '#7C4DFF', strftime('%s', 'now') * 1000)")
                    }

                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
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
    }
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
