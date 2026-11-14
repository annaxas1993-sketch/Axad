package com.axas.aiphotovideo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PhotoVideoScreen() }
    }

    @Composable
    private fun PhotoVideoScreen() {
        var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }
        var caption by remember { mutableStateOf("") }
        var exporting by remember { mutableStateOf(false) }
        var status by remember { mutableStateOf("Ընտրիր նկար և ավելացրու քո տեքստը") }

        val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                try {
                    contentResolver.openInputStream(uri)?.use { photoBitmap = BitmapFactory.decodeStream(it) }
                    status = if (photoBitmap != null) "Նկարը պատրաստ է" else "Չհաջողվեց բացել նկարը"
                } catch (_: Exception) { status = "Չհաջողվեց բացել նկարը" }
            }
        }

        MaterialTheme(colorScheme = darkColorScheme(
            primary = ComposeColor(0xFFB69CFF),
            background = ComposeColor(0xFF101019),
            surface = ComposeColor(0xFF1C1B29)
        )) {
            Surface(Modifier.fillMaxSize(), color = ComposeColor(0xFF101019)) {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("AI PHOTO VIDEO", fontSize = 27.sp, fontWeight = FontWeight.Black, color = ComposeColor.White)
                    Text("Նկարներ • Տեքստ • Տեսանյութ", color = ComposeColor(0xFFB9B5C9))
                    Spacer(Modifier.height(22.dp))
                    Box(
                        Modifier.fillMaxWidth().height(390.dp)
                            .background(ComposeColor(0xFF242333), RoundedCornerShape(22.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        val bmp = photoBitmap
                        if (bmp != null) {
                            Image(bmp.asImageBitmap(), contentDescription = "Ընտրված նկար", modifier = Modifier.fillMaxSize())
                            if (caption.isNotBlank()) {
                                Text(caption, color = ComposeColor.White, fontSize = 25.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(18.dp)
                                        .background(ComposeColor(0x99000000), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp))
                            }
                        } else Text("Քո նկարը կհայտնվի այստեղ", color = ComposeColor(0xFFD2CDE2))
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { picker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                        Text("＋  Ընտրել նկար հեռախոսից")
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = caption, onValueChange = { caption = it },
                        label = { Text("Գրիր տեքստ նկարի վրա") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = {
                        val source = photoBitmap
                        if (source == null) {
                            Toast.makeText(this@MainActivity, "Նախ ընտրիր նկար", Toast.LENGTH_SHORT).show()
                        } else if (!exporting) {
                            exporting = true
                            status = "Պատրաստվում է տեսանյութը…"
                            thread {
                                try {
                                    val file = exportVideo(source, caption)
                                    runOnUiThread {
                                        exporting = false
                                        status = "Պատրաստ է՝ ${file.absolutePath}"
                                        Toast.makeText(this, "Տեսանյութը պահպանվեց հավելվածի Movies պանակում", Toast.LENGTH_LONG).show()
                                    }
                                } catch (e: Exception) {
                                    runOnUiThread {
                                        exporting = false
                                        status = "Սխալ՝ ${e.message ?: "չհաջողվեց ստեղծել տեսանյութը"}"
                                    }
                                }
                            }
                        }
                    }, enabled = !exporting, modifier = Modifier.fillMaxWidth()) {
                        Text(if (exporting) "Ստեղծվում է…" else "▶  Ստեղծել 5 վայրկյանանոց MP4")
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(status, color = ComposeColor(0xFFB9B5C9), fontSize = 12.sp)
                    Spacer(Modifier.height(20.dp))
                    Text("AI նկարների գեներացումը և AI անիմացիան կավելացվեն արտաքին AI ծառայություն միացնելուց հետո։",
                        color = ComposeColor(0xFF8F8BA1), fontSize = 12.sp)
                }
            }
        }
    }

    private fun exportVideo(source: Bitmap, text: String): File {
        val width = 720
        val height = 1280
        val base = getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: cacheDir
        val dir = File(base, "AIPhotoVideo").apply { mkdirs() }
        val output = File(dir, "video_${System.currentTimeMillis()}.mp4")
        val recorder = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(this) else @Suppress("DEPRECATION") MediaRecorder()
        try {
            recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            recorder.setVideoSize(width, height)
            recorder.setVideoFrameRate(30)
            recorder.setVideoEncodingBitRate(4_000_000)
            recorder.setOutputFile(output.absolutePath)
            recorder.prepare()
            val surface = recorder.surface
            recorder.start()
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE; textSize = 54f; textAlign = Paint.Align.CENTER
                setShadowLayer(5f, 2f, 2f, Color.BLACK)
                typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
            }
            val scaled = Bitmap.createScaledBitmap(source, width, height, true)
            val start = System.currentTimeMillis()
            while (System.currentTimeMillis() - start < 5000) {
                val canvas: Canvas = surface.lockCanvas(null)
                try {
                    canvas.drawBitmap(scaled, 0f, 0f, paint)
                    if (text.isNotBlank()) {
                        val lines = text.split("\n").take(3)
                        val y0 = height - 150f - (lines.size - 1) * 62f
                        lines.forEachIndexed { index, line -> canvas.drawText(line.take(35), width / 2f, y0 + index * 62f, textPaint) }
                    }
                } finally { surface.unlockCanvasAndPost(canvas) }
                Thread.sleep(33)
            }
            recorder.stop()
            scaled.recycle()
            return output
        } catch (e: Exception) {
            output.delete()
            throw e
        } finally {
            try { recorder.reset(); recorder.release() } catch (_: Exception) {}
        }
    }
}
