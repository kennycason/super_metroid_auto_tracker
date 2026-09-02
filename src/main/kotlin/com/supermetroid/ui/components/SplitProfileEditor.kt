package com.supermetroid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import com.supermetroid.autosplits.SplitProfiles
import com.supermetroid.model.Split
import com.supermetroid.model.SplitProfile
import com.supermetroid.service.SplitProfileDeleteResult
import com.supermetroid.service.SplitProfileSaveResult
import com.supermetroid.service.SplitProfileService
import com.supermetroid.ui.theme.TrackerColors
import kotlinx.coroutines.launch

@Composable
fun SplitProfileManagementSection(
    splitProfileService: SplitProfileService,
    modifier: Modifier = Modifier
) {
    val currentProfile by splitProfileService.currentProfile.collectAsState()
    val availableProfiles by splitProfileService.availableProfiles.collectAsState()
    val scope = rememberCoroutineScope()

    var dropdownExpanded by remember { mutableStateOf(false) }
    var editorProfile by remember { mutableStateOf<SplitProfile?>(null) }
    var creatingProfile by remember { mutableStateOf(false) }
    var mutationError by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = modifier.fillMaxWidth(0.95f),
        colors = CardDefaults.cardColors(containerColor = TrackerColors.SurfaceVariant),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Split Profile",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TrackerColors.Primary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = if (splitProfileService.isBuiltIn(currentProfile.id)) {
                            "Built-in • names can be customized"
                        } else {
                            "Custom • version ${currentProfile.version}"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TrackerColors.OnSurfaceVariant
                        )
                    )
                }
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { dropdownExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TrackerColors.SurfaceOverlayLight,
                        contentColor = TrackerColors.OnSurface
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${currentProfile.name} (${currentProfile.splits.size} splits)",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        Text(if (dropdownExpanded) "▲" else "▼")
                    }
                }

                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    modifier = Modifier.background(TrackerColors.Surface)
                ) {
                    availableProfiles.forEach { profile ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = "${profile.name} (${profile.splits.size})",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (profile.id == currentProfile.id) {
                                            TrackerColors.Primary
                                        } else {
                                            TrackerColors.OnSurface
                                        }
                                    )
                                    if (!splitProfileService.isBuiltIn(profile.id)) {
                                        Text(
                                            text = "Custom • v${profile.version}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TrackerColors.OnSurfaceVariant
                                        )
                                    }
                                }
                            },
                            onClick = {
                                dropdownExpanded = false
                                mutationError = null
                                scope.launch {
                                    if (!splitProfileService.setProfile(profile)) {
                                        mutationError =
                                            "Finish, reset, or discard the active run before switching profiles."
                                    }
                                }
                            },
                            colors = MenuDefaults.itemColors(textColor = TrackerColors.OnSurface)
                        )
                    }

                    Divider(color = TrackerColors.OnSurfaceVariant.copy(alpha = 0.25f))
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = {
                            Text(
                                "Create New Split Profile…",
                                color = TrackerColors.Primary,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        onClick = {
                            dropdownExpanded = false
                            creatingProfile = true
                        }
                    )
                }
            }

            Button(
                onClick = { editorProfile = currentProfile },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TrackerColors.SurfaceOverlayLight,
                    contentColor = TrackerColors.OnSurface
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(4.dp))
                Text(
                    if (splitProfileService.isBuiltIn(currentProfile.id)) "Customize Names" else "Edit Profile",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            mutationError?.let {
                Text(it, color = TrackerColors.Error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    if (creatingProfile) {
        SplitProfileEditorWindow(
            splitProfileService = splitProfileService,
            profile = null,
            onClose = { creatingProfile = false }
        )
    }

    editorProfile?.let { profile ->
        SplitProfileEditorWindow(
            splitProfileService = splitProfileService,
            profile = profile,
            onClose = { editorProfile = null }
        )
    }

}

@Composable
private fun SplitProfileEditorWindow(
    splitProfileService: SplitProfileService,
    profile: SplitProfile?,
    onClose: () -> Unit
) {
    val builtIn = profile?.let { splitProfileService.isBuiltIn(it.id) } == true
    val title = when {
        profile == null -> "Create Split Profile"
        builtIn -> "Customize ${profile.name}"
        else -> "Edit ${profile.name}"
    }

    DialogWindow(
        onCloseRequest = onClose,
        title = title,
        state = rememberDialogState(width = 820.dp, height = 620.dp),
        resizable = true
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = TrackerColors.Background) {
            SplitProfileEditorContent(
                splitProfileService = splitProfileService,
                profile = profile,
                builtIn = builtIn,
                onClose = onClose
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SplitProfileEditorContent(
    splitProfileService: SplitProfileService,
    profile: SplitProfile?,
    builtIn: Boolean,
    onClose: () -> Unit
) {
    val catalog = splitProfileService.splitCatalog
    val catalogById = remember(catalog) { catalog.associateBy { it.id } }
    val initialSplitIds = remember(profile?.id) {
        profile?.splits?.map { it.id } ?: listOf("ship")
    }
    val initialOverrides = remember(profile?.id) {
        profile?.let { splitProfileService.splitNameOverrides(it.id) }.orEmpty()
    }
    val defaultNames = remember(profile?.id, builtIn, catalog) {
        if (builtIn && profile != null) {
            SplitProfiles.BY_ID[profile.id]?.splits?.associate { it.id to it.name }.orEmpty()
        } else {
            catalog.associate { it.id to it.name }
        }
    }

    var profileName by remember(profile?.id) { mutableStateOf(profile?.name.orEmpty()) }
    var selectedSplitIds by remember(profile?.id) { mutableStateOf(initialSplitIds) }
    var splitNames by remember(profile?.id) {
        mutableStateOf(
            initialSplitIds.associateWith { splitId ->
                initialOverrides[splitId]
                    ?: profile?.splits?.firstOrNull { it.id == splitId }?.name
                    ?: defaultNames[splitId].orEmpty()
            }
        )
    }
    var search by remember { mutableStateOf("") }
    var saveError by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val unusedSplits = remember(catalog, selectedSplitIds, search) {
        catalog
            .filter { it.id !in selectedSplitIds && it.id != "ship" }
            .filter {
                search.isBlank() || it.name.contains(search, ignoreCase = true) ||
                    it.id.contains(search, ignoreCase = true) ||
                    it.type.contains(search, ignoreCase = true)
            }
            .sortedWith(compareBy<Split> { it.type }.thenBy { it.name })
    }
    val nameError = if (builtIn) null else {
        splitProfileService.validateProfileName(profileName, excludingProfileId = profile?.id)
    }
    val routeSplitIds = remember(selectedSplitIds, builtIn) {
        if (builtIn) selectedSplitIds else selectedSplitIds.filterNot { it == "ship" }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (profile == null) "Create Split Profile" else "Split Profile Editor",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = TrackerColors.Primary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = when {
                        builtIn -> "Built-in ordering is locked; display names are editable."
                        profile == null -> "Choose and order automatic splits. Ship is always the finish."
                        else -> "Structural changes create version ${profile.version + 1} when saved."
                    },
                    style = MaterialTheme.typography.bodySmall.copy(color = TrackerColors.OnSurfaceVariant)
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = TrackerColors.OnSurface)
            }
        }

        if (builtIn) {
            Text(
                text = profile?.name.orEmpty(),
                style = MaterialTheme.typography.titleMedium.copy(color = TrackerColors.OnSurface)
            )
        } else {
            OutlinedTextField(
                value = profileName,
                onValueChange = {
                    if (it.length <= SplitProfileService.MAX_PROFILE_NAME_LENGTH) profileName = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Profile name") },
                singleLine = true,
                isError = nameError != null,
                supportingText = nameError?.let { message -> { Text(message) } }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!builtIn) {
                Card(
                    modifier = Modifier.weight(0.38f).fillMaxHeight(),
                    colors = CardDefaults.cardColors(containerColor = TrackerColors.SurfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Available Splits",
                            color = TrackerColors.Primary,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = search,
                            onValueChange = { search = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Search") },
                            singleLine = true
                        )
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (unusedSplits.isEmpty()) {
                                item {
                                    Text(
                                        text = if (search.isBlank()) {
                                            "Every available split is already in this route."
                                        } else {
                                            "No matching splits are available. Clear the search or remove the split from the route."
                                        },
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TrackerColors.OnSurfaceVariant
                                        )
                                    )
                                }
                            }
                            items(unusedSplits, key = { it.id }) { split ->
                                AvailableSplitRow(split) {
                                    val insertionIndex = selectedSplitIds.indexOf("ship")
                                        .takeIf { it >= 0 } ?: selectedSplitIds.size
                                    selectedSplitIds = selectedSplitIds.toMutableList().apply {
                                        add(insertionIndex, split.id)
                                    }
                                    splitNames = splitNames + (split.id to split.name)
                                }
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.weight(if (builtIn) 1f else 0.62f).fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = TrackerColors.SurfaceVariant)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (builtIn) {
                                "Splits (${selectedSplitIds.size})"
                            } else {
                                "Route Splits (${routeSplitIds.size})"
                            },
                            color = TrackerColors.Primary,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = {
                            splitNames = selectedSplitIds.associateWith { id -> defaultNames[id].orEmpty() }
                        }) {
                            Text("Restore All Names", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (!builtIn && routeSplitIds.isEmpty()) {
                            item {
                                Text(
                                    "Add splits from the left. New splits are appended in route order.",
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TrackerColors.OnSurfaceVariant
                                    )
                                )
                            }
                        }
                        itemsIndexed(routeSplitIds, key = { _, id -> id }) { index, splitId ->
                            val split = catalogById[splitId] ?: return@itemsIndexed
                            val selectedIndex = selectedSplitIds.indexOf(splitId)
                            SelectedSplitRow(
                                split = split,
                                positionLabel = "${index + 1}",
                                displayName = splitNames[splitId] ?: split.name,
                                defaultName = defaultNames[splitId] ?: split.name,
                                structureLocked = builtIn,
                                canMoveUp = selectedIndex > 0,
                                canMoveDown = selectedIndex in 0 until selectedSplitIds.lastIndex - 1,
                                canRemove = !builtIn,
                                onDisplayNameChanged = { value ->
                                    if (value.length <= SplitProfileService.MAX_SPLIT_NAME_LENGTH) {
                                        splitNames = splitNames + (splitId to value)
                                    }
                                },
                                onResetName = {
                                    splitNames = splitNames + (splitId to (defaultNames[splitId] ?: split.name))
                                },
                                onMoveUp = {
                                    selectedSplitIds = selectedSplitIds.moved(selectedIndex, selectedIndex - 1)
                                },
                                onMoveDown = {
                                    selectedSplitIds = selectedSplitIds.moved(selectedIndex, selectedIndex + 1)
                                },
                                onRemove = {
                                    selectedSplitIds = selectedSplitIds - splitId
                                    splitNames = splitNames - splitId
                                }
                            )
                        }
                    }

                    if (!builtIn) {
//                        Divider(color = TrackerColors.OnSurfaceVariant.copy(alpha = 0.2f))
                        val ship = catalogById["ship"]
                        if (ship != null) {
                            SelectedSplitRow(
                                split = ship,
                                positionLabel = "Finish",
                                displayName = splitNames["ship"] ?: ship.name,
                                defaultName = defaultNames["ship"] ?: ship.name,
                                structureLocked = true,
                                canMoveUp = false,
                                canMoveDown = false,
                                canRemove = false,
                                supportingLabel = "Always the final split",
                                onDisplayNameChanged = { value ->
                                    if (value.length <= SplitProfileService.MAX_SPLIT_NAME_LENGTH) {
                                        splitNames = splitNames + ("ship" to value)
                                    }
                                },
                                onResetName = {
                                    splitNames = splitNames + ("ship" to (defaultNames["ship"] ?: ship.name))
                                },
                                onMoveUp = {},
                                onMoveDown = {},
                                onRemove = {}
                            )
                        }
                    }
                }
            }
        }

        saveError?.let {
            Text(it, color = TrackerColors.Error, style = MaterialTheme.typography.bodySmall)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (profile != null && !builtIn) {
                TextButton(
                    onClick = { showDeleteConfirmation = true },
                    enabled = !saving && !deleting,
                    colors = ButtonDefaults.textButtonColors(contentColor = TrackerColors.Error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Delete Profile")
                }
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onClose, enabled = !saving && !deleting) { Text("Cancel") }
            Spacer(Modifier.size(8.dp))
            Button(
                onClick = {
                    saveError = null
                    saving = true
                    scope.launch {
                        val overrides = selectedSplitIds.associateWith { id ->
                            splitNames[id].orEmpty()
                        }
                        val result = if (profile == null) {
                            splitProfileService.createCustomProfile(profileName, selectedSplitIds, overrides)
                        } else {
                            splitProfileService.saveProfile(
                                profile.id,
                                if (builtIn) profile.name else profileName,
                                selectedSplitIds,
                                overrides
                            )
                        }
                        when (result) {
                            is SplitProfileSaveResult.Success -> onClose()
                            is SplitProfileSaveResult.Failure -> {
                                saveError = result.message
                                saving = false
                            }
                        }
                    }
                },
                enabled = !saving && !deleting && nameError == null && selectedSplitIds.lastOrNull() == "ship",
                colors = ButtonDefaults.buttonColors(
                    containerColor = TrackerColors.Primary,
                    contentColor = TrackerColors.OnPrimary
                )
            ) {
                Text(if (saving) "Saving…" else "Save")
            }
        }
    }

    if (showDeleteConfirmation && profile != null && !builtIn) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete split profile?") },
            text = {
                Text(
                    "Delete '${profile.name}'? Its definition will be archived and backed up. All recorded runs and LiveSplit data will be kept."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        saveError = null
                        deleting = true
                        scope.launch {
                            when (val result = splitProfileService.deleteCustomProfile(profile.id)) {
                                SplitProfileDeleteResult.Success -> onClose()
                                is SplitProfileDeleteResult.Failure -> {
                                    saveError = result.message
                                    deleting = false
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = TrackerColors.Error)
                ) { Text("Delete Profile") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) { Text("Cancel") }
            },
            containerColor = TrackerColors.Surface
        )
    }
}

@Composable
private fun AvailableSplitRow(split: Split, onAdd: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, TrackerColors.OnSurfaceVariant.copy(alpha = 0.18f), RoundedCornerShape(5.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SpriteIcon(
            itemId = getSplitItemId(split),
            isObtained = true,
            size = 34
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                split.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall.copy(color = TrackerColors.OnSurface)
            )
            Text(
                split.type.replace('_', ' ').uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TrackerColors.OnSurfaceVariant.copy(alpha = 0.7f)
                )
            )
            split.description?.let { description ->
                Text(
                    description,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TrackerColors.OnSurfaceVariant
                    )
                )
            }
        }
        IconButton(onClick = onAdd, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Add, contentDescription = "Add ${split.name}", modifier = Modifier.size(17.dp))
        }
    }
}

@Composable
private fun SelectedSplitRow(
    split: Split,
    positionLabel: String,
    displayName: String,
    defaultName: String,
    structureLocked: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    canRemove: Boolean,
    supportingLabel: String? = null,
    onDisplayNameChanged: (String) -> Unit,
    onResetName: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, TrackerColors.OnSurfaceVariant.copy(alpha = 0.18f), RoundedCornerShape(5.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = positionLabel,
            style = MaterialTheme.typography.labelSmall.copy(color = TrackerColors.OnSurfaceVariant)
        )
        SpriteIcon(
            itemId = getSplitItemId(split),
            isObtained = true,
            size = 34
        )
        CompactSplitNameField(
            value = displayName,
            onValueChange = onDisplayNameChanged,
            modifier = Modifier.weight(1f),
            defaultName = defaultName,
            splitType = split.type,
            description = supportingLabel ?: split.description
        )
        if (displayName.trim() != defaultName) {
            TextButton(
                onClick = onResetName,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                modifier = Modifier.size(44.dp, 24.dp)
            ) {
                Text("Default", style = MaterialTheme.typography.labelSmall)
            }
        }

        if (!structureLocked) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = canMoveUp,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, "Move up", modifier = Modifier.size(16.dp))
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = canMoveDown,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, "Move down", modifier = Modifier.size(16.dp))
                }
                IconButton(
                    onClick = onRemove,
                    enabled = canRemove,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Close, "Remove ${split.name}", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun CompactSplitNameField(
    value: String,
    onValueChange: (String) -> Unit,
    defaultName: String,
    splitType: String,
    description: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall.copy(color = TrackerColors.OnSurface),
            cursorBrush = SolidColor(TrackerColors.Primary),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            defaultName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TrackerColors.OnSurfaceVariant.copy(alpha = 0.65f)
                            )
                        )
                    }
                    innerTextField()
                }
            }
        )
        Text(
            splitType.replace('_', ' ').uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                color = TrackerColors.OnSurfaceVariant.copy(alpha = 0.7f)
            )
        )
        description?.let {
            Text(
                it,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall.copy(color = TrackerColors.OnSurfaceVariant)
            )
        }
    }
}

private fun <T> List<T>.moved(fromIndex: Int, toIndex: Int): List<T> {
    if (fromIndex !in indices || toIndex !in indices) return this
    return toMutableList().apply {
        val item = removeAt(fromIndex)
        add(toIndex, item)
    }
}
