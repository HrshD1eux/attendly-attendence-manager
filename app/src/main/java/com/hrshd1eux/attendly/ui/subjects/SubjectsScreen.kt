package com.hrshd1eux.attendly.ui.subjects

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hrshd1eux.attendly.core.model.AttendanceStatus
import com.hrshd1eux.attendly.ui.components.AttendlyTopBar
import com.hrshd1eux.attendly.ui.components.SubjectAttendanceCard
import com.hrshd1eux.attendly.ui.theme.LocalStatusColors
import java.util.Locale

@Composable
fun SubjectsScreen(
    onNavigateToSubjectDetail: (Long) -> Unit,
    viewModel: SubjectsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val statusColors = LocalStatusColors.current

    var isSearchVisible by remember { mutableStateOf(false) }
    var isSortMenuOpen by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AttendlyTopBar(
                title = "Subjects",
                actions = {
                    // Search toggle button
                    IconButton(onClick = {
                        isSearchVisible = !isSearchVisible
                        if (!isSearchVisible) viewModel.setSearchQuery("")
                    }) {
                        Icon(
                            imageVector = if (isSearchVisible) Icons.Default.Clear else Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }

                    // Sort menu button
                    Box {
                        IconButton(onClick = { isSortMenuOpen = true }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                        }

                        DropdownMenu(
                            expanded = isSortMenuOpen,
                            onDismissRequest = { isSortMenuOpen = false }
                        ) {
                            SubjectSort.entries.forEach { sortOption ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = sortOption.displayName,
                                            fontWeight = if (uiState.selectedSort == sortOption) FontWeight.Bold else FontWeight.Normal,
                                            color = if (uiState.selectedSort == sortOption) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        viewModel.setSort(sortOption)
                                        isSortMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::openAddDialog,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Subject")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Expandable Search Bar
            AnimatedVisibility(
                visible = isSearchVisible,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = viewModel::setSearchQuery,
                        placeholder = { Text("Search subjects by name, code...") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            }

            // Hero Semester Overview Banner
            if (uiState.totalActiveCount > 0 && uiState.selectedFilter != SubjectFilter.ARCHIVED) {
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "SEMESTER OVERVIEW",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = String.format(Locale.US, "%.1f%%", uiState.overallAveragePercentage),
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            color = if (uiState.criticalCount > 0) statusColors.warning else statusColors.present
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Average",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.padding(bottom = 3.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${uiState.totalActiveCount} Subjects • Target: ${uiState.defaultTarget}%",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }

                            // Quick Stats Indicator Badges
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(statusColors.presentContainer)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "🟢 ${uiState.safeCount} Safe",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = statusColors.onPresentContainer
                                        )
                                    )
                                }

                                if (uiState.criticalCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(statusColors.absentContainer)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "🔴 ${uiState.criticalCount} Low",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = statusColors.onAbsentContainer
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Interactive Filter Chips Row
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = uiState.selectedFilter == SubjectFilter.ALL,
                    onClick = { viewModel.setFilter(SubjectFilter.ALL) },
                    label = { Text("All (${uiState.totalActiveCount})", fontWeight = FontWeight.SemiBold) },
                    shape = RoundedCornerShape(12.dp)
                )

                FilterChip(
                    selected = uiState.selectedFilter == SubjectFilter.SAFE,
                    onClick = { viewModel.setFilter(SubjectFilter.SAFE) },
                    label = { Text("🟢 Safe (${uiState.safeCount})", fontWeight = FontWeight.SemiBold) },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = statusColors.presentContainer,
                        selectedLabelColor = statusColors.onPresentContainer
                    )
                )

                FilterChip(
                    selected = uiState.selectedFilter == SubjectFilter.CRITICAL,
                    onClick = { viewModel.setFilter(SubjectFilter.CRITICAL) },
                    label = { Text("🔴 Attention (${uiState.criticalCount})", fontWeight = FontWeight.SemiBold) },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = statusColors.absentContainer,
                        selectedLabelColor = statusColors.onAbsentContainer
                    )
                )

                FilterChip(
                    selected = uiState.selectedFilter == SubjectFilter.ARCHIVED,
                    onClick = { viewModel.setFilter(SubjectFilter.ARCHIVED) },
                    label = { Text("📦 Archived (${uiState.archivedSubjects.size})", fontWeight = FontWeight.SemiBold) },
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main List
            if (uiState.selectedFilter == SubjectFilter.ARCHIVED) {
                // Archived Subjects
                if (uiState.filteredArchivedSubjects.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Archive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (uiState.searchQuery.isNotEmpty()) "No archived subjects match search" else "No archived subjects",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Archived subjects from finished semesters will appear here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.filteredArchivedSubjects, key = { it.id }) { subject ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = subject.name,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Target: ${subject.targetPercentage}% • Archived",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Row {
                                        IconButton(onClick = { viewModel.setArchivedStatus(subject.id, false) }) {
                                            Icon(
                                                imageVector = Icons.Default.Unarchive,
                                                contentDescription = "Unarchive",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(onClick = { viewModel.deleteSubject(subject.id) }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(72.dp)) }
                    }
                }
            } else {
                // Active Subjects
                if (uiState.filteredActiveSubjects.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Class,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (uiState.searchQuery.isNotEmpty()) "No subjects matching \"${uiState.searchQuery}\"" else if (uiState.selectedFilter == SubjectFilter.CRITICAL) "No critical subjects! Great job 🎉" else "No subjects in this category",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (uiState.totalActiveCount == 0) "Tap the + button to add your first subject." else "Try clearing your filter or search query.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(uiState.filteredActiveSubjects, key = { it.subject.id }) { detail ->
                            SubjectAttendanceCard(
                                detail = detail,
                                onClick = { onNavigateToSubjectDetail(detail.subject.id) },
                                onMarkPresent = {
                                    viewModel.markSubjectAttendance(detail.subject.id, AttendanceStatus.PRESENT)
                                },
                                onMarkAbsent = {
                                    viewModel.markSubjectAttendance(detail.subject.id, AttendanceStatus.ABSENT)
                                },
                                onMarkCancelled = {
                                    viewModel.markSubjectAttendance(detail.subject.id, AttendanceStatus.CANCELLED)
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(72.dp)) }
                    }
                }
            }
        }

        // Add/Edit Dialog
        if (uiState.isAddDialogOpen) {
            AddEditSubjectDialog(
                subjectToEdit = uiState.subjectToEdit,
                defaultTarget = uiState.defaultTarget,
                onDismiss = viewModel::closeDialog,
                onSave = viewModel::saveSubject
            )
        }
    }
}
