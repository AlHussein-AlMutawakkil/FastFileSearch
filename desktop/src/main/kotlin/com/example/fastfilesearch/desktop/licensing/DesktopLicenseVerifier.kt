package com.example.fastfilesearch.desktop.licensing

/**
 * Verifies 16-char V2 activation codes against a Windows hardware fingerprint + install_nonce.
 * Accepts ONLY Platform = Windows (1) or Both (2). Rejects Android-only or mismatched device/nonce codes.
 */
object DesktopLicenseVerifier {

    fun cleanLicenseCode(rawCode: String): String {
        return V2LicenseCodec.formatGroupedCode16(rawCode)
    }

    fun verify(
        rawCode: String,
        deviceFpType: Int = V2LicenseCodec.FP_TYPE_WIN,
        deviceHash6: ByteArray,
        installNonce: Int
    ): LicenseVerificationResult {
        if (rawCode.isBlank()) {
            return LicenseVerificationResult(
                status = LicenseStatus.INVALID,
                errorMessage = "كود الترخيص فارغ"
            )
        }

        if (!V2LicenseCodec.looksLikeV2LicenseCode(rawCode)) {
            return LicenseVerificationResult(
                status = LicenseStatus.INVALID,
                errorMessage = "صيغة كود الترخيص غير صحيحة (يجب أن يتكون من 16 حرفاً)"
            )
        }

        if (deviceHash6.size != 6 || installNonce !in 0..0xFFFF) {
            return LicenseVerificationResult(
                status = LicenseStatus.INVALID,
                errorMessage = "تعذّر التحقق من بصمة الجهاز"
            )
        }

        val decoded = V2LicenseCodec.decodeAndVerifyShortLicenseCode(
            rawCode = rawCode,
            expectedFpType = deviceFpType,
            expectedDeviceHash6 = deviceHash6,
            expectedInstallNonce = installNonce
        ) ?: return LicenseVerificationResult(
            status = LicenseStatus.INVALID,
            errorMessage = "كود الترخيص غير صحيح أو غير مخصص لهذا الجهاز"
        )

        if (decoded.platform != V2LicenseCodec.PLATFORM_WINDOWS &&
            decoded.platform != V2LicenseCodec.PLATFORM_BOTH
        ) {
            return LicenseVerificationResult(
                status = LicenseStatus.INVALID,
                errorMessage = "كود الترخيص مخصّص لنظام أندرويد وليس لويندوز"
            )
        }

        if (!decoded.allowAccess) {
            return LicenseVerificationResult(
                status = LicenseStatus.INVALID,
                errorMessage = "تم إيقاف صلاحية الوصول لهذا الترخيص"
            )
        }

        val derivedId = V2LicenseCodec.deriveInstallationId(deviceFpType, deviceHash6, installNonce)
        val payload = LicensePayload(
            v = 2,
            id = derivedId,
            iat = 0L,
            exp = decoded.expEpochSeconds,
            mw = decoded.maxFolders,
            ex = if (decoded.allowExcel) 1 else 0,
            lk = if (decoded.allowAppLock) 1 else 0,
            bg = if (decoded.allowAccess) 1 else 0,
            planType = decoded.planTypeName
        )

        return LicenseVerificationResult(
            status = LicenseStatus.ACTIVE,
            payload = payload
        )
    }
}
