package com.lloyd.attendance.feature.syllabus

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader

class SyllabusRepository private constructor(private val context: Context) {

    private val gson = Gson()
    private var cachedPayload: SyllabusPayload? = null

    suspend fun getSyllabus(): SyllabusPayload = withContext(Dispatchers.IO) {
        cachedPayload?.let { return@withContext it }
        val assetManager = context.assets
        assetManager.open("aktu_cse_syllabus_2026_27.json").use { stream ->
            InputStreamReader(stream, Charsets.UTF_8).use { reader ->
                val payload = gson.fromJson(reader, SyllabusPayload::class.java)
                cachedPayload = payload
                payload
            }
        }
    }

    suspend fun getCourseById(courseId: String): SyllabusCourse? = withContext(Dispatchers.IO) {
        getSyllabus().courses.firstOrNull { it.id.equals(courseId, ignoreCase = true) }
    }

    companion object {
        @Volatile
        private var instance: SyllabusRepository? = null

        fun getInstance(context: Context): SyllabusRepository =
            instance ?: synchronized(this) {
                instance ?: SyllabusRepository(context.applicationContext).also { instance = it }
            }
    }
}
