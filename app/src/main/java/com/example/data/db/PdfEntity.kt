package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pdf_files")
data class PdfEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val path: String,
    val sizeFormatted: String,
    val sizeBytes: Long = 1024 * 500,
    val pageCount: Int = 1,
    val dateModifiedFormatted: String = "Today",
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val category: String = "General",
    val extractedText: String? = null,
    val isPasswordProtected: Boolean = false
)
