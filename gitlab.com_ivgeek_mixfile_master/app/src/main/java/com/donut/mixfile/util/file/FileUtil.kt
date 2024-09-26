package com.donut.mixfile.util.file

import android.annotation.SuppressLint
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import com.donut.mixfile.MainActivity
import com.donut.mixfile.app
import com.donut.mixfile.appScope
import com.donut.mixfile.server.StreamContent
import com.donut.mixfile.server.accessKey
import com.donut.mixfile.server.localClient
import com.donut.mixfile.server.utils.bean.MixShareInfo
import com.donut.mixfile.ui.component.common.MixDialogBuilder
import com.donut.mixfile.ui.routes.home.getLocalServerAddress
import com.donut.mixfile.ui.routes.home.tryResolveFile
import com.donut.mixfile.ui.routes.home.uploadTasks
import com.donut.mixfile.util.cachedMutableOf
import com.donut.mixfile.util.catchError
import com.donut.mixfile.util.errorDialog
import com.donut.mixfile.util.getFileName
import com.donut.mixfile.util.getFileSize
import com.donut.mixfile.util.objects.ProgressContent
import com.donut.mixfile.util.showToast
import io.ktor.client.plugins.onDownload
import io.ktor.client.plugins.onUpload
import io.ktor.client.plugins.timeout
import io.ktor.client.request.parameter
import io.ktor.client.request.prepareGet
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext

suspend fun putUploadFile(
    data: Any?,
    name: String,
    add: Boolean = true,
    progressContent: ProgressContent = ProgressContent(),
): String {
    val response = localClient.put {
        timeout {
            requestTimeoutMillis = 1000 * 60 * 60 * 24 * 30L
        }
        url("${getLocalServerAddress()}/api/upload")
        onUpload(progressContent.ktorListener)
        parameter("name", name)
        parameter("add", add)
        parameter("accessKey", accessKey)
        setBody(data)
    }
    val message = response.bodyAsText()
    if (!response.status.isSuccess()) {
        throw Exception("上传失败: $message")
    }
    return message
}


fun doUploadFile(data: Any?, name: String, add: Boolean = true) {
    MixDialogBuilder(
        "上传中", properties = DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = false
        )
    ).apply {
        var job: Job? = null
        setContent {
            val progressContent = remember {
                ProgressContent("上传中")
            }
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                progressContent.LoadingContent()
            }
            val scope = rememberCoroutineScope()
            LaunchedEffect(Unit) {
                job = appScope.launch(Dispatchers.IO) {
                    errorDialog("上传失败") {
                        val message = putUploadFile(data, name, add, progressContent)
                        if (!scope.coroutineContext.isActive) {
                            return@launch
                        }
                        withContext(Dispatchers.Main) {
                            tryResolveFile(message)
                        }
                        showToast("上传成功!")
                    }
                    closeDialog()
                }
            }
        }
        setPositiveButton("后台上传") {
            closeDialog()
        }
        setNegativeButton("取消") {
            job?.cancel()
            showToast("上传已取消")
            closeDialog()
        }
        show()
    }
}

var multiUploadTaskCount by cachedMutableOf(5, "mix_file_multi_upload_task_count")

val uploadSemaphore = Semaphore(multiUploadTaskCount.toInt())
var uploadQueue by mutableIntStateOf(0)
var totalFileCount by mutableIntStateOf(0)
var successFileCount by mutableIntStateOf(0)
private val multiUploadJobs = mutableListOf<Job>()

fun cancelAllMultiUpload() {
    uploadQueue = 0
    multiUploadJobs.forEach { it.cancel() }
    multiUploadJobs.clear()
    uploadTasks.forEach { it.stop() }
    totalFileCount = 0
    successFileCount = 0
}

inline fun uploadUri(uri: Uri, uploader: (StreamContent, String) -> Unit) {
    val resolver = app.contentResolver
    val fileSize = uri.getFileSize()
    val fileStream = resolver.openInputStream(uri)
    if (fileStream == null) {
        showToast("打开文件失败")
        return
    }
    val stream = StreamContent(fileStream, fileSize)
    val fileName = uri.getFileName()
    uploader(stream, fileName)
}

@SuppressLint("Recycle")
fun selectAndUploadFile() {
    MainActivity.mixFileSelector.openSelect { uriList ->
        val taskList = mutableListOf<suspend () -> Unit>()
        uploadQueue += uriList.size
        totalFileCount += uriList.size
        uriList.forEach { uri ->
            taskList.add {
                uploadUri(uri) { stream, name ->
                    putUploadFile(stream, name)
                }
            }
        }
        if (taskList.isEmpty()) {
            return@openSelect
        }
        val job = appScope.launch(Dispatchers.IO) {
            val deferredList = mutableListOf<Deferred<Unit>>()
            taskList.forEach { task ->
                uploadSemaphore.acquire()
                withContext(Dispatchers.Main) {
                    uploadQueue--
                }
                deferredList.add(async {
                    catchError {
                        task()
                        successFileCount++
                    }
                    uploadSemaphore.release()
                })
            }
            deferredList.awaitAll()
            withContext(Dispatchers.Main) {
                if (uploadQueue == 0) {
                    totalFileCount = 0
                    successFileCount = 0
                }
            }
        }
        multiUploadJobs.add(job)
    }
}

suspend fun saveFileToStorage(
    url: String,
    displayName: String,
    progress: ProgressContent,
    directory: String = Environment.DIRECTORY_DOWNLOADS,
    storeUri: Uri = MediaStore.Files.getContentUri("external"),
): Uri? {
    val resolver = app.contentResolver
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
//        put(MediaStore.MediaColumns.MIME_TYPE, "image/gif")
        put(MediaStore.MediaColumns.RELATIVE_PATH, directory)
    }


    val fileUri = resolver.insert(storeUri, contentValues)
    coroutineContext.job.invokeOnCompletion { throwable ->
        if (throwable !is CancellationException) {
            return@invokeOnCompletion
        }
        if (fileUri == null) {
            return@invokeOnCompletion
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            resolver.delete(fileUri, null)
        }
    }
    if (fileUri == null) {
        return null
    }
    localClient.prepareGet {
        timeout {
            requestTimeoutMillis = 1000 * 60 * 60 * 24 * 30L
        }
        url(url)
        onDownload(progress.ktorListener)
    }.execute {
        if (!it.status.isSuccess()) {
            val text = if ((it.contentLength()
                    ?: (1024 * 1024)) < 1024 * 500
            ) it.bodyAsText() else "未知错误"
            throw Exception("下载失败: ${text}")
        }
        resolver.openOutputStream(fileUri)?.use { output ->
            it.bodyAsChannel().toInputStream().use { input ->
                input.copyTo(output)
            }
        }
    }
    return fileUri
}

fun resolveMixShareInfo(value: String): MixShareInfo? {
    return parseShareCode(value)
}