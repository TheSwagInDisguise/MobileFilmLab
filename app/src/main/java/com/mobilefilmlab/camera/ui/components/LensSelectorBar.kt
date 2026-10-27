package com.mobilefilmlab.camera.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilefilmlab.camera.data.model.LensType
import com.mobilefilmlab.camera.ui.theme.AccentGold

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LensSelectorBar(activeLens: LensType,cropFactor: Float,available: Set<LensType>,enabled: Boolean,
                    onLensSelected: (LensType)->Unit,onToggle2xCrop: ()->Unit,onCrop: (Float)->Unit,modifier: Modifier=Modifier) {
    var menu by remember { mutableStateOf(false) }
    Row(modifier.background(Color.Black.copy(alpha=.52f),RoundedCornerShape(28.dp)).padding(5.dp),horizontalArrangement=Arrangement.spacedBy(4.dp),verticalAlignment=Alignment.CenterVertically) {
        listOf(LensType.ULTRA_WIDE,LensType.WIDE,LensType.TELEPHOTO,LensType.PERISCOPE).forEach { lens ->
            val accessible=lens in available
            Box(Modifier.size(44.dp).background(if(activeLens==lens) Color.White else Color.White.copy(alpha=.1f),CircleShape)
                .clickable(enabled=enabled && accessible) { onLensSelected(lens) },contentAlignment=Alignment.Center) {
                Text(lens.label,color=if(!accessible) Color.Gray else if(activeLens==lens) Color.Black else Color.White,fontSize=12.sp)
            }
        }
        Box {
            Box(Modifier.height(44.dp).widthIn(min=46.dp).background(if(cropFactor>1f) AccentGold else Color.White.copy(alpha=.1f),RoundedCornerShape(15.dp))
                .combinedClickable(enabled=enabled,onClick=onToggle2xCrop,onLongClick={ menu=true }).padding(horizontal=9.dp),contentAlignment=Alignment.Center) {
                Text(if(cropFactor==1f || cropFactor==2f) "2×" else "${cropFactor}×",fontSize=12.sp,color=if(cropFactor>1f) Color.Black else Color.White)
            }
            DropdownMenu(expanded=menu,onDismissRequest={ menu=false }) {
                listOf(1f,1.5f,2f,3f).forEach { factor -> DropdownMenuItem(text={ Text("${factor}× crop${if(factor==1f) " · full frame" else " · native pixels"}") },onClick={ onCrop(factor); menu=false }) }
            }
        }
    }
}
