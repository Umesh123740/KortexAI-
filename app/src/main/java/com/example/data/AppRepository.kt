package com.example.data

import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream
import com.example.R
import com.example.network.GeminiApiService
import com.example.network.GenerateContentRequest
import com.example.network.Content
import com.example.network.Part
import com.example.network.InlineData
import com.example.network.GenerationConfig
import com.example.network.ImageConfig
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class AppRepository(
    private val context: android.content.Context,
    private val doubtDao: DoubtDao,
    private val streakDao: StreakDao,
    private val goalDao: GoalDao,
    private val userProfileDao: UserProfileDao,
    private val geminiApiService: GeminiApiService
) {
    private val databaseId = context.getString(R.string.firestore_database_id)
    private val db = FirebaseFirestore.getInstance(databaseId)
    private val auth = Firebase.auth

    val allDoubts: Flow<List<Doubt>> = doubtDao.getAllDoubts()
    val allStreaks: Flow<List<Streak>> = streakDao.getAllStreaks()
    val userProfile: Flow<UserProfile?> = userProfileDao.getUserProfile()

    suspend fun getFirestoreProfile(uid: String): UserProfile? {
        return try {
            val doc = db.collection("users").document(uid).get().await()
            if (doc.exists()) {
                UserProfile(
                    userId = uid,
                    name = doc.getString("name") ?: "",
                    language = doc.getString("language") ?: "",
                    board = doc.getString("board") ?: "",
                    className = doc.getString("className") ?: "",
                    isPro = doc.getBoolean("isPro") ?: false,
                    mobileNumber = doc.getString("mobileNumber") ?: "",
                    profilePicture = doc.getString("profilePicture"),
                    isOnboarded = doc.getBoolean("isOnboarded") ?: false
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun syncProfileToFirestore(profile: UserProfile) {
        val uid = auth.currentUser?.uid ?: return
        val data = mapOf(
            "userId" to uid,
            "name" to profile.name,
            "language" to profile.language,
            "board" to profile.board,
            "className" to profile.className,
            "isPro" to profile.isPro,
            "mobileNumber" to profile.mobileNumber,
            "profilePicture" to profile.profilePicture,
            "isOnboarded" to profile.isOnboarded,
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        // Use merge to avoid overwriting createdAt if it exists
        db.collection("users").document(uid).set(data + mapOf("createdAt" to FieldValue.serverTimestamp()), SetOptions.merge()).await()
    }

    fun getGoalsForDate(date: String): Flow<List<Goal>> = goalDao.getGoalsForDate(date)

    suspend fun insertDoubt(doubt: Doubt) = doubtDao.insertDoubt(doubt)
    suspend fun insertStreak(streak: Streak) = streakDao.insertStreak(streak)
    suspend fun insertGoal(goal: Goal) = goalDao.insertGoal(goal)
    suspend fun updateGoal(goal: Goal) = goalDao.updateGoal(goal)
    suspend fun insertUserProfile(userProfile: UserProfile) = userProfileDao.insertUserProfile(userProfile)
    suspend fun updateUserProfile(userProfile: UserProfile) = userProfileDao.updateUserProfile(userProfile)

    suspend fun solveDoubt(
        question: String,
        subject: String,
        className: String,
        board: String,
        language: String,
        isPro: Boolean,
        imageBitmap: Bitmap? = null
    ): String {
        val model = if (isPro) "gemini-3.1-pro-preview" else "gemini-3.5-flash"
        
        val systemInstruction = if (isPro) {
            "You are Kortex AI (Pro), a master tutor. Provide deep, detailed, step-by-step reasoning for the student in $className, $board. " +
            "Explain the concepts clearly, use bold text for key terms, and format math using LaTeX (e.g., \$\$x^2\$\$). Respond in $language."
        } else {
            "You are Kortex AI, a helpful tutor. Solve the following question for a student in $className, $board. " +
            "Provide a clear step-by-step solution. Respond in $language."
        }
        
        val parts = mutableListOf<Part>()
        parts.add(Part(text = question))
        
        imageBitmap?.let {
            val base64Image = it.toBase64()
            parts.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image)))
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = parts)),
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction))),
            generationConfig = GenerationConfig(temperature = 0.4f, topP = 0.9f)
        )

        var retries = 0
        val maxRetries = 3
        var backoff = 2000L

        while (retries <= maxRetries) {
            try {
                val response = geminiApiService.generateContent(model, request = request)
                return response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Could not generate solution."
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 429) {
                    if (retries == maxRetries) return "Kortex is a bit busy, please try again in a moment."
                    kotlinx.coroutines.delay(backoff)
                    retries++
                    backoff *= 2
                } else {
                    return "Error: ${e.message()}"
                }
            } catch (e: Exception) {
                return "Error: ${e.message ?: "Unknown error"}"
            }
        }
        return "Could not connect to Kortex AI."
    }

    suspend fun generateInfographic(topic: String, subject: String): Bitmap? {
        val prompt = "Create a professional educational infographic about $topic in the field of $subject. " +
                    "Include key diagrams, labels, and clear explanations. Make it visually appealing for a student."
        
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(
                imageConfig = ImageConfig(aspectRatio = "9:16", imageSize = "1K"),
                responseModalities = listOf("TEXT", "IMAGE")
            )
        )

        try {
            val response = geminiApiService.generateContent("gemini-2.5-flash-image", request = request)
            val base64Image = response.candidates.firstOrNull()?.content?.parts?.find { it.inlineData != null }?.inlineData?.data
            return base64Image?.let {
                val imageBytes = android.util.Base64.decode(it, android.util.Base64.DEFAULT)
                android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            }
        } catch (e: Exception) {
            return null
        }
    }

    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }
}
