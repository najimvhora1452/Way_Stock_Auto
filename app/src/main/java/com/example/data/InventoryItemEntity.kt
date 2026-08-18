package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory_items")
data class InventoryItemEntity(
    @PrimaryKey val key: String,
    val name: String,
    val displayName: String = "",
    val type: String = "item", // "folder" or "item"
    val parent: String = "root",
    val toggleOn: Boolean = false,
    val allowedUnitsCsv: String = "Box,Packet,Bunch,Kg", // Comma-separated list
    val currentUnit: String = "Box"
)
