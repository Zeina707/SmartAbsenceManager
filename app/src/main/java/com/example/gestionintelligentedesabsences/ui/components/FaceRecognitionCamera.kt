package com.example.gestionintelligentedesabsences.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.gestionintelligentedesabsences.data.model.Student
import com.example.gestionintelligentedesabsences.util.FaceRecognitionService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@Composable
fun FaceRecognitionCamera(
    student: Student,
    onRecognitionResult: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val faceRecognitionService = remember { FaceRecognitionService(context) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    var isProcessing by remember { mutableStateOf(false) }
    var captureError by remember { mutableStateOf<String?>(null) }
    var recognitionMessage by remember { mutableStateOf<String?>(null) }

    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(key1 = Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                imageCapture = imageCapture,
                context = context,
                lifecycleOwner = lifecycleOwner
            )

            // Capture button
            IconButton(
                onClick = {
                    if (!isProcessing) {
                        isProcessing = true
                        captureError = null
                        recognitionMessage = "Capture en cours..."

                        captureImage(
                            imageCapture = imageCapture,
                            executor = cameraExecutor,
                            onImageCaptured = { bitmap ->
                                recognitionMessage = "Analyse du visage en cours..."

                                // Process the captured image
                                processCapturedImage(
                                    bitmap = bitmap,
                                    student = student,
                                    faceRecognitionService = faceRecognitionService,
                                    onResult = { isRecognized ->
                                        isProcessing = false
                                        if (isRecognized) {
                                            recognitionMessage = "Visage reconnu !"
                                        } else {
                                            recognitionMessage = "Visage non reconnu. Veuillez réessayer."
                                        }

                                        onRecognitionResult(isRecognized)
                                    }
                                )
                            },
                            onError = { message ->
                                isProcessing = false
                                captureError = message
                                recognitionMessage = null
                            }
                        )
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .size(72.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color.White, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = "Prendre une photo",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Close button
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Fermer",
                    tint = Color.White
                )
            }

            // Loading indicator and messages
            if (isProcessing) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    recognitionMessage?.let {
                        Text(
                            text = it,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Display error message if any
            captureError?.let {
                Text(
                    text = it,
                    color = Color.Red,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                        .fillMaxWidth()
                )
            }

            // Display recognition message if not processing
            if (!isProcessing && recognitionMessage != null) {
                Text(
                    text = recognitionMessage!!,
                    color = if (recognitionMessage!!.contains("reconnu !")) Color.Green else Color.Red,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                        .fillMaxWidth()
                )
            }

            // Display student info
            Text(
                text = "${student.firstName} ${student.lastName}",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Permission de caméra requise",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }) {
                    Text("Demander la permission")
                }
            }
        }
    }
}

@Composable
private fun CameraPreview(
    modifier: Modifier = Modifier,
    imageCapture: ImageCapture,
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner
) {
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                } catch (e: Exception) {
                    Log.e("CameraPreview", "Use case binding failed", e)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = modifier
    )
}

private fun captureImage(
    imageCapture: ImageCapture,
    executor: Executor,
    onImageCaptured: (Bitmap) -> Unit,
    onError: (String) -> Unit
) {
    imageCapture.takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                try {
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)

                    val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                    // Rotate the bitmap if needed
                    val rotationDegrees = image.imageInfo.rotationDegrees
                    val rotatedBitmap = if (rotationDegrees != 0) {
                        val matrix = Matrix()
                        matrix.postRotate(rotationDegrees.toFloat())
                        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    } else {
                        bitmap
                    }

                    onImageCaptured(rotatedBitmap)
                } catch (e: Exception) {
                    onError("Erreur lors de la capture: ${e.message}")
                } finally {
                    image.close()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                onError("Erreur lors de la capture: ${exception.message}")
            }
        }
    )
}

private fun processCapturedImage(
    bitmap: Bitmap,
    student: Student,
    faceRecognitionService: FaceRecognitionService,
    onResult: (Boolean) -> Unit
) {
    kotlinx.coroutines.MainScope().launch {
        val isRecognized = withContext(Dispatchers.IO) {
            faceRecognitionService.matchFaceWithStudent(bitmap, student.userId)
        }
        onResult(isRecognized)
    }
}

@Composable
fun FaceRegistrationDialog(
    student: Student,
    onRegistrationComplete: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val faceRecognitionService = remember { FaceRecognitionService(context) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    var isProcessing by remember { mutableStateOf(false) }
    var captureError by remember { mutableStateOf<String?>(null) }
    var registrationMessage by remember { mutableStateOf<String?>(null) }

    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(key1 = Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                imageCapture = imageCapture,
                context = context,
                lifecycleOwner = lifecycleOwner
            )

            // Capture button
            IconButton(
                onClick = {
                    if (!isProcessing) {
                        isProcessing = true
                        captureError = null
                        registrationMessage = "Capture en cours..."

                        captureImage(
                            imageCapture = imageCapture,
                            executor = cameraExecutor,
                            onImageCaptured = { bitmap ->
                                registrationMessage = "Enregistrement de la photo en cours..."

                                // Register photo
                                kotlinx.coroutines.MainScope().launch {
                                    val success = withContext(Dispatchers.IO) {
                                        faceRecognitionService.registerFace(bitmap, student.userId)
                                    }

                                    isProcessing = false
                                    if (success) {
                                        registrationMessage = "Photo enregistrée avec succès !"
                                    } else {
                                        registrationMessage = "Échec de l'enregistrement. Veuillez réessayer."
                                    }

                                    onRegistrationComplete(success)
                                }
                            },
                            onError = { message ->
                                isProcessing = false
                                captureError = message
                                registrationMessage = null
                            }
                        )
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .size(72.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color.White, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = "Prendre une photo",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Close button
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Fermer",
                    tint = Color.White
                )
            }

            // Loading indicator and messages
            if (isProcessing) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    registrationMessage?.let {
                        Text(
                            text = it,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Display error message if any
            captureError?.let {
                Text(
                    text = it,
                    color = Color.Red,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                        .fillMaxWidth()
                )
            }

            // Display registration message if not processing
            if (!isProcessing && registrationMessage != null) {
                Text(
                    text = registrationMessage!!,
                    color = if (registrationMessage!!.contains("succès")) Color.Green else Color.Red,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                        .fillMaxWidth()
                )
            }

            // Display student info
            Text(
                text = "Enregistrer la photo de ${student.firstName} ${student.lastName}",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Permission de caméra requise",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }) {
                    Text("Demander la permission")
                }
            }
        }
    }
}