package com.example.fastfilesearch.licenseadmin.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_uploads")
data class PendingUploadEntity(
    @PrimaryKey(autoGenerate = true)
    val localId: Long = 0L,

    @ColumnInfo(name = "issued_at")
    val issuedAt: String,

    @ColumnInfo(name = "issuer_id")
    val issuerId: String,

    @ColumnInfo(name = "issuer_device")
    val issuerDevice: String,

    @ColumnInfo(name = "customer_name")
    val customerName: String?,

    @ColumnInfo(name = "installation_id")
    val installationId: String,

    @ColumnInfo(name = "client_device_id")
    val clientDeviceId: String?,

    @ColumnInfo(name = "client_manufacturer")
    val clientManufacturer: String?,

    @ColumnInfo(name = "client_model")
    val clientModel: String?,

    @ColumnInfo(name = "client_os")
    val clientOs: String?,

    @ColumnInfo(name = "client_abi")
    val clientAbi: String?,

    @ColumnInfo(name = "client_app_version")
    val clientAppVersion: String?,

    @ColumnInfo(name = "plan_type")
    val planType: String,

    @ColumnInfo(name = "duration_days")
    val durationDays: Int,

    @ColumnInfo(name = "expires_at")
    val expiresAt: String?,

    @ColumnInfo(name = "max_folders")
    val maxFolders: Int,

    @ColumnInfo(name = "allow_excel")
    val allowExcel: Boolean,

    @ColumnInfo(name = "allow_app_lock")
    val allowAppLock: Boolean,

    @ColumnInfo(name = "license_code")
    val licenseCode: String,

    @ColumnInfo(name = "attempts")
    val attempts: Int = 0
)
