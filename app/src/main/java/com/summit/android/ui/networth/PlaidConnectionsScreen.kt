package com.summit.android.ui.networth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.summit.android.service.StoredPlaidItem
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaidConnectionsScreen(
    onBack: () -> Unit,
    viewModel: PlaidConnectionsViewModel = viewModel()
) {
    val items by viewModel.items.collectAsState()
    val syncingItemId by viewModel.syncingItemId.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plaid Connections") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            item {
                SectionHeader("Linked Items")
            }

            if (items.isEmpty()) {
                item {
                    Text(
                        "No banks linked yet.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            } else {
                items(items) { item ->
                    PlaidItemRow(
                        item = item,
                        isSyncing = syncingItemId == item.itemId,
                        onSync = { viewModel.syncItem(item) },
                        onUnlink = { viewModel.unlinkItem(item.itemId) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun PlaidItemRow(
    item: StoredPlaidItem,
    isSyncing: Boolean,
    onSync: () -> Void,
    onUnlink: () -> Unit
) {
    val df = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val dateStr = df.format(Date(item.linkedAt))

    ListItem(
        headlineContent = { Text(item.institutionName ?: item.itemId) },
        supportingContent = { Text("Linked $dateStr") },
        trailingContent = {
            Row {
                if (isSyncing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    IconButton(onClick = { onSync() }) {
                        Icon(Icons.Default.Sync, contentDescription = "Sync")
                    }
                }
                IconButton(onClick = onUnlink) {
                    Icon(Icons.Default.Delete, contentDescription = "Unlink")
                }
            }
        }
    )
}
