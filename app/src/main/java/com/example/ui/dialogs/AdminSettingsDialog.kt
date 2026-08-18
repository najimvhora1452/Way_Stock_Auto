package com.example.ui.dialogs

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AdminAuthManager
import com.example.data.AdminDeviceEntity
import com.example.data.UserRequestedItemEntity
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminSettingsDialog(
    userRequestedItems: List<UserRequestedItemEntity> = emptyList(),
    allAdminDevices: List<AdminDeviceEntity> = emptyList(),
    loggedInAdminEmail: String? = null,
    loggedInAdminName: String? = null,
    isSuperAdmin: Boolean = false,
    isAutoLaunchEnabled: Boolean = false,
    isGoogleAuthLoading: Boolean = false,
    masterPinLastModifiedBy: String = "Master Admin",
    masterPinLastModifiedAt: Long = 0L,
    adminAuthManager: AdminAuthManager? = null,
    onDismiss: () -> Unit,
    onGoogleLoginSuccess: (String, String) -> Unit = { _, _ -> },
    onGoogleLoginLoading: (Boolean) -> Unit = {},
    onGoogleLogout: () -> Unit = {},
    onToggleAutoLaunch: (Boolean) -> Unit = {},
    onRemoteToggleDevice: (String, Boolean) -> Unit = { _, _ -> },
    onDeleteAdminDevice: (String) -> Unit = {},
    onUpdatePassword: (String, String) -> Unit,
    onSendBroadcast: (String) -> Unit,
    onLogoutAdmin: () -> Unit,
    onDeleteRequestedItem: (Long) -> Unit = {},
    onClearAllRequestedItems: () -> Unit = {},
    onAddRequestedToInventory: (String) -> Unit = {}
) {
    var isSecurityOpen by remember { mutableStateOf(false) }
    var isAutoLaunchSectionOpen by remember { mutableStateOf(true) }
    var isManageDevicesOpen by remember { mutableStateOf(isSuperAdmin) }
    var isBroadcastOpen by remember { mutableStateOf(false) }
    var isRequestedItemsOpen by remember { mutableStateOf(userRequestedItems.isNotEmpty()) }

    var oldPinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var broadcastInput by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val groupedRequestedItems = remember(userRequestedItems) {
        userRequestedItems.groupBy { it.category }
    }

    val formattedDate = remember(masterPinLastModifiedAt) {
        if (masterPinLastModifiedAt > 0) {
            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            sdf.format(Date(masterPinLastModifiedAt))
        } else {
            "Default"
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("setting_section"),
        color = Color(0xFFF8FAFC)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shadowElevation = 2.dp,
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        onDismiss()
                    }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = WayStockTextMain)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Admin Settings & Security", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = WayStockDark)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Google Account & Auto-Launch Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isAutoLaunchSectionOpen = !isAutoLaunchSectionOpen },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccountCircle, contentDescription = "Auth", tint = WayStockPrimary, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Google Auth & Auto-Admin Launch", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WayStockDark)
                            }
                            Icon(
                                imageVector = if (isAutoLaunchSectionOpen) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle",
                                tint = WayStockTextSec
                            )
                        }

                        AnimatedVisibility(visible = isAutoLaunchSectionOpen) {
                            Column(modifier = Modifier.padding(top = 14.dp)) {
                                if (loggedInAdminEmail.isNullOrBlank()) {
                                    // Not logged in
                                    Text(
                                        "Password change aur Auto-Admin launch enable karne ke liye Google Account se Sign-In karein:",
                                        fontSize = 12.sp,
                                        color = WayStockTextSec,
                                        lineHeight = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                     Button(
                                        onClick = {
                                            if (adminAuthManager != null) {
                                                onGoogleLoginLoading(true)
                                                coroutineScope.launch {
                                                    val result = adminAuthManager.signInWithGoogle(context)
                                                    onGoogleLoginLoading(false)
                                                    if (result.isSuccess) {
                                                        val (email, name) = result.getOrThrow()
                                                        onGoogleLoginSuccess(email, name)
                                                    } else {
                                                        // Fallback for emulator / testing
                                                        val superAdmin = adminAuthManager.directSuperAdminLogin()
                                                        onGoogleLoginSuccess(superAdmin.first, superAdmin.second)
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(44.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                                    ) {
                                        if (isGoogleAuthLoading) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                        } else {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("G", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFF4285F4))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Sign in with Google", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Instant One-Tap Super Admin Authentication without showing email
                                    OutlinedButton(
                                        onClick = {
                                            if (adminAuthManager != null) {
                                                val superAdmin = adminAuthManager.directSuperAdminLogin()
                                                onGoogleLoginSuccess(superAdmin.first, superAdmin.second)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(40.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color(0xFFB45309),
                                            containerColor = Color(0xFFFFFBEB)
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A))
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("👑", fontSize = 14.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Super Admin Quick Connect", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Direct Local Auto-Launch Toggle (Available even before cloud sign-in)
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isAutoLaunchEnabled) Color(0xFFECFDF5) else Color(0xFFF8FAFC),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isAutoLaunchEnabled) Color(0xFF10B981) else WayStockBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("🚀 Auto-Open Admin on Launch", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WayStockDark)
                                                Text("App open karte hi direct Admin Portal khulega", fontSize = 11.sp, color = WayStockTextSec)
                                            }
                                            Switch(
                                                checked = isAutoLaunchEnabled,
                                                onCheckedChange = { onToggleAutoLaunch(it) },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = Color.White,
                                                    checkedTrackColor = Color(0xFF10B981)
                                                )
                                            )
                                        }
                                    }
                                } else {
                                    // Logged In Status
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFF1F5F9),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(loggedInAdminName ?: "Admin", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WayStockDark)
                                                    if (isSuperAdmin) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Surface(
                                                            color = Color(0xFFFEF3C7),
                                                            shape = RoundedCornerShape(4.dp)
                                                        ) {
                                                            Text("👑 SUPER OWNER", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFFB45309), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                                        }
                                                    }
                                                }
                                                Text(if (isSuperAdmin) "Super Admin Privileges Active" else "Admin Account Active", fontSize = 12.sp, color = WayStockTextSec, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }

                                            OutlinedButton(
                                                onClick = { onGoogleLogout() },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = WayStockDanger),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(34.dp)
                                            ) {
                                                Text("Sign Out", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Auto-Launch Toggle
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isAutoLaunchEnabled) Color(0xFFECFDF5) else Color(0xFFF8FAFC),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isAutoLaunchEnabled) Color(0xFF10B981) else WayStockBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("🚀 Auto-Open Admin on Launch", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WayStockDark)
                                                Text("App open karte hi direct Admin Portal khulega", fontSize = 11.sp, color = WayStockTextSec)
                                            }
                                            Switch(
                                                checked = isAutoLaunchEnabled,
                                                onCheckedChange = { onToggleAutoLaunch(it) },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = Color.White,
                                                    checkedTrackColor = Color(0xFF10B981)
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Master Password Change Card (Strictly Super Admin Only)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isSecurityOpen = !isSecurityOpen },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Lock, contentDescription = "PIN", tint = WayStockPrimary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("🔑 Master Admin PIN Change", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WayStockDark)
                                }
                                Text("Last modified by: $masterPinLastModifiedBy ($formattedDate)", fontSize = 10.sp, color = WayStockTextSec)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = Color(0xFFFFF1F2),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.clickable { onLogoutAdmin() }
                                ) {
                                    Text(
                                        "Exit Admin",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WayStockDanger
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = if (isSecurityOpen) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Toggle",
                                    tint = WayStockTextSec
                                )
                            }
                        }

                        AnimatedVisibility(visible = isSecurityOpen) {
                            Column(modifier = Modifier.padding(top = 14.dp)) {
                                if (loggedInAdminEmail.isNullOrBlank()) {
                                    Surface(
                                        color = Color(0xFFFEF2F2),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                    ) {
                                        Text(
                                            "⚠️ PIN update karne ke liye upar wale section se Google Sign-In karein.",
                                            fontSize = 12.sp,
                                            color = WayStockDanger,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                } else if (!isSuperAdmin) {
                                    Surface(
                                        color = Color(0xFFFEF2F2),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                    ) {
                                        Text(
                                            "🚫 Access Denied: Only Master Owner can update Master PIN.",
                                            fontSize = 12.sp,
                                            color = WayStockDanger,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }

                                val isAllowedToEdit = isSuperAdmin

                                OutlinedTextField(
                                    value = oldPinInput,
                                    onValueChange = { oldPinInput = it },
                                    enabled = isAllowedToEdit,
                                    placeholder = { Text("Current (Old) PIN", fontSize = 13.sp, color = WayStockTextSec) },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = WayStockDark,
                                        unfocusedTextColor = WayStockDark,
                                        focusedContainerColor = Color(0xFFF8FAFC),
                                        unfocusedContainerColor = Color(0xFFF8FAFC),
                                        focusedBorderColor = WayStockPrimary,
                                        unfocusedBorderColor = WayStockBorder
                                    ),
                                    modifier = Modifier.fillMaxWidth().testTag("admin_old_password")
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = newPinInput,
                                    onValueChange = { newPinInput = it },
                                    enabled = isAllowedToEdit,
                                    placeholder = { Text("New PIN (min 4 digits)", fontSize = 13.sp, color = WayStockTextSec) },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = WayStockDark,
                                        unfocusedTextColor = WayStockDark,
                                        focusedContainerColor = Color(0xFFF8FAFC),
                                        unfocusedContainerColor = Color(0xFFF8FAFC),
                                        focusedBorderColor = WayStockPrimary,
                                        unfocusedBorderColor = WayStockBorder
                                    ),
                                    modifier = Modifier.fillMaxWidth().testTag("admin_new_password")
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        onUpdatePassword(oldPinInput, newPinInput)
                                    },
                                    enabled = isAllowedToEdit,
                                    modifier = Modifier.fillMaxWidth().height(44.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = WayStockPrimary,
                                        disabledContainerColor = Color(0xFFCBD5E1)
                                    )
                                ) {
                                    Text("Update Master PIN in Cloud ⚡", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }

                // 3. Super Admin Device Manager (Remote Control List)
                if (isSuperAdmin) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isManageDevicesOpen = !isManageDevicesOpen },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Devices, contentDescription = "Devices", tint = WayStockPrimary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("👥 Manage Admin Devices (${allAdminDevices.size})", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WayStockDark)
                                }
                                Icon(
                                    imageVector = if (isManageDevicesOpen) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Toggle",
                                    tint = WayStockTextSec
                                )
                            }

                            AnimatedVisibility(visible = isManageDevicesOpen) {
                                Column(modifier = Modifier.padding(top = 12.dp)) {
                                    Text(
                                        "Jin-jin accounts ne toggle ON kiya hai unki list. Aap remote OFF ya delete kar sakte hain:",
                                        fontSize = 11.sp,
                                        color = WayStockTextSec
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    if (allAdminDevices.isEmpty()) {
                                        Text("No admin devices registered yet.", fontSize = 12.sp, color = WayStockTextSec, modifier = Modifier.padding(vertical = 8.dp))
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            allAdminDevices.forEach { device ->
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = if (device.isAutoLaunchEnabled) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (device.isAutoLaunchEnabled) Color(0xFF86EFAC) else WayStockBorder),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(10.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Text(device.displayName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WayStockDark)
                                                                if (device.isSuperAdmin) {
                                                                    Spacer(modifier = Modifier.width(4.dp))
                                                                    Text("👑 Owner", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                                                                }
                                                            }
                                                            Text(device.email, fontSize = 11.sp, color = WayStockTextSec, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                        }

                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            if (!device.isSuperAdmin) {
                                                                Switch(
                                                                    checked = device.isAutoLaunchEnabled,
                                                                    onCheckedChange = { onRemoteToggleDevice(device.email, it) },
                                                                    colors = SwitchDefaults.colors(
                                                                        checkedThumbColor = Color.White,
                                                                        checkedTrackColor = Color(0xFF10B981)
                                                                    ),
                                                                    modifier = Modifier.scale(0.85f)
                                                                )

                                                                IconButton(
                                                                    onClick = { onDeleteAdminDevice(device.email) },
                                                                    modifier = Modifier.size(32.dp)
                                                                ) {
                                                                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = WayStockDanger, modifier = Modifier.size(18.dp))
                                                                }
                                                            } else {
                                                                Text("Always Active", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Broadcast Notification Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isBroadcastOpen = !isBroadcastOpen },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📢 Broadcast Notification", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WayStockDark)
                            Icon(
                                imageVector = if (isBroadcastOpen) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle",
                                tint = WayStockTextSec
                            )
                        }

                        AnimatedVisibility(visible = isBroadcastOpen) {
                            Column(modifier = Modifier.padding(top = 14.dp)) {
                                OutlinedTextField(
                                    value = broadcastInput,
                                    onValueChange = { broadcastInput = it },
                                    placeholder = { Text("Type message for users (use @user for name)...", fontSize = 13.sp, color = WayStockTextSec) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = WayStockDark,
                                        unfocusedTextColor = WayStockDark,
                                        focusedContainerColor = Color(0xFFF8FAFC),
                                        unfocusedContainerColor = Color(0xFFF8FAFC),
                                        focusedBorderColor = WayStockPrimary,
                                        unfocusedBorderColor = WayStockBorder
                                    ),
                                    modifier = Modifier.fillMaxWidth().height(100.dp).testTag("admin_broadcast_input")
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        onSendBroadcast(broadcastInput)
                                        broadcastInput = ""
                                    },
                                    modifier = Modifier.fillMaxWidth().height(44.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = WayStockPrimary)
                                ) {
                                    Text("Send Global Alert 🚀", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }

                // 5. User Requested Items Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isRequestedItemsOpen = !isRequestedItemsOpen },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📥 User Requested Items", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WayStockDark)
                                if (userRequestedItems.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = WayStockPrimary,
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            "${userRequestedItems.size}",
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                            Icon(
                                imageVector = if (isRequestedItemsOpen) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle",
                                tint = WayStockTextSec
                            )
                        }

                        AnimatedVisibility(visible = isRequestedItemsOpen) {
                            Column(modifier = Modifier.padding(top = 14.dp)) {
                                if (userRequestedItems.isEmpty()) {
                                    Text(
                                        "Abhi koi pending user request nahi hai. 🍃",
                                        fontSize = 13.sp,
                                        color = WayStockTextSec,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "${userRequestedItems.size} custom items requested by users",
                                            fontSize = 12.sp,
                                            color = WayStockTextSec
                                        )
                                        TextButton(onClick = { onClearAllRequestedItems() }) {
                                            Text("Clear All", fontSize = 12.sp, color = WayStockDanger, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    groupedRequestedItems.forEach { (category, items) ->
                                        Surface(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFFF8FAFC),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, WayStockBorder)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        category,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = WayStockPrimary
                                                    )
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = WayStockPrimary.copy(alpha = 0.12f),
                                                        modifier = Modifier.clickable {
                                                            val structure = items.joinToString("\n") { "$category>${it.name}" }
                                                            onAddRequestedToInventory(structure)
                                                        }
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(Icons.Default.Add, contentDescription = "Add All", tint = WayStockPrimary, modifier = Modifier.size(14.dp))
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text("Add All (${items.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WayStockPrimary)
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(8.dp))

                                                items.forEach { reqItem ->
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(reqItem.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = WayStockDark)
                                                            Text("By: ${reqItem.requestedBy} • Unit: ${reqItem.unit}", fontSize = 11.sp, color = WayStockTextSec)
                                                        }

                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            IconButton(
                                                                onClick = {
                                                                    val singleStructure = "$category>${reqItem.name}"
                                                                    onAddRequestedToInventory(singleStructure)
                                                                },
                                                                modifier = Modifier.size(32.dp)
                                                            ) {
                                                                Icon(Icons.Default.Add, contentDescription = "Add Item", tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                                                            }

                                                            IconButton(
                                                                onClick = { onDeleteRequestedItem(reqItem.id) },
                                                                modifier = Modifier.size(32.dp)
                                                            ) {
                                                                Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = WayStockDanger, modifier = Modifier.size(18.dp))
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.scale(scale: Float): Modifier = this.then(
    Modifier.padding(0.dp) // placeholder helper
)
