package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.LumenViewModel
import com.example.ui.ScreenDestination
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ProgressScreen
import com.example.ui.screens.QuizScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudyScreen
import com.example.ui.theme.LumenTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: LumenViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()
            val accentColor by viewModel.accentColor.collectAsState()

            LumenTheme(
                themeMode = themeMode,
                accentColor = accentColor
            ) {
                LumenApp(viewModel = viewModel)
            }
        }
    }
}

data class BottomNavItem(
    val label: String,
    val destination: ScreenDestination,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

@Composable
fun LumenApp(viewModel: LumenViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    val navItems = listOf(
        BottomNavItem("Home", ScreenDestination.Home, Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
        BottomNavItem("Ask", ScreenDestination.Chat, Icons.Filled.Chat, Icons.Outlined.Chat, "nav_chat"),
        BottomNavItem("Study", ScreenDestination.Study, Icons.Filled.MenuBook, Icons.Outlined.MenuBook, "nav_study"),
        BottomNavItem("Quiz", ScreenDestination.Quiz, Icons.Filled.Quiz, Icons.Outlined.Quiz, "nav_quiz"),
        BottomNavItem("Library", ScreenDestination.Library, Icons.Filled.Style, Icons.Outlined.Style, "nav_library"),
        BottomNavItem("Progress", ScreenDestination.Progress, Icons.Filled.TrendingUp, Icons.Outlined.TrendingUp, "nav_progress")
    )

    val showBottomBar = currentScreen !is ScreenDestination.Onboarding

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    navItems.forEach { item ->
                        val isSelected = currentScreen::class == item.destination::class
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { viewModel.navigateTo(item.destination) },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag(item.tag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ScreenTransition"
            ) { targetScreen ->
                when (targetScreen) {
                    is ScreenDestination.Onboarding -> OnboardingScreen(viewModel = viewModel)
                    is ScreenDestination.Home -> HomeScreen(viewModel = viewModel)
                    is ScreenDestination.Chat -> ChatScreen(viewModel = viewModel)
                    is ScreenDestination.Study -> StudyScreen(viewModel = viewModel)
                    is ScreenDestination.Quiz -> QuizScreen(viewModel = viewModel)
                    is ScreenDestination.Library -> LibraryScreen(viewModel = viewModel)
                    is ScreenDestination.Progress -> ProgressScreen(viewModel = viewModel)
                    is ScreenDestination.Settings -> SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
