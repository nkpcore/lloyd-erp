package com.lloyd.attendance.feature.syllabus

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.min

data class SyllabusPdfTocItem(
    val title: String,
    val pageNumber: Int // 1-based page number
)

val AKTU_SYLLABUS_TOC = listOf(
    SyllabusPdfTocItem("Syllabus Index", 1),
    SyllabusPdfTocItem("Quantum Physics and Applications", 2),
    SyllabusPdfTocItem("Applied Chemistry for Smart Systems", 4),
    SyllabusPdfTocItem("Calculus and Linear Algebra", 6),
    SyllabusPdfTocItem("Numerical Methods", 8),
    SyllabusPdfTocItem("Fundamentals of Electrical Engineering", 10),
    SyllabusPdfTocItem("Fundamentals of Electronics Engineering", 12),
    SyllabusPdfTocItem("Programming Languages", 14),
    SyllabusPdfTocItem("Essentials of Data Structure", 16),
    SyllabusPdfTocItem("Introduction to Indian Knowledge System", 18),
    SyllabusPdfTocItem("Professional Communication & Technical Writing", 20),
    SyllabusPdfTocItem("Introduction to AI & Prompt Engineering", 24),
    SyllabusPdfTocItem("Fundamentals of Mechanical Engineering", 26),
    SyllabusPdfTocItem("Innovation & Design Thinking Lab", 28),
    SyllabusPdfTocItem("Environment & Sustainability", 32)
)

/**
 * Returns the 0-indexed page in AKTU_Syllabus_With_Index.pdf for a given course code or title.
 */
fun getPdfPageForCourse(courseCode: String?, courseTitle: String?): Int {
    val code = courseCode?.uppercase() ?: ""
    val title = courseTitle?.lowercase() ?: ""
    return when {
        code.contains("101E") || code.contains("201E") || title.contains("quantum") || title.contains("physics") -> 1 // Page 2
        code.contains("102D") || code.contains("202D") || title.contains("chemistry") -> 3 // Page 4
        code.contains("103D") || title.contains("calculus") -> 5 // Page 6
        code.contains("203D") || title.contains("numerical") -> 7 // Page 8
        code.contains("AEE") || title.contains("electrical") -> 9 // Page 10
        code.contains("AEC") || title.contains("electronics") -> 11 // Page 12
        code.contains("ACS101") || title.contains("programming") -> 13 // Page 14
        code.contains("ACS201") || title.contains("data structure") -> 15 // Page 16
        code.contains("104") || code.contains("204") || title.contains("indian knowledge") || title.contains("iks") -> 17 // Page 18
        code.contains("105") || code.contains("205") || title.contains("communication") -> 19 // Page 20
        code.contains("AAI") || title.contains("prompt") || title.contains("artificial") -> 23 // Page 24
        code.contains("AME") || title.contains("mechanical") -> 25 // Page 26
        code.contains("AID") || title.contains("design thinking") || title.contains("innovation") -> 27 // Page 28
        code.contains("AAC") || title.contains("environment") || title.contains("sustainability") -> 31 // Page 32
        else -> 0 // Page 1 (Index)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyllabusPdfViewerScreen(
    initialPage: Int = 0,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentPageIndex by remember { mutableIntStateOf(initialPage.coerceAtLeast(0)) }
    var totalPages by remember { mutableIntStateOf(33) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showTocSheet by remember { mutableStateOf(false) }

    // Zoom and pan state
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Reset zoom when page changes
    LaunchedEffect(currentPageIndex) {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    // Render PDF page
    LaunchedEffect(currentPageIndex) {
        isLoading = true
        errorMessage = null
        withContext(Dispatchers.IO) {
            try {
                val assetName = "aktu_syllabus_2026_27.pdf"
                val cacheFile = File(context.cacheDir, "aktu_syllabus_2026_27.pdf")
                if (!cacheFile.exists() || cacheFile.length() == 0L) {
                    context.assets.open(assetName).use { input ->
                        cacheFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }

                val pfd = ParcelFileDescriptor.open(cacheFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)
                val count = renderer.pageCount
                withContext(Dispatchers.Main) {
                    totalPages = count
                }

                val safeIndex = currentPageIndex.coerceIn(0, max(0, count - 1))
                val page = renderer.openPage(safeIndex)

                // High-resolution rendering (1080 width base scaled)
                val targetWidth = 1080
                val targetHeight = ((page.height.toFloat() / page.width.toFloat()) * targetWidth).toInt()

                val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                renderer.close()
                pfd.close()

                withContext(Dispatchers.Main) {
                    currentBitmap = bitmap
                    isLoading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage = e.localizedMessage ?: "Failed to render PDF page"
                    isLoading = false
                }
            }
        }
    }

    fun openInExternalApp() {
        try {
            val cacheFile = File(context.cacheDir, "aktu_syllabus_2026_27.pdf")
            if (!cacheFile.exists() || cacheFile.length() == 0L) {
                context.assets.open("aktu_syllabus_2026_27.pdf").use { input ->
                    cacheFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Open AKTU Syllabus PDF with..."))
        } catch (e: Exception) {
            Toast.makeText(context, "No external PDF viewer found: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "AKTU Syllabus PDF",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Page ${currentPageIndex + 1} of $totalPages • B.Tech CSE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Jump to Course Index
                    IconButton(onClick = { showTocSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = "Table of Contents"
                        )
                    }
                    // Open in external PDF viewer
                    IconButton(onClick = { openInExternalApp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open in External App"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 6.dp,
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = {
                            if (currentPageIndex > 0) currentPageIndex--
                        },
                        enabled = currentPageIndex > 0,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Prev")
                    }

                    // Page Quick Slider / Selector
                    Slider(
                        value = currentPageIndex.toFloat(),
                        onValueChange = { currentPageIndex = it.toInt() },
                        valueRange = 0f..max(0f, (totalPages - 1).toFloat()),
                        steps = max(0, totalPages - 2),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    )

                    FilledTonalButton(
                        onClick = {
                            if (currentPageIndex < totalPages - 1) currentPageIndex++
                        },
                        enabled = currentPageIndex < totalPages - 1,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Next")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surfaceDim)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 4f)
                        if (scale > 1f) {
                            val maxOffsetX = (1080f * (scale - 1f)) / 2f
                            val maxOffsetY = (1400f * (scale - 1f)) / 2f
                            offsetX = (offsetX + pan.x * scale).coerceIn(-maxOffsetX, maxOffsetX)
                            offsetY = (offsetY + pan.y * scale).coerceIn(-maxOffsetY, maxOffsetY)
                        } else {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(36.dp))
                        Text(
                            text = "Loading Page ${currentPageIndex + 1}...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                errorMessage != null -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "Failed to load page",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = { openInExternalApp() }) {
                            Text("Open in External Viewer")
                        }
                    }
                }
                currentBitmap != null -> {
                    Image(
                        bitmap = currentBitmap!!.asImageBitmap(),
                        contentDescription = "PDF Page ${currentPageIndex + 1}",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            )
                    )
                }
            }

            // Quick Zoom Controls Overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        scale = if (scale >= 2.5f) 1f else scale + 0.75f
                        if (scale == 1f) {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    },
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(
                        imageVector = if (scale > 1f) Icons.Default.ZoomOut else Icons.Default.ZoomIn,
                        contentDescription = "Zoom Toggle",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Table of Contents Modal Bottom Sheet
        if (showTocSheet) {
            ModalBottomSheet(
                onDismissRequest = { showTocSheet = false },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = "Jump to Course Page",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    AKTU_SYLLABUS_TOC.forEach { tocItem ->
                        val isCurrent = (currentPageIndex + 1) == tocItem.pageNumber
                        Surface(
                            onClick = {
                                currentPageIndex = tocItem.pageNumber - 1
                                showTocSheet = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tocItem.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "Page ${tocItem.pageNumber}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
