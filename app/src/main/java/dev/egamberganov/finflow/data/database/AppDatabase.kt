package dev.egamberganov.finflow.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.egamberganov.finflow.data.dao.AccountDao
import dev.egamberganov.finflow.data.dao.AppSettingsDao
import dev.egamberganov.finflow.data.dao.CategoryDao
import dev.egamberganov.finflow.data.dao.ScheduledPaymentDao
import dev.egamberganov.finflow.data.dao.TransactionDao
import dev.egamberganov.finflow.data.entity.AccountEntity
import dev.egamberganov.finflow.data.entity.AppSettingsEntity
import dev.egamberganov.finflow.data.entity.CategoryEntity
import dev.egamberganov.finflow.data.entity.ScheduledPaymentEntity
import dev.egamberganov.finflow.data.entity.TransactionEntity

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        ScheduledPaymentEntity::class,
        AppSettingsEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun scheduledPaymentDao(): ScheduledPaymentDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `transactions_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `type` TEXT NOT NULL,
                        `amount` INTEGER NOT NULL,
                        `currency` TEXT NOT NULL,
                        `categoryId` INTEGER,
                        `accountId` INTEGER NOT NULL,
                        `toAccountId` INTEGER,
                        `exchangeRate` REAL,
                        `convertedAmount` INTEGER,
                        `dateMillis` INTEGER NOT NULL,
                        `note` TEXT,
                        `attachmentUri` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`toAccountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `transactions_new` (
                        `id`, `type`, `amount`, `currency`, `categoryId`, `accountId`,
                        `toAccountId`, `exchangeRate`, `convertedAmount`, `dateMillis`,
                        `note`, `attachmentUri`, `createdAt`, `updatedAt`
                    )
                    SELECT
                        `id`, `type`, `amount`, `currency`, `categoryId`, `accountId`,
                        NULL, NULL, NULL, `dateMillis`,
                        `note`, `attachmentUri`, `createdAt`, `updatedAt`
                    FROM `transactions`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `transactions`")
                db.execSQL("ALTER TABLE `transactions_new` RENAME TO `transactions`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_accountId` ON `transactions` (`accountId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_toAccountId` ON `transactions` (`toAccountId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_categoryId` ON `transactions` (`categoryId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_dateMillis` ON `transactions` (`dateMillis`)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "personal_finance.db"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
