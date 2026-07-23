package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfDao {
    @Query("SELECT * FROM pdf_files ORDER BY timestamp DESC")
    fun getAllPdfs(): Flow<List<PdfEntity>>

    @Query("SELECT * FROM pdf_files ORDER BY timestamp DESC LIMIT 10")
    fun getRecentPdfs(): Flow<List<PdfEntity>>

    @Query("SELECT * FROM pdf_files WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoritePdfs(): Flow<List<PdfEntity>>

    @Query("SELECT * FROM pdf_files WHERE title LIKE '%' || :query || '%' OR extractedText LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchPdfs(query: String): Flow<List<PdfEntity>>

    @Query("SELECT * FROM pdf_files WHERE id = :id")
    fun getPdfById(id: Int): Flow<PdfEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPdf(pdf: PdfEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pdfs: List<PdfEntity>)

    @Update
    suspend fun updatePdf(pdf: PdfEntity)

    @Delete
    suspend fun deletePdf(pdf: PdfEntity)

    @Query("UPDATE pdf_files SET isFavorite = :isFav WHERE id = :id")
    suspend fun setFavorite(id: Int, isFav: Boolean)

    @Query("DELETE FROM pdf_files WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT COUNT(*) FROM pdf_files")
    suspend fun getCount(): Int
}
