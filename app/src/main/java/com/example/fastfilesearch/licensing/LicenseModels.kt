package com.example.fastfilesearch.licensing

enum class LicenseStatus {
    ACTIVE,
    NOT_ACTIVATED,
    EXPIRED,
    CLOCK_TAMPERED,
    INVALID
}

data class LicensePayload(
    val v: Int = 1,
    val id: String,
    val n: String? = null,
    val iat: Long, // Unix Epoch Seconds
    val exp: Long, // Unix Epoch Seconds (0 = Permanent)
    val mw: Int = 0, // Max Workspaces (0 = Unlimited)
    val ex: Int = 1, // Allow Excel Export (1 = true, 0 = false)
    val lk: Int = 1, // Allow App Lock (1 = true, 0 = false)
    val bg: Int = 1  // Allow Background Sync (1 = true, 0 = false)
) {
    val isPermanent: Boolean get() = exp <= 0L

    val allowExcelExport: Boolean get() = ex == 1
    val allowAppLock: Boolean get() = lk == 1
    val allowBackgroundSync: Boolean get() = bg == 1
    val maxWorkspaces: Int get() = mw
}

data class LicenseVerificationResult(
    val status: LicenseStatus,
    val payload: LicensePayload? = null,
    val errorMessage: String? = null
)
