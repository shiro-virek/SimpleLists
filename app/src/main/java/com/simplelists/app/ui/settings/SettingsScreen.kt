package com.simplelists.app.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simplelists.app.data.db.DbProvider
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    dbEpoch: Int,
    onBack: () -> Unit,
    onDbReplaced: () -> Unit
) {
    val vm: SettingsViewModel = viewModel(key = "settings_$dbEpoch")
    val tags by vm.tags.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var newTagName by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<com.simplelists.app.data.db.TagEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<com.simplelists.app.data.db.TagEntity?>(null) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            vm.consumeMessage()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                busy = true
                val ok = DbProvider.export(context, uri)
                busy = false
                snackbarHostState.showSnackbar(if (ok) "Backup exportado correctamente" else "Error al exportar el backup")
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) pendingImportUri = uri
    }

    pendingImportUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text("Importar base de datos") },
            text = { Text("Se reemplazará TODO el contenido actual por el del backup elegido. ¿Continuar?") },
            confirmButton = {
                TextButton(onClick = {
                    pendingImportUri = null
                    scope.launch {
                        busy = true
                        val ok = DbProvider.import(context, uri)
                        busy = false
                        if (ok) {
                            onDbReplaced()
                            snackbarHostState.showSnackbar("Base de datos importada")
                        } else {
                            snackbarHostState.showSnackbar("El archivo no es una base de datos válida")
                        }
                    }
                }) { Text("Importar") }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "tags_header") {
                Text(
                    "Etiquetas",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Las etiquetas se definen acá y se asignan a los ítems.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item(key = "tag_input") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newTagName,
                        onValueChange = { newTagName = it },
                        label = { Text("Nueva etiqueta") },
                        singleLine = true,
                        isError = tags.any { it.name.equals(newTagName.trim(), ignoreCase = true) },
                        supportingText = {
                            if (tags.any { it.name.equals(newTagName.trim(), ignoreCase = true) }) {
                                Text("Ya existe")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.height(0.dp))
                    Button(
                        onClick = {
                            vm.addTag(newTagName)
                            newTagName = ""
                        },
                        enabled = newTagName.isNotBlank() &&
                            !tags.any { it.name.equals(newTagName.trim(), ignoreCase = true) },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Agregar etiqueta")
                    }
                }
            }

            items(tags, key = { it.id }) { tag ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                    ) {
                        Text(tag.name, Modifier.weight(1f))
                        IconButton(onClick = { renameTarget = tag }) {
                            Text(
                                "Renombrar",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { deleteTarget = tag }) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = "Eliminar etiqueta",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            item(key = "backup_header") {
                Text(
                    "Copia de seguridad",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Exportá la base completa a un archivo .db o importala para restaurar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item(key = "backup_buttons") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = {
                            exportLauncher.launch("simplelists-backup-${LocalDate.now()}.db")
                        },
                        enabled = !busy
                    ) {
                        Icon(Icons.Rounded.Upload, contentDescription = null)
                        Text("Exportar", modifier = Modifier.padding(start = 6.dp))
                    }
                    OutlinedButton(
                        onClick = {
                            importLauncher.launch(arrayOf("*/*"))
                        },
                        enabled = !busy
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null)
                        Text("Importar", modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }
        }
    }

    renameTarget?.let { tag ->
        TextEntryDialog(
            title = "Renombrar etiqueta",
            label = "Nombre",
            initialValue = tag.name,
            onDismiss = { renameTarget = null },
            onConfirm = {
                vm.renameTag(tag, it)
                renameTarget = null
            }
        )
    }

    deleteTarget?.let { tag ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Eliminar etiqueta") },
            text = { Text("La etiqueta \"${tag.name}\" se quitará de todos los ítems que la usan.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteTag(tag)
                    deleteTarget = null
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun TextEntryDialog(
    title: String,
    label: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(initialValue) }
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
            TextButton(enabled = value.isNotBlank(), onClick = { onConfirm(value.trim()) }) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
