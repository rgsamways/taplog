package ca.taplog.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Organisation::class,
        Site::class,
        Asset::class,
        Inspection::class,
        Deficiency::class,
        ScanEvent::class,
        TagEvent::class,
        VerticalConfigEntity::class
    ],
    version = 8,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun organisationDao(): OrganisationDao
    abstract fun siteDao(): SiteDao
    abstract fun assetDao(): AssetDao
    abstract fun inspectionDao(): InspectionDao
    abstract fun deficiencyDao(): DeficiencyDao
    abstract fun scanEventDao(): ScanEventDao
    abstract fun tagEventDao(): TagEventDao
    abstract fun verticalConfigDao(): VerticalConfigDao

    companion object {

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {

                // organisations table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS organisations (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        phone TEXT,
                        email TEXT,
                        address TEXT,
                        city TEXT,
                        province TEXT NOT NULL DEFAULT 'ON',
                        subscriptionTier TEXT NOT NULL DEFAULT 'SOLO',
                        subscriptionStatus TEXT NOT NULL DEFAULT 'TRIAL',
                        isSynced INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())

                // sites table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS sites (
                        id TEXT NOT NULL PRIMARY KEY,
                        organisationId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        address TEXT NOT NULL,
                        city TEXT NOT NULL,
                        province TEXT NOT NULL DEFAULT 'ON',
                        postalCode TEXT,
                        clientName TEXT,
                        clientPhone TEXT,
                        contactName TEXT,
                        contactPhone TEXT,
                        notes TEXT,
                        isActive INTEGER NOT NULL DEFAULT 1,
                        isSynced INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY (organisationId) REFERENCES organisations(id) ON DELETE CASCADE
                    )
                """.trimIndent())

                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_sites_organisationId ON sites (organisationId)"
                )

                // rebuild assets — drop buildingName, add siteId
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS assets_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        nfcTagId TEXT NOT NULL,
                        siteId TEXT NOT NULL DEFAULT '',
                        name TEXT NOT NULL,
                        assetType TEXT NOT NULL,
                        location TEXT NOT NULL,
                        installDate INTEGER NOT NULL,
                        lastInspectedAt INTEGER,
                        nextInspectionDue INTEGER,
                        isActive INTEGER NOT NULL DEFAULT 1,
                        isSynced INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY (siteId) REFERENCES sites(id) ON DELETE CASCADE
                    )
                """.trimIndent())

                db.execSQL("""
                    INSERT INTO assets_new
                    SELECT id, nfcTagId, '' as siteId, name, assetType,
                           location, installDate, lastInspectedAt,
                           nextInspectionDue, isActive, createdAt
                    FROM assets
                """.trimIndent())

                db.execSQL("DROP TABLE assets")
                db.execSQL("ALTER TABLE assets_new RENAME TO assets")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_assets_siteId ON assets (siteId)"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS scan_events (
                        id TEXT NOT NULL PRIMARY KEY,
                        tagId TEXT NOT NULL,
                        assetId TEXT NOT NULL,
                        inspectorId TEXT,
                        inspectorName TEXT NOT NULL,
                        scannedAt INTEGER NOT NULL,
                        eventType TEXT NOT NULL,
                        isSynced INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (assetId) REFERENCES assets(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_scan_events_assetId ON scan_events (assetId)"
                )

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS tag_events (
                        id TEXT NOT NULL PRIMARY KEY,
                        assetId TEXT NOT NULL,
                        tagId TEXT NOT NULL,
                        attachedAt INTEGER NOT NULL,
                        retiredAt INTEGER,
                        retiredReason TEXT,
                        retiredByInspectorId TEXT,
                        isSynced INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (assetId) REFERENCES assets(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_tag_events_assetId ON tag_events (assetId)"
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Organisation entity gains isSynced (was missing from MIGRATION_2_3 and v4 schema)
                db.execSQL(
                    "ALTER TABLE organisations ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE assets ADD COLUMN vertical TEXT NOT NULL DEFAULT 'EMBER'"
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS vertical_configs (
                        verticalCode TEXT NOT NULL PRIMARY KEY,
                        configJson TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE organisations ADD COLUMN licensedVerticals TEXT NOT NULL DEFAULT '[\"EMBER\"]'"
                )
            }
        }

        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "taplog_ember.db")
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}