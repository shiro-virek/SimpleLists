package com.simplelists.app.ui.lists

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.simplelists.app.data.db.DbProvider
import com.simplelists.app.data.db.ItemEntity
import com.simplelists.app.data.db.ItemWithTags
import com.simplelists.app.data.db.TabEntity
import com.simplelists.app.data.db.TagEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ListsViewModel(app: Application) : AndroidViewModel(app) {

    private val db = DbProvider.get(app)

    val tabs: StateFlow<List<TabEntity>> = db.tabDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allTags: StateFlow<List<TagEntity>> = db.tagDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedTabId = MutableStateFlow<Long?>(null)
    val selectedFilterTags = MutableStateFlow<Set<Long>>(emptySet())
    val searchQuery = MutableStateFlow("")

    val tabCounts: StateFlow<Map<Long, Int>> = db.tabDao().observeItemCounts()
        .map { list -> list.associate { it.tabId to it.count } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun consumeMessage() {
        _message.value = null
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    init {
        viewModelScope.launch {
            db.tabDao().observeAll().collect { list ->
                if (selectedTabId.value == null || list.none { it.id == selectedTabId.value }) {
                    selectedTabId.value = list.firstOrNull()?.id
                }
            }
        }
    }

    private val rawItems = selectedTabId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else db.itemDao().observeByTab(id)
    }

    val items: StateFlow<List<ItemWithTags>> =
        combine(rawItems, selectedFilterTags, searchQuery) { list, filter, query ->
            val byTag = if (filter.isEmpty()) list else list.filter { row ->
                row.tags.any { it.id in filter }
            }
            if (query.isBlank()) byTag
            else byTag.filter { row ->
                row.item.name.contains(query, ignoreCase = true) ||
                    row.item.description.contains(query, ignoreCase = true)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectTab(id: Long) {
        selectedTabId.value = id
        selectedFilterTags.value = emptySet()
        searchQuery.value = ""
    }

    fun toggleTagFilter(tagId: Long) {
        selectedFilterTags.value = selectedFilterTags.value.toMutableSet().apply {
            if (!add(tagId)) remove(tagId)
        }
    }

    fun clearFilter() {
        selectedFilterTags.value = emptySet()
    }

    fun clearSearch() {
        searchQuery.value = ""
    }

    fun addTab(name: String) = viewModelScope.launch {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return@launch
        val position = db.tabDao().nextPosition() ?: 0
        val newId = db.tabDao().insert(TabEntity(name = trimmed, position = position))
        selectedTabId.value = newId
        selectedFilterTags.value = emptySet()
        searchQuery.value = ""
    }

    fun renameTab(tab: TabEntity, newName: String) = viewModelScope.launch {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return@launch
        db.tabDao().update(tab.copy(name = trimmed))
    }

    fun deleteTab(tab: TabEntity) = viewModelScope.launch {
        db.tabDao().delete(tab)
    }

    fun persistTabOrder(orderedIds: List<Long>) = viewModelScope.launch {
        db.tabDao().saveOrder(orderedIds)
    }

    fun saveItem(
        existing: ItemWithTags?,
        tabId: Long,
        name: String,
        description: String,
        tagIds: List<Long>
    ) = viewModelScope.launch {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return@launch
        val conflict = db.withTransaction {
            val duplicate = db.itemDao().findIdByName(
                tabId = tabId,
                name = trimmedName,
                excludeId = existing?.item?.id ?: -1L
            )
            if (duplicate != null) {
                true
            } else {
                if (existing == null) {
                    val position = db.itemDao().nextPosition(tabId) ?: 0
                    val id = db.itemDao().insert(
                        ItemEntity(
                            tabId = tabId,
                            name = trimmedName,
                            description = description.trim(),
                            createdAt = System.currentTimeMillis(),
                            position = position
                        )
                    )
                    db.itemDao().setTags(id, tagIds)
                } else {
                    db.itemDao().update(
                        existing.item.copy(name = trimmedName, description = description.trim())
                    )
                    db.itemDao().setTags(existing.item.id, tagIds)
                }
                false
            }
        }
        if (conflict) {
            _message.value = "Ya existe un ítem llamado \"$trimmedName\" en esta pestaña"
        }
    }

    fun deleteItem(item: ItemEntity) = viewModelScope.launch {
        db.itemDao().delete(item)
    }

    fun persistOrder(orderedIds: List<Long>) = viewModelScope.launch {
        db.itemDao().saveOrder(orderedIds)
    }
}
