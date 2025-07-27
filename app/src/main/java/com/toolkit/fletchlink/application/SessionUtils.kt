package com.toolkit.fletchlink.application

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.lenni0451.commons.httpclient.HttpClient
import net.raphimc.minecraftauth.MinecraftAuth
import net.raphimc.minecraftauth.step.bedrock.session.StepFullBedrockSession
import net.raphimc.minecraftauth.util.MicrosoftConstants
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

private const val TAG = "MinecraftAuthApp"
private const val SESSION_FILE = "bedrock_session.json"

 val BEDROCK_REALMS_AUTH_FLOW = MinecraftAuth.builder()
    .withClientId(MicrosoftConstants.BEDROCK_ANDROID_TITLE_ID)
    .withScope(MicrosoftConstants.SCOPE_TITLE_AUTH)
    .deviceCode()
    .withDeviceToken("Android")
    .sisuTitleAuthentication(MicrosoftConstants.BEDROCK_XSTS_RELYING_PARTY)
    .buildMinecraftBedrockChainStep(true, true)

fun loadSavedSession(context: Context, httpClient: HttpClient): StepFullBedrockSession.FullBedrockSession? {
    return try {
        val file = File(context.filesDir, SESSION_FILE)
        if (!file.exists()) {
            Log.d(TAG, "Session file does not exist: ${file.absolutePath}")
            return null
        }
        val jsonString = FileInputStream(file).use { fis ->
            fis.readBytes().toString(Charsets.UTF_8)
        }
        if (jsonString.isBlank()) {
            Log.e(TAG, "Session file is empty")
            deleteSession(context)
            return null
        }
        val json = JsonParser.parseString(jsonString) as JsonObject
        val session = BEDROCK_REALMS_AUTH_FLOW.fromJson(json)
        if (session.realmsXsts == null) {
            Log.e(TAG, "Session missing realmsXsts token, discarding")
            deleteSession(context)
            return null
        }
        if (session.isExpiredOrOutdated()) {
            Log.d(TAG, "Session is expired/outdated, attempting to refresh")
            try {
                val refreshedSession = BEDROCK_REALMS_AUTH_FLOW.refresh(httpClient, session)
                if (refreshedSession.realmsXsts == null) {
                    Log.e(TAG, "Refreshed session missing realmsXsts token")
                    deleteSession(context)
                    return null
                }
                saveSession(context, refreshedSession)
                refreshedSession
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh session", e)
                deleteSession(context)
                return null
            }
        } else {
            session
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to load session", e)
        deleteSession(context)
        null
    }
}

fun saveSession(context: Context, session: StepFullBedrockSession.FullBedrockSession) {
    try {
        val json = BEDROCK_REALMS_AUTH_FLOW.toJson(session)
        val file = File(context.filesDir, SESSION_FILE)
        FileOutputStream(file).use { fos ->
            fos.write(json.toString().toByteArray())
            Log.d(TAG, "Session saved to ${file.absolutePath}")
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to save session", e)
        Toast.makeText(context, "Failed to save session: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

fun deleteSession(context: Context) {
    try {
        val file = File(context.filesDir, SESSION_FILE)
        if (file.exists()) {
            if (file.delete()) {
                Log.d(TAG, "Session file deleted: ${file.absolutePath}")
            } else {
                Log.e(TAG, "Failed to delete session file: ${file.absolutePath}")
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error deleting session file", e)
        Toast.makeText(context, "Failed to delete session: ${e.message}", Toast.LENGTH_LONG).show()
    }
}