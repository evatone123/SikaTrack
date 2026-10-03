package com.example.data.backup

import android.content.Context
import android.net.Uri
import com.example.data.repository.FinanceRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream

class BackupManager(
    private val context: Context,
    private val repository: FinanceRepository
) {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(BackupData::class.java).indent("  ")

    suspend fun createBackupJson(): String = withContext(Dispatchers.IO) {
        val backup = BackupData(
            version = 1,
            exportedAt = System.currentTimeMillis(),
            app = "SikaTrack",
            accounts = repository.getAllAccountsDirect(),
            categories = repository.getAllCategoriesDirect(),
            transactions = repository.getAllTransactionsDirect(),
            budgets = repository.getAllBudgetsDirect(),
            savingsGoals = repository.getAllSavingsGoalsDirect()
        )
        adapter.toJson(backup)
    }

    suspend fun exportBackupToFile(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val jsonString = createBackupJson()
            context.contentResolver.openOutputStream(uri)?.use { outputStream: OutputStream ->
                outputStream.bufferedWriter().use { it.write(jsonString) }
            }
            Result.success(1)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportToLocalAppDirectory(): Result<File> = withContext(Dispatchers.IO) {
        try {
            val jsonString = createBackupJson()
            val backupDir = File(context.filesDir, "backups").apply { if (!exists()) mkdirs() }
            val fileName = "pocketledger_backup_${System.currentTimeMillis()}.json"
            val file = File(backupDir, fileName)
            file.writeText(jsonString)
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreBackupFromJson(jsonString: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val backup = adapter.fromJson(jsonString) ?: return@withContext Result.failure(
                IllegalArgumentException("Invalid or empty backup file")
            )
            if (backup.version > 1) {
                return@withContext Result.failure(
                    IllegalStateException("Backup version ${backup.version} is not supported by this app version.")
                )
            }
            repository.restoreDatabase(
                accounts = backup.accounts,
                categories = backup.categories,
                transactions = backup.transactions,
                budgets = backup.budgets,
                savingsGoals = backup.savingsGoals
            )
            val summary = "Restored ${backup.accounts.size} accounts, ${backup.transactions.size} transactions, ${backup.budgets.size} budgets, and ${backup.savingsGoals.size} goals."
            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreBackupFromUri(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream: InputStream ->
                inputStream.bufferedReader().use { it.readText() }
            } ?: return@withContext Result.failure(IllegalArgumentException("Could not read backup file"))

            restoreBackupFromJson(jsonString)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getLocalBackupFiles(): List<File> {
        val backupDir = File(context.filesDir, "backups")
        if (!backupDir.exists()) return emptyList()
        return backupDir.listFiles { file -> file.extension == "json" }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
}
