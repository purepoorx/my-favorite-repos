package com.donut.mixfile.util

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.donut.mixfile.appScope
import com.donut.mixfile.currentActivity
import com.donut.mixfile.ui.component.common.MixDialogBuilder
import com.donut.mixfile.ui.theme.MainTheme

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


fun addContentView(view: View): () -> Unit {
    currentActivity.addContentView(
        view,
        ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    )
    return {
        appScope.launch(Dispatchers.Main) {
            view.removeView()
        }
    }
}

fun View.removeView() {
    this.parent.isNotNull {
        (it as ViewGroup).removeView(this)
    }
}

@Composable
fun OnDispose(block: () -> Unit) {
    DisposableEffect(Unit) {
        onDispose {
            block()
        }
    }
}

fun addComposeView(content: @Composable (removeView: () -> Unit) -> Unit): () -> Unit {
    return addContentView(
        ComposeView(currentActivity).apply {
            setContent {
                MainTheme {
                    content {
                        this.removeView()
                    }
                }
            }
        }
    )
}

@Composable
fun <T> ProvidableCompositionLocal<T>.Provide(value: T, content: @Composable () -> Unit) {
    CompositionLocalProvider(this provides value) {
        content()
    }
}


@Composable
fun TipText(content: String, onClick: () -> Unit = {}) {
    Text(
        text = content,
        color = Color.Gray,
        style = TextStyle(
            fontSize = 16.sp,
            lineHeight = 12.sp
        ),
        modifier = Modifier
            .clickable(onClick = onClick)
            .fillMaxWidth()
            .padding(10.dp),
    )
}

@Composable
fun OnResume(block: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleObserver = remember {
        LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                block()
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }
}

@Composable
@NonRestartableComposable
fun UseEffect(
    vararg keys: Any?,
    block: suspend CoroutineScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(*keys) {
        scope.launch(Dispatchers.IO, block = block)
    }
}

@Composable
@NonRestartableComposable
fun UseEffect(
    block: suspend CoroutineScope.() -> Unit,
) {
    UseEffect(Unit, block = block)
}

@Composable
fun screenWidthInDp(): Dp {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    return screenWidthDp.dp
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewComponent(url: String) {
    GenWebViewClient {
        loadUrl(url)
    }
}


@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GenWebViewClient(modifier: Modifier = Modifier, block: WebView.() -> Unit) =
    AndroidView(factory = { context ->
        WebView(context).apply {
            webViewClient = WebViewClient()
            settings.apply {
                domStorageEnabled = true
                javaScriptEnabled = true
                allowUniversalAccessFromFileURLs = true
                allowContentAccess = true
                allowFileAccessFromFileURLs = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }
            block()
        }
    }, modifier = modifier)

fun showErrorDialog(e: Throwable, title: String = "发生错误") {
    MixDialogBuilder(title).apply {
        setContent {
            Column(
                modifier = Modifier
                    .heightIn(0.dp, 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = e.message ?: "未知错误",
                    color = Color.Red,
                    fontSize = 20.sp
                )
                Text(text = e.stackTraceToString())
            }
        }
        setPositiveButton("复制错误信息") {
            e.stackTraceToString().copyToClipboard()
        }
        setNegativeButton("关闭") {
            closeDialog()
        }
        show()
    }
}

