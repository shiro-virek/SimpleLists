package com.simplelists.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Junction
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class ItemWithTags(
    @Embedded val item: ItemEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ItemTagCrossRef::class,
            parentColumn = "itemId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity>
)

@Dao
interface TabDao {

    @Query("SELECT * FROM tabs ORDER BY position")
    fun observeAll(): Flow<List<TabEntity>>

    @Query("SELECT COUNT(*) FROM tabs")
    suspend fun count(): Int

    @Insert
    suspend fun insert(tab: TabEntity): Long

    @Update
    suspend fun update(tab: TabEntity)

    @Delete
    suspend fun delete(tab: TabEntity)

    @Query("SELECT MAX(position) + 1 FROM tabs")
    suspend fun nextPosition(): Int?

    @Transaction
    suspend fun saveOrder(ids: List<Long>) {
        ids.forEachIndexed { index, id -> updatePosition(id, index) }
    }

    @Query("UPDATE tabs SET position = :position WHERE id = :id")
    suspend fun updatePosition(id: Long, position: Int)
}

@Dao
interface ItemDao {

    @Transaction
    @Query("SELECT * FROM items WHERE tabId = :tabId ORDER BY position")
    fun observeByTab(tabId: Long): Flow<List<ItemWithTags>>

    @Query("SELECT MAX(position) + 1 FROM items WHERE tabId = :tabId")
    suspend fun nextPosition(tabId: Long): Int?

    @Insert
    suspend fun insert(item: ItemEntity): Long

    @Update
    suspend fun update(item: ItemEntity)

    @Delete
    suspend fun delete(item: ItemEntity)

    @Query("UPDATE items SET position = :position WHERE id = :id")
    suspend fun updatePosition(id: Long, position: Int)

    @Transaction
    suspend fun saveOrder(ids: List<Long>) {
        ids.forEachIndexed { index, id -> updatePosition(id, index) }
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRefs(refs: List<ItemTagCrossRef>)

    @Query("DELETE FROM item_tags WHERE itemId = :itemId")
    suspend fun clearRefs(itemId: Long)

    @Transaction
    suspend fun setTags(itemId: Long, tagIds: List<Long>) {
        clearRefs(itemId)
        if (tagIds.isNotEmpty()) {
            insertRefs(tagIds.map { ItemTagCrossRef(itemId, it) })
        }
    }
}

@Dao
interface TagDao {

    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<TagEntity>>

    @Query("SELECT id FROM tags WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByName(name: String): Long?

    @Insert
    suspend fun insert(tag: TagEntity): Long

    @Update
    suspend fun update(tag: TagEntity)

    @Delete
    suspend fun delete(tag: TagEntity)
}
