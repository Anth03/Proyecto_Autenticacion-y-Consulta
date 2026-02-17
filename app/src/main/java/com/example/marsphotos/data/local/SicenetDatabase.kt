package com.example.marsphotos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.marsphotos.data.local.entities.*

@Database(
    entities = [
        ProfileStudentEntity::class,
        CargaAcademicaEntity::class,
        KardexEntity::class,
        CalificacionUnidadEntity::class,
        CalificacionFinalEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SicenetDatabase : RoomDatabase() {
    abstract fun sicenetDao(): SicenetDao

    companion object {
        @Volatile
        private var INSTANCE: SicenetDatabase? = null

        fun getDatabase(context: Context): SicenetDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SicenetDatabase::class.java,
                    "sicenet_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
