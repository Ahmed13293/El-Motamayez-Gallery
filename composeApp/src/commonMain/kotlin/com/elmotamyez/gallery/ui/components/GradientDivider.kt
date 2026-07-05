package com.elmotamyez.gallery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A horizontal divider that fades in from both edges — softer than a hard line.
 *
 * @param color      The mid-point colour of the gradient. Defaults to a neutral grey.
 * @param thickness  Line thickness.
 * @param indent     Horizontal padding from screen edges (0 = full-bleed).
 * @param alpha      Opacity of the solid mid section.
 */
@Composable
fun GradientDivider(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFCBD5E1),
    thickness: Dp = 1.dp,
    indent: Dp = 0.dp,
    alpha: Float = 0.7f
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = indent)
            .height(thickness)
            .background(
                Brush.horizontalGradient(
                    0.00f to Color.Transparent,
                    0.15f to color.copy(alpha = alpha),
                    0.85f to color.copy(alpha = alpha),
                    1.00f to Color.Transparent,
                )
            )
    )
}
