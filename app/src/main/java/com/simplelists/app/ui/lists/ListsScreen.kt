package com.simplelists.app.ui.lists

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simplelists.app.R
import com.simplelists.app.data.db.ItemWithTags
import com.simplelists.app.data.db.TabEntity
import com.simplelists.app.data.db.TagEntity
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListsScreen(dbEpoch: Int, onOpenSettings: () -> Unit) {
    val vm: ListsViewModel = viewModel(key = "lists_$dbEpoch")
    val tabs by vm.tabs.collectAsStateWithLifecycle()
    val allTags by vm.allTags.collectAsStateWithLifecycle()
    val items by vm.items.collectAsStateWithLifecycle()
    val selectedTabId by vm.selectedTabId.collectAsStateWithLifecycle()
    val filterTags by vm.selectedFilterTags.collectAsStateWithLifecycle()

    var showAddTabDialog by rememberSaveable { mutableStateOf(false) }
    var showManageTabs by rememberSaveable { mutableStateOf(false) }
    var renameTabTarget by remember { mutableStateOf<TabEntity?>(null) }
    var deleteTabTarget by remember { mutableStateOf<TabEntity?>(null) }
    var editingItem by remember { mutableStateOf<ItemWithTags?>(null) }
    var showNewItemDialog by rememberSaveable { mutableStateOf(false) }

    val displayList = remember { SnapshotStateList<ItemWithTags>() }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(items) {
        if (!dragging) {
            displayList.clear()
            displayList.addAll(items)
        }
    }
    val filterActive = filterTags.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Ajustes")
                    }
                }
            )
        },
        floatingActionButton = {
            if (tabs.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showNewItemDialog = true },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text("Ítem") }
                )
            }
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            TabsRow(
                tabs = tabs,
                selectedTabId = selectedTabId,
                onTabSelected = { vm.selectTab(it) },
                onAddTab = { showAddTabDialog = true },
                onManageTabs = { showManageTabs = true },
                onRenameTab = { renameTabTarget = it },
                onDeleteTab = { deleteTabTarget = it }
            )

            FilterRow(
                tags = allTags,
                selectedIds = filterTags,
                onToggle = { vm.toggleTagFilter(it) },
                onClear = { vm.clearFilter() }
            )

            AnimatedVisibility(visible = filterActive) {
                Text(
                    text = "Filtro activo: el arrastre está deshabilitado",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            ItemsList(
                hasTabs = tabs.isNotEmpty(),
                filterActive = filterActive,
                displayList = displayList,
                onStartDrag = { dragging = true },
                onStopDrag = {
                    dragging = false
                    vm.persistOrder(displayList.map { it.item.id })
                },
                onItemClick = { editingItem = it }
            )
        }
    }

    if (showAddTabDialog) {
        TextEntryDialog(
            title = "Nueva pestaña",
            label = "Nombre de la pestaña",
            onDismiss = { showAddTabDialog = false },
            onConfirm = {
                vm.addTab(it)
                showAddTabDialog = false
            }
        )
    }

    renameTabTarget?.let { tab ->
        TextEntryDialog(
            title = "Renombrar pestaña",
            label = "Nombre de la pestaña",
            initialValue = tab.name,
            onDismiss = { renameTabTarget = null },
            onConfirm = {
                vm.renameTab(tab, it)
                renameTabTarget = null
            }
        )
    }

    deleteTabTarget?.let { tab ->
        AlertDialog(
            onDismissRequest = { deleteTabTarget = null },
            title = { Text("Eliminar pestaña") },
            text = { Text("¿Eliminar \"${tab.name}\" y todos sus ítems? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteTab(tab)
                    deleteTabTarget = null
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTabTarget = null }) { Text("Cancelar") }
            }
        )
    }

    if (showManageTabs) {
        ManageTabsSheet(
            tabs = tabs,
            onDismiss = { showManageTabs = false },
            onPersistOrder = { vm.persistTabOrder(it) },
            onRename = { renameTabTarget = it },
            onDelete = { deleteTabTarget = it }
        )
    }

    if (showNewItemDialog || editingItem != null) {
        val targetTabId = selectedTabId ?: tabs.firstOrNull()?.id
        ItemEditorDialog(
            item = editingItem,
            allTags = allTags,
            canSave = targetTabId != null,
            onDismiss = {
                showNewItemDialog = false
                editingItem = null
            },
            onSave = { name, desc, ids ->
                if (targetTabId != null) {
                    vm.saveItem(editingItem, targetTabId, name, desc, ids)
                }
                showNewItemDialog = false
                editingItem = null
            },
            onDelete = editingItem?.let { item ->
                ({
                    vm.deleteItem(item.item)
                    editingItem = null
                })
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TabsRow(
    tabs: List<TabEntity>,
    selectedTabId: Long?,
    onTabSelected: (Long) -> Unit,
    onAddTab: () -> Unit,
    onManageTabs: () -> Unit,
    onRenameTab: (TabEntity) -> Unit,
    onDeleteTab: (TabEntity) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(tabs, key = { it.id }) { tab ->
            TabChip(
                tab = tab,
                selected = tab.id == selectedTabId,
                onClick = { onTabSelected(tab.id) },
                onRename = { onRenameTab(tab) },
                onDelete = { onDeleteTab(tab) }
            )
        }
        item(key = "add_tab") {
            IconButton(onClick = onAddTab) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = "Agregar pestaña",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item(key = "manage_tabs") {
            IconButton(onClick = onManageTabs) {
                Icon(
                    Icons.Rounded.Edit,
                    contentDescription = "Editar pestañas",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TabChip(
    tab: TabEntity,
    selected: Boolean,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            shape = RoundedCornerShape(50),
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { menuExpanded = true }
                )
        ) {
            Text(
                text = tab.name,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            DropdownMenuItem(
                text = { Text("Renombrar") },
                onClick = {
                    menuExpanded = false
                    onRename()
                }
            )
            DropdownMenuItem(
                text = { Text("Eliminar") },
                onClick = {
                    menuExpanded = false
                    onDelete()
                }
            )
        }
    }
}

@Composable
private fun FilterRow(
    tags: List<TagEntity>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit,
    onClear: () -> Unit
) {
    if (tags.isEmpty()) return
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (selectedIds.isNotEmpty()) {
            item(key = "clear_filter") {
                FilterChip(
                    selected = false,
                    onClick = onClear,
                    label = { Text("Limpiar") },
                    leadingIcon = { Icon(Icons.Rounded.Close, contentDescription = null) }
                )
            }
        }
        items(tags, key = { it.id }) { tag ->
            FilterChip(
                selected = tag.id in selectedIds,
                onClick = { onToggle(tag.id) },
                label = { Text(tag.name) }
            )
        }
    }
}

@Composable
private fun ItemsList(
    hasTabs: Boolean,
    filterActive: Boolean,
    displayList: SnapshotStateList<ItemWithTags>,
    onStartDrag: () -> Unit,
    onStopDrag: () -> Unit,
    onItemClick: (ItemWithTags) -> Unit
) {
    when {
        !hasTabs -> EmptyState(
            title = "Creá una pestaña",
            subtitle = "Usá el botón + para crear tu primera lista"
        )

        displayList.isEmpty() && filterActive -> EmptyState(
            title = "Sin resultados",
            subtitle = "Ningún ítem coincide con el filtro seleccionado"
        )

        displayList.isEmpty() -> EmptyState(
            title = "Lista vacía",
            subtitle = "Tocá «Ítem» para agregar el primero"
        )

        else -> {
            val lazyListState = rememberLazyListState()
            val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
                if (!filterActive && from.index < displayList.size && to.index < displayList.size) {
                    displayList.add(to.index, displayList.removeAt(from.index))
                }
            }

            LazyColumn(
                state = lazyListState,
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(displayList, key = { it.item.id }) { row ->
                    ReorderableItem(reorderableState, key = row.item.id) { isDragging ->
                        val elevation by animateDpAsState(
                            targetValue = if (isDragging) 8.dp else 1.dp,
                            label = "card_elevation"
                        )
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem(),
                            shape = RoundedCornerShape(14.dp),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = elevation
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    Modifier
                                        .weight(1f)
                                        .clickable { onItemClick(row) }
                                ) {
                                    ItemContent(row)
                                }
                                val dragModifier = if (filterActive) Modifier
                                else Modifier.draggableHandle(
                                    onDragStarted = { onStartDrag() },
                                    onDragStopped = { onStopDrag() }
                                )
                                IconButton(onClick = {}, modifier = dragModifier) {
                                    Icon(
                                        Icons.Rounded.DragHandle,
                                        contentDescription = "Mover",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ItemContent(row: ItemWithTags) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = row.item.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (row.item.description.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = row.item.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = Instant.ofEpochMilli(row.item.createdAt)
                    .atZone(ZoneId.systemDefault())
                    .format(dateFormat),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
        if (row.tags.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.tags.forEach { tag ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                    ) {
                        Text(
                            text = tag.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManageTabsSheet(
    tabs: List<TabEntity>,
    onDismiss: () -> Unit,
    onPersistOrder: (List<Long>) -> Unit,
    onRename: (TabEntity) -> Unit,
    onDelete: (TabEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val orderedTabs = remember { SnapshotStateList<TabEntity>() }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(tabs) {
        if (!dragging) {
            orderedTabs.clear()
            orderedTabs.addAll(tabs)
        }
    }

    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        if (from.index < orderedTabs.size && to.index < orderedTabs.size) {
            orderedTabs.add(to.index, orderedTabs.removeAt(from.index))
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Text(
            text = "Editar pestañas",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
        )
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            items(orderedTabs, key = { it.id }) { tab ->
                ReorderableItem(reorderableState, key = tab.id) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        IconButton(
                            onClick = {},
                            modifier = Modifier.draggableHandle(
                                onDragStarted = { dragging = true },
                                onDragStopped = {
                                    dragging = false
                                    onPersistOrder(orderedTabs.map { it.id })
                                }
                            )
                        ) {
                            Icon(
                                Icons.Rounded.DragHandle,
                                contentDescription = "Mover pestaña",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = tab.name,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onRename(tab) }) {
                            Icon(
                                Icons.Rounded.Edit,
                                contentDescription = "Renombrar pestaña",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onDelete(tab) }) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = "Eliminar pestaña",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Rounded.FormatListBulleted,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ItemEditorDialog(
    item: ItemWithTags?,
    allTags: List<TagEntity>,
    canSave: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, description: String, tagIds: List<Long>) -> Unit,
    onDelete: (() -> Unit)?
) {
    var name by remember { mutableStateOf(item?.item?.name ?: "") }
    var description by remember { mutableStateOf(item?.item?.description ?: "") }
    var selectedIds by remember { mutableStateOf(item?.tags?.map { it.id }?.toSet() ?: emptySet<Long>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "Nuevo ítem" else "Editar ítem") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    isError = name.isBlank(),
                    supportingText = { if (name.isBlank()) Text("El nombre es obligatorio") }
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción (opcional)") },
                    minLines = 2,
                    maxLines = 5
                )
                if (allTags.isNotEmpty()) {
                    Text(
                        "Etiquetas",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        allTags.forEach { tag ->
                            FilterChip(
                                selected = tag.id in selectedIds,
                                onClick = {
                                    selectedIds = selectedIds.toMutableSet().apply {
                                        if (!add(tag.id)) remove(tag.id)
                                    }
                                },
                                label = { Text(tag.name) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave && name.isNotBlank(),
                onClick = { onSave(name.trim(), description.trim(), selectedIds.toList()) }
            ) { Text("Guardar") }
        },
        dismissButton = {
            androidx.compose.foundation.layout.Row {
                onDelete?.let { delete ->
                    TextButton(onClick = delete) {
                        Text("Eliminar", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        }
    )
}

@Composable
private fun TextEntryDialog(
    title: String,
    label: String,
    initialValue: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by rememberSaveable { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(label) },
                singleLine = true,
                isError = value.isBlank()
            )
        },
        confirmButton = {
            TextButton(
                enabled = value.isNotBlank(),
                onClick = { onConfirm(value.trim()) }
            ) { Text("Aceptar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
