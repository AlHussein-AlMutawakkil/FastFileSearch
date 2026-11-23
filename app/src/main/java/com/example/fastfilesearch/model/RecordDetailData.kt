package com.example.fastfilesearch.model

data class RecordDetailData(
    val fileName: String,
    val sheetName: String?,
    val rowNumber: String,
    val cells: List<String>,
    val headers: List<String>,
    val query: String = "",
    val searchFilter: SearchFilter = SearchFilter()
)
