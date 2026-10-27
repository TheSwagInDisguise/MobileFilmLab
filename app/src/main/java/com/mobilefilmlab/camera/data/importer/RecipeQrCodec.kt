package com.mobilefilmlab.camera.data.importer

import android.graphics.Bitmap
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.mobilefilmlab.camera.data.model.Recipe
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream

object RecipeQrCodec {
    private const val PREFIX="FILMRECIPE:1:"
    fun payload(recipe: Recipe): String {
        val output=ByteArrayOutputStream()
        DeflaterOutputStream(output).use { it.write(RecipeJsonSerializer.toJson(listOf(recipe)).toByteArray(Charsets.UTF_8)) }
        return (PREFIX+Base64.getUrlEncoder().withoutPadding().encodeToString(output.toByteArray())).also {
            require(it.length<=2200) { "Recipe is too large for QR; export a .filmrecipe file instead" }
        }
    }
    fun decodePayload(text: String): kotlin.Result<List<Recipe>> = runCatching {
        require(text.startsWith(PREFIX) && text.length<=2200) { "Not a supported recipe QR code" }
        val input=Base64.getUrlDecoder().decode(text.removePrefix(PREFIX))
        val output=ByteArrayOutputStream()
        InflaterInputStream(ByteArrayInputStream(input)).use { stream ->
            val buffer=ByteArray(1024)
            while(true) {
                val n=stream.read(buffer); if(n<0) break
                require(output.size()+n<=RecipeJsonSerializer.MAX_BYTES) { "QR payload expands beyond the size limit" }
                output.write(buffer,0,n)
            }
        }
        RecipeJsonSerializer.fromJson(output.toString("UTF-8")).getOrThrow()
    }
    fun bitmap(recipe: Recipe): Bitmap {
        val matrix=MultiFormatWriter().encode(payload(recipe),BarcodeFormat.QR_CODE,720,720,
            mapOf(EncodeHintType.CHARACTER_SET to "UTF-8",EncodeHintType.MARGIN to 3))
        val pixels=IntArray(matrix.width*matrix.height) { i -> if(matrix[i%matrix.width,i/matrix.width]) -0x1000000 else -1 }
        return Bitmap.createBitmap(pixels,matrix.width,matrix.height,Bitmap.Config.ARGB_8888)
    }
    fun scan(bitmap: Bitmap): kotlin.Result<List<Recipe>> = runCatching {
        val pixels=IntArray(bitmap.width*bitmap.height)
        bitmap.getPixels(pixels,0,bitmap.width,0,0,bitmap.width,bitmap.height)
        var source: LuminanceSource=RGBLuminanceSource(bitmap.width,bitmap.height,pixels)
        var text: String?=null
        for(invert in listOf(false,true)) {
            text=runCatching { MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(if(invert) source.invert() else source)),mapOf(DecodeHintType.TRY_HARDER to true)).text }.getOrNull()
            if(text!=null) break
        }
        decodePayload(text ?: error("No readable recipe QR found in this image")).getOrThrow()
    }
}
