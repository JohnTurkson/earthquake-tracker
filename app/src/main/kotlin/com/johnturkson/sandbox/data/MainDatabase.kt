package com.johnturkson.sandbox.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ItemDatabaseModel::class],
    version = 1,
)
abstract class MainDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
}
