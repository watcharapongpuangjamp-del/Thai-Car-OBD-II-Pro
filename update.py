import re

with open('./app/src/main/java/com/example/ui/ThaiObdApp.kt', 'r') as f:
    content = f.read()

# Add imports
content = content.replace(
    "import com.example.ui.screens.*",
    "import androidx.compose.material.icons.filled.Info\nimport com.example.ui.screens.*\nimport com.example.ui.screens.developer.DeveloperStoryScreen"
)

# Add Screen.DeveloperStory
content = content.replace(
    'object Profile : Screen("profile", "ข้อมูลรถ", Icons.Default.DirectionsCar)\n}',
    'object Profile : Screen("profile", "ข้อมูลรถ", Icons.Default.DirectionsCar)\n    object DeveloperStory : Screen("developer_story", "เรื่องราวนักพัฒนา", Icons.Default.Info)\n}'
)

# Add TopAppBar action
topbar_original = '''                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },'''
topbar_new = '''                actions = {
                    IconButton(onClick = { currentScreen = Screen.DeveloperStory }) {
                        Icon(Icons.Default.Info, contentDescription = "Developer Story", tint = CyanPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },'''
content = content.replace(topbar_original, topbar_new)

# Add Screen handling
screen_handling_original = '''                        viewModel.addMaintenanceLog(title, cost, mileage, category)
                    }
                )
            }
        }
    }
}'''
screen_handling_new = '''                        viewModel.addMaintenanceLog(title, cost, mileage, category)
                    }
                )
                Screen.DeveloperStory -> DeveloperStoryScreen(
                    onNavigateBack = { currentScreen = Screen.Dashboard }
                )
            }
        }
    }
}'''
content = content.replace(screen_handling_original, screen_handling_new)

with open('./app/src/main/java/com/example/ui/ThaiObdApp.kt', 'w') as f:
    f.write(content)
