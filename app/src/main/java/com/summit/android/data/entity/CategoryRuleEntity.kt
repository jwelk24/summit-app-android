package com.summit.android.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.*

@Entity(
    tableName = "category_rules",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoryId")]
)
data class CategoryRuleEntity(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val priority: Int = 100,
    val matchField: String, // "merchant", "memo"
    val matchKind: String, // "contains", "equals", "startsWith", "endsWith"
    val pattern: String,
    val caseSensitive: Boolean = false,
    val enabled: Boolean = true,
    val categoryId: UUID?,
    val timesApplied: Int = 0,
    val lastAppliedAt: Date? = null
)
