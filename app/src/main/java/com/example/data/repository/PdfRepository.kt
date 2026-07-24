package com.example.data.repository

import android.content.Context
import com.example.data.db.AppDatabase
import com.example.data.db.PdfDao
import com.example.data.db.PdfEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class PdfRepository(private val pdfDao: PdfDao) {

    val allPdfs: Flow<List<PdfEntity>> = pdfDao.getAllPdfs()
    val recentPdfs: Flow<List<PdfEntity>> = pdfDao.getRecentPdfs()
    val favoritePdfs: Flow<List<PdfEntity>> = pdfDao.getFavoritePdfs()

    fun searchPdfs(query: String): Flow<List<PdfEntity>> = pdfDao.searchPdfs(query)

    fun getPdfById(id: Int): Flow<PdfEntity?> = pdfDao.getPdfById(id)

    suspend fun getPdfByPath(path: String): PdfEntity? = pdfDao.getPdfByPath(path)

    suspend fun getPdfByTitleAndSize(title: String, sizeBytes: Long): PdfEntity? = pdfDao.getPdfByTitleAndSize(title, sizeBytes)

    suspend fun insertPdf(pdf: PdfEntity): Long = pdfDao.insertPdf(pdf)

    suspend fun updatePdf(pdf: PdfEntity) = pdfDao.updatePdf(pdf)

    suspend fun deletePdf(pdf: PdfEntity) = pdfDao.deletePdf(pdf)

    suspend fun toggleFavorite(id: Int, currentFav: Boolean) {
        pdfDao.setFavorite(id, !currentFav)
    }

    suspend fun deleteById(id: Int) = pdfDao.deleteById(id)

    suspend fun seedInitialDataIfEmpty() {
        // No seed/demo data created for production app.
    }

    companion object {
        @Volatile
        private var INSTANCE: PdfRepository? = null

        fun getInstance(context: Context): PdfRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getDatabase(context)
                val repo = PdfRepository(db.pdfDao())
                CoroutineScope(Dispatchers.IO).launch {
                    repo.seedInitialDataIfEmpty()
                }
                INSTANCE = repo
                repo
            }
        }
    }
}
