package com.example.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * MEDIO-06 — LOTE 1:
 * Base de datos local Room privada para `CRED & EDI Antropometría 1.0`.
 *
 * - Ubicación: almacenamiento interno privado de la aplicación (`context.getDatabasePath(DATABASE_NAME)`).
 * - Política de migración: sin destrucción silenciosa de datos (`fallbackToDestructiveMigration` NO activado).
 */
@Database(
    entities = [PersistedEvaluationEntity::class],
    version = 1,
    exportSchema = false
)
abstract class CredEdiDatabase : RoomDatabase() {

    abstract fun evaluationDao(): EvaluationDao

    companion object {
        const val DATABASE_NAME = "cred_edi_clinical_evaluations_v1.db"

        @Volatile
        private var INSTANCE: CredEdiDatabase? = null

        fun getInstance(context: Context): CredEdiDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CredEdiDatabase::class.java,
                    DATABASE_NAME
                ).build().also { INSTANCE = it }
            }
        }

        /**
         * Constructor de base de datos en memoria para pruebas unitarias y Robolectric deterministas.
         */
        fun createInMemoryForTesting(context: Context): CredEdiDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                CredEdiDatabase::class.java
            )
                .allowMainThreadQueries()
                .build()
        }
    }
}
