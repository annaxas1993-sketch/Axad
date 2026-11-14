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
        var page by remember { mutableStateOf("home") }
        var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }
        var caption by remember { mutableStateOf("") }
        var prompt by remember { mutableStateOf("") }
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
                    Text("Արհեստական բանականությամբ ստեղծագործիր", color = ComposeColor(0xFFB9B5C9))
                    Spacer(Modifier.height(28.dp))

                    if (page == "home") {
                        Spacer(Modifier.height(55.dp))
                        Box(
                            Modifier.fillMaxWidth().height(190.dp)
                                .background(ComposeColor(0xFF242333), RoundedCornerShape(24.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("✨", fontSize = 48.sp)
                                Text("Քո գաղափարները՝ նկարների և վիդեոների մեջ",
                                    color = ComposeColor.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Spacer(Modifier.height(28.dp))
                        Button(
                            onClick = { page = "image" },
                            modifier = Modifier.fillMaxWidth().height(62.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) { Text("🎨  Նկար ստեղծել", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { page = "video" },
                            modifier = Modifier.fillMaxWidth().height(62.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ComposeColor(0xFF514477))
                        ) { Text("🎬  Նկարից վիդեո", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.height(24.dp))
                        Text("Ընտրիր գործողությունը", color = ComposeColor(0xFFB9B5C9))
                    } else {
                        TextButton(onClick = { page = "home" }, modifier = Modifier.align(Alignment.Start)) {
                            Text("← Գլխավոր էջ")
                        }
                        if (page == "image") {
                            Spacer(Modifier.height(25.dp))
                            Text("Նկար ստեղծել", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = ComposeColor.White)
                            Spacer(Modifier.height(18.dp))
                            OutlinedTextField(
                                value = prompt, onValueChange = { prompt = it },
                                label = { Text("Նկարագրիր՝ ինչ նկար ես ուզում") },
                                placeholder = { Text("Օրինակ՝ սպորտային մեքենա գիշերային քաղաքում") },
                                modifier = Modifier.fillMaxWidth(), minLines = 3
                            )
                            Spacer(Modifier.height(18.dp))
                            Box(
                                Modifier.fillMaxWidth().height(280.dp)
                                    .background(ComposeColor(0xFF242333), RoundedCornerShape(20.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Այստեղ կհայտնվի ստեղծված նկարը", color = ComposeColor(0xFFD2CDE2))
                            }
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    Toast.makeText(this@MainActivity,
                                        "AI նկարի ստեղծման ծառայությունը դեռ միացված չէ",
                                        Toast.LENGTH_LONG).show()
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            ) { Text("Ստեղծել AI նկար") }
                            Spacer(Modifier.height(12.dp))
                            Text("AI նկար ստեղծելու համար դեռ պետք է միացնել արտաքին AI ծառայություն։",
                                color = ComposeColor(0xFFB9B5C9), fontSize = 13.sp)
                        } else {
                            Spacer(Modifier.height(12.dp))
                            Text("Նկարից վիդեո", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = ComposeColor.White)
                            Spacer(Modifier.height(16.dp))
                            Box(
                                Modifier.fillMaxWidth().height(350.dp)
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
                            Spacer(Modifier.height(14.dp))
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
                                                Toast.makeText(this@MainActivity, "Տեսանյութը պահպանվեց հավելվածի Movies պանակում", Toast.LENGTH_LONG).show()
                                            }
                                        } catch (e: Exception) {
                                            runOnUiThread {
                                                exporting = false
                                                status = "Սխալ՝ ${e.message ?: "չհաջողվեց ստեղծել տեսանյութը"}"
                                            }
                                        }
                                    }
                                }
                            }, enabled = !exporting, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                                Text(if (exporting) "Ստեղծվում է…" else "▶  Ստեղծել 15 րոպեանոց MP4")
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(status, color = ComposeColor(0xFFB9B5C9), fontSize = 12.sp)
                            Text("Սա ստեղծում է ստատիկ նկարով և տեքստով 5 վայրկյանանոց տեսանյութ։ AI շարժապատկերը դեռ միացված չէ։",
                                color = ComposeColor(0xFF8F8BA1), fontSize = 12.sp)
                        }
                    }
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
            while (System.currentTimeMillis() - start < 900000) {
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
