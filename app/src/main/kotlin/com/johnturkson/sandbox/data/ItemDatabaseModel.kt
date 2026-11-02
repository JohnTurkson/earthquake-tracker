package com.johnturkson.sandbox.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity data class ItemDatabaseModel(@PrimaryKey override val id: String) : Item
