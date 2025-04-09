package com.focusflow.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.focusflow.data.local.dao.SessionDao
import com.focusflow.data.local.dao.TreeDao
import com.focusflow.data.local.entity.SessionEntity
import com.focusflow.data.local.entity.TreeEntity
import com.focusflow.data.local.util.DateConverter

@Database(
    entities = [SessionEntity::class, TreeEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun sessionDao(): SessionDao
    abstract fun treeDao(): TreeDao

    companion object {
        private const val DATABASE_NAME = "focus_flow_db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
