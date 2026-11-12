package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppTab
import com.example.ui.theme.CardSurface
import com.example.ui.theme.ClinicalGreen
import com.example.ui.theme.LocalGenderPalette
import com.example.ui.theme.MutedText
import com.example.ui.theme.SoftGreenPill

@Composable
fun ClinicalSectionCard(
    modifier: Modifier = Modifier,
    borderColor: Color = LocalGenderPalette.current.borderLight,
    backgroundColor: Color = CardSurface,
    cornerRadius: Dp = 20.dp,
    contentPadding: Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun PillBadge(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    borderColor: Color? = null,
    fontSizeSp: Int = 11,
    horizontalPadding: Dp = 10.dp,
    verticalPadding: Dp = 4.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(backgroundColor)
            .then(
                if (borderColor != null) {
                    Modifier.border(1.dp, borderColor, RoundedCornerShape(50))
                } else {
                    Modifier
                }
            )
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = fontSizeSp.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = (fontSizeSp + 2).sp
        )
    }
}

@Composable
fun StatusGreenDot(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(SoftGreenPill),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(ClinicalGreen)
        )
    }
}

@Composable
fun CredEdiBottomBar(
    currentTab: AppTab,
    onSelectTab: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalGenderPalette.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        HorizontalDivider(thickness = 1.dp, color = palette.divider)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomTabItem(
                label = "Calculadora",
                selected = currentTab == AppTab.CALCULADORA,
                showDot = false,
                testTag = "tab_calculadora",
                onClick = { onSelectTab(AppTab.CALCULADORA) },
                iconContent = { tint ->
                    MathGridIcon(tint = tint, modifier = Modifier.size(22.dp))
                }
            )
            BottomTabItem(
                label = "Curvas OMS",
                selected = currentTab == AppTab.CURVAS_OMS,
                showDot = currentTab == AppTab.CURVAS_OMS,
                testTag = "tab_curvas",
                onClick = { onSelectTab(AppTab.CURVAS_OMS) },
                iconContent = { tint ->
                    TrendLineIcon(tint = tint, modifier = Modifier.size(22.dp))
                }
            )
            BottomTabItem(
                label = "Diagnóstico",
                selected = currentTab == AppTab.DIAGNOSTICO,
                showDot = currentTab == AppTab.DIAGNOSTICO,
                testTag = "tab_diagnostico",
                onClick = { onSelectTab(AppTab.DIAGNOSTICO) },
                iconContent = { tint ->
                    CheckBoxOutlineIcon(tint = tint, modifier = Modifier.size(22.dp))
                }
            )
            BottomTabItem(
                label = "EDI",
                selected = currentTab == AppTab.EDI,
                showDot = false,
                testTag = "tab_edi",
                onClick = { onSelectTab(AppTab.EDI) },
                iconContent = { tint ->
                    HeadGearIcon(tint = tint, modifier = Modifier.size(22.dp))
                }
            )
        }
    }
}

@Composable
private fun BottomTabItem(
    label: String,
    selected: Boolean,
    showDot: Boolean,
    testTag: String,
    onClick: () -> Unit,
    iconContent: @Composable (Color) -> Unit
) {
    val palette = LocalGenderPalette.current
    val iconTint = if (selected) Color.White else MutedText
    val labelColor = if (selected) palette.navy else MutedText

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(if (selected) palette.navy else Color.Transparent)
                .padding(horizontal = 16.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                iconContent(iconTint)
                if (showDot) {
                    Box(
                        modifier = Modifier
                            .offset(x = 6.dp, y = (-2).dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(palette.primary)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = labelColor,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium
        )
    }
}

@Composable
fun MathGridIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val w = size.width
        val h = size.height
        val pad = w * 0.12f
        drawRoundRect(
            color = tint,
            topLeft = Offset(pad, pad),
            size = Size(w - 2 * pad, h - 2 * pad),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            style = stroke
        )
        // Mid lines
        drawLine(tint, Offset(w * 0.5f, pad), Offset(w * 0.5f, h - pad), strokeWidth = 1.3.dp.toPx())
        drawLine(tint, Offset(pad, h * 0.5f), Offset(w - pad, h * 0.5f), strokeWidth = 1.3.dp.toPx())
        // Top-left: +
        val tlX = w * 0.31f
        val tlY = h * 0.31f
        val s = w * 0.07f
        drawLine(tint, Offset(tlX - s, tlY), Offset(tlX + s, tlY), strokeWidth = 1.5.dp.toPx())
        drawLine(tint, Offset(tlX, tlY - s), Offset(tlX, tlY + s), strokeWidth = 1.5.dp.toPx())
        // Top-right: -
        val trX = w * 0.69f
        val trY = h * 0.31f
        drawLine(tint, Offset(trX - s, trY), Offset(trX + s, trY), strokeWidth = 1.5.dp.toPx())
        // Bottom-left: x
        val blX = w * 0.31f
        val blY = h * 0.69f
        val sx = s * 0.8f
        drawLine(tint, Offset(blX - sx, blY - sx), Offset(blX + sx, blY + sx), strokeWidth = 1.5.dp.toPx())
        drawLine(tint, Offset(blX - sx, blY + sx), Offset(blX + sx, blY - sx), strokeWidth = 1.5.dp.toPx())
        // Bottom-right: =
        val brX = w * 0.69f
        val brY = h * 0.69f
        drawLine(tint, Offset(brX - s, brY - s * 0.45f), Offset(brX + s, brY - s * 0.45f), strokeWidth = 1.4.dp.toPx())
        drawLine(tint, Offset(brX - s, brY + s * 0.45f), Offset(brX + s, brY + s * 0.45f), strokeWidth = 1.4.dp.toPx())
    }
}

@Composable
fun TrendLineIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.12f, h * 0.72f)
            lineTo(w * 0.38f, h * 0.44f)
            lineTo(w * 0.58f, h * 0.62f)
            lineTo(w * 0.88f, h * 0.28f)
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = 2.1.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun CheckBoxOutlineIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val w = size.width
        val h = size.height
        val pad = w * 0.14f
        drawRoundRect(
            color = tint,
            topLeft = Offset(pad, pad),
            size = Size(w - 2 * pad, h - 2 * pad),
            cornerRadius = CornerRadius(3.5.dp.toPx(), 3.5.dp.toPx()),
            style = stroke
        )
        val check = Path().apply {
            moveTo(w * 0.32f, h * 0.52f)
            lineTo(w * 0.46f, h * 0.65f)
            lineTo(w * 0.70f, h * 0.37f)
        }
        drawPath(check, color = tint, style = stroke)
    }
}

@Composable
fun HeadGearIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 1.7.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val w = size.width
        val h = size.height
        // Head profile silhouette
        val headPath = Path().apply {
            moveTo(w * 0.32f, h * 0.86f)
            lineTo(w * 0.32f, h * 0.70f)
            cubicTo(w * 0.16f, h * 0.58f, w * 0.18f, h * 0.25f, w * 0.46f, h * 0.15f)
            cubicTo(w * 0.72f, h * 0.12f, w * 0.84f, h * 0.32f, w * 0.82f, h * 0.50f)
            lineTo(w * 0.88f, h * 0.58f)
            lineTo(w * 0.80f, h * 0.62f)
            lineTo(w * 0.78f, h * 0.74f)
            lineTo(w * 0.62f, h * 0.74f)
            lineTo(w * 0.62f, h * 0.86f)
        }
        drawPath(headPath, color = tint, style = stroke)
        // Inner gear circle
        val cx = w * 0.49f
        val cy = h * 0.42f
        val r = w * 0.12f
        drawCircle(color = tint, radius = r, center = Offset(cx, cy), style = stroke)
        // Gear spokes
        val spoke = w * 0.18f
        drawLine(tint, Offset(cx, cy - spoke), Offset(cx, cy + spoke), strokeWidth = 1.4.dp.toPx())
        drawLine(tint, Offset(cx - spoke, cy), Offset(cx + spoke, cy), strokeWidth = 1.4.dp.toPx())
    }
}
