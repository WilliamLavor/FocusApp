package com.example.focusapp.data

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver

@Database(
    entities = [FocusSession::class, Local::class],
    version = 2,
    exportSchema = false
)

abstract class AppDatabase : RoomDatabase(){
    abstract fun focusDao(): FocusDao
    abstract fun localDao(): LocalDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {

            return INSTANCE ?: synchronized(this) {

                INSTANCE ?: Room.databaseBuilder<AppDatabase>(
                    context.applicationContext,
                    "focus_app.db"
                )
                    .setDriver(AndroidSQLiteDriver())
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}