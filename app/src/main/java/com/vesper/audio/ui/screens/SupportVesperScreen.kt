package com.vesper.audio.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vesper.audio.ui.theme.LocalVesperAccentColor
import com.vesper.audio.ui.theme.VesperBackground
import com.vesper.audio.ui.theme.VesperBurgundyAccent
import com.vesper.audio.ui.theme.VesperBurgundyHighlight
import com.vesper.audio.ui.theme.VesperDeepBurgundy
import com.vesper.audio.ui.theme.VesperSurface
import com.vesper.audio.ui.theme.VesperSurfaceElevated
import com.vesper.audio.ui.theme.VesperTextMuted
import com.vesper.audio.ui.theme.VesperTextPrimary
import com.vesper.audio.ui.theme.VesperTextSecondary
import com.vesper.audio.ui.theme.VesperTypography
import kotlinx.coroutines.launch

private data class ContributionTier(
    val amount: String,
    val title: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportVesperScreen(
    onBack: () -> Unit,
    isSupporter: Boolean = false,
    onSupporterStatusToggled: (Boolean) -> Unit = {}
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val accentColor = LocalVesperAccentColor.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val tiers = remember {
        listOf(
            ContributionTier("₹49", "A little love", Icons.Outlined.Coffee),
            ContributionTier("₹149", "Indie supporter", Icons.Outlined.FavoriteBorder),
            ContributionTier("₹499", "Founding patron", Icons.Outlined.WorkspacePremium)
        )
    }

    var selectedTierIndex by remember { mutableStateOf(1) } // Default to ₹149 as in screenshot
    var customAmount by remember { mutableStateOf<String?>(null) }
    var showCustomAmountDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = VesperBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Ambient wave backdrop on top
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
            ) {
                val width = size.width
                val height = size.height

                // Wave 1
                val path1 = Path().apply {
                    moveTo(0f, height * 0.35f)
                    cubicTo(
                        width * 0.25f, height * 0.15f,
                        width * 0.7f, height * 0.6f,
                        width, height * 0.3f
                    )
                    lineTo(width, 0f)
                    lineTo(0f, 0f)
                    close()
                }
                drawPath(
                    path = path1,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            VesperBurgundyAccent.copy(alpha = 0.28f),
                            Color.Transparent
                        )
                    )
                )

                // Wave 2
                val path2 = Path().apply {
                    moveTo(0f, height * 0.5f)
                    cubicTo(
                        width * 0.35f, height * 0.7f,
                        width * 0.75f, height * 0.25f,
                        width, height * 0.45f
                    )
                    lineTo(width, 0f)
                    lineTo(0f, 0f)
                    close()
                }
                drawPath(
                    path = path2,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            VesperBurgundyHighlight.copy(alpha = 0.2f),
                            Color.Transparent
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(width, height)
                    )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(
                        top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                        bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp
                    )
            ) {
                // Top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("support_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = VesperTextPrimary
                        )
                    }
                    Text(
                        text = "Support VESPER",
                        style = VesperTypography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = VesperTextPrimary
                        ),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Centered Logo & Branding
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Circular Vesper Logo Badge
                    Surface(
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape,
                        color = VesperDeepBurgundy.copy(alpha = 0.6f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, VesperBurgundyAccent.copy(alpha = 0.4f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Canvas(modifier = Modifier.size(36.dp)) {
                                val r = size.minDimension / 2f
                                val center = Offset(size.width / 2f, size.height / 2f)

                                // Crescent moon in gold/champagne
                                drawCircle(
                                    color = Color(0xFFF5D6A0),
                                    radius = r,
                                    center = center
                                )
                                drawCircle(
                                    color = VesperDeepBurgundy,
                                    radius = r * 0.85f,
                                    center = Offset(center.x + r * 0.35f, center.y - r * 0.15f)
                                )

                                // Audio wave bars
                                val barColor = Color(0xFFF5D6A0)
                                val barWidth = 2.5f
                                val bars = listOf(0.4f, 0.75f, 1.0f, 0.6f, 0.35f)
                                val startX = center.x - (bars.size * barWidth * 2.5f) / 2f
                                bars.forEachIndexed { i, h ->
                                    val x = startX + i * barWidth * 2.5f
                                    val barH = r * h * 0.8f
                                    drawLine(
                                        color = barColor,
                                        start = Offset(x, center.y - barH / 2f),
                                        end = Offset(x, center.y + barH / 2f),
                                        strokeWidth = barWidth
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "V E S P E R",
                        style = VesperTypography.labelMedium.copy(
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 4.sp,
                            color = VesperTextSecondary.copy(alpha = 0.8f)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Keep independent music moving.",
                        style = VesperTypography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = VesperTextPrimary,
                            textAlign = TextAlign.Center
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Your support helps us build a private, ad-free music player and make independent artists easier to discover.",
                        style = VesperTypography.bodyMedium.copy(
                            color = VesperTextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // "Your music stays yours" Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = VesperSurface.copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VesperBurgundyAccent.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = VesperDeepBurgundy.copy(alpha = 0.8f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFFF5A0B8),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Your music stays yours",
                                style = VesperTypography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = VesperTextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "No account required. No tracking. No ads. Your contribution does not buy access to anyone's copyrighted catalogue.",
                                style = VesperTypography.bodySmall.copy(
                                    color = VesperTextSecondary,
                                    lineHeight = 17.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Section 1: Choose a contribution
                SectionHeader(title = "Choose a contribution")

                Spacer(modifier = Modifier.height(14.dp))

                // 3 Tiers Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    tiers.forEachIndexed { index, tier ->
                        val isSelected = selectedTierIndex == index && customAmount == null
                        val displayAmount = if (isSelected && customAmount != null) "₹$customAmount" else tier.amount

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(115.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    selectedTierIndex = index
                                    customAmount = null
                                }
                                .testTag("contribution_tier_$index"),
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) VesperDeepBurgundy.copy(alpha = 0.65f) else VesperSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.8.dp else 1.dp,
                                color = if (isSelected) accentColor else Color.White.copy(alpha = 0.08f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 14.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = displayAmount,
                                    style = VesperTypography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = VesperTextPrimary
                                    )
                                )
                                Text(
                                    text = tier.title,
                                    style = VesperTypography.labelSmall.copy(
                                        color = VesperTextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                )
                                Icon(
                                    imageVector = tier.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) accentColor else VesperTextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // "Choose another amount" button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showCustomAmountDialog = true }
                            .testTag("choose_another_amount_button"),
                        shape = RoundedCornerShape(12.dp),
                        color = VesperSurface.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (customAmount != null) accentColor else Color.White.copy(alpha = 0.08f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = null,
                                tint = if (customAmount != null) accentColor else VesperTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (customAmount != null) "Custom amount: ₹$customAmount" else "Choose another amount",
                                style = VesperTypography.labelMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = if (customAmount != null) accentColor else VesperTextSecondary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Section 2: What your support helps fund
                SectionHeader(title = "What your support helps fund")

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FundingBenefitCard(
                        icon = Icons.Outlined.Headphones,
                        title = "Better listening",
                        description = "Performance, smoother playback, richer artwork and improved accessibility."
                    )
                    FundingBenefitCard(
                        icon = Icons.Outlined.People,
                        title = "Independent artists",
                        description = "Responsible music discovery, proper attribution and links to original artist pages."
                    )
                    FundingBenefitCard(
                        icon = Icons.Outlined.Spa,
                        title = "Privacy first",
                        description = "Build and maintain on-device features with no unnecessary data collection."
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Disclaimer footnote
                Text(
                    text = "Contributions are voluntary. Core local playback stays free.\nSupporter extras are optional and never bypass artist licences.",
                    style = VesperTypography.bodySmall.copy(
                        color = VesperTextMuted.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                        lineHeight = 17.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Main CTA Button: Continue to contribute
                Button(
                    onClick = {
                        val selectedAmount = customAmount ?: tiers.getOrNull(selectedTierIndex)?.amount ?: "₹149"
                        onSupporterStatusToggled(true)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Thank you for supporting VESPER with $selectedAmount! Supporter features activated."
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(54.dp)
                        .testTag("continue_to_contribute_button"),
                    shape = RoundedCornerShape(27.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VesperBurgundyAccent
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Continue to contribute",
                        style = VesperTypography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Secondary CTA Button: Maybe later
                TextButton(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .testTag("maybe_later_button")
                ) {
                    Text(
                        text = "Maybe later",
                        style = VesperTypography.bodyMedium.copy(
                            color = VesperTextSecondary
                        )
                    )
                }
            }
        }
    }

    // Custom Amount Dialog
    if (showCustomAmountDialog) {
        var inputVal by remember { mutableStateOf(customAmount ?: "") }
        AlertDialog(
            onDismissRequest = { showCustomAmountDialog = false },
            containerColor = VesperSurfaceElevated,
            title = {
                Text(
                    text = "Choose Contribution Amount",
                    style = VesperTypography.titleMedium.copy(color = VesperTextPrimary, fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter any amount in Indian Rupees (₹):",
                        style = VesperTypography.bodyMedium.copy(color = VesperTextSecondary),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = inputVal,
                        onValueChange = { inputVal = it.filter { ch -> ch.isDigit() } },
                        leadingIcon = { Text("₹", style = VesperTypography.titleMedium.copy(color = VesperTextPrimary)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = VesperTextPrimary,
                            unfocusedTextColor = VesperTextPrimary,
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("custom_amount_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val num = inputVal.toIntOrNull()
                        if (num != null && num > 0) {
                            customAmount = num.toString()
                        }
                        showCustomAmountDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("Set Amount", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomAmountDialog = false }) {
                    Text("Cancel", color = VesperTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(LocalVesperAccentColor.current)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            style = VesperTypography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = VesperTextPrimary
            )
        )
    }
}

@Composable
private fun FundingBenefitCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = VesperSurface.copy(alpha = 0.8f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = VesperDeepBurgundy.copy(alpha = 0.6f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color(0xFFF5A0B8),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = VesperTypography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = VesperTextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = description,
                    style = VesperTypography.bodySmall.copy(
                        color = VesperTextSecondary,
                        lineHeight = 17.sp
                    )
                )
            }
        }
    }
}
