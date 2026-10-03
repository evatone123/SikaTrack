package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.CurrencyFormatter

/**
 * 3D Glassmorphism Specular Border & Surface Brushes.
 */
object GlassmorphismDefaults {

    @Composable
    fun isDark(): Boolean {
        return MaterialTheme.colorScheme.background.red < 0.3f
    }

    /**
     * Primary Frosted Glass Surface Brush
     */
    @Composable
    fun glassSurfaceBrush(
        tint: Color = Color.Unspecified,
        opacity: Float = 1.0f
    ): Brush {
        val isDark = isDark()
        val baseTint = if (tint != Color.Unspecified) tint else if (isDark) Color(0xFF162A26) else Color.White

        return if (isDark) {
            Brush.linearGradient(
                colors = listOf(
                    baseTint.copy(alpha = 0.55f * opacity),
                    baseTint.copy(alpha = 0.35f * opacity),
                    Color(0xFF0F1E1B).copy(alpha = 0.45f * opacity)
                ),
                start = Offset.Zero,
                end = Offset.Infinite
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.82f * opacity),
                    Color(0xFFF5FAF8).copy(alpha = 0.65f * opacity),
                    Color(0xFFE8F2EE).copy(alpha = 0.50f * opacity)
                ),
                start = Offset.Zero,
                end = Offset.Infinite
            )
        }
    }

    /**
     * 3D Specular Highlight Glass Border
     * Light bounces intensely off the top-left edge, fading along the perimeter.
     */
    @Composable
    fun glassBorderBrush(
        accentColor: Color = Color.Unspecified
    ): Brush {
        val isDark = isDark()
        val highlight = if (accentColor != Color.Unspecified) accentColor else Color.White

        return if (isDark) {
            Brush.linearGradient(
                colors = listOf(
                    highlight.copy(alpha = 0.50f),
                    Color.White.copy(alpha = 0.18f),
                    Color.White.copy(alpha = 0.05f),
                    highlight.copy(alpha = 0.25f)
                ),
                start = Offset(0f, 0f),
                end = Offset(400f, 600f)
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.95f),
                    Color(0xFFB2DFDB).copy(alpha = 0.40f),
                    Color.White.copy(alpha = 0.20f),
                    Color(0xFF80CBC4).copy(alpha = 0.50f)
                ),
                start = Offset(0f, 0f),
                end = Offset(400f, 600f)
            )
        }
    }
}

/**
 * Modifier applying 3D Glassmorphic surface, specular border highlight, and soft depth shadow.
 */
@Composable
fun Modifier.glassmorphic(
    shape: Shape = RoundedCornerShape(20.dp),
    borderWidth: Dp = 1.2.dp,
    elevation: Dp = 6.dp,
    tint: Color = Color.Unspecified,
    accentBorder: Color = Color.Unspecified,
    opacity: Float = 1.0f
): Modifier {
    val isDark = GlassmorphismDefaults.isDark()
    val shadowColor = if (isDark) Color(0xFF001511).copy(alpha = 0.7f) else Color(0xFF00332C).copy(alpha = 0.10f)

    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            spotColor = shadowColor,
            ambientColor = shadowColor
        )
        .clip(shape)
        .background(GlassmorphismDefaults.glassSurfaceBrush(tint = tint, opacity = opacity))
        .border(
            width = borderWidth,
            brush = GlassmorphismDefaults.glassBorderBrush(accentColor = accentBorder),
            shape = shape
        )
}

/**
 * 3D Glass Card container with specular reflection and depth.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    elevation: Dp = 8.dp,
    tint: Color = Color.Unspecified,
    accentBorder: Color = Color.Unspecified,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.glassmorphic(
            shape = shape,
            elevation = elevation,
            tint = tint,
            accentBorder = accentBorder
        )
    ) {
        content()
    }
}

/**
 * Ambient Luminous Background providing colorful glow orbs behind the glass surfaces.
 */
@Composable
fun GlassmorphicAtmosphere(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isDark = GlassmorphismDefaults.isDark()

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                // Top-Left Emerald Luminous Glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            if (isDark) Color(0xFF00695C).copy(alpha = 0.35f) else Color(0xFF80CBC4).copy(alpha = 0.35f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.15f, size.height * 0.12f),
                        radius = size.width * 0.70f
                    )
                )

                // Top-Right Gold Currency Glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            if (isDark) Color(0xFFFFB300).copy(alpha = 0.22f) else Color(0xFFFFE082).copy(alpha = 0.35f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.88f, size.height * 0.25f),
                        radius = size.width * 0.65f
                    )
                )

                // Center-Left Deep Teal Orb
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            if (isDark) Color(0xFF004D40).copy(alpha = 0.30f) else Color(0xFFB2DFDB).copy(alpha = 0.25f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.05f, size.height * 0.58f),
                        radius = size.width * 0.60f
                    )
                )

                // Bottom-Right Cyan Wealth Orb
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            if (isDark) Color(0xFF00897B).copy(alpha = 0.28f) else Color(0xFFA7F0E0).copy(alpha = 0.30f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.85f, size.height * 0.82f),
                        radius = size.width * 0.75f
                    )
                )
            }
    ) {
        content()
    }
}

/**
 * 3D Glass Quick Action Button with top specular sheen and floating badge.
 */
@Composable
fun GlassQuickActionButton(
    title: String,
    color: Color,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = GlassmorphismDefaults.isDark()

    Box(
        modifier = modifier
            .height(86.dp)
            .glassmorphic(
                shape = RoundedCornerShape(18.dp),
                elevation = 6.dp,
                accentBorder = color.copy(alpha = 0.6f)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Glass Badge
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                color.copy(alpha = if (isDark) 0.30f else 0.22f),
                                color.copy(alpha = if (isDark) 0.12f else 0.08f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.5f),
                                color.copy(alpha = 0.3f)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(19.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    lineHeight = 14.sp
                ),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 3D Glass Stat Card for Monthly Metrics.
 */
@Composable
fun GlassStatCard(
    title: String,
    amount: Double,
    currency: String,
    hideBalances: Boolean,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    val isDark = GlassmorphismDefaults.isDark()

    Box(
        modifier = modifier
            .glassmorphic(
                shape = RoundedCornerShape(20.dp),
                elevation = 6.dp,
                accentBorder = color.copy(alpha = 0.45f)
            )
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    color.copy(alpha = if (isDark) 0.32f else 0.22f),
                                    color.copy(alpha = 0.08f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                listOf(Color.White.copy(alpha = 0.6f), color.copy(alpha = 0.2f))
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column {
                Text(
                    text = CurrencyFormatter.format(amount, currency, hideBalances),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
