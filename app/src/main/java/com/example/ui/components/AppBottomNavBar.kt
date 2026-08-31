package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandPrimaryVariant
import com.example.ui.theme.appColors

enum class NavScreen(val route: String) {
    HOME("home"),
    TRANSACTIONS("transactions"),
    STATISTICS("statistics"),
    MENU("menu")
}

@Composable
fun AppBottomNavBar(
    currentScreen: NavScreen,
    onNavigate: (NavScreen) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val barColor = MaterialTheme.colorScheme.surface
    val fabBorderColor = barColor

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Bottom Bar Surface
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = barColor,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 6.dp)
                    .height(60.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home Tab
                EditorialBottomNavItem(
                    icon = Icons.Outlined.Home,
                    label = stringResource(R.string.nav_home),
                    isSelected = currentScreen == NavScreen.HOME,
                    onClick = { onNavigate(NavScreen.HOME) },
                    testTag = "nav_home_button",
                    modifier = Modifier.weight(1f)
                )

                // Transactions Tab
                EditorialBottomNavItem(
                    icon = Icons.Outlined.ReceiptLong,
                    label = stringResource(R.string.nav_transactions),
                    isSelected = currentScreen == NavScreen.TRANSACTIONS,
                    onClick = { onNavigate(NavScreen.TRANSACTIONS) },
                    testTag = "nav_transactions_button",
                    modifier = Modifier.weight(1f)
                )

                // Center placeholder space for the elevated FAB
                Spacer(modifier = Modifier.weight(1.1f))

                // Statistics Tab
                EditorialBottomNavItem(
                    icon = Icons.Outlined.BarChart,
                    label = stringResource(R.string.nav_statistics),
                    isSelected = currentScreen == NavScreen.STATISTICS,
                    onClick = { onNavigate(NavScreen.STATISTICS) },
                    testTag = "nav_statistics_button",
                    modifier = Modifier.weight(1f)
                )

                // Menu Tab
                EditorialBottomNavItem(
                    icon = Icons.Outlined.GridView,
                    label = stringResource(R.string.nav_menu),
                    isSelected = currentScreen == NavScreen.MENU,
                    onClick = { onNavigate(NavScreen.MENU) },
                    testTag = "nav_menu_button",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Clean Elevated Center Add Button
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-18).dp)
        ) {
            Surface(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .testTag("nav_add_button"),
                shape = CircleShape,
                color = MaterialTheme.appColors.brand,
                border = BorderStroke(3.5.dp, fabBorderColor),
                shadowElevation = 6.dp,
                onClick = onAddClick
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    BrandPrimaryVariant,
                                    BrandPrimary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_transaction),
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorialBottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.appColors.isDark
    val activeColor = MaterialTheme.appColors.brand
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

    val contentColor = if (isSelected) activeColor else inactiveColor
    val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .padding(vertical = 2.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (isSelected) activeColor.copy(alpha = if (isDark) 0.22f else 0.15f)
                    else Color.Transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = fontWeight,
            fontSize = 11.sp,
            color = contentColor,
            maxLines = 1
        )
    }
}
