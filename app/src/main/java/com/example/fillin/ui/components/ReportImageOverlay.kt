
package com.example.fillin.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Report image + (top/bottom) dark overlays.
 *
 * Use this anywhere you draw a report image (MyPage saved reports, MyReports, Expiring reports) so
 * the styling stays identical.
 */
@Composable
fun ReportImageWithOverlay(
    @DrawableRes imageRes: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    topOverlayHeight: Dp = 48.dp,
    bottomOverlayHeight: Dp = 56.dp,
    topOverlayAlpha: Float = 0.45f,
    bottomOverlayAlpha: Float = 0.55f,
    content: @Composable BoxScope.() -> Unit = {}
) {
    Box(modifier = modifier) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = null,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize()
        )

        // Top gradient overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(topOverlayHeight)
                .align(Alignment.TopCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = topOverlayAlpha),
                            Color.Transparent
                        )
                    )
                )
        )

        // Bottom gradient overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(bottomOverlayHeight)
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = bottomOverlayAlpha)
                        )
                    )
                )
        )

        // Extra overlays (views badge, category chip, delete icon, etc.)
        content()
    }
}

// Small helper overload if you prefer not to pass a slot.
@Composable
fun ReportImageWithOverlay(
    @DrawableRes imageRes: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    ReportImageWithOverlay(
        imageRes = imageRes,
        modifier = modifier,
        contentScale = contentScale,
        content = {}
    )
}

