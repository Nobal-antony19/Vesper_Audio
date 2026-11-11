package com.vesper.audio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vesper.audio.ui.theme.LocalVesperAccentColor
import com.vesper.audio.ui.theme.VesperBackgroundSecondary
import com.vesper.audio.ui.theme.VesperBorderAccent
import com.vesper.audio.ui.theme.VesperDeepBurgundy
import com.vesper.audio.ui.theme.VesperOutline
import com.vesper.audio.ui.theme.VesperSurfaceElevated
import com.vesper.audio.ui.theme.VesperTextMuted
import com.vesper.audio.ui.theme.VesperTextPrimary
import com.vesper.audio.ui.theme.VesperTypography

val ALPHABET_INDEX_CHARACTERS: List<Char> = listOf('#') + ('A'..'Z').toList()

/**
 * Rectangular drag-to-scroll index bar with boxy VESPER container styling and
 * a rectangular section preview badge. Consumes vertical drag/tap events strictly
 * within its right-edge track so horizontal swipe navigation across main pages works cleanly.
 */
@Composable
fun AlphabeticalFastScrollRail(
    characters: List<Char> = ALPHABET_INDEX_CHARACTERS,
    availableLetters: Set<Char>,
    onLetterSelected: (Char) -> Unit,
    modifier: Modifier = Modifier
) {
    val dynamicAccent = LocalVesperAccentColor.current
    var railHeightPx by remember { mutableStateOf(1f) }
    var activeLetter by remember { mutableStateOf<Char?>(null) }
    var isTouching by remember { mutableStateOf(false) }

    fun resolveLetterAt(y: Float) {
        if (railHeightPx <= 0f || characters.isEmpty()) return
        val clampedY = y.coerceIn(0f, railHeightPx - 1f)
        val itemHeight = railHeightPx / characters.size.toFloat()
        val index = (clampedY / itemHeight).toInt().coerceIn(0, characters.lastIndex)
        val letter = characters[index]
        activeLetter = letter
        onLetterSelected(letter)
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(24.dp)
            .padding(vertical = 8.dp)
            .testTag("alphabet_fast_scroll_rail"),
        contentAlignment = Alignment.CenterEnd
    ) {
        // Rectangular Track Container
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(VesperBackgroundSecondary.copy(alpha = 0.92f))
                .border(
                    width = 1.dp,
                    color = if (isTouching) dynamicAccent else VesperOutline,
                    shape = RoundedCornerShape(6.dp)
                )
                .padding(vertical = 4.dp)
                .onGloballyPositioned { coordinates ->
                    railHeightPx = coordinates.size.height.toFloat()
                }
                .pointerInput(characters) {
                    detectTapGestures(
                        onPress = { offset ->
                            isTouching = true
                            resolveLetterAt(offset.y)
                            tryAwaitRelease()
                            isTouching = false
                            activeLetter = null
                        }
                    )
                }
                .pointerInput(characters) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isTouching = true
                            resolveLetterAt(offset.y)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            resolveLetterAt(change.position.y)
                        },
                        onDragEnd = {
                            isTouching = false
                            activeLetter = null
                        },
                        onDragCancel = {
                            isTouching = false
                            activeLetter = null
                        }
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            characters.forEach { char ->
                val hasMatch = char in availableLetters
                val isSelected = activeLetter == char

                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (isSelected) dynamicAccent
                            else androidx.compose.ui.graphics.Color.Transparent
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = char.toString(),
                        fontSize = 9.sp,
                        lineHeight = 10.sp,
                        fontWeight = if (isSelected || hasMatch) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                            isSelected -> VesperTextPrimary
                            hasMatch -> VesperTextPrimary
                            else -> VesperTextMuted.copy(alpha = 0.38f)
                        },
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Rectangular Floating Index Badge
        AnimatedVisibility(
            visible = isTouching && activeLetter != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(end = 46.dp)
        ) {
            activeLetter?.let { letter ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = VesperDeepBurgundy,
                    modifier = Modifier
                        .size(54.dp)
                        .border(1.5.dp, VesperBorderAccent, RoundedCornerShape(12.dp))
                        .testTag("fast_scroll_badge")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = letter.toString(),
                            style = VesperTypography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = VesperTextPrimary
                            )
                        )
                    }
                }
            }
        }
    }
}
