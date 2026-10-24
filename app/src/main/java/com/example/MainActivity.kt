package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.database.AppDatabase
import com.example.data.repository.TruckRepository
import com.example.ui.TruckViewModel
import com.example.ui.TruckViewModelFactory
import com.example.ui.components.DimensionsCalculatorDialog
import com.example.ui.components.ExportBackupDialog
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.NewEntryScreen
import com.example.ui.screens.RecordsListScreen
import com.example.ui.screens.SmsBroadcastScreen
import com.example.ui.screens.StatisticsScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val viewModel: TruckViewModel by viewModels {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = TruckRepository(database.truckEntryDao())
        TruckViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                // Set Arabic RTL Layout Direction
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    TruckCubicApp(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TruckCubicApp(
    viewModel: TruckViewModel,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val entries by viewModel.entries.collectAsState()
    val totalCount = entries.size

    val snackbarHostState = remember { SnackbarHostState() }

    // Handle back button: return to home tab if on secondary tab
    BackHandler(enabled = selectedTab != 0) {
        viewModel.onTabSelect(0)
    }

    // Listen to UI messages and display in snackbar
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collectLatest { event ->
            snackbarHostState.showSnackbar(event.message)
        }
    }

    // Include the Dimension Calculator Dialog
    DimensionsCalculatorDialog(viewModel = viewModel)

    // Include the Export and Backup Dialog
    ExportBackupDialog(viewModel = viewModel)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 720.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalShipping,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "تكعيب الغضب",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "حساب وتوثيق سعة السيارات والشاحنات",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.onTabSelect(2) },
                            modifier = Modifier.testTag("top_bar_sms_broadcast_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sms,
                                contentDescription = "رسائل SMS والأسعار",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = { viewModel.showExportBackupSheet.value = true },
                            modifier = Modifier.testTag("top_bar_export_backup_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "تصدير ونسخ احتياطي",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.testTag("bottom_nav_bar")
                    ) {
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { viewModel.onTabSelect(0) },
                            icon = { Icon(Icons.Default.AddBox, contentDescription = "إدخال جديد") },
                            label = { Text("إدخال") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("nav_tab_new_entry")
                        )

                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { viewModel.onTabSelect(1) },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (totalCount > 0) {
                                            Badge { Text("$totalCount") }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.FormatListNumbered, contentDescription = "سجل السيارات")
                                }
                            },
                            label = { Text("السجل") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("nav_tab_records")
                        )

                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { viewModel.onTabSelect(2) },
                            icon = { Icon(Icons.Default.Sms, contentDescription = "رسائل SMS والأسعار") },
                            label = { Text("رسائل SMS") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("nav_tab_sms_broadcast")
                        )

                        NavigationBarItem(
                            selected = selectedTab == 3,
                            onClick = { viewModel.onTabSelect(3) },
                            icon = { Icon(Icons.Default.Analytics, contentDescription = "الإحصائيات") },
                            label = { Text("إحصائيات") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("nav_tab_statistics")
                        )

                        NavigationBarItem(
                            selected = selectedTab == 4,
                            onClick = { viewModel.onTabSelect(4) },
                            icon = { Icon(Icons.Default.Info, contentDescription = "حول التطبيق") },
                            label = { Text("حول") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("nav_tab_about")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Adaptive Side Navigation Rail for tablets/wide screens
                if (isWideScreen) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        NavigationRailItem(
                            selected = selectedTab == 0,
                            onClick = { viewModel.onTabSelect(0) },
                            icon = { Icon(Icons.Default.AddBox, contentDescription = "إدخال جديد") },
                            label = { Text("إدخال") }
                        )
                        NavigationRailItem(
                            selected = selectedTab == 1,
                            onClick = { viewModel.onTabSelect(1) },
                            icon = {
                                BadgedBox(badge = { if (totalCount > 0) Badge { Text("$totalCount") } }) {
                                    Icon(Icons.Default.FormatListNumbered, contentDescription = "سجل السيارات")
                                }
                            },
                            label = { Text("السجل") }
                        )
                        NavigationRailItem(
                            selected = selectedTab == 2,
                            onClick = { viewModel.onTabSelect(2) },
                            icon = { Icon(Icons.Default.Sms, contentDescription = "رسائل SMS والأسعار") },
                            label = { Text("رسائل SMS") }
                        )
                        NavigationRailItem(
                            selected = selectedTab == 3,
                            onClick = { viewModel.onTabSelect(3) },
                            icon = { Icon(Icons.Default.Analytics, contentDescription = "الإحصائيات") },
                            label = { Text("إحصائيات") }
                        )
                        NavigationRailItem(
                            selected = selectedTab == 4,
                            onClick = { viewModel.onTabSelect(4) },
                            icon = { Icon(Icons.Default.Info, contentDescription = "حول التطبيق") },
                            label = { Text("حول") }
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                // Tab Content with Smooth Transitions
                Box(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "tab_content_transition"
                    ) { tab ->
                        when (tab) {
                            0 -> NewEntryScreen(viewModel = viewModel)
                            1 -> RecordsListScreen(viewModel = viewModel)
                            2 -> SmsBroadcastScreen(viewModel = viewModel)
                            3 -> StatisticsScreen(viewModel = viewModel)
                            4 -> AboutScreen()
                        }
                    }
                }
            }
        }
    }
}
