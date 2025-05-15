package com.example.gestionintelligentedesabsences.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.TensorImage
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.pow
import kotlin.math.sqrt

class FaceRecognitionService(private val context: Context) {
    private lateinit var faceDetector: FaceDetector
    private lateinit var interpreter: Interpreter
    private var isInitialized = false
    private val embeddingDim = 512 // Updated to match the model's output (2048 bytes / 4 bytes per float)

    companion object {
        private const val TAG = "FaceRecognitionService"
        private const val MODEL_FILE = "facenet_model.tflite"
        private const val THRESHOLD = 0.9f // Adjusted similarity threshold was 0.6
        private const val PHOTO_DIR = "student_photos"
    }

    init {
        setupFaceDetector()
        setupFaceRecognition()
    }

    private fun setupFaceDetector() {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
        faceDetector = FaceDetection.getClient(options)
    }

    private fun setupFaceRecognition() {
        try {
            val modelFile = FileUtil.loadMappedFile(context, MODEL_FILE)
            interpreter = Interpreter(modelFile)
            isInitialized = true
            Log.d(TAG, "FaceNet model loaded successfully")
        } catch (e: Exception) {
            isInitialized = false
            Log.e(TAG, "Error loading TFLite model: ${e.message}", e)
        }
    }

    suspend fun detectFace(bitmap: Bitmap): List<Face> = withContext(Dispatchers.Default) {
        val image = InputImage.fromBitmap(bitmap, 0)
        try {
            faceDetector.process(image).await().also {
                Log.d(TAG, "Detected ${it.size} face(s)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Face detection failed: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun extractFaceEmbedding(bitmap: Bitmap, face: Face): FloatArray? = withContext(Dispatchers.Default) {
        try {
            val boundingBox = face.boundingBox
            val faceBitmap = Bitmap.createBitmap(
                bitmap,
                boundingBox.left.coerceAtLeast(0),
                boundingBox.top.coerceAtLeast(0),
                boundingBox.width().coerceAtMost(bitmap.width - boundingBox.left),
                boundingBox.height().coerceAtMost(bitmap.height - boundingBox.top)
            )

            val resizedBitmap = Bitmap.createScaledBitmap(faceBitmap, 160, 160, false)
            val tensorImage = TensorImage(DataType.FLOAT32).apply { load(resizedBitmap) }
            val inputFeature = tensorImage.buffer

            val outputBuffer = ByteBuffer.allocateDirect(embeddingDim * 4).apply {
                order(java.nio.ByteOrder.nativeOrder())
            }

            interpreter.run(inputFeature, outputBuffer)
            outputBuffer.rewind()
            val embedding = FloatArray(embeddingDim).apply {
                for (i in 0 until embeddingDim) {
                    this[i] = outputBuffer.float
                }
            }

            val norm = sqrt(embedding.map { it * it }.sum())
            embedding.forEachIndexed { i, value -> embedding[i] = value / norm }
            embedding
        } catch (e: Exception) {
            Log.e(TAG, "Face embedding extraction failed: ${e.message}", e)
            null
        }
    }

    suspend fun matchFaceWithStudent(bitmap: Bitmap, studentId: String): Boolean = withContext(Dispatchers.IO) {
        if (!isInitialized) {
            Log.e(TAG, "Face recognition model not initialized. Ensure '$MODEL_FILE' is in assets.")
            return@withContext false
        }

        try {
            val storedBitmap = loadStudentPhoto(studentId) ?: run {
                Log.w(TAG, "No photo stored for student $studentId")
                return@withContext false
            }

            val capturedFaces = detectFace(bitmap)
            if (capturedFaces.isEmpty()) {
                Log.w(TAG, "No face detected in captured image")
                return@withContext false
            }

            val storedFaces = detectFace(storedBitmap)
            if (storedFaces.isEmpty()) {
                Log.w(TAG, "No face detected in stored photo for student $studentId")
                return@withContext false
            }

            val capturedFace = capturedFaces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
            val storedFace = storedFaces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }

            if (capturedFace == null || storedFace == null) {
                Log.w(TAG, "Could not identify a valid face for comparison")
                return@withContext false
            }

            val capturedEmbedding = extractFaceEmbedding(bitmap, capturedFace)
            val storedEmbedding = extractFaceEmbedding(storedBitmap, storedFace)

            if (capturedEmbedding == null || storedEmbedding == null) {
                Log.w(TAG, "Failed to extract embeddings for comparison")
                return@withContext false
            }

            val similarity = calculateCosineSimilarity(capturedEmbedding, storedEmbedding)
            Log.d(TAG, "Face similarity: $similarity, Threshold: $THRESHOLD")
            similarity > THRESHOLD
        } catch (e: Exception) {
            Log.e(TAG, "Face matching failed: ${e.message}", e)
            false
        }
    }

    suspend fun registerFace(bitmap: Bitmap, studentId: String): Boolean = withContext(Dispatchers.IO) {
        if (!isInitialized) {
            Log.e(TAG, "Face recognition model not initialized. Ensure '$MODEL_FILE' is in assets.")
            return@withContext false
        }

        try {
            val faces = detectFace(bitmap)
            if (faces.isEmpty()) {
                Log.w(TAG, "No face detected for registration")
                return@withContext false
            }

            val face = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
                ?: return@withContext false

            val embedding = extractFaceEmbedding(bitmap, face) ?: return@withContext false
            saveStudentPhoto(bitmap, studentId)
            Log.d(TAG, "Photo saved successfully for student $studentId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Face registration failed: ${e.message}", e)
            false
        }
    }

    private fun saveStudentPhoto(bitmap: Bitmap, studentId: String) {
        val photoDir = File(context.filesDir, PHOTO_DIR).apply { mkdirs() }
        val photoFile = File(photoDir, "$studentId.jpg")

        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val encryptedFile = EncryptedFile.Builder(
            context,
            photoFile,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        encryptedFile.openFileOutput().use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.flush()
        }
    }

    private fun loadStudentPhoto(studentId: String): Bitmap? {
        val photoFile = File(context.filesDir, "$PHOTO_DIR/$studentId.jpg")
        if (!photoFile.exists()) return null

        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedFile.Builder(
            context,
            photoFile,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build().openFileInput().use { inputStream ->
            BitmapFactory.decodeStream(inputStream)
        } ?: run {
            Log.e(TAG, "Failed to decode bitmap from encrypted file")
            null
        }
    }

    private fun calculateCosineSimilarity(vector1: FloatArray, vector2: FloatArray): Float {
        var dotProduct = 0f
        var norm1 = 0f
        var norm2 = 0f

        for (i in vector1.indices) {
            dotProduct += vector1[i] * vector2[i]
            norm1 += vector1[i].pow(2)
            norm2 += vector2[i].pow(2)
        }

        norm1 = sqrt(norm1)
        norm2 = sqrt(norm2)

        return if (norm1 > 0 && norm2 > 0) dotProduct / (norm1 * norm2) else 0f
    }
}