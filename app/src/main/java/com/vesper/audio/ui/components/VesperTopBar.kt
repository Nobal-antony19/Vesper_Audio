package com.vesper.audio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vesper.audio.ui.theme.VesperBurgundyAccent
import com.vesper.audio.ui.theme.VesperDeepBurgundy
import com.vesper.audio.ui.theme.VesperTextPrimary
import com.vesper.audio.ui.theme.VesperTypography

import androidx.compose.foundation.clickable

@Composable
fun VesperTopBar(
    title: String = "Vesper",
    onOpenDrawer: () -> Unit,
    onLogoClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = if (onLogoClick != null) Modifier.clickable(onClick = onLogoClick) else Modifier
        ) {
            // Cosmic Crescent Logo Pill
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(VesperDeepBurgundy)
                    .border(1.2.dp, com.vesper.audio.ui.theme.LocalVesperAccentColor.current, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                VesperLogo(
                    size = 24.dp,
                    animated = false
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = title,
                style = VesperTypography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = VesperTextPrimary,
                    letterSpacing = 0.5.sp
                )
            )
        }

        // Top-Right Hamburger Menu Trigger
        IconButton(
            onClick = onOpenDrawer,
            modifier = Modifier
                .size(48.dp)
                .testTag("vesper_hamburger_button")
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Open navigation menu",
                tint = VesperTextPrimary,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
