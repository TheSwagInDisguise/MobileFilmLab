package com.mobilefilmlab.camera.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilefilmlab.camera.data.model.*
import com.mobilefilmlab.camera.settings.LensCalibration
import com.mobilefilmlab.camera.ui.theme.*

@Composable
fun RecipeBottomSheet(
    recipes: List<Recipe>,activeRecipe: Recipe,puckTone: Float,puckColor: Float,previewFrame: Bitmap?,calibration: LensCalibration,
    onRecipeSelected: (Recipe)->Unit,onToggleFavourite: (Recipe)->Unit,onPuckMoved: (Float,Float)->Unit,onResetPuck: ()->Unit,
    onSaveCustomRecipe: (String)->Unit,onDeleteCustom: (Recipe)->Unit,onExport: ()->Unit,onImport: ()->Unit,
    onQr: ()->Unit,onImportQr: ()->Unit,onDismiss: ()->Unit,modifier: Modifier=Modifier
) {
    var category by remember { mutableStateOf(activeRecipe.group.displayName) }
    var save by remember { mutableStateOf(false) }; var delete by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    Column(modifier.fillMaxWidth().heightIn(max=(LocalConfiguration.current.screenHeightDp*.82f).dp)
        .background(SurfaceDark,RoundedCornerShape(topStart=24.dp,topEnd=24.dp))
        .padding(18.dp).verticalScroll(rememberScrollState()),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(activeRecipe.name,fontSize=17.sp,fontWeight=FontWeight.Bold,color=Color.White,maxLines=2)
                Text("${activeRecipe.group.displayName} · ${activeRecipe.summary()}",fontSize=10.sp,color=TextSecondary)
            }
            IconButton(onClick={ onToggleFavourite(activeRecipe) }) { Icon(if(activeRecipe.isFavourite) Icons.Default.Star else Icons.Outlined.StarOutline,"Toggle favourite",tint=AccentGold) }
            IconButton(onDismiss) { Icon(Icons.Default.Close,"Close recipes") }
        }
        LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            items(listOf("★ Favourites")+RecipeGroup.values().sortedBy { it.sortOrder }.map { it.displayName }) { group ->
                FilterChip(category==group,{ category=group },label={ Text(group,fontSize=10.sp) })
            }
        }
        val filtered=recipes.filter { if(category=="★ Favourites") it.isFavourite else it.group.displayName==category }
        if(filtered.isEmpty()) Text("No recipes here yet. Star a recipe or import a custom preset.",fontSize=11.sp,color=TextSecondary)
        else LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            items(filtered,key={ it.id }) { r ->
                Column(Modifier.width(100.dp).border(if(r.id==activeRecipe.id) 2.dp else 1.dp,if(r.id==activeRecipe.id) AccentGold else BorderDark,RoundedCornerShape(10.dp))
                    .clickable { onRecipeSelected(r) }.padding(5.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                    RecipeLiveThumbnail(previewFrame,r,calibration,Modifier.fillMaxWidth().height(72.dp))
                    Text(r.name,fontSize=10.sp,color=Color.White,maxLines=2,modifier=Modifier.padding(top=5.dp))
                }
            }
        }
        Text("Live colour previews · final grain, sharpness and OEM processing may differ",fontSize=9.sp,color=TextSecondary)
        PhotographicStylesSquarePad(puckTone,puckColor,onPuckMoved)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
            Text("Tone ${"%+.0f".format(puckTone)} · Warmth ${"%+.0f".format(puckColor)}",fontSize=12.sp)
            TextButton(onResetPuck) { Text("Reset") }
        }
        Button(onClick={ name="${activeRecipe.name} Custom".take(80); save=true },modifier=Modifier.fillMaxWidth()) { Text("Save custom recipe") }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onExport,Modifier.weight(1f)) { Text("Export file",fontSize=11.sp) }
            OutlinedButton(onImport,Modifier.weight(1f)) { Text("Import file",fontSize=11.sp) }
        }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onQr,Modifier.weight(1f)) { Text("Show QR",fontSize=11.sp) }
            OutlinedButton(onImportQr,Modifier.weight(1f)) { Text("Import QR image",fontSize=10.sp) }
        }
        if(activeRecipe.isCustom) TextButton(onClick={ delete=true }) { Text("Delete this custom recipe",color=Color(0xFFFFB4A9)) }
        TextButton(onClick={ onRecipeSelected(RecipeCatalog.getDefaultRecipe()); category=RecipeGroup.OPPO.displayName }) { Text("Return to Oppo Natural (Camera2)") }
    }
    if(save) AlertDialog(onDismissRequest={ save=false },title={ Text("Save custom recipe") },text={ OutlinedTextField(name,{ name=it.take(80) },singleLine=true,label={ Text("Name") }) },
        confirmButton={ TextButton(onClick={ if(name.isNotBlank()) { onSaveCustomRecipe(name.trim()); save=false } },enabled=name.isNotBlank()) { Text("Save") } },
        dismissButton={ TextButton(onClick={ save=false }) { Text("Cancel") } })
    if(delete) AlertDialog(onDismissRequest={ delete=false },title={ Text("Delete custom recipe?") },text={ Text("Built-in presets and photos will not be changed.") },
        confirmButton={ TextButton(onClick={ onDeleteCustom(activeRecipe); delete=false }) { Text("Delete") } },dismissButton={ TextButton(onClick={ delete=false }) { Text("Cancel") } })
}
