package com.hrshd1eux.attendly.ui.simulator

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hrshd1eux.attendly.core.model.TimeFormat
import com.hrshd1eux.attendly.core.util.TimeUtils
import com.hrshd1eux.attendly.domain.calculator.AttendanceCalculator
import com.hrshd1eux.attendly.domain.model.ScenarioSimulationResult
import com.hrshd1eux.attendly.ui.theme.LocalStatusColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BunkSimulatorScreen(
    onNavigateBack: () -> Unit,
    viewModel: BunkSimulatorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val statusColors = LocalStatusColors.current
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "What-If Bunk Simulator",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::resetSimulation) {
                        Icon(imageVector = Icons.Default.RestartAlt, contentDescription = "Reset")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode Tabs: Quick vs Fest Planner
            item {
                TabRow(
                    selectedTabIndex = if (uiState.isFestMode) 1 else 0,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = !uiState.isFestMode,
                        onClick = { viewModel.setFestMode(false) },
                        text = { Text("Quick Bunk Meter", fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = uiState.isFestMode,
                        onClick = { viewModel.setFestMode(true) },
                        text = { Text("Fest & Leave Planner", fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            // Subject Filter Row
            item {
                Column {
                    Text(
                        text = "Select Target Subject",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = uiState.selectedSubjectId == null,
                                onClick = { viewModel.selectSubject(null) },
                                label = { Text("Overall Average") }
                            )
                        }
                        items(uiState.subjects) { sub ->
                            FilterChip(
                                selected = uiState.selectedSubjectId == sub.id,
                                onClick = { viewModel.selectSubject(sub.id) },
                                label = { Text(sub.name) }
                            )
                        }
                    }
                }
            }

            // Simulation Projection Card
            item {
                uiState.simulationResult?.let { sim ->
                    ProjectionDashboardCard(simulation = sim)
                }
            }

            // Interactive Controls
            if (!uiState.isFestMode) {
                // Quick Counter Mode
                item {
                    QuickCounterControls(
                        additionalBunked = uiState.additionalBunked,
                        additionalAttended = uiState.additionalAttended,
                        onAddBunk = { viewModel.addBunks(it) },
                        onAddAttended = { viewModel.addAttended(it) },
                        onReset = viewModel::resetSimulation
                    )
                }
            } else {
                // Fest & Leave Planner Mode
                item {
                    FestPlannerControls(
                        festStartDate = uiState.festStartDate,
                        festEndDate = uiState.festEndDate,
                        festClasses = uiState.festClasses,
                        onDateRangeChanged = { s, e -> viewModel.setFestDateRange(s, e) },
                        onToggleClass = { date, subId, min -> viewModel.toggleFestClass(date, subId, min) },
                        onToggleAll = { viewModel.toggleAllFest(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectionDashboardCard(
    simulation: ScenarioSimulationResult
) {
    val statusColors = LocalStatusColors.current
    val isDebar = simulation.isDebarRisk
    val delta = simulation.deltaPercentage

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDebar) {
                statusColors.absentContainer.copy(alpha = 0.7f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Projected Standing",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Status Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDebar) statusColors.absent else statusColors.present)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isDebar) "⚠️ DEBAR RISK" else "✓ SAFE",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Percentage Comparison
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Current",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = simulation.currentSummary.formattedPercentage,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "➔",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Projected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = simulation.projectedSummary.formattedPercentage,
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = if (isDebar) statusColors.absent else statusColors.present
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (delta >= 0) "+${AttendanceCalculator.formatPercentage(delta)}" else AttendanceCalculator.formatPercentage(delta),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (delta < 0) statusColors.absent else statusColors.present
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Advice / Recovery Insight Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isDebar) Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (isDebar) statusColors.absent else statusColors.present,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    val message = if (isDebar && simulation.recoveryClassesNeeded != null) {
                        "Dropping to this percentage requires attending ${simulation.recoveryClassesNeeded} classes in a row to reach ${simulation.projectedSummary.targetPercentage}% again."
                    } else if (simulation.projectedSummary.safeAbsences > 0) {
                        "You still have ${simulation.projectedSummary.safeAbsences} safe absences remaining above your target."
                    } else {
                        simulation.projectedSummary.statusMessage
                    }
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickCounterControls(
    additionalBunked: Int,
    additionalAttended: Int,
    onAddBunk: (Int) -> Unit,
    onAddAttended: (Int) -> Unit,
    onReset: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Simulate Attendance",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            // Bunks Counter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Classes to Bunk / Skip", fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "Simulate missing upcoming lectures",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onAddBunk(-1) },
                        enabled = additionalBunked > 0,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "$additionalBunked",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.width(28.dp),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(
                        onClick = { onAddBunk(1) },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Attended Counter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Classes to Attend", fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "Simulate future classes attended",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onAddAttended(-1) },
                        enabled = additionalAttended > 0,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "$additionalAttended",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.width(28.dp),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(
                        onClick = { onAddAttended(1) },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Quick Shortcut Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onAddBunk(1) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("+1 Bunk", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = { onAddBunk(3) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("+3 Bunk", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = { onAddAttended(5) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("+5 Attend", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun FestPlannerControls(
    festStartDate: LocalDate,
    festEndDate: LocalDate,
    festClasses: List<SimulatorFestClass>,
    onDateRangeChanged: (LocalDate, LocalDate) -> Unit,
    onToggleClass: (LocalDate, Long, Int) -> Unit,
    onToggleAll: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d")

    fun pickDate(initial: LocalDate, onPicked: (LocalDate) -> Unit) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                onPicked(LocalDate.of(year, month + 1, dayOfMonth))
            },
            initial.year,
            initial.monthValue - 1,
            initial.dayOfMonth
        ).show()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "College Fest / Absence Date Window",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        pickDate(festStartDate) { newStart ->
                            onDateRangeChanged(newStart, festEndDate)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("From: ${festStartDate.format(dateFormatter)}")
                }

                OutlinedButton(
                    onClick = {
                        pickDate(festEndDate) { newEnd ->
                            onDateRangeChanged(festStartDate, newEnd)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("To: ${festEndDate.format(dateFormatter)}")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${festClasses.size} scheduled classes in window",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row {
                    TextButton(onClick = { onToggleAll(true) }) {
                        Text("Skip All", fontSize = 12.sp)
                    }
                    TextButton(onClick = { onToggleAll(false) }) {
                        Text("Attend All", fontSize = 12.sp)
                    }
                }
            }

            if (festClasses.isEmpty()) {
                Text(
                    text = "No classes found in timetable for this date window.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                festClasses.take(15).forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .clickable {
                                onToggleClass(item.date, item.subjectId, item.startMinute)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.subjectName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${item.date.format(dateFormatter)} (${item.dayOfWeek.shortName}) • ${TimeUtils.minutesToTimeString(item.startMinute, TimeFormat.FORMAT_12H)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Checkbox(
                            checked = item.isBunked,
                            onCheckedChange = {
                                onToggleClass(item.date, item.subjectId, item.startMinute)
                            }
                        )
                    }
                }
            }
        }
    }
}
