package com.mobilefilmlab.camera

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import com.mobilefilmlab.camera.pipeline.ImageProcessor
import com.mobilefilmlab.camera.pipeline.RawMediaStoreSaver
import com.mobilefilmlab.camera.settings.CaptureFormat
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver
import java.io.File
import java.io.IOException

/** Tests real MediaStore-writing/rollback code with a recording provider, not camera hardware or DNG encoding. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class RawMediaStoreSaverTest {
    private class Provider : ContentProvider() {
        data class Row(val values: ContentValues,val file: File)
        val rows=linkedMapOf<Uri,Row>();val deleted=mutableListOf<Uri>();var next=0;var publishes=0
        var failJpegOpen=false;var failSecondPublish=false;var allStreamsCompleteAtPublish=true
        override fun onCreate()=true
        override fun insert(uri: Uri,values: ContentValues?): Uri {
            val result=Uri.parse("content://media/external/images/media/${++next}")
            rows[result]=Row(ContentValues(values),File(context!!.cacheDir,"store-test-${next}.bin").apply { if(exists()) delete() })
            return result
        }
        override fun openFile(uri: Uri,mode: String): ParcelFileDescriptor {
            val row=rows[uri] ?: throw IOException("Missing row")
            if(failJpegOpen && row.values.getAsString(MediaStore.Images.Media.MIME_TYPE)=="image/jpeg") throw IOException("JPEG output failed")
            return ParcelFileDescriptor.open(row.file,ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_TRUNCATE)
        }
        override fun update(uri: Uri,values: ContentValues?,selection: String?,selectionArgs: Array<out String>?): Int {
            publishes++;allStreamsCompleteAtPublish=allStreamsCompleteAtPublish && rows.values.all { it.file.length()>0 }
            if(failSecondPublish && publishes==2) throw IOException("Publish failed")
            rows.getValue(uri).values.putAll(values);return 1
        }
        override fun delete(uri: Uri,selection: String?,selectionArgs: Array<out String>?): Int { deleted+=uri;rows.remove(uri)?.file?.delete();return 1 }
        override fun query(uri: Uri,projection: Array<out String>?,selection: String?,selectionArgs: Array<out String>?,sortOrder: String?): Cursor?=null
        override fun getType(uri: Uri): String?=rows[uri]?.values?.getAsString(MediaStore.Images.Media.MIME_TYPE)
    }
    private fun setup(): Pair<Context,Provider> {
        val context=RuntimeEnvironment.getApplication();val provider=Provider()
        provider.attachInfo(context,ProviderInfo().apply { authority="media" })
        ShadowContentResolver.registerProviderInternal("media",provider)
        return context to provider
    }
    @Test fun rawOnlyCreatesOneDngAndNeverCallsJpegProducer()=runBlocking {
        val (context,p)=setup();var jpegCalled=false
        val saved=RawMediaStoreSaver.save(context,CaptureFormat.RAW,4000,3000,{ it.write("test-sensor-payload".toByteArray()) },{
            jpegCalled=true;error("RAW-only must not render JPEG")
        }).getOrThrow()
        assertFalse(jpegCalled);assertNull(saved.jpeg);assertEquals(1,p.rows.size)
        val values=p.rows.values.single().values
        assertEquals("image/x-adobe-dng",values.getAsString(MediaStore.Images.Media.MIME_TYPE))
        assertTrue(values.getAsString(MediaStore.Images.Media.DISPLAY_NAME).endsWith(".dng"))
        assertEquals(0,values.getAsInteger(MediaStore.Images.Media.IS_PENDING).toInt())
    }
    @Test fun pairUsesTheSameStemAndPublishesOnlyAfterBothStreamsAreWritten()=runBlocking {
        val (context,p)=setup()
        val result=RawMediaStoreSaver.save(context,CaptureFormat.RAW_JPEG,4000,3000,{ it.write(byteArrayOf(1,2,3)) },{ ImageProcessor.Output(byteArrayOf(4,5,6),3000,2000,"test") }).getOrThrow()
        assertNotNull(result.jpeg);assertEquals(2,p.rows.size);assertTrue(p.allStreamsCompleteAtPublish)
        val names=p.rows.values.map { it.values.getAsString(MediaStore.Images.Media.DISPLAY_NAME) }
        assertEquals(names[0].substringBeforeLast('.'),names[1].substringBeforeLast('.'))
        assertEquals(setOf("image/x-adobe-dng","image/jpeg"),p.rows.values.map { it.values.getAsString(MediaStore.Images.Media.MIME_TYPE) }.toSet())
        assertEquals(4000,result.rawWidth);assertEquals(3000,result.jpegWidth)
    }
    @Test fun jpegProcessingFailureRemovesThePreviouslyWrittenPendingDng()=runBlocking {
        val (context,p)=setup()
        val result=RawMediaStoreSaver.save(context,CaptureFormat.RAW_JPEG,4000,3000,{ it.write(byteArrayOf(1)) },{ throw IOException("JPEG processing failed") })
        assertTrue(result.isFailure);assertTrue(p.rows.isEmpty());assertEquals(1,p.deleted.size)
    }
    @Test fun secondOutputFailureRollsBackBothRows()=runBlocking {
        val (context,p)=setup();p.failJpegOpen=true
        val result=RawMediaStoreSaver.save(context,CaptureFormat.RAW_JPEG,4000,3000,{ it.write(byteArrayOf(1)) },{ ImageProcessor.Output(byteArrayOf(2),4000,3000,"") })
        assertTrue(result.isFailure);assertTrue(p.rows.isEmpty());assertEquals(2,p.deleted.size)
    }
    @Test fun publicationFailureRemovesBothFilesEvenIfOneWasBrieflyPublished()=runBlocking {
        val (context,p)=setup();p.failSecondPublish=true
        val result=RawMediaStoreSaver.save(context,CaptureFormat.RAW_JPEG,4000,3000,{ it.write(byteArrayOf(1)) },{ ImageProcessor.Output(byteArrayOf(2),4000,3000,"") })
        assertTrue(result.isFailure);assertTrue(p.rows.isEmpty());assertEquals(2,p.deleted.size)
    }
}
