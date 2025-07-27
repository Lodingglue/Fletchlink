package com.toolkit.fletchlink.application

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.toolkit.fletchlink.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.lenni0451.commons.httpclient.HttpClient
import net.raphimc.minecraftauth.MinecraftAuth
import net.raphimc.minecraftauth.step.bedrock.session.StepFullBedrockSession
import net.raphimc.minecraftauth.step.msa.StepMsaDeviceCode
import net.raphimc.minecraftauth.util.MicrosoftConstants
import com.toolkit.fletchlink.application.MainActivity
import com.toolkit.fletchlink.ui.theme.FletchlinkTheme
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.CompletableFuture

class AuthActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MinecraftAuthApp"
        private const val SESSION_FILE = "bedrock_session.json"
        private val BEDROCK_REALMS_AUTH_FLOW = MinecraftAuth.builder()
            .withClientId(MicrosoftConstants.BEDROCK_ANDROID_TITLE_ID)
            .withScope(MicrosoftConstants.SCOPE_TITLE_AUTH)
            .deviceCode()
            .withDeviceToken("Android")
            .sisuTitleAuthentication(MicrosoftConstants.BEDROCK_XSTS_RELYING_PARTY)
            .buildMinecraftBedrockChainStep(true, true)

        fun newIntent(context: Context): Intent {
            return Intent(context, AuthActivity::class.java)
        }
    }

    private sealed class AuthState {
        object Initial : AuthState()
        object Loading : AuthState()
        object AwaitingAuth : AuthState()
        data class Error(val message: String) : AuthState()
        object Success : AuthState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FletchlinkTheme {
                LoginScreen { session ->
                    saveSession(this, session)
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            }
        }
    }

    @Composable
    fun LoginScreen(onLoginSuccess: (StepFullBedrockSession.FullBedrockSession) -> Unit) {
        val context = LocalContext.current
        val clipboardManager = LocalClipboardManager.current
        val httpClient = remember { MinecraftAuth.createHttpClient() }
        var authState by remember { mutableStateOf<AuthState>(AuthState.Initial) }
        var userCode by remember { mutableStateOf<String?>(null) }
        var verificationUri by remember { mutableStateOf<String?>(null) }
        var showWebView by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                MaterialTheme.colorScheme.background
                            ),
                            radius = 800f
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    
                    AppBrandingSection()

                    Spacer(modifier = Modifier.height(48.dp))

                    
                    AuthCard(
                        authState = authState,
                        userCode = userCode,
                        verificationUri = verificationUri,
                        onCopyCode = {
                            userCode?.let { code ->
                                clipboardManager.setText(AnnotatedString(code))
                                Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onCopyUrl = {
                            verificationUri?.let { url ->
                                clipboardManager.setText(AnnotatedString(url))
                                Toast.makeText(context, "URL copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onOpenWebView = { showWebView = true },
                        onLogin = {
                            authState = AuthState.Loading
                            coroutineScope.launch(Dispatchers.IO) {
                                startLoginFlow(httpClient) { session, code, uri, error ->
                                    coroutineScope.launch(Dispatchers.Main) {
                                        when {
                                            error != null -> {
                                                authState = AuthState.Error(error)
                                                userCode = null
                                                verificationUri = null
                                            }
                                            session != null -> {
                                                authState = AuthState.Success
                                                onLoginSuccess(session)
                                            }
                                            else -> {
                                                authState = AuthState.AwaitingAuth
                                                userCode = code
                                                verificationUri = uri
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    )
                }

                
                if (showWebView && verificationUri != null && userCode != null) {
                    WebViewDialog(
                        url = verificationUri!!,
                        code = userCode!!,
                        onDismiss = { showWebView = false }
                    )
                }
            }
        }
    }

    @Composable
    private fun AppBrandingSection() {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher_foreground), 
                        contentDescription = "App Logo",
                        modifier = Modifier.size(90.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "FletchLink",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Minecraft Account Manager",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    @Composable
    private fun AuthCard(
        authState: AuthState,
        userCode: String?,
        verificationUri: String?,
        onCopyCode: () -> Unit,
        onCopyUrl: () -> Unit,
        onOpenWebView: () -> Unit,
        onLogin: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (authState) {
                    AuthState.Initial -> {
                        InitialAuthContent(onLogin = onLogin)
                    }
                    AuthState.Loading -> {
                        LoadingAuthContent()
                    }
                    AuthState.AwaitingAuth -> {
                        AwaitingAuthContent(
                            userCode = userCode,
                            verificationUri = verificationUri,
                            onCopyCode = onCopyCode,
                            onCopyUrl = onCopyUrl,
                            onOpenWebView = onOpenWebView
                        )
                    }
                    is AuthState.Error -> {
                        ErrorAuthContent(
                            error = authState.message,
                            onRetry = onLogin
                        )
                    }
                    AuthState.Success -> {
                        SuccessAuthContent()
                    }
                }
            }
        }
    }

    @Composable
    private fun InitialAuthContent(onLogin: () -> Unit) {
        Icon(
            imageVector = ImageVector.vectorResource(ir.alirezaivaz.tablericons.R.drawable.ic_fingerprint),
            contentDescription = "Security",
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Sign in to Microsoft",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Connect your Minecraft account to access your Realms and account information",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onLogin,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(ir.alirezaivaz.tablericons.R.drawable.ic_login),
                contentDescription = "Login",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Continue with Microsoft",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }

    @Composable
    private fun LoadingAuthContent() {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            strokeWidth = 4.dp,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Connecting...",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Setting up your authentication",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    @Composable
    private fun AwaitingAuthContent(
        userCode: String?,
        verificationUri: String?,
        onCopyCode: () -> Unit,
        onCopyUrl: () -> Unit,
        onOpenWebView: () -> Unit
    ) {
        Text(
            text = "Almost there!",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Complete the authentication in your browser",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (userCode != null) {
            AuthCodeCard(
                code = userCode,
                onCopy = onCopyCode
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onOpenWebView,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(ir.alirezaivaz.tablericons.R.drawable.ic_webhook),
                contentDescription = "Web",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open in Browser")
        }
    }

    @Composable
    private fun AuthCodeCard(
        code: String,
        onCopy: () -> Unit
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.clickable { onCopy() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Your Code",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = code,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onCopy) {
                    Icon(
                        imageVector = ImageVector.vectorResource(ir.alirezaivaz.tablericons.R.drawable.ic_copy),
                        contentDescription = "Copy",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }



    @Composable
    private fun ErrorAuthContent(
        error: String,
        onRetry: () -> Unit
    ) {
        Image(
            painter = painterResource(id = ir.alirezaivaz.tablericons.R.drawable.ic_forbid_2), 
            contentDescription = "Error Icon",
            modifier = Modifier
                .size(48.dp)
                .clip(MaterialTheme.shapes.medium),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Authentication Failed",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Try Again")
        }
    }

    @Composable
    private fun SuccessAuthContent() {
        Image(
            painter = painterResource(id = ir.alirezaivaz.tablericons.R.drawable.ic_circle_dashed_check
            ), 
            contentDescription = "Success Icon",
            modifier = Modifier
                .size(48.dp)
                .clip(MaterialTheme.shapes.medium),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Success!",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Redirecting to your account...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    @Composable
    private fun WebViewDialog(
        url: String,
        code: String,
        onDismiss: () -> Unit
    ) {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(600.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val requestUrl = request?.url.toString()
                                    Log.d(TAG, "WebView URL: $requestUrl")
                                    return false
                                }

                                override fun onPageFinished(view: WebView?, pageUrl: String?) {
                                    super.onPageFinished(view, pageUrl)
                                    Log.d(TAG, "Page loaded: $pageUrl")
                                    view?.evaluateJavascript(
                                        """
                                        (function() {
                                            var input = document.querySelector('input[name="otc"], input[type="text"]');
                                            if (input) {
                                                input.value = '$code';
                                                input.dispatchEvent(new Event('input'));
                                                var form = input.closest('form');
                                                if (form) {
                                                    var submitButton = form.querySelector('button[type="submit"]');
                                                    if (submitButton) {
                                                        submitButton.click();
                                                    }
                                                }
                                            }
                                        })();
                                        """.trimIndent()
                                    ) { result ->
                                        Log.d(TAG, "JavaScript injection result: $result")
                                    }
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    super.onReceivedError(view, request, error)
                                    Log.e(TAG, "WebView error: ${error?.description}")
                                }
                            }
                            val authUrl = url.replace("authcode", code)
                            Log.d(TAG, "Loading WebView URL: $authUrl")
                            loadUrl(authUrl)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    private fun startLoginFlow(
        httpClient: HttpClient,
        callback: (StepFullBedrockSession.FullBedrockSession?, String?, String?, String?) -> Unit
    ) {
        CompletableFuture.supplyAsync {
            try {
                val authFlow = BEDROCK_REALMS_AUTH_FLOW
                Log.d(TAG, "Auth flow created with Realms support")
                Log.d(TAG, "Initiating device code authentication flow")
                val session = authFlow.getFromInput(httpClient, StepMsaDeviceCode.MsaDeviceCodeCallback { msaDeviceCode ->
                    Log.d(TAG, "Device code callback - User code: ${msaDeviceCode.userCode}")
                    runOnUiThread {
                        callback(null, msaDeviceCode.userCode, msaDeviceCode.verificationUri, null)
                    }
                }) as StepFullBedrockSession.FullBedrockSession
                Log.d(TAG, "Authentication flow completed successfully")
                Log.d(TAG, "Session components:")
                Log.d(TAG, "  MCChain: ${session.mcChain != null}")
                Log.d(TAG, "  PlayFab: ${session.playFabToken != null}")
                Log.d(TAG, "  RealmsXsts: ${session.realmsXsts != null}")
                if (session.mcChain == null) {
                    throw IllegalStateException("Authentication failed: MCChain is missing")
                }
                if (session.playFabToken == null) {
                    throw IllegalStateException("Authentication failed: PlayFab token is missing")
                }
                if (session.realmsXsts == null) {
                    Log.e(TAG, "Critical: Authentication succeeded but realmsXsts token is missing!")
                    throw IllegalStateException("Authentication succeeded but realmsXsts token is missing.")
                }
                Log.d(TAG, "All session components verified successfully")
                session
            } catch (e: Exception) {
                Log.e(TAG, "Login failed with exception", e)
                runOnUiThread {
                    callback(null, null, null, e.message ?: "Unknown error")
                }
                null
            }
        }.thenAccept { session ->
            if (session != null) {
                runOnUiThread {
                    callback(session, null, null, null)
                }
            }
        }
    }

    private fun saveSession(context: Context, session: StepFullBedrockSession.FullBedrockSession) {
        try {
            val json = BEDROCK_REALMS_AUTH_FLOW.toJson(session)
            val file = File(context.filesDir, SESSION_FILE)
            FileOutputStream(file).use { fos ->
                fos.write(json.toString().toByteArray())
                Log.d(TAG, "Session saved to ${file.absolutePath}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save session", e)
            runOnUiThread {
                Toast.makeText(context, "Failed to save session: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}