package com.example.fastfilesearch.ui

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fastfilesearch.SearchUiState
import com.example.fastfilesearch.model.SearchResult
import com.example.fastfilesearch.parser.FileParserDispatcher
import kotlinx.coroutines.launch

data class FileResultGroup(
    val fileUri: String,
    val fileName: String,
    val isDocument: Boolean,
    val results: List<SearchResult>
)

// Official Microsoft Excel Colors
private val ExcelDarkGreen = Color(0xFF107C41)
private val ExcelHeaderGray = Color(0xFFF3F2F1)
private val ExcelGridLine = Color(0xFFD4D4D4)
private val ExcelHeaderBorder = Color(0xFFC8C6C4)
private val ExcelHeaderText = Color(0xFF505050)
private val ExcelSelectedBg = Color(0xFFE8F5E9)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    uiState: SearchUiState,
    onSelectFolderClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onClearQuery: () -> Unit,
    onOpenFile: (String) -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(uiState.userMessage) {
        val msg = uiState.userMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            onDismissMessage()
        }
    }

    // Group results by file URI: Word/PDF use Method #1 (Grouped), Excel/Text use Method #4 (Official Excel Grid)
    val groupedResults = remember(uiState.searchResults) {
        uiState.searchResults
            .groupBy { it.fileUri }
            .map { (uri, results) ->
                val fileName = results.firstOrNull()?.fileName ?: ""
                FileResultGroup(
                    fileUri = uri,
                    fileName = fileName,
                    isDocument = TableCellHelper.isDocumentType(fileName),
                    results = results
                )
            }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Column {
                    CenterAlignedTopAppBar(
                        title = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (uiState.folderName.isNotBlank()) uiState.folderName else "البحث السريع في الملفات",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (uiState.folderUri != null) {
                                    Text(
                                        text = "${uiState.filesCount} ملف مفهرس • ${uiState.segmentsCount} مقطع",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onSelectFolderClick) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = "اختيار مجلد",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        actions = {
                            val isIndexing = uiState.indexProgress.isIndexing
                            val rotation = if (isIndexing) {
                                val infiniteTransition = rememberInfiniteTransition(label = "spin")
                                val angle by infiniteTransition.animateFloat(
                                    initialValue = 0f,
                                    targetValue = 360f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1000, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Restart
                                    ),
                                    label = "spinAngle"
                                )
                                angle
                            } else 0f

                            IconButton(
                                onClick = onRefreshClick,
                                enabled = uiState.folderUri != null && !isIndexing
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "تحديث الفهرس",
                                    modifier = Modifier.rotate(rotation),
                                    tint = if (uiState.folderUri != null && !isIndexing)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    // Indexing Progress Bar
                    AnimatedVisibility(visible = uiState.indexProgress.isIndexing) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            val progress = uiState.indexProgress
                            if (progress.total > 0) {
                                LinearProgressIndicator(
                                    progress = { progress.current.toFloat() / progress.total.toFloat() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = progress.statusMessage,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Instant Search Bar
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onQueryChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    placeholder = {
                        Text(
                            text = "ابحث",
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "بحث",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (uiState.isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = onClearQuery) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "مسح"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Content View depending on State
                when {
                    uiState.folderUri == null -> {
                        Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                            EmptyFolderPrompt(onSelectFolderClick = onSelectFolderClick)
                        }
                    }

                    uiState.searchQuery.isBlank() -> {
                        Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                            ReadyToSearchPrompt(
                                folderName = uiState.folderName,
                                filesCount = uiState.filesCount,
                                segmentsCount = uiState.segmentsCount,
                                onRefreshClick = onRefreshClick,
                                isIndexing = uiState.indexProgress.isIndexing
                            )
                        }
                    }

                    uiState.searchResults.isEmpty() && !uiState.isSearching -> {
                        Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                            NoResultsPrompt(query = uiState.searchQuery)
                        }
                    }

                    else -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp, horizontal = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "تم العثور على ${uiState.searchResults.size} نتيجة في ${groupedResults.size} ملف",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(
                                items = groupedResults,
                                key = { it.fileUri }
                            ) { group ->
                                if (group.isDocument) {
                                    // Method #1: Grouped Card for Word (.docx) & PDF (.pdf) with padding
                                    Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                                        WordPdfGroupedCard(
                                            group = group,
                                            onOpenFile = { onOpenFile(group.fileUri) },
                                            onCopySnippet = { text ->
                                                clipboardManager.setText(AnnotatedString(text))
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("تم نسخ الفقرة إلى الحافظة")
                                                }
                                            }
                                        )
                                    }
                                } else {
                                    // Method #4: Pure flat data table replica matching screenshot (Edge-to-Edge)
                                    OfficialExcelTableCard(
                                        group = group,
                                        onOpenFile = { onOpenFile(group.fileUri) },
                                        onCopyRow = { text ->
                                            clipboardManager.setText(AnnotatedString(text))
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("تم نسخ بيانات الصف إلى الحافظة")
                                            }
                                        }
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

/**
 * Method #1: Grouped Document Card for Word (.docx) & PDF (.pdf).
 */
@Composable
fun WordPdfGroupedCard(
    group: FileResultGroup,
    onOpenFile: () -> Unit,
    onCopySnippet: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val visual = FileTypeHelper.getVisual(group.fileName)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Document Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenFile)
                    .background(visual.color.copy(alpha = 0.08f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(visual.color),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = visual.icon,
                        contentDescription = visual.typeName,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.fileName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${group.results.size} مطابقة في هذا المستند",
                        fontSize = 12.sp,
                        color = visual.color,
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(
                    onClick = onOpenFile,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "فتح الملف",
                        tint = visual.color,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            HorizontalDivider(color = visual.color.copy(alpha = 0.2f), thickness = 1.dp)

            // Document Snippets List
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                group.results.forEach { result ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .clickable(onClick = onOpenFile)
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(visual.color.copy(alpha = 0.12f))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    tint = visual.color,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = result.location,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = visual.color
                                )
                            }

                            IconButton(
                                onClick = { onCopySnippet(result.snippet) },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "نسخ الفقرة",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        val highlightedSnippet = remember(result.snippet, result.query) {
                            TextHighlighter.highlight(result.snippet, result.query)
                        }

                        Text(
                            text = highlightedSnippet,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Exact replica of the data table screen from the user's reference:
 * - Edge-to-edge dark table layout without any bulky cards, shadows, or outer paddings.
 * - Top header bar with file type icon, file name, and NO search/refresh icons.
 * - Table column header with ≡ menu icon above row numbers and "عمود 1", "عمود 2", "عمود 3"...
 * - Dark theme grid with row numbers on the side.
 * - Dropdown menu on row number click containing ONLY "نسخ" and "مشاركة".
 */
@Composable
fun OfficialExcelTableCard(
    group: FileResultGroup,
    onOpenFile: () -> Unit,
    onCopyRow: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val visual = remember(group.fileName) { FileTypeHelper.getVisual(group.fileName) }

    // Multi-sheet support
    val sheetGroups = remember(group.results) {
        group.results.groupBy { result ->
            extractSheetAndRow(result.location).first
        }
    }

    val onShareRow: (String) -> Unit = remember(context) {
        { text ->
            try {
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_TEXT, text)
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, "مشاركة بيانات الصف")
                context.startActivity(shareIntent)
            } catch (e: Exception) {
                // Ignore fallback
            }
        }
    }

    // Colors matching the dark screenshot exactly
    val topBarBg = Color(0xFF1C1F23)
    val colHeaderBg = Color(0xFF22262B)
    val tableBg = Color(0xFF131518)
    val rowAltBg = Color(0xFF16191D)
    val rowNumBg = Color(0xFF1B1E22)
    val gridBorderColor = Color(0xFF2D3137)
    val headerBorderColor = Color(0xFF33383F)
    val headerTextColor = Color(0xFFB5B9BF)
    val cellTextColor = Color(0xFFE4E7EB)
    val matchBg = Color(0xFF1A3822)
    val matchBorder = Color(0xFF388E3C)
    val popupBg = Color(0xFF202328)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(tableBg)
    ) {
        // 1. Top Bar matching the screenshot: File type icon on right, file name, and Open File button on left
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(topBarBg)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon representing the file type
            Icon(
                imageVector = visual.icon,
                contentDescription = visual.typeName,
                tint = visual.color,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            // File Name in place of "جدول سريع"
            Text(
                text = group.fileName,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Matches Count Badge
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color.White.copy(alpha = 0.12f)
            ) {
                Text(
                    text = "${group.results.size} صف",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Open File Button on the left (بدل زر البحث السابق في الصورة كما طلب المستخدم)
            IconButton(
                onClick = onOpenFile,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "فتح الملف",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        HorizontalDivider(color = gridBorderColor, thickness = 1.dp)

        // 2. Render Sheet Tables (Flat and direct)
        sheetGroups.forEach { (sheetName, sheetResults) ->
            val rowsData = remember(sheetResults) {
                sheetResults.map { res ->
                    val (_, rowNum) = extractSheetAndRow(res.location)
                    val cells = TableCellHelper.splitIntoCells(res.snippet)
                    Triple(rowNum, cells, res)
                }
            }
            val maxCols = remember(rowsData) {
                rowsData.maxOfOrNull { it.second.size }?.coerceAtLeast(1) ?: 1
            }

            // Sheet Name Strip if more than 1 sheet
            if (sheetGroups.size > 1 || sheetName != "ورقة 1") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF181B1F))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF107C41))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = sheetName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = headerTextColor
                    )
                }
                HorizontalDivider(color = gridBorderColor, thickness = 0.5.dp)
            }

            // State for which row number's DropdownMenu is open
            var menuExpandedRowIndex by remember { mutableStateOf<Int?>(null) }

            // 3. Exact Data Grid with Automatic Dynamic Column Width (Edge-to-Edge)
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val totalWidth = maxWidth
                val rowNumWidth = 44.dp
                val remainingWidth = if (totalWidth > rowNumWidth) totalWidth - rowNumWidth else 0.dp
                val minColWidth = 110.dp
                val totalNeededWidth = minColWidth * maxCols
                val dynamicColWidth = if (totalNeededWidth < remainingWidth && maxCols > 0) {
                    remainingWidth / maxCols
                } else {
                    minColWidth
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                        // Column Header Row: [≡] | [عمود 1] | [عمود 2] | [عمود 3] | ...
                        Row(
                            modifier = Modifier
                                .background(colHeaderBg)
                                .border(0.5.dp, headerBorderColor)
                        ) {
                            // Right Corner: [≡] Menu icon above row numbers (matching screenshot)
                            Box(
                                modifier = Modifier
                                    .width(rowNumWidth)
                                    .height(38.dp)
                                    .border(0.5.dp, headerBorderColor)
                                    .background(colHeaderBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "قائمة",
                                    tint = headerTextColor,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            // Column Titles: عمود 1, عمود 2, عمود 3... with dynamic auto width
                            for (colIndex in 0 until maxCols) {
                                Box(
                                    modifier = Modifier
                                        .width(dynamicColWidth)
                                        .height(38.dp)
                                        .border(0.5.dp, headerBorderColor)
                                        .background(colHeaderBg)
                                        .padding(horizontal = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "عمود ${colIndex + 1}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = headerTextColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        // Data Rows (NO click on row/cell opens file - only Open File button does)
                        rowsData.forEachIndexed { index, (rowNum, cells, result) ->
                            val isEven = index % 2 == 0
                            val rowBg = if (isEven) tableBg else rowAltBg

                            Row(
                                modifier = Modifier.background(rowBg),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Row Number Cell: Click opens DropdownMenu with ONLY Copy & Share
                                Box(
                                    modifier = Modifier
                                        .width(rowNumWidth)
                                        .height(IntrinsicSize.Min)
                                        .border(0.5.dp, headerBorderColor)
                                        .background(rowNumBg)
                                        .clickable { menuExpandedRowIndex = index }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = rowNum,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = headerTextColor,
                                        fontFamily = FontFamily.Monospace
                                    )

                                    // Popup Menu on Row Number click (ONLY "نسخ" and "مشاركة" as requested)
                                    DropdownMenu(
                                        expanded = menuExpandedRowIndex == index,
                                        onDismissRequest = { menuExpandedRowIndex = null },
                                        modifier = Modifier
                                            .background(popupBg)
                                            .border(0.5.dp, gridBorderColor, RoundedCornerShape(8.dp))
                                    ) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "نسخ",
                                                    color = Color.White,
                                                    fontSize = 14.sp
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.ContentCopy,
                                                    contentDescription = "نسخ",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            onClick = {
                                                onCopyRow(result.snippet)
                                                menuExpandedRowIndex = null
                                            }
                                        )
                                        HorizontalDivider(color = gridBorderColor, thickness = 0.5.dp)
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "مشاركة",
                                                    color = Color.White,
                                                    fontSize = 14.sp
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Share,
                                                    contentDescription = "مشاركة",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            onClick = {
                                                onShareRow(result.snippet)
                                                menuExpandedRowIndex = null
                                            }
                                        )
                                    }
                                }

                                // Data Cells (Auto dynamic width, NO click opens file)
                                for (colIndex in 0 until maxCols) {
                                    val cellText = cells.getOrNull(colIndex) ?: ""
                                    val isMatch = remember(cellText, result.query) {
                                        TableCellHelper.isCellMatching(cellText, result.query)
                                    }
                                    val highlightedCellText = remember(cellText, result.query) {
                                        TextHighlighter.highlight(cellText, result.query)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .width(dynamicColWidth)
                                            .background(if (isMatch) matchBg else Color.Transparent)
                                            .border(
                                                width = if (isMatch) 1.5.dp else 0.5.dp,
                                                color = if (isMatch) matchBorder else gridBorderColor,
                                                shape = RectangleShape
                                            )
                                            .padding(horizontal = 10.dp, vertical = 10.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(
                                            text = highlightedCellText,
                                            fontSize = 13.sp,
                                            color = cellTextColor,
                                            fontWeight = if (isMatch) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 5,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = gridBorderColor, thickness = 1.dp)
        }
    }
}

/**
 * Extracts sheet name and row number from location string.
 */
private fun extractSheetAndRow(location: String): Pair<String, String> {
    var sheet = "ورقة 1"
    var row = "1"

    if (location.contains("ورقة:")) {
        val parts = location.split("،")
        sheet = parts.getOrNull(0)?.substringAfter("ورقة:")?.trim() ?: "ورقة 1"
        row = parts.getOrNull(1)?.replace("صف:", "")?.replace("صف", "")?.trim() ?: "1"
    } else if (location.contains("سطر")) {
        sheet = "بيانات جدولية"
        row = location.replace("سطر", "").replace(":", "").trim()
    } else {
        row = location.filter { it.isDigit() }.ifEmpty { "1" }
    }

    return Pair(sheet, row)
}

/**
 * Returns Excel column letters: 0 -> A, 1 -> B, 25 -> Z, 26 -> AA, 27 -> AB...
 */
private fun getExcelColumnLetter(index: Int): String {
    var n = index
    val sb = StringBuilder()
    while (n >= 0) {
        sb.insert(0, ('A'.code + (n % 26)).toChar())
        n = n / 26 - 1
    }
    return sb.toString()
}

@Composable
fun EmptyFolderPrompt(
    onSelectFolderClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "اختر مجلداً للبدء",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "قم بتحديد المجلد الذي يحتوي على مستنداتك وسيقوم التطبيق بفهرستها ومزامنتها تلقائياً للبحث الفوري دون إنترنت.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        FilledTonalButton(
            onClick = onSelectFolderClick,
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "تحديد مجلد المستندات", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ReadyToSearchPrompt(
    folderName: String,
    filesCount: Int,
    segmentsCount: Int,
    onRefreshClick: () -> Unit,
    isIndexing: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "جاهز للبحث الفوري",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "المجلد الحالي: $folderName\nتمت فهرسة $filesCount ملف بنجاح ($segmentsCount مقطع جاهز)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Supported Formats Pills
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FormatBadge(name = "Excel (جداول رسمية)", color = ExcelDarkGreen)
            FormatBadge(name = "Word (تجميعي)", color = Color(0xFF185ABD))
            FormatBadge(name = "PDF (تجميعي)", color = Color(0xFFD32F2F))
            FormatBadge(name = "نصوص وجداول", color = Color(0xFFE65100))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "يدعم البحث الفوري بجميع صيغ الحروف العربية (أ/إ/آ، ة/ه، ى/ي) ومع أو بدون التشكيل.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FormatBadge(name: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = name,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun NoResultsPrompt(
    query: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(60.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "لم يتم العثور على نتائج",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "لا توجد أي مطابقة للكلمة \"$query\" داخل الملفات المفهرسة.\nجرب كلمة أخرى أو تأكد من مزامنة المجلد.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
