package com.toolkit.fletchlink.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.toolkit.fletchlink.ui.components.EmptyState
import com.toolkit.fletchlink.ui.components.ErrorState
import com.toolkit.fletchlink.ui.components.LoadingState
import com.toolkit.fletchlink.ui.components.RealmCard
import com.toolkit.fletchlink.ui.theme.FletchlinkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.lenni0451.commons.httpclient.HttpClient
import net.raphimc.minecraftauth.MinecraftAuth
import net.raphimc.minecraftauth.service.realms.BedrockRealmsService
import net.raphimc.minecraftauth.service.realms.model.RealmsWorld
import net.raphimc.minecraftauth.step.bedrock.session.StepFullBedrockSession
import java.util.concurrent.CompletableFuture

@Composable
fun RealmScreen(session: StepFullBedrockSession.FullBedrockSession) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val httpClient = remember { MinecraftAuth.createHttpClient() }
    var realms by remember { mutableStateOf<List<RealmsWorld>>(emptyList()) }
    var realmAddresses by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var statusMessage by remember { mutableStateOf("Loading Realms...") }
    var uiState by remember { mutableStateOf(UiState.Loading) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf<RealmsWorld?>(null) }
    val coroutineScope = rememberCoroutineScope()

    fun fetchRealmsData() {
        uiState = UiState.Loading
        coroutineScope.launch(Dispatchers.IO) {
            fetchRealms(httpClient, session) { worlds, error ->
                if (error != null) {
                    statusMessage = error
                    uiState = UiState.Error
                } else {
                    realms = worlds ?: emptyList()
                    uiState = if (realms.isEmpty()) UiState.Empty else UiState.Success
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchRealmsData()
    }

    FletchlinkTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when (uiState) {
                UiState.Loading -> {
                    LoadingState(message = "Loading your Realms...")
                }

                UiState.Error -> {
                    ErrorState(
                        message = statusMessage,
                        onRetry = { fetchRealmsData() }
                    )
                }

                UiState.Empty -> {
                    EmptyState(
                        title = "No Realms Found",
                        message = "You're not part of any Minecraft Realms yet. Join or create a Realm to get started!",
                        actionText = "Refresh",
                        onAction = { fetchRealmsData() }
                    )
                }

                UiState.Success -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        
                        RealmScreenHeader(
                            realmCount = realms.size,
                            onlineCount = realms.count { it.isCompatible && !it.isExpired },
                            onRefresh = { fetchRealmsData() },
                            onJoinWithCode = { showJoinDialog = true }
                        )

                        
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(realms) { world ->
                                RealmCard(
                                    world = world,
                                    serverAddress = realmAddresses[world.id],
                                    onJoinClick = {
                                        coroutineScope.launch(Dispatchers.IO) {
                                            joinRealm(httpClient, session, world) { address, error ->
                                                if (error != null) {
                                                    launch {
                                                        Toast.makeText(
                                                            context,
                                                            "Error joining Realm: $error",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                    }
                                                } else if (address != null) {
                                                    realmAddresses = realmAddresses + (world.id to address)
                                                    launch {
                                                        Toast.makeText(
                                                            context,
                                                            "Realm address retrieved successfully!",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    onAddressCopy = {
                                        realmAddresses[world.id]?.let { address ->
                                            clipboardManager.setText(AnnotatedString(address))
                                            Toast.makeText(
                                                context,
                                                "Address copied to clipboard",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    onLeaveRealm = {
                                        coroutineScope.launch(Dispatchers.IO) {
                                            leaveRealm(httpClient, session, world) { success, error ->
                                                launch {
                                                    if (success) {
                                                        Toast.makeText(
                                                            context,
                                                            "Left realm successfully",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                        fetchRealmsData() 
                                                    } else {
                                                        Toast.makeText(
                                                            context,
                                                            "Error leaving realm: $error",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    onShowDetails = {
                                        showDetailsDialog = world
                                    }
                                )
                            }
                        }
                    }
                }
            }

            
            if (showJoinDialog) {
                JoinRealmDialog(
                    onDismiss = { showJoinDialog = false },
                    onJoinWithCode = { inviteCode ->
                        showJoinDialog = false
                        coroutineScope.launch(Dispatchers.IO) {
                            acceptInvite(httpClient, session, inviteCode) { world, error ->
                                launch {
                                    if (world != null) {
                                        Toast.makeText(
                                            context,
                                            "Successfully joined realm: ${world.name}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        fetchRealmsData() 
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Error joining realm: $error",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        }
                    }
                )
            }

            
            showDetailsDialog?.let { world ->
                RealmDetailsDialog(
                    world = world,
                    onDismiss = { showDetailsDialog = null }
                )
            }
        }
    }
}

@Composable
private fun RealmScreenHeader(
    realmCount: Int,
    onlineCount: Int,
    onRefresh: () -> Unit,
    onJoinWithCode: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Your Realms",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            StatChip(
                                label = "Total",
                                value = realmCount.toString()
                            )
                            StatChip(
                                label = "Online",
                                value = onlineCount.toString(),
                                isSuccess = true
                            )
                        }
                    }

                    Row {
                        IconButton(
                            onClick = onJoinWithCode,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Join with Code",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onRefresh,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JoinRealmDialog(
    onDismiss: () -> Unit,
    onJoinWithCode: (String) -> Unit
) {
    var inviteCode by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Join Realm with Code",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Enter the invite code shared by the realm owner",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = inviteCode,
                    onValueChange = { inviteCode = it },
                    label = { Text("Invite Code") },
                    placeholder = { Text("Enter code...") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (inviteCode.isNotBlank()) {
                                onJoinWithCode(inviteCode.trim())
                            }
                        }
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (inviteCode.isNotBlank()) {
                                onJoinWithCode(inviteCode.trim())
                            }
                        },
                        enabled = inviteCode.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Join Realm")
                    }
                }
            }
        }
    }
}

@Composable
private fun RealmDetailsDialog(
    world: RealmsWorld,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "Realm Details",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                
                DetailItem(label = "Name", value = world.name ?: "Unknown")
                world.motd?.takeIf { it.isNotBlank() }?.let {
                    DetailItem(label = "Description", value = it)
                }
                DetailItem(label = "Owner", value = world.ownerName ?: "Unknown")
                DetailItem(label = "Realm ID", value = world.id.toString())
                DetailItem(label = "State", value = world.state ?: "Unknown")
                DetailItem(label = "World Type", value = world.worldType ?: "NORMAL")
                DetailItem(label = "Max Players", value = world.maxPlayers.toString())
                DetailItem(label = "Compatible", value = if (world.isCompatible) "Yes" else "No")
                DetailItem(label = "Expired", value = if (world.isExpired) "Yes" else "No")
                world.activeVersion?.takeIf { it.isNotBlank() }?.let {
                    DetailItem(label = "Active Version", value = it)
                }
                world.ownerUuidOrXuid?.takeIf { it.isNotBlank() }?.let {
                    DetailItem(label = "Owner UUID/XUID", value = it)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun DetailItem(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun StatChip(
    label: String,
    value: String,
    isSuccess: Boolean = false
) {
    Surface(
        color = if (isSuccess) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = if (isSuccess) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private enum class UiState {
    Loading, Success, Error, Empty
}


private fun fetchRealms(
    httpClient: HttpClient,
    session: StepFullBedrockSession.FullBedrockSession?,
    callback: (List<RealmsWorld>?, String?) -> Unit
) {
    if (session == null || session.realmsXsts == null) {
        callback(null, "No session or Realms token available.")
        return
    }
    CompletableFuture.supplyAsync {
        try {
            val realmsService = BedrockRealmsService(httpClient, "1.21.94", session.realmsXsts)
            realmsService.isAvailable().thenAccept { isAvailable ->
                if (!isAvailable) {
                    callback(null, "Realms not supported for client version 1.21.94")
                } else {
                    realmsService.getWorlds().thenAccept { worlds ->
                        callback(worlds, null)
                    }.exceptionally { e ->
                        callback(null, e.cause?.message ?: "Unknown error fetching worlds")
                        null
                    }
                }
            }.exceptionally { e ->
                callback(null, e.cause?.message ?: "Unknown error checking availability")
                null
            }
        } catch (e: Exception) {
            callback(null, e.message ?: "Unknown error in fetchRealms")
        }
    }
}

private fun joinRealm(
    httpClient: HttpClient,
    session: StepFullBedrockSession.FullBedrockSession?,
    world: RealmsWorld,
    callback: (String?, String?) -> Unit
) {
    if (session == null || session.realmsXsts == null) {
        callback(null, "No session or Realms token available.")
        return
    }
    CompletableFuture.supplyAsync {
        try {
            val realmsService = BedrockRealmsService(httpClient, "1.21.94", session.realmsXsts)
            realmsService.joinWorld(world).thenAccept { address ->
                callback(address.toString(), null)
            }.exceptionally { e ->
                callback(null, e.cause?.message ?: "Unknown error joining realm")
                null
            }
        } catch (e: Exception) {
            callback(null, e.message ?: "Unknown error in joinRealm")
        }
    }
}


private fun acceptInvite(
    httpClient: HttpClient,
    session: StepFullBedrockSession.FullBedrockSession?,
    inviteCode: String,
    callback: (RealmsWorld?, String?) -> Unit
) {
    if (session == null || session.realmsXsts == null) {
        callback(null, "No session or Realms token available.")
        return
    }
    CompletableFuture.supplyAsync {
        try {
            val realmsService = BedrockRealmsService(httpClient, "1.21.94", session.realmsXsts)
            realmsService.acceptInvite(inviteCode).thenAccept { world ->
                callback(world, null)
            }.exceptionally { e ->
                callback(null, e.cause?.message ?: "Unknown error accepting invite")
                null
            }
        } catch (e: Exception) {
            callback(null, e.message ?: "Unknown error in acceptInvite")
        }
    }
}


private fun leaveRealm(
    httpClient: HttpClient,
    session: StepFullBedrockSession.FullBedrockSession?,
    world: RealmsWorld,
    callback: (Boolean, String?) -> Unit
) {
    if (session == null || session.realmsXsts == null) {
        callback(false, "No session or Realms token available.")
        return
    }
    CompletableFuture.supplyAsync {
        try {
            val realmsService = BedrockRealmsService(httpClient, "1.21.94", session.realmsXsts)
            realmsService.leaveInvitedRealm(world).thenAccept {
                callback(true, null)
            }.exceptionally { e ->
                callback(false, e.cause?.message ?: "Unknown error leaving realm")
                null
            }
        } catch (e: Exception) {
            callback(false, e.message ?: "Unknown error in leaveRealm")
        }
    }
}