package com.auralis.player.ui.screens.playlists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.auralis.player.domain.model.SmartField
import com.auralis.player.domain.model.SmartOperator
import com.auralis.player.domain.model.SmartPlaylist
import com.auralis.player.domain.model.SmartRule
import com.auralis.player.domain.model.SmartSort
import com.auralis.player.ui.components.AuralisChip
import com.auralis.player.ui.components.AuralisDialog
import com.auralis.player.ui.components.AuralisTextField
import com.auralis.player.ui.components.AccentIconButton
import com.auralis.player.ui.components.ConfirmDialog
import com.auralis.player.ui.components.GlassPanel
import com.auralis.player.ui.components.SectionHeader
import com.auralis.player.ui.components.SegmentedControl
import com.auralis.player.ui.theme.AuralisTheme
import com.auralis.player.ui.theme.AuralisType
import com.auralis.player.ui.theme.display
import com.auralis.player.ui.theme.localizedStyle

/**
 * Rule builder for a smart playlist.
 *
 * The whole screen is a single scrolling column so nothing can be clipped off
 * the right edge: every control wraps instead of overflowing horizontally.
 * A live match count sits at the top, so an unusable rule is visible
 * immediately rather than after saving.
 */
@Composable
fun SmartPlaylistEditorScreen(
    initial: SmartPlaylist,
    matchCount: (SmartPlaylist) -> Int,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onSave: (SmartPlaylist) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing

    var draft by remember { mutableStateOf(initial) }
    var fieldPickerIndex by remember { mutableStateOf(-1) }
    var operatorPickerIndex by remember { mutableStateOf(-1) }
    var confirmDelete by remember { mutableStateOf(false) }
    var nameAttempted by remember { mutableStateOf(false) }

    val trimmedName = draft.name.trim()
    val nameError = nameAttempted && trimmedName.isEmpty()
    val count = matchCount(draft)

    fun updateRule(index: Int, transform: (SmartRule) -> SmartRule) {
        draft = draft.copy(
            rules = draft.rules.mapIndexed { i, rule -> if (i == index) transform(rule) else rule }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = spacing.screen, vertical = spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            AccentIconButton(Icons.Rounded.ArrowBack, "Back", size = 40.dp) { onBack() }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (draft.id == 0L) "New smart playlist" else "Edit smart playlist",
                    style = AuralisTheme.style.display(AuralisType.title),
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$count tracks match",
                    style = AuralisType.bodySmall,
                    color = colors.accent
                )
            }
            if (draft.id != 0L) {
                AccentIconButton(
                    Icons.Rounded.DeleteOutline,
                    "Delete smart playlist",
                    size = 40.dp,
                    tint = colors.danger
                ) { confirmDelete = true }
            }
            AccentIconButton(Icons.Rounded.Check, "Save", size = 44.dp, filled = true) {
                if (trimmedName.isEmpty()) {
                    nameAttempted = true
                } else {
                    onSave(draft.copy(name = trimmedName))
                }
            }
        }

        LazyColumn(
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Column(modifier = Modifier.padding(horizontal = spacing.screen)) {
                    AuralisTextField(
                        value = draft.name,
                        onValueChange = { if (it.length <= 60) draft = draft.copy(name = it) },
                        placeholder = "Name",
                        isError = nameError,
                        errorText = if (nameError) "A name is required" else null
                    )
                }
            }

            item { SectionHeader(title = "Rules") }

            if (draft.rules.isEmpty()) {
                item {
                    Text(
                        text = "No rules yet — this playlist currently includes your whole library. Add a condition to narrow it down.",
                        style = AuralisType.bodySmall,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(horizontal = spacing.screen, vertical = spacing.xs)
                    )
                }
            }

            itemsIndexed(draft.rules) { index, rule ->
                GlassPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.md, vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(spacing.md),
                        verticalArrangement = Arrangement.spacedBy(spacing.sm)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                        ) {
                            Text(
                                text = "Rule ${index + 1}",
                                style = AuralisType.overline,
                                color = colors.textTertiary,
                                modifier = Modifier.weight(1f)
                            )
                            AccentIconButton(
                                Icons.Rounded.Close,
                                "Remove rule ${index + 1}",
                                size = 34.dp,
                                tint = colors.textTertiary
                            ) {
                                draft = draft.copy(
                                    rules = draft.rules.filterIndexed { i, _ -> i != index }
                                )
                            }
                        }

                        AuralisChip(
                            label = rule.field.label,
                            selected = true,
                            onClick = { fieldPickerIndex = index }
                        )
                        AuralisChip(
                            label = rule.operator.label,
                            selected = false,
                            onClick = { operatorPickerIndex = index }
                        )

                        if (!rule.field.isBoolean) {
                            AuralisTextField(
                                value = rule.value,
                                onValueChange = { value -> updateRule(index) { it.copy(value = value) } },
                                placeholder = when {
                                    rule.field.isDate -> "Days"
                                    rule.field.isNumber -> "Number"
                                    else -> "Text"
                                },
                                helperText = if (rule.field == SmartField.DURATION) {
                                    "In seconds"
                                } else {
                                    null
                                }
                            )
                            if (rule.operator == SmartOperator.BETWEEN) {
                                AuralisTextField(
                                    value = rule.valueTo,
                                    onValueChange = { value -> updateRule(index) { it.copy(valueTo = value) } },
                                    placeholder = "And"
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.screen, vertical = spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AccentIconButton(Icons.Rounded.Add, "Add rule", size = 40.dp, filled = true) {
                        draft = draft.copy(rules = draft.rules + SmartRule())
                    }
                    Text(text = "Add rule", style = AuralisType.body, color = colors.textSecondary)
                }
            }

            item { SectionHeader(title = "Match") }
            item {
                Box(modifier = Modifier.padding(horizontal = spacing.screen)) {
                    SegmentedControl(
                        options = listOf("All rules", "Any rule"),
                        selectedIndex = if (draft.matchAll) 0 else 1,
                        onSelect = { draft = draft.copy(matchAll = it == 0) }
                    )
                }
            }

            item { SectionHeader(title = "Sort") }
            item {
                Column(
                    modifier = Modifier.padding(horizontal = spacing.screen),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    SmartSort.entries.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                            row.forEach { option ->
                                AuralisChip(
                                    label = option.label,
                                    selected = draft.sort == option,
                                    onClick = { draft = draft.copy(sort = option) }
                                )
                            }
                        }
                    }
                    SegmentedControl(
                        options = listOf("Ascending", "Descending"),
                        selectedIndex = if (draft.sortDescending) 1 else 0,
                        onSelect = { draft = draft.copy(sortDescending = it == 1) }
                    )
                }
            }

            item { SectionHeader(title = "Limit") }
            item {
                Column(modifier = Modifier.padding(horizontal = spacing.screen)) {
                    AuralisTextField(
                        value = if (draft.limit == 0) "" else draft.limit.toString(),
                        onValueChange = { value ->
                            val digits = value.filter { it.isDigit() }.take(4)
                            draft = draft.copy(limit = digits.toIntOrNull() ?: 0)
                        },
                        placeholder = "Unlimited",
                        helperText = "Leave empty for every matching track",
                        modifier = Modifier.width(220.dp)
                    )
                }
            }
        }
    }

    if (fieldPickerIndex >= 0) {
        val index = fieldPickerIndex
        OptionPickerDialog(
            title = "Field",
            options = SmartField.entries.map { it.label },
            selectedIndex = SmartField.entries.indexOf(draft.rules[index].field),
            onSelect = { chosen ->
                val field = SmartField.entries[chosen]
                updateRule(index) {
                    // Keep the operator only if it still applies to the new field.
                    val operator = if (it.operator in field.operators) it.operator else field.operators.first()
                    it.copy(field = field, operator = operator)
                }
                fieldPickerIndex = -1
            },
            onDismiss = { fieldPickerIndex = -1 }
        )
    }

    if (operatorPickerIndex >= 0) {
        val index = operatorPickerIndex
        val operators = draft.rules[index].field.operators
        OptionPickerDialog(
            title = "Condition",
            options = operators.map { it.label },
            selectedIndex = operators.indexOf(draft.rules[index].operator),
            onSelect = { chosen ->
                updateRule(index) { it.copy(operator = operators[chosen]) }
                operatorPickerIndex = -1
            },
            onDismiss = { operatorPickerIndex = -1 }
        )
    }

    if (confirmDelete) {
        ConfirmDialog(
            icon = Icons.Rounded.DeleteOutline,
            title = "Delete smart playlist",
            message = "\"${draft.name}\" will be removed. Your audio files are not affected.",
            confirmLabel = "Delete",
            onConfirm = {
                confirmDelete = false
                onDelete(draft.id)
            },
            onDismiss = { confirmDelete = false }
        )
    }
}

/** Simple single-choice dialog built from the design system chips. */
@Composable
private fun OptionPickerDialog(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AuralisDialog(onDismiss = onDismiss) { dismiss ->
        val colors = AuralisTheme.colors
        val spacing = AuralisTheme.spacing
        Text(
            text = title,
            style = AuralisType.title,
            color = colors.textPrimary,
            modifier = Modifier.fillMaxWidth()
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            options.forEachIndexed { index, label ->
                AuralisChip(
                    label = label,
                    selected = index == selectedIndex,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onSelect(index)
                        dismiss()
                    }
                )
            }
        }
    }
}

/** Row shown in the playlists list for one user-defined smart playlist. */
@Composable
fun SmartPlaylistRow(
    name: String,
    trackCount: Int,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing
    com.auralis.player.ui.components.PressableSurface(
        onClick = onOpen,
        onLongClick = onEdit,
        modifier = modifier
    ) {
        GlassPanel(accentWash = true, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.md)
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(22.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = localizedStyle(AuralisType.body, name),
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$trackCount tracks • rule based",
                        style = AuralisType.bodySmall,
                        color = colors.textSecondary
                    )
                }
                AccentIconButton(
                    icon = Icons.Rounded.Edit,
                    contentDescription = "Edit $name",
                    size = 38.dp
                ) { onEdit() }
                AccentIconButton(
                    icon = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete $name",
                    size = 38.dp
                ) { onDelete() }
            }
        }
    }
}
