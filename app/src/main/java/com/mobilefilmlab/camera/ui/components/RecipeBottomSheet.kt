package com.mobilefilmlab.camera.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilefilmlab.camera.data.model.Recipe
import com.mobilefilmlab.camera.data.model.RecipeGroup
import com.mobilefilmlab.camera.ui.theme.AccentGold
import com.mobilefilmlab.camera.ui.theme.BorderDark
import com.mobilefilmlab.camera.ui.theme.SurfaceCard
import com.mobilefilmlab.camera.ui.theme.SurfaceDark
import com.mobilefilmlab.camera.ui.theme.TextMuted
import com.mobilefilmlab.camera.ui.theme.TextSecondary

@Composable
fun RecipeBottomSheet(
    recipes: List<Recipe>,
    activeRecipe: Recipe,
    puckTone: Float,
    puckColor: Float,
    onRecipeSelected: (Recipe) -> Unit,
    onToggleFavourite: (Recipe) -> Unit,
    onPuckMoved: (tone: Float, color: Float) -> Unit,
    onResetPuck: () -> Unit,
    onSaveCustomRecipe: (name: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedGroup by remember { mutableStateOf(activeRecipe.group) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderDark, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag Handle
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF4A5164))
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Header: Recipe Name, Category & Favourite
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = activeRecipe.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "${activeRecipe.group.displayName} • ${activeRecipe.summary()}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }

                IconButton(onClick = { onToggleFavourite(activeRecipe) }) {
                    Icon(
                        imageVector = if (activeRecipe.isFavourite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                        contentDescription = "Toggle Favourite",
                        tint = if (activeRecipe.isFavourite) AccentGold else TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Category Filter Pills
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(RecipeGroup.values()) { group ->
                    val isSelected = group == selectedGroup
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) AccentGold else SurfaceCard)
                            .clickable { selectedGroup = group }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = group.displayName,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.Black else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Horizontal Recipe Swatch Strip for the selected group
            val groupRecipes = recipes.filter { it.group == selectedGroup }
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(groupRecipes) { r ->
                    val isActive = r.id == activeRecipe.id
                    Box(
                        modifier = Modifier
                            .width(88.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceCard)
                            .border(
                                width = if (isActive) 2.dp else 1.dp,
                                color = if (isActive) AccentGold else BorderDark,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onRecipeSelected(r) }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(if (r.isMonochrome) Color.LightGray else AccentGold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = r.name,
                                fontSize = 9.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                color = if (isActive) Color.White else TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2D Photographic Style Square Pad
            PhotographicStylesSquarePad(
                puckTone = puckTone,
                puckColor = puckColor,
                onPuckMoved = onPuckMoved
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Coordinates Readout & Reset
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tone: ${String.format("%+.0f", puckTone)}  ·  Warmth: ${String.format("%+.0f", puckColor)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                OutlinedButton(
                    onClick = onResetPuck,
                    modifier = Modifier.height(30.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                ) {
                    Text("RESET", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Save As Custom Recipe Button
            Button(
                onClick = {
                    val customName = "${activeRecipe.name} Custom"
                    onSaveCustomRecipe(customName)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGold)
            ) {
                Text(
                    text = "SAVE AS CUSTOM RECIPE",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}
