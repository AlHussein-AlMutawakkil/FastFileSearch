package com.example.fastfilesearch.model

data class Workspace(
    val id: String,
    val name: String,
    val uri: String,
    val addedAt: Long = System.currentTimeMillis()
)
