package com.purchasely.shaker.ui.screen.mixing

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.purchasely.shaker.domain.model.Cocktail
import com.purchasely.shaker.domain.repository.CocktailRepository
import com.purchasely.shaker.ui.components.CocktailArt
import com.purchasely.shaker.ui.theme.Shaker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

class MixingViewModel(
    private val repository: CocktailRepository,
    cocktailId: String,
) : ViewModel() {
    private val _cocktail = MutableStateFlow(repository.getCocktail(cocktailId))
    val cocktail: StateFlow<Cocktail?> = _cocktail
}

@Composable
fun MixingScreen(
    cocktailId: String,
    onExit: () -> Unit,
    viewModel: MixingViewModel = koinViewModel { parametersOf(cocktailId) },
) {
    val tokens = Shaker.tokens
    val cocktail by viewModel.cocktail.collectAsStateWithLifecycle()
    val c = cocktail ?: return
    val total = c.instructions.size
    var step by remember { mutableIntStateOf(0) }
    val pct by animateFloatAsState(targetValue = ((step + 1).toFloat() / total.coerceAtLeast(1)))

    val bg = if (tokens.dark) Color(0xFF0A0B16) else Color(0xFF15182B)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x1AFFFFFF))
                    .clickable(onClick = onExit),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.weight(1f))
            Text(
                "Step ${step + 1} of $total",
                color = Color(0xB3FFFFFF),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.size(36.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0x1AFFFFFF)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(pct)
                    .fillMaxSize()
                    .background(tokens.gold),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .border(3.dp, Color(0x1AFFFFFF), CircleShape),
            ) {
                CocktailArt(cocktail = c, modifier = Modifier.fillMaxSize())
            }
            Spacer(Modifier.height(24.dp))
            Text(
                c.name.uppercase(),
                color = Color(0x99FFFFFF),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                c.instructions.getOrNull(step) ?: "",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .border(1.5.dp, Color.White, RoundedCornerShape(100.dp))
                    .clickable(enabled = step > 0) { if (step > 0) step-- },
                contentAlignment = Alignment.Center,
            ) {
                Text("Back", color = Color.White.copy(alpha = if (step == 0) 0.4f else 1f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            val isLast = step == total - 1
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(if (isLast) tokens.green else tokens.accent)
                    .clickable {
                        if (isLast) onExit() else step++
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (isLast) "Done — Cheers!" else "Next step",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
