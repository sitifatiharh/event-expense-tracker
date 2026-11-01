package com.example.notacara.ui.components

import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeTopBar(
    isSelectionMode: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    dateFilter: String?,
    onDateClick: () -> Unit,
    onClearDate: () -> Unit,
    onMenuClick: () -> Unit,
    isMenuExpanded: Boolean,
    onDismissMenu: () -> Unit,
    onSelectItemsClick: () -> Unit,
    onHelpClick: () -> Unit,
    selectedCount: Int,
    onCloseSelection: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onBack: () -> Unit = {},
    onClearSearch: () -> Unit = {},
    enabled: Boolean = true,
    menuLabel: String = "Pilih Acara",
    onSearchFocused: (Boolean) -> Unit = {},
    onSearchPositioned: (LayoutCoordinates) -> Unit = {},
    onDatePositioned: (LayoutCoordinates) -> Unit = {},
    onMenuPositioned: (LayoutCoordinates) -> Unit = {}
) {
    Surface(
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.statusBarsPadding()) {
            if (isSelectionMode) {
                ContextualTopBar(
                    selectedCount = selectedCount,
                    onClose = onCloseSelection,
                    onEdit = onEditClick,
                    onDelete = onDeleteClick
                )
            } else {
                NormalTopBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = onSearchQueryChange,
                    dateFilter = dateFilter,
                    onDateClick = onDateClick,
                    onClearDate = onClearDate,
                    onMenuClick = onMenuClick,
                    isMenuExpanded = isMenuExpanded,
                    onDismissMenu = onDismissMenu,
                    onSelectItemsClick = onSelectItemsClick,
                    onHelpClick = onHelpClick,
                    onBack = onBack,
                    onClearSearch = onClearSearch,
                    enabled = enabled,
                    menuLabel = menuLabel,
                    onSearchFocused = onSearchFocused,
                    onSearchPositioned = onSearchPositioned,
                    onDatePositioned = onDatePositioned,
                    onMenuPositioned = onMenuPositioned
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NormalTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    dateFilter: String?,
    onDateClick: () -> Unit,
    onClearDate: () -> Unit,
    onMenuClick: () -> Unit,
    isMenuExpanded: Boolean,
    onDismissMenu: () -> Unit,
    onSelectItemsClick: () -> Unit,
    onHelpClick: () -> Unit,
    onBack: () -> Unit,
    onClearSearch: () -> Unit,
    enabled: Boolean,
    menuLabel: String,
    onSearchFocused: (Boolean) -> Unit,
    onSearchPositioned: (LayoutCoordinates) -> Unit,
    onDatePositioned: (LayoutCoordinates) -> Unit,
    onMenuPositioned: (LayoutCoordinates) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, start = 16.dp, end = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .onGloballyPositioned { onSearchPositioned(it) },
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier.weight(1f)
                ) {
                    if (searchQuery.isEmpty()) {
                        Text("Cari", color = Color.Gray, fontSize = 16.sp)
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        textStyle = TextStyle(fontSize = 16.sp),
                        singleLine = true,
                        enabled = enabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_bar")
                            .focusProperties { canFocus = enabled }
                            .onFocusChanged { onSearchFocused(it.isFocused) }
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Clear Search",
                        tint = Color.Gray,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(20.dp)
                            .clickable { onClearSearch() }
                    )
                }
            }
        }

        if (dateFilter == null) {
            OutlinedButton(
                onClick = onDateClick,
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)),
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier
                    .height(44.dp)
                    .onGloballyPositioned { onDatePositioned(it) },
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = Color.Gray)
            ) {
                Icon(Icons.Default.DateRange, null, modifier = Modifier.size(18.dp), tint = Color.Gray)
                Spacer(Modifier.width(6.dp))
                Text("Tanggal", fontSize = 14.sp, color = Color.Gray)
            }
        } else {
            InputChip(
                selected = true,
                onClick = onDateClick,
                label = { Text(dateFilter, fontSize = 12.sp) },
                trailingIcon = {
                    Icon(
                        Icons.Default.Close, null, modifier = Modifier.size(18.dp).clickable { onClearDate() }
                    )
                },
                leadingIcon = { Icon(Icons.Default.DateRange, null, Modifier.size(18.dp)) },
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.onGloballyPositioned { onDatePositioned(it) },
                colors = InputChipDefaults.inputChipColors(selectedContainerColor = Color.White, selectedLabelColor = Color.Black),
                border = InputChipDefaults.inputChipBorder(enabled = true, selected = true, selectedBorderColor = Color(0xFFE0E0E0))
            )
        }

        Box(modifier = Modifier.onGloballyPositioned { onMenuPositioned(it) }) {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.MoreVert, contentDescription = "Menu")
            }
            DropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = onDismissMenu,
                modifier = Modifier.background(Color.White)
            ) {
                DropdownMenuItem(
                    text = { Text(menuLabel) },
                    onClick = {
                        onDismissMenu()
                        onSelectItemsClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Bantuan") },
                    onClick = {
                        onDismissMenu()
                        onHelpClick()
                    }
                )
            }
        }
    }
}

@Composable
fun ContextualTopBar(
    selectedCount: Int,
    onClose: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Close") }
        Text(text = selectedCount.toString(), fontSize = 22.sp, modifier = Modifier.weight(1f).padding(start = 12.dp))
        OutlinedButton(
            onClick = onEdit,
            enabled = selectedCount == 1,
            modifier = Modifier.padding(end = 8.dp),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
        ) { Text("Edit") }
        OutlinedButton(
            onClick = onDelete,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red)
        ) { Text("Hapus") }
    }
}
