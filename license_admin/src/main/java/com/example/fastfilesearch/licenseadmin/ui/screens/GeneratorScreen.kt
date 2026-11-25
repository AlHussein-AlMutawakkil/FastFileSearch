package com.example.fastfilesearch.licenseadmin.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fastfilesearch.licenseadmin.R
import com.example.fastfilesearch.licenseadmin.engine.AdminLicenseGenerator
import com.example.fastfilesearch.licenseadmin.engine.ClientDeviceInfo
import com.example.fastfilesearch.licenseadmin.engine.ParsedActivationRequest
import com.example.fastfilesearch.licenseadmin.engine.V2LicenseCodec
import com.example.fastfilesearch.licenseadmin.history.IssuedLicenseItem
import com.example.fastfilesearch.licenseadmin.history.LicenseHistoryManager
import com.example.fastfilesearch.licenseadmin.history.LicenseRenewalDraft
import com.example.fastfilesearch.licenseadmin.security.KeyStorageManager
import com.example.fastfilesearch.licenseadmin.sync.LicenseLogSyncManager
import com.example.fastfilesearch.licenseadmin.ui.components.AppPrimaryButton
import com.example.fastfilesearch.licenseadmin.ui.components.AppSecondaryButton
import com.example.fastfilesearch.licenseadmin.ui.components.AppSectionCard
import com.example.fastfilesearch.licenseadmin.ui.components.AppTextField
import com.example.fastfilesearch.licenseadmin.ui.components.AppToggleRow
import com.example.fastfilesearch.licenseadmin.ui.components.AppTopBar
import com.example.fastfilesearch.licenseadmin.ui.theme.AdminDesignTokens
import com.example.fastfilesearch.licenseadmin.ui.theme.LocalAdminColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GeneratorScreen(
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    renewalDraft: LicenseRenewalDraft? = null,
    onConsumeRenewalDraft: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalAdminColors.current

    var hasKey by remember { mutableStateOf(KeyStorageManager.hasKey(context)) }
    var importErrorMessage by remember { mutableStateOf<String?>(null) }
    var showPasteKeyDialog by remember { mutableStateOf(false) }
    var pastedKeyText by remember { mutableStateOf("") }

    var installationId by remember { mutableStateOf("") }
    var parsedR2Request by remember { mutableStateOf<ParsedActivationRequest?>(null) }
    var clientDeviceInfo by remember { mutableStateOf<ClientDeviceInfo?>(null) }
    var clientDeviceId by remember { mutableStateOf<String?>(null) }
    var clientName by remember { mutableStateOf("") }
    var generatedCode by remember { mutableStateOf<String?>(null) }
    var generationError by remember { mutableStateOf<String?>(null) }

    fun handleInstallationInput(input: String) {
        val parsed = AdminLicenseGenerator.parseActivationRequest(input)
        installationId = parsed.installationId
        if (parsed.isR2Request) {
            parsedR2Request = parsed
            clientDeviceInfo = parsed.deviceInfo
            clientDeviceId = parsed.clientDeviceId
        } else if (input.contains("BAR1.", ignoreCase = true) || input.isBlank()) {
            parsedR2Request = null
            clientDeviceInfo = parsed.deviceInfo
            clientDeviceId = parsed.clientDeviceId
        } else if (parsed.deviceInfo != null || parsed.clientDeviceId != null) {
            parsedR2Request = null
            clientDeviceInfo = parsed.deviceInfo
            clientDeviceId = parsed.clientDeviceId
        } else {
            if (parsed.installationId != parsedR2Request?.installationId) {
                parsedR2Request = null
            }
        }
        generationError = null
    }

    val durationOptions = remember {
        listOf(
            Triple("7 أيام", 7L, "trial"),
            Triple("30 يوماً", 30L, "monthly"),
            Triple("90 يوماً", 90L, "quarterly"),
            Triple("سنة", 365L, "yearly"),
            Triple("دائم", 0L, "lifetime"),
            Triple("مخصص...", -1L, "custom")
        )
    }
    var selectedDurationIndex by remember { mutableIntStateOf(1) } // Default 30 days
    var customDaysText by remember { mutableStateOf("60") }

    var maxWorkspacesText by remember { mutableStateOf("0") }
    var excelExport by remember { mutableStateOf(true) }
    var appLock by remember { mutableStateOf(true) }
    var bgSync by remember { mutableStateOf(true) }

    LaunchedEffect(renewalDraft?.requestId) {
        val draft = renewalDraft ?: return@LaunchedEffect
        val item = draft.item
        installationId = item.installationId
        clientName = item.clientName ?: ""
        val savedCdid = item.clientDeviceId.takeIf { it.isNotBlank() }
        clientDeviceId = savedCdid
        val hasDeviceMeta = item.manufacturer.isNotBlank() ||
            item.model.isNotBlank() ||
            item.osVersion.isNotBlank() ||
            item.abi.isNotBlank() ||
            item.appVersion.isNotBlank()
        clientDeviceInfo = if (hasDeviceMeta) {
            ClientDeviceInfo(
                manufacturer = item.manufacturer,
                model = item.model,
                osVersion = item.osVersion,
                abi = item.abi,
                appVersion = item.appVersion,
                clientDeviceId = savedCdid
            )
        } else {
            null
        }
        val hexStr = item.r2DeviceHash6Hex
        val restoredHash6 = if (hexStr != null && hexStr.length == 12) {
            try {
                ByteArray(6) { i -> hexStr.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
        parsedR2Request = if (item.r2FpType != null && restoredHash6 != null && item.r2InstallNonce != null) {
            ParsedActivationRequest(
                installationId = item.installationId,
                deviceInfo = clientDeviceInfo,
                clientDeviceId = savedCdid,
                platformCode = item.platformCode ?: V2LicenseCodec.PLATFORM_ANDROID,
                fpType = item.r2FpType,
                deviceHash6 = restoredHash6,
                installNonce = item.r2InstallNonce
            )
        } else {
            null
        }
        maxWorkspacesText = item.maxWorkspaces.toString()
        excelExport = item.excelExport
        appLock = item.appLock
        bgSync = item.bgSync
        if (item.exp <= 0L) {
            selectedDurationIndex = 4
        } else {
            val days = ((item.exp - item.iat + 43200L) / 86400L).coerceAtLeast(1L)
            when (days) {
                7L -> selectedDurationIndex = 0
                30L -> selectedDurationIndex = 1
                90L -> selectedDurationIndex = 2
                365L -> selectedDurationIndex = 3
                else -> {
                    selectedDurationIndex = 5
                    customDaysText = days.toString()
                }
            }
        }
        generatedCode = null
        generationError = null
        onConsumeRenewalDraft()
    }

    fun processUri(uri: Uri?) {
        if (uri == null) return
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val result = KeyStorageManager.importKeyFromStream(context, stream)
                if (result.isSuccess) {
                    hasKey = true
                    importErrorMessage = null
                    Toast.makeText(context, "تم استيراد وتوثيق المفتاح الخاص بنجاح!", Toast.LENGTH_SHORT).show()
                } else {
                    importErrorMessage = result.exceptionOrNull()?.message ?: "فشل استيراد المفتاح"
                }
            } ?: run {
                importErrorMessage = "تعذر فتح ملف المفتاح من هذا المسار"
            }
        } catch (e: Exception) {
            importErrorMessage = "خطأ في قراءة الملف: ${e.message}"
        }
    }

    // Universal Chooser File Picker: 100% compatible across all Android versions and OEM managers
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            processUri(uri)
        }
    }

    fun launchSafeFilePicker() {
        importErrorMessage = null
        try {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            val chooser = Intent.createChooser(intent, "اختر ملف المفتاح (private_key.pem)")
            filePickerLauncher.launch(chooser)
        } catch (_: Exception) {
            try {
                val openDocIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    type = "*/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
                filePickerLauncher.launch(openDocIntent)
            } catch (e2: Exception) {
                importErrorMessage = "تعذر فتح مدير الملفات (${e2.localizedMessage}). يمكنك استخدام زر لصق نص المفتاح مباشرة."
            }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(colors.screenBackground)
        ) {
            // Top Bar
            AppTopBar(
                title = "بحث آمن - تفعيل",
                actions = {
                    IconButton(
                        onClick = onNavigateToHistory,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = "السجل",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "الإعدادات",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .imePadding()
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // If private key is NOT imported yet, display import banner
                if (!hasKey) {
                    KeyImportBanner(
                        errorMessage = importErrorMessage,
                        onImportFileClick = {
                            launchSafeFilePicker()
                        },
                        onPasteKeyClick = {
                            // Check if clipboard already contains private key
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            val clipItem = clipboard?.primaryClip?.getItemAt(0)
                            val text = clipItem?.text?.toString()?.trim()
                            if (!text.isNullOrBlank() && (text.contains("BEGIN PRIVATE KEY") || text.contains("BEGIN EC PRIVATE KEY"))) {
                                val result = KeyStorageManager.importKeyFromPem(context, text)
                                if (result.isSuccess) {
                                    hasKey = true
                                    importErrorMessage = null
                                    Toast.makeText(context, "تم استيراد المفتاح من الحافظة وتوثيقه بنجاح!", Toast.LENGTH_SHORT).show()
                                } else {
                                    pastedKeyText = text
                                    showPasteKeyDialog = true
                                }
                            } else {
                                pastedKeyText = text ?: ""
                                showPasteKeyDialog = true
                            }
                        }
                    )
                } else {
                    // 1. Unified Request / Installation ID Field
                    AppTextField(
                        value = installationId,
                        onValueChange = {
                            handleInstallationInput(it)
                        },
                        label = "كود طلب التفعيل / معرّف الجهاز *",
                        placeholder = "مثال: كود R2... أو BAR1... أو BA-A2B3-C4D5-E6F7",
                        isMonospace = true,
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val item = clipboard?.primaryClip?.getItemAt(0)
                                    val text = item?.text?.toString()?.trim()
                                    if (!text.isNullOrBlank()) {
                                        handleInstallationInput(text)
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentPaste,
                                    contentDescription = "لصق من الحافظة",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    )

                    // Client Device Info Card (displayed if request contained device info)
                    if (clientDeviceInfo != null && clientDeviceInfo?.isAvailable == true) {
                        val dev = clientDeviceInfo!!
                        AppSectionCard {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp)
                                ) {
                                    Text(
                                        text = "بيانات جهاز العميل",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    if (dev.manufacturer.isNotBlank()) {
                                        DeviceDetailRow(title = "نوع الهاتف", value = dev.manufacturer)
                                    }
                                    if (dev.model.isNotBlank()) {
                                        DeviceDetailRow(title = "موديل الهاتف", value = dev.model)
                                    }
                                    if (dev.osVersion.isNotBlank()) {
                                        DeviceDetailRow(title = "إصدار النظام", value = dev.osVersion)
                                    }
                                    if (dev.abi.isNotBlank()) {
                                        DeviceDetailRow(title = "المعمارية", value = dev.abi)
                                    }
                                    if (dev.appVersion.isNotBlank()) {
                                        DeviceDetailRow(title = "إصدار التطبيق", value = dev.appVersion)
                                    }
                                }
                            }
                        }
                    }

                    // 2. Client Name Field
                    AppTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = "اسم العميل / الجهة (اختياري)",
                        placeholder = "مثال: شركة الأفق للتقنية"
                    )

                    // 3. Duration Selection
                    Column {
                        Text(
                            text = "مدة الصلاحية",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            durationOptions.forEachIndexed { index, pair ->
                                val isSelected = index == selectedDurationIndex
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(AdminDesignTokens.ChipCornerRadius))
                                        .background(if (isSelected) colors.controlActiveBackground else colors.cardBackground)
                                        .border(
                                            AdminDesignTokens.DividerThickness,
                                            if (isSelected) colors.controlActiveBackground else colors.divider,
                                            RoundedCornerShape(AdminDesignTokens.ChipCornerRadius)
                                        )
                                        .clickable { selectedDurationIndex = index }
                                        .padding(horizontal = 14.dp, vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = pair.first,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) colors.controlActiveContent else colors.textPrimary
                                    )
                                }
                            }
                        }

                        // Custom days input if "مخصص" is selected
                        if (selectedDurationIndex == 5) {
                            Spacer(modifier = Modifier.height(10.dp))
                            AppTextField(
                                value = customDaysText,
                                onValueChange = { customDaysText = it.filter { ch -> ch.isDigit() } },
                                label = "عدد الأيام المخصص",
                                placeholder = "مثال: 60",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }

                    // 4. Max Workspaces
                    AppTextField(
                        value = maxWorkspacesText,
                        onValueChange = { maxWorkspacesText = it.filter { ch -> ch.isDigit() } },
                        label = "الحد الأقصى للمجلدات (0 = غير محدود)",
                        placeholder = "0",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    // 5. Permission Toggles
                    AppSectionCard {
                        item {
                            AppToggleRow(
                                title = "تصدير Excel (.xlsx)",
                                subtitle = "السماح بتصدير نتائج البحث كملف إكسل",
                                icon = Icons.Outlined.Description,
                                checked = excelExport,
                                onCheckedChange = { excelExport = it }
                            )
                        }
                        item {
                            AppToggleRow(
                                title = "قفل التطبيق بالبصمة",
                                subtitle = "تمكين ميزة قفل التطبيق بالبصمة ورمز الدخول",
                                icon = Icons.Outlined.Lock,
                                checked = appLock,
                                onCheckedChange = { appLock = it }
                            )
                        }
                        item {
                            AppToggleRow(
                                title = "المزامنة التلقائية للمجلدات",
                                subtitle = "تمكين المزامنة في الخلفية",
                                icon = Icons.Outlined.Sync,
                                checked = bgSync,
                                onCheckedChange = { bgSync = it }
                            )
                        }
                    }

                    // Error text if generation failed
                    if (generationError != null) {
                        Text(
                            text = generationError ?: "",
                            color = colors.statusError,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    // Generate Button
                    AppPrimaryButton(
                        text = "توليد كود الترخيص",
                        icon = Icons.Outlined.Key,
                        onClick = {
                            val activeR2 = parsedR2Request
                            val cleanId = AdminLicenseGenerator.cleanInstallationId(installationId)
                            if (cleanId.isBlank() && activeR2 == null) {
                                generationError = "يرجى إدخال معرّف التثبيت."
                                return@AppPrimaryButton
                            }

                            val selectedPlan = durationOptions[selectedDurationIndex]
                            val days = if (selectedDurationIndex == 5) {
                                customDaysText.toLongOrNull() ?: 30L
                            } else {
                                selectedPlan.second
                            }
                            val planType = selectedPlan.third
                            val maxWs = maxWorkspacesText.toIntOrNull() ?: 0
                            val nowSeconds = System.currentTimeMillis() / 1000L

                            val result: Result<String>
                            if (activeR2 != null && activeR2.isR2Request) {
                                result = AdminLicenseGenerator.generateV2License(
                                    parsedRequest = activeR2,
                                    durationDays = days,
                                    maxWorkspaces = maxWs,
                                    excelExport = excelExport,
                                    appLock = appLock,
                                    allowAccess = bgSync,
                                    platform = activeR2.platformCode ?: V2LicenseCodec.PLATFORM_ANDROID,
                                    planType = planType,
                                    nowSeconds = nowSeconds
                                )
                            } else {
                                val privKey = KeyStorageManager.getPrivateKey(context)
                                if (privKey == null) {
                                    generationError = "تعذر قراءة المفتاح الخاص مشفّراً."
                                    return@AppPrimaryButton
                                }
                                result = AdminLicenseGenerator.generateLicense(
                                    privateKey = privKey,
                                    installationId = cleanId,
                                    clientName = clientName.takeIf { it.isNotBlank() },
                                    durationDays = days,
                                    maxWorkspaces = maxWs,
                                    excelExport = excelExport,
                                    appLock = appLock,
                                    bgSync = bgSync
                                )
                            }

                            if (result.isSuccess) {
                                val code = result.getOrThrow()
                                generatedCode = code
                                generationError = null

                                // Save to visible History
                                val effectiveId = activeR2?.installationId ?: cleanId
                                val trimmedClientName = clientName.trim().takeIf { it.isNotBlank() }
                                val payload = AdminLicenseGenerator.parsePayload(
                                    licenseCode = code,
                                    installationId = effectiveId,
                                    clientName = trimmedClientName,
                                    issuedAt = nowSeconds
                                )
                                LicenseHistoryManager.addLicense(
                                    context,
                                    IssuedLicenseItem(
                                        installationId = payload.id,
                                        clientName = payload.n,
                                        iat = payload.iat,
                                        exp = payload.exp,
                                        maxWorkspaces = payload.mw,
                                        excelExport = payload.ex == 1,
                                        appLock = payload.lk == 1,
                                        bgSync = payload.bg == 1,
                                        licenseCode = code,
                                        manufacturer = clientDeviceInfo?.manufacturer ?: "",
                                        model = clientDeviceInfo?.model ?: "",
                                        osVersion = clientDeviceInfo?.osVersion ?: "",
                                        abi = clientDeviceInfo?.abi ?: "",
                                        appVersion = clientDeviceInfo?.appVersion ?: "",
                                        clientDeviceId = (clientDeviceId ?: clientDeviceInfo?.clientDeviceId).orEmpty(),
                                        r2FpType = activeR2?.fpType,
                                        r2DeviceHash6Hex = activeR2?.deviceHash6?.let { V2LicenseCodec.bytesToHexLower(it) },
                                        r2InstallNonce = activeR2?.installNonce,
                                        platformCode = activeR2?.platformCode
                                    )
                                )

                                // Save silently in hidden Room queue & schedule WorkManager upload
                                val pendingEntity = LicenseLogSyncManager.buildPendingUploadEntity(
                                    context = context,
                                    payload = payload,
                                    licenseCode = code,
                                    deviceInfo = clientDeviceInfo,
                                    clientDeviceId = clientDeviceId ?: clientDeviceInfo?.clientDeviceId,
                                    planType = planType,
                                    durationDays = days.toInt()
                                )
                                LicenseLogSyncManager.enqueueLicenseUpload(context, pendingEntity)

                                Toast.makeText(context, "تم توليد وتوثيق كود الترخيص بنجاح!", Toast.LENGTH_SHORT).show()
                            } else {
                                generationError = result.exceptionOrNull()?.message ?: "فشل توليد الترخيص"
                            }
                        }
                    )

                    // 6. Generated Output Card
                    if (generatedCode != null) {
                        LicenseOutputCard(
                            licenseCode = generatedCode!!,
                            onCopy = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("License Code", generatedCode!!)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "تم نسخ كود الترخيص إلى الحافظة!", Toast.LENGTH_SHORT).show()
                            },
                            onShare = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, generatedCode!!)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "مشاركة كود الترخيص"))
                            }
                        )
                    }
                }
            }
        }

        // Paste Key Dialog
        if (showPasteKeyDialog) {
            AlertDialog(
                onDismissRequest = { showPasteKeyDialog = false },
                containerColor = colors.cardBackground,
                shape = RoundedCornerShape(AdminDesignTokens.CardCornerRadius),
                title = {
                    Text(
                        text = "لصق نص المفتاح الخاص (private_key.pem)",
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        fontSize = 16.sp
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "الصق نص المفتاح الكامل بما يشمل -----BEGIN PRIVATE KEY-----:",
                            color = colors.textSecondary,
                            fontSize = 13.sp
                        )
                        AppTextField(
                            value = pastedKeyText,
                            onValueChange = { pastedKeyText = it },
                            placeholder = "-----BEGIN PRIVATE KEY-----\nMIG2AgEAMBAGByqGSM49AgEGBSuBBAAiBIGeMIGbAgEBBD...\n-----END PRIVATE KEY-----",
                            singleLine = false,
                            isMonospace = true,
                            modifier = Modifier.height(150.dp)
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (pastedKeyText.isNotBlank()) {
                                val res = KeyStorageManager.importKeyFromPem(context, pastedKeyText)
                                if (res.isSuccess) {
                                    hasKey = true
                                    importErrorMessage = null
                                    showPasteKeyDialog = false
                                    Toast.makeText(context, "تم استيراد المفتاح وتوثيقه بنجاح!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, res.exceptionOrNull()?.message ?: "فشل الاستيراد", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = colors.textPrimary)
                    ) {
                        Text("استيراد وتوثيق", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showPasteKeyDialog = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = colors.textSecondary)
                    ) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }
}

@Composable
fun KeyImportBanner(
    errorMessage: String?,
    onImportFileClick: () -> Unit,
    onPasteKeyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAdminColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AdminDesignTokens.CardCornerRadius))
            .background(colors.cardBackground)
            .border(
                AdminDesignTokens.DividerThickness,
                colors.divider,
                RoundedCornerShape(AdminDesignTokens.CardCornerRadius)
            )
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(colors.subCardBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_encrypted_shield),
                contentDescription = null,
                tint = colors.textPrimary,
                modifier = Modifier.size(32.dp)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "استيراد المفتاح الخاص (private_key.pem)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Text(
                text = "يتطلب توليد التراخيص اختيار ملف المفتاح الخاص أو لصق نصه مباشرة. يتم تشفير المفتاح وحفظه محلياً بأمان تام عبر Android KeyStore.",
                fontSize = 13.sp,
                color = colors.textSecondary,
                lineHeight = 18.sp
            )
        }

        if (errorMessage != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.statusError.copy(alpha = 0.1f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = colors.statusError,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = errorMessage,
                    color = colors.statusError,
                    fontSize = 12.sp
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppPrimaryButton(
                text = "اختيار ملف المفتاح (.pem)",
                icon = Icons.Outlined.FileOpen,
                onClick = onImportFileClick
            )

            AppSecondaryButton(
                text = "لصق نص المفتاح مباشرة (Paste)",
                icon = Icons.Outlined.ContentPaste,
                onClick = onPasteKeyClick
            )
        }
    }
}

@Composable
fun LicenseOutputCard(
    licenseCode: String,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAdminColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AdminDesignTokens.CardCornerRadius))
            .background(colors.cardBackground)
            .border(
                AdminDesignTokens.DividerThickness,
                colors.divider,
                RoundedCornerShape(AdminDesignTokens.CardCornerRadius)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(colors.statusSuccess),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = colors.controlActiveContent,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = "كود الترخيص الرقمي الموثّق:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AdminDesignTokens.SubCardCornerRadius))
                .background(colors.subCardBackground)
                .padding(14.dp)
        ) {
            Text(
                text = licenseCode,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = colors.textPrimary,
                lineHeight = 18.sp
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                AppPrimaryButton(
                    text = "نسخ الكود",
                    icon = Icons.Outlined.ContentCopy,
                    onClick = onCopy
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                AppSecondaryButton(
                    text = "مشاركة (واتساب)",
                    icon = Icons.Outlined.Share,
                    onClick = onShare
                )
            }
        }
    }
}

@Composable
private fun DeviceDetailRow(title: String, value: String) {
    val colors = LocalAdminColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            color = colors.textSecondary
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textPrimary
        )
    }
}
