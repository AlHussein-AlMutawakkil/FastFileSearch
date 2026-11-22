package com.example.fastfilesearch.ui

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalClipboardManager
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
                    .padding(horizontal = 14.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Instant Search Bar
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onQueryChanged,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "ابحث في محتوى الملفات (Excel, Word, PDF, نصوص)...",
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
                        EmptyFolderPrompt(onSelectFolderClick = onSelectFolderClick)
                    }

                    uiState.searchQuery.isBlank() -> {
                        ReadyToSearchPrompt(
                            folderName = uiState.folderName,
                            filesCount = uiState.filesCount,
                            segmentsCount = uiState.segmentsCount,
                            onRefreshClick = onRefreshClick,
                            isIndexing = uiState.indexProgress.isIndexing
                        )
                    }

                    uiState.searchResults.isEmpty() && !uiState.isSearching -> {
                        NoResultsPrompt(query = uiState.searchQuery)
                    }

                    else -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp, horizontal = 4.dp),
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
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(
                                items = groupedResults,
                                key = { it.fileUri }
                            ) { group ->
                                if (group.isDocument) {
                                    // Method #1: Grouped Card for Word (.docx) & PDF (.pdf)
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
                                } else {
                                    // Method #4: Authentic Official Excel Spreadsheet Table
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
 * Method #4: Official Authentic Microsoft Excel Spreadsheet Card.
 * Designed exactly like Excel with official green header, authentic sheet tab,
 * contiguous gridlines, column letters (A, B, C...) and row numbers, plus active selection border.
 */
@Composable
fun OfficialExcelTableCard(
    group: FileResultGroup,
    onOpenFile: () -> Unit,
    onCopyRow: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.5.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Official Excel Title Bar (Excel Brand Green)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ExcelDarkGreen)
                    .clickable(onClick = onOpenFile)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.TableChart,
                    contentDescription = "Excel",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = group.fileName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "${group.results.size} صف",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onOpenFile,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "فتح في إكسل",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // 2. Rows Presentation
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                group.results.forEach { result ->
                    val cells = remember(result.snippet) {
                        TableCellHelper.splitIntoCells(result.snippet)
                    }

                    val (sheetName, rowNumber) = remember(result.location) {
                        extractSheetAndRow(result.location)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ExcelGridLine, RoundedCornerShape(6.dp))
                            .clip(RoundedCornerShape(6.dp))
                    ) {
                        // Excel Sheet Tab & Row Control Strip
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ExcelHeaderGray)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Sheet Tab Look
                            Surface(
                                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                                color = Color.White,
                                border = androidx.compose.foundation.BorderStroke(1.dp, ExcelGridLine)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(ExcelDarkGreen)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = sheetName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ExcelHeaderText
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (cells.size > 2) {
                                    Text(
                                        text = "↔️ مرر لرؤية الأعمدة",
                                        fontSize = 10.sp,
                                        color = ExcelHeaderText.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(end = 6.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onCopyRow(result.snippet) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "نسخ الصف",
                                        tint = ExcelHeaderText,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = ExcelGridLine, thickness = 1.dp)

                        // 3. Official Excel Spreadsheet Grid with Contiguous Cells
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White)
                                .horizontalScroll(rememberScrollState())
                        ) {
                            Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                                // A) Excel Column Header Row: [ # ] | [ A ] | [ B ] | [ C ] ...
                                Row(
                                    modifier = Modifier
                                        .background(ExcelHeaderGray)
                                        .border(0.5.dp, ExcelHeaderBorder)
                                ) {
                                    // Row Header Corner
                                    Box(
                                        modifier = Modifier
                                            .width(44.dp)
                                            .height(26.dp)
                                            .border(0.5.dp, ExcelHeaderBorder)
                                            .background(ExcelHeaderGray),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "#",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ExcelHeaderText,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    // Columns A, B, C, D...
                                    cells.forEachIndexed { colIndex, _ ->
                                        Box(
                                            modifier = Modifier
                                                .widthIn(min = 100.dp)
                                                .height(26.dp)
                                                .border(0.5.dp, ExcelHeaderBorder)
                                                .background(ExcelHeaderGray)
                                                .padding(horizontal = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = getExcelColumnLetter(colIndex),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ExcelHeaderText,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }

                                // B) Excel Data Row: [ 45 ] | [ Data 1 ] | [ Data 2 (Active) ] | [ Data 3 ] ...
                                Row(
                                    modifier = Modifier
                                        .clickable(onClick = onOpenFile)
                                        .background(Color.White)
                                ) {
                                    // Row Number Cell (Left/Right margin)
                                    Box(
                                        modifier = Modifier
                                            .width(44.dp)
                                            .border(0.5.dp, ExcelHeaderBorder)
                                            .background(ExcelHeaderGray)
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = rowNumber,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ExcelHeaderText,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    // Actual Data Cells
                                    cells.forEach { cellText ->
                                        val isMatch = remember(cellText, result.query) {
                                            TableCellHelper.isCellMatching(cellText, result.query)
                                        }

                                        val highlightedCellText = remember(cellText, result.query) {
                                            TextHighlighter.highlight(cellText, result.query)
                                        }

                                        Box(
                                            modifier = Modifier
                                                .widthIn(min = 100.dp)
                                                .background(if (isMatch) ExcelSelectedBg else Color.White)
                                                .border(
                                                    width = if (isMatch) 2.dp else 0.5.dp,
                                                    color = if (isMatch) ExcelDarkGreen else ExcelGridLine,
                                                    shape = RectangleShape
                                                )
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            Text(
                                                text = highlightedCellText,
                                                fontSize = 13.sp,
                                                color = Color(0xFF202124),
                                                fontWeight = if (isMatch) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 3,
                                                overflow = TextOverflow.Ellipsis
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
