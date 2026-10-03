package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.CurrencyFormatter
import com.example.domain.MonthTrendData
import com.example.ui.theme.ExpenseColor
import com.example.ui.theme.IncomeColor

@Composable
fun BarTrendChart(
    trends: List<MonthTrendData>,
    currency: String,
    modifier: Modifier = Modifier
) {
    if (trends.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No history available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val maxVal = trends.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(100.0) ?: 100.0
    val progress = remember { Animatable(0f) }

    LaunchedEffect(trends) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(durationMillis = 800))
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(IncomeColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Income",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(20.dp))

            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(ExpenseColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Expense",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Canvas Bars
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
        ) {
            val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)

            Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                val chartHeight = size.height
                val count = trends.size
                val groupWidth = size.width / count
                val barWidth = (groupWidth * 0.32f).coerceAtMost(22.dp.toPx())
                val barSpacing = 4.dp.toPx()

                // Baseline
                drawLine(
                    color = gridColor,
                    start = Offset(0f, chartHeight),
                    end = Offset(size.width, chartHeight),
                    strokeWidth = 1.dp.toPx()
                )

                // Draw Bars
                trends.forEachIndexed { index, trend ->
                    val groupCenter = index * groupWidth + (groupWidth / 2)

                    // Income bar
                    val incomeHeight = ((trend.income / maxVal) * chartHeight * progress.value).toFloat()
                    val incomeLeft = groupCenter - barWidth - (barSpacing / 2)
                    drawRoundRect(
                        color = IncomeColor,
                        topLeft = Offset(incomeLeft, chartHeight - incomeHeight),
                        size = Size(barWidth, incomeHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )

                    // Expense bar
                    val expenseHeight = ((trend.expense / maxVal) * chartHeight * progress.value).toFloat()
                    val expenseLeft = groupCenter + (barSpacing / 2)
                    drawRoundRect(
                        color = ExpenseColor,
                        topLeft = Offset(expenseLeft, chartHeight - expenseHeight),
                        size = Size(barWidth, expenseHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
            }

            // Month labels below
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                trends.forEach { trend ->
                    Text(
                        text = trend.monthLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
