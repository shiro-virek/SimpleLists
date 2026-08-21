package com.simplelists.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.simplelists.app.data.db.DbProvider
import com.simplelists.app.data.db.TagEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val db = DbProvider.get(app)

    val tags: StateFlow<List<TagEntity>> = db.tagDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun addTag(name: String) = viewModelScope.launch {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return@launch
        if (db.tagDao().findByName(trimmed) != null) {
            _message.value = "Ya existe una etiqueta con ese nombre"
            return@launch
        }
        db.tagDao().insert(TagEntity(name = trimmed))
    }

    fun renameTag(tag: TagEntity, newName: String) = viewModelScope.launch {
        val trimmed = newName.trim()
        if (trimmed.isEmpty() || trimmed == tag.name) return@launch
        if (db.tagDao().findByName(trimmed) != null) {
            _message.value = "Ya existe una etiqueta con ese nombre"
            return@launch
        }
        db.tagDao().update(tag.copy(name = trimmed))
    }

    fun deleteTag(tag: TagEntity) = viewModelScope.launch {
        db.tagDao().delete(tag)
        _message.value = "Etiqueta \"${tag.name}\" eliminada"
    }

    fun consumeMessage() {
        _message.value = null
    }
}
