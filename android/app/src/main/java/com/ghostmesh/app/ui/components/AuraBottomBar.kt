package com.ghostmesh.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostmesh.app.ui.theme.AuraTheme

/**
 * Strictly the 3 core tabs of Sync:
 * 1. Radar (Real-world proximity discovery)
 * 2. Discovered (Persistent encounters feed & incoming requests)
 * 3. Profile (User profile, double-verified credentials & settings)
 */
enum class AuraTab(val label: String, val icon: ImageVector) {
    RADAR("Radar", Icons.Outlined.Radar),
    DISCOVERED("Discovered", Icons.Outlined.NearMe),
    PROFILE("Profile", Icons.Outlined.Person)
}

typealias SyncTab = AuraTab

@Composable
fun AuraBottomBar(
    currentTab: AuraTab,
    onTabSelected: (AuraTab) -> Unit,
    pendingRequestsCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(AuraTheme.colors.border)
        )
        NavigationBar(
            containerColor = AuraTheme.colors.surface,
            contentColor = AuraTheme.colors.inkSecondary,
            tonalElevation = 0.dp
        ) {
        AuraTab.entries.forEach { tab ->
            val isSelected = currentTab == tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    if (tab == AuraTab.DISCOVERED && pendingRequestsCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = AuraTheme.colors.accent,
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = pendingRequestsCount.toString(),
                                        fontFamily = com.ghostmesh.app.ui.theme.DmMonoFontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                label = {
                    Text(
                        text = tab.label,
                        fontFamily = com.ghostmesh.app.ui.theme.DmSansFontFamily,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AuraTheme.colors.accent,
                    selectedTextColor = AuraTheme.colors.accent,
                    indicatorColor = AuraTheme.colors.accentSubtle,
                    unselectedIconColor = AuraTheme.colors.inkSecondary,
                    unselectedTextColor = AuraTheme.colors.inkSecondary
                )
            )
        }
    }
}
}
