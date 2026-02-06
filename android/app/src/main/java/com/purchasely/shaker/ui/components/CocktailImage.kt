package com.purchasely.shaker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.purchasely.shaker.domain.model.Cocktail

@Composable
fun CocktailImage(
    cocktail: Cocktail,
    modifier: Modifier = Modifier
) {
    val bgColor = spiritBackgroundColor(cocktail.spirit)
    val fgColor = spiritForegroundColor(cocktail.spirit)

    Box(
        modifier = modifier.background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(fgColor.copy(alpha = 0.3f))
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        ) {
            Text(
                text = cocktail.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = fgColor,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = cocktail.spirit.replaceFirstChar { it.uppercase() },
                fontSize = 12.sp,
                color = fgColor.copy(alpha = 0.7f)
            )
        }
    }
}

private fun spiritBackgroundColor(spirit: String): Color = when (spirit.lowercase()) {
    "rum" -> Color(0xFFD4A574)
    "whiskey" -> Color(0xFFC68E17)
    "gin" -> Color(0xFF87CEEB)
    "tequila" -> Color(0xFF98FB98)
    "vodka" -> Color(0xFFE8E8E8)
    else -> Color(0xFFFFB6C1)
}

private fun spiritForegroundColor(spirit: String): Color = when (spirit.lowercase()) {
    "rum" -> Color(0xFF8B6914)
    "whiskey" -> Color(0xFF8B5C0D)
    "gin" -> Color(0xFF4682B4)
    "tequila" -> Color(0xFF228B22)
    "vodka" -> Color(0xFF808080)
    else -> Color(0xFFB35975)
}
