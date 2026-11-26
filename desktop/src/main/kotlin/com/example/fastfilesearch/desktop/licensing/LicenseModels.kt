package com.example.fastfilesearch.desktop.licensing

enum class LicenseStatus {
    ACTIVE,
    NOT_ACTIVATED,
    EXPIRED,
    CLOCK_TAMPERED,
    INVALID
}

data class LicensePayload(
    val v: Int = 2,
    val id: String,
    val iat: Long = 0L,
    val exp: Long = 0L,
    val mw: Int = 0,
    val ex: Int = 1,
    val lk: Int = 1,
    val bg: Int = 1,
    val planType: String = "custom"
) {
    val isPermanent: Boolean get() = exp <= 0L
    val allowExcelExport: Boolean get() = ex == 1
    val allowAppLock: Boolean get() = lk == 1
    val allowAccess: Boolean get() = bg == 1
    val maxWorkspaces: Int get() = mw
}

data class LicenseVerificationResult(
    val status: LicenseStatus,
    val payload: LicensePayload? = null,
    val errorMessage: String? = null
)
