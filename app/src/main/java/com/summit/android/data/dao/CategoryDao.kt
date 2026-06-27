package com.summit.android.data.dao

import androidx.room.*
import com.summit.android.data.entity.CategoryEntity
import com.summit.android.data.entity.CategoryGroupEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface CategoryDao {
    @Query("SELECT * FROM category_groups ORDER BY sort ASC")
    fun getGroups(): Flow<List<CategoryGroupEntity>>

    @Query("SELECT * FROM categories ORDER BY sort ASC")
    fun getCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: CategoryGroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)
}
