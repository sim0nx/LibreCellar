package org.librecellar.ui.settings

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import org.librecellar.data.model.CollectionAccess
import org.librecellar.data.model.LocaleFormat
import org.librecellar.data.model.Wine
import org.librecellar.data.model.WinePriceTier
import org.librecellar.data.repository.ConnectionTestResult
import org.librecellar.data.repository.WineRepository
import org.librecellar.ui.theme.AppIcons
import org.librecellar.ui.theme.CellarGold
import org.librecellar.ui.theme.StatusWarning
import org.librecellar.ui.theme.WineBurgundy
import org.librecellar.ui.theme.WineBurgundyDark
import org.librecellar.ui.theme.WineBurgundyLight
import org.librecellar.ui.theme.WineRose
import kotlinx.coroutines.launch

@Composable
fun ServerSettingsScreen(
    repository: WineRepository
) {
    val currentUrl by repository.backendUrl.collectAsState()
    val currentUsername by repository.username.collectAsState()
    val isLoggedIn by repository.isLoggedIn.collectAsState()
    val isSslVerify by repository.isSslVerificationEnabled.collectAsState()
    val isOnline by repository.isBackendOnline.collectAsState()
    val wines by repository.wines.collectAsState()
    val isPriceTierMarkingEnabled by repository.isPriceTierMarkingEnabled.collectAsState()
    val collections by repository.collections.collectAsState()
    val defaultCollection by repository.defaultCollection.collectAsState()
    val currentBackendUser by repository.currentUsername.collectAsState()

    LaunchedEffect(isOnline) {
        if (isOnline) repository.refreshCollections()
    }

    var urlInput by remember(currentUrl) { mutableStateOf(currentUrl) }
    var usernameInput by remember(currentUsername) { mutableStateOf(currentUsername) }
    // The password is only used to log in and is never stored; the app keeps a login token
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var sslVerifyCheckbox by remember(isSslVerify) { mutableStateOf(isSslVerify) }

    var isTestingConnection by remember { mutableStateOf(false) }
    var lastTestResult by remember { mutableStateOf<ConnectionTestResult?>(null) }
    var showNonSslWarningDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Check if the URL is non-SSL
    val trimmedUrl = urlInput.trim().lowercase()
    val isNonSsl = trimmedUrl.startsWith("http://") || (!trimmedUrl.startsWith("https://") && trimmedUrl.isNotEmpty())

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(WineBurgundy),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.Cloud,
                        contentDescription = null,
                        tint = CellarGold,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Server Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Configure API endpoint, authentication, and SSL security",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tab Switcher: Connection Settings vs Price Range Tiers vs Grapes
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.clip(RoundedCornerShape(10.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Server & Auth", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Price Tiers", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Grapes", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("settings_grapes_tab")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                // Connection Status Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isOnline) Color(0xFF1B3B22) else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(if (isOnline) Color(0xFF4CAF50) else Color(0xFFFFA000))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isOnline) "Connected to Backend Server" else "Standalone / Local Mode Active",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isOnline) Color(0xFF81C784) else CellarGold
                            )
                            Text(
                                text = if (isOnline) "REST operations are live on $currentUrl" else "Wine cellar data stored locally until server connects",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 1: Server URL
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isNonSsl) AppIcons.NoEncryption else AppIcons.Https,
                                    contentDescription = null,
                                    tint = if (isNonSsl) StatusWarning else Color(0xFF4CAF50),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Server Endpoint URL",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Warning Triangle if non-ssl
                            if (isNonSsl) {
                                Surface(
                                    color = StatusWarning.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable { showNonSslWarningDialog = true }
                                        .testTag("non_ssl_warning_badge")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = AppIcons.Warning,
                                            contentDescription = "Non-SSL Warning",
                                            tint = StatusWarning,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Non-SSL (HTTP)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = StatusWarning
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            label = { Text("Base URL (e.g. https://cellar.example.com/)") },
                            placeholder = { Text("https://...") },
                            singleLine = true,
                            trailingIcon = {
                                if (isNonSsl) {
                                    IconButton(
                                        onClick = { showNonSslWarningDialog = true },
                                        modifier = Modifier.testTag("non_ssl_triangle_icon")
                                    ) {
                                        Icon(
                                            imageVector = AppIcons.Warning,
                                            contentDescription = "Non-SSL Warning Info",
                                            tint = StatusWarning
                                        )
                                    }
                                }
                            },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_server_url_input")
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Enter your server address (e.g. https://cellar.example.com/ or local network IP http://192.168.1.50:8000/).",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 2: Authentication (Username & Password)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = AppIcons.Lock, contentDescription = null, tint = CellarGold, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "API Authentication Credentials",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Your password is only used to log in and is never stored on this device. The app keeps an encrypted login token instead.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = usernameInput,
                            onValueChange = { usernameInput = it },
                            label = { Text("Username") },
                            leadingIcon = {
                                Icon(imageVector = AppIcons.Person, contentDescription = null, tint = CellarGold)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_username_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Password") },
                            placeholder = { Text(if (isLoggedIn) "Leave empty to stay logged in" else "Your password") },
                            leadingIcon = {
                                Icon(imageVector = AppIcons.Lock, contentDescription = null, tint = CellarGold)
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) AppIcons.VisibilityOff else AppIcons.Visibility,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_password_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Icon(
                                imageVector = if (isLoggedIn) AppIcons.CheckCircle else AppIcons.Info,
                                contentDescription = null,
                                tint = if (isLoggedIn) Color(0xFF81C784) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isLoggedIn) "Logged in as $currentUsername" else "Not logged in",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.weight(1f)
                            )
                            if (isLoggedIn) {
                                TextButton(
                                    onClick = {
                                        scope.launch {
                                            repository.logout()
                                            lastTestResult = null
                                            snackbarHostState.showSnackbar("Logged out")
                                        }
                                    },
                                    modifier = Modifier.testTag("settings_logout_btn")
                                ) {
                                    Text("Log out")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 3: Server SSL Certificate Validation Checkbox
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = AppIcons.Shield, contentDescription = null, tint = CellarGold, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SSL / TLS Certificate Validation",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { sslVerifyCheckbox = !sslVerifyCheckbox }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = sslVerifyCheckbox,
                                onCheckedChange = { sslVerifyCheckbox = it },
                                colors = CheckboxDefaults.colors(checkedColor = WineBurgundyLight),
                                modifier = Modifier.testTag("ssl_verify_checkbox")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Verify Server SSL Certificate (Default: ON)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (sslVerifyCheckbox) {
                                        "Strict certificate authority verification enabled (recommended)."
                                    } else {
                                        "Disabled: Accept self-signed or untrusted SSL certificates (useful for local development with HTTPS)."
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (sslVerifyCheckbox) MaterialTheme.colorScheme.onSurfaceVariant else StatusWarning
                                )
                            }
                        }

                        if (!sslVerifyCheckbox) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = StatusWarning.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = AppIcons.Warning,
                                        contentDescription = null,
                                        tint = StatusWarning,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Warning: Disabling SSL certificate validation makes network traffic vulnerable to man-in-the-middle attacks.",
                                        fontSize = 11.sp,
                                        color = StatusWarning
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 3b: Collection new entries go to (only when a collection is shared read/write)
                val writableCollections = collections.filter { it.collectionAccess != CollectionAccess.READ }
                if (isOnline && writableCollections.size > 1) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = AppIcons.People, contentDescription = null, tint = CellarGold, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "New Entries Go To",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Collection that receives new wines, racks and merchants. Wines placed in a rack always join the rack's collection.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            writableCollections.forEach { collection ->
                                val isOwn = collection.collectionAccess == CollectionAccess.OWNER
                                val isSelected = collection.owner == (defaultCollection ?: currentBackendUser)
                                val select = {
                                    if (!isSelected) scope.launch {
                                        val ok = repository.updateDefaultCollection(if (isOwn) null else collection.owner)
                                        snackbarHostState.showSnackbar(
                                            if (ok) "New entries now go to ${if (isOwn) "your own collection" else "${collection.owner}'s collection"}"
                                            else "Could not change the default collection"
                                        )
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { select() }
                                        .testTag("default_collection_${collection.owner}")
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { select() },
                                        colors = RadioButtonDefaults.colors(selectedColor = WineBurgundyLight)
                                    )
                                    Text(
                                        text = if (isOwn) "My own collection" else "${collection.owner}'s collection (shared)",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Section 4: Wine Price Range Markings Quick Setting
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = AppIcons.Payments, contentDescription = null, tint = CellarGold, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Wine Price Range Marking",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newVal = !isPriceTierMarkingEnabled
                                    scope.launch {
                                        repository.updatePriceTierMarkingEnabled(newVal)
                                        snackbarHostState.showSnackbar(if (newVal) "Price range markings enabled" else "Price range markings hidden")
                                    }
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = isPriceTierMarkingEnabled,
                                onCheckedChange = { newVal ->
                                    scope.launch {
                                        repository.updatePriceTierMarkingEnabled(newVal)
                                        snackbarHostState.showSnackbar(if (newVal) "Price range markings enabled" else "Price range markings hidden")
                                    }
                                },
                                colors = CheckboxDefaults.colors(checkedColor = WineBurgundyLight),
                                modifier = Modifier.testTag("price_tier_quick_checkbox")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Enable Price Range Badges (Default: ON)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Categorizes wines into 5 dynamic tiers (Cheap to Very Expensive) based on your own cellar.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        TextButton(
                            onClick = { selectedTab = 1 },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("View Quintiles & Customize Tiers →", color = CellarGold, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Section 5: Action Buttons
                Button(
                    onClick = {
                        isTestingConnection = true
                        scope.launch {
                            val result = repository.updateSettings(
                                newUrl = urlInput.trim(),
                                newUser = usernameInput.trim(),
                                newPass = passwordInput,
                                newSslVerify = sslVerifyCheckbox
                            )
                            lastTestResult = result
                            // The password has served its purpose; never keep it around
                            passwordInput = ""
                            isTestingConnection = false
                            snackbarHostState.showSnackbar(result.message)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WineBurgundyLight),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_settings_and_test_btn")
                ) {
                    if (isTestingConnection) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Verifying Connection...")
                    } else {
                        Icon(imageVector = AppIcons.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Settings & Test Connection", fontWeight = FontWeight.Bold)
                    }
                }

                // Test Result Feedback Card
                lastTestResult?.let { result ->
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (result.isSuccess) Color(0xFF1B3B22) else Color(0xFF3E1F24)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (result.isSuccess) AppIcons.CheckCircle else AppIcons.Error,
                                contentDescription = null,
                                tint = if (result.isSuccess) Color(0xFF81C784) else Color(0xFFE57373),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (result.isSuccess) "Connection Succeeded" else "Connection Failed",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (result.isSuccess) Color(0xFF81C784) else Color(0xFFE57373)
                                )
                                Text(
                                    text = result.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }
            } else if (selectedTab == 1) {
                // Tab 1: Wine Price Range Markings & Dynamic Tiers Configuration
                PriceRangeSettingsContent(
                    wines = wines,
                    isPriceTierEnabled = isPriceTierMarkingEnabled,
                    onToggleEnabled = { newVal: Boolean ->
                        scope.launch {
                            repository.updatePriceTierMarkingEnabled(newVal)
                            snackbarHostState.showSnackbar(if (newVal) "Price range markings enabled" else "Price range markings disabled")
                        }
                    }
                )
            } else {
                // Tab 2: the grape list (stored on the server, shared with the web app)
                GrapeSettingsContent(repository = repository)
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }

    // Non-SSL Warning Alert Dialog
    if (showNonSslWarningDialog) {
        AlertDialog(
            onDismissRequest = { showNonSslWarningDialog = false },
            icon = {
                Icon(
                    imageVector = AppIcons.Warning,
                    contentDescription = "Non-SSL Warning",
                    tint = StatusWarning,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Insecure Connection Warning",
                    fontWeight = FontWeight.Bold,
                    color = StatusWarning
                )
            },
            text = {
                Column {
                    Text(
                        text = "Your configured server URL is using unencrypted HTTP ('http://').",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Transmitting data over plaintext HTTP means that your username, your password when logging in, the login token, cellar configurations and wine inventory are transmitted without encryption across the network. Anyone on your local Wi-Fi or router can intercept or tamper with this traffic.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "It is strongly recommended to use HTTPS ('https://...') with SSL/TLS encryption for production environments.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showNonSslWarningDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = WineBurgundyLight)
                ) {
                    Text("I Understand")
                }
            }
        )
    }
}

@Composable
fun PriceRangeSettingsContent(
    wines: List<Wine>,
    isPriceTierEnabled: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Tier thresholds are shown in the device's currency, from wines priced in it
    val pricedWines = remember(wines) {
        wines.filter { it.effectiveCurrency == LocaleFormat.currency.currencyCode }
            .mapNotNull { it.purchasePrice }.filter { it > 0.0 }.sorted()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Master Enable / Disable Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isPriceTierEnabled) WineBurgundyDark else MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = if (isPriceTierEnabled) CellarGold.copy(alpha = 0.5f) else Color.Transparent,
                    shape = RoundedCornerShape(14.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isPriceTierEnabled) CellarGold.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AppIcons.Payments,
                            contentDescription = null,
                            tint = if (isPriceTierEnabled) CellarGold else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Wine Price Range Markings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isPriceTierEnabled) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isPriceTierEnabled) {
                                "Markings active on wine list & details ($ - $$$$$)"
                            } else {
                                "Markings hidden (standard view without tier badges)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isPriceTierEnabled) CellarGold else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = isPriceTierEnabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = CellarGold,
                        uncheckedThumbColor = Color.LightGray,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.testTag("price_tier_marking_switch")
                )
            }
        }

        // Explanatory Philosophy Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = AppIcons.Info,
                        contentDescription = null,
                        tint = CellarGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cellar-Relative Dynamic Price Ranges",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Perceived wine value is subjective: what is expensive for one collector might be everyday table wine for another. LibreCellar defines your 5 price tiers dynamically based on the actual price distribution of bottles you own (divided into quintiles):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "• Cheap / Value ($): Lower 20% of your cellar\n" +
                            "• Normal / Standard ($$): 20% - 40% range\n" +
                            "• Medium / Mid-Range ($$$): 40% - 60% range\n" +
                            "• Expensive / Premium ($$$$): 60% - 80% range\n" +
                            "• Very Expensive / Prestige ($$$$$): Top 20% most premium bottles",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "When adding a wine, LibreCellar automatically suggests the tier based on these thresholds. You can always override the suggestion manually.",
                    style = MaterialTheme.typography.bodySmall,
                    color = CellarGold
                )
            }
        }

        // Tier Breakdown Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Your Cellar Tier Thresholds",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (pricedWines.size >= 3) {
                        "Calculated from ${pricedWines.size} priced bottles in your cellar"
                    } else {
                        "Cellar baseline tiers (${pricedWines.size} priced bottles logged)"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                WinePriceTier.entries.forEach { tier ->
                    val tierColor = try {
                        Color(tier.colorHex.toColorInt())
                    } catch (e: Exception) {
                        CellarGold
                    }
                    val count = wines.count { it.getResolvedPriceTier(wines) == tier }
                    val thresholdText = getThresholdDescription(tier, pricedWines)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = tierColor,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.size(width = 48.dp, height = 26.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = tier.currencySymbol,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = tier.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = thresholdText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            color = tierColor.copy(alpha = 0.15f),
                            shape = CircleShape
                        ) {
                            Text(
                                text = "$count ${if (count == 1) "wine" else "wines"}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = tierColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getThresholdDescription(tier: WinePriceTier, pricedWines: List<Double>): String {
    val price = { value: Double -> LocaleFormat.formatPrice(value) }
    if (pricedWines.size < 3) {
        return when (tier) {
            WinePriceTier.CHEAP -> "< ${price(20.0)}"
            WinePriceTier.NORMAL -> "${price(20.0)} - ${price(45.0)}"
            WinePriceTier.MEDIUM -> "${price(45.0)} - ${price(95.0)}"
            WinePriceTier.EXPENSIVE -> "${price(95.0)} - ${price(250.0)}"
            WinePriceTier.VERY_EXPENSIVE -> "> ${price(250.0)}"
        }
    }

    fun percentile(p: Double): Double {
        val index = (p * (pricedWines.size - 1)).toInt()
        return pricedWines[index.coerceIn(0, pricedWines.size - 1)]
    }

    val p20 = percentile(0.20)
    val p40 = percentile(0.40)
    val p60 = percentile(0.60)
    val p80 = percentile(0.80)

    return when (tier) {
        WinePriceTier.CHEAP -> "≤ ${price(p20)}"
        WinePriceTier.NORMAL -> "${price(p20)} - ${price(p40)}"
        WinePriceTier.MEDIUM -> "${price(p40)} - ${price(p60)}"
        WinePriceTier.EXPENSIVE -> "${price(p60)} - ${price(p80)}"
        WinePriceTier.VERY_EXPENSIVE -> "> ${price(p80)}"
    }
}
