package org.librecellar.ui.settings

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Https
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NoEncryption
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.TabRow
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
import org.librecellar.data.model.CollectionAccess
import org.librecellar.data.model.LocaleFormat
import org.librecellar.data.model.Wine
import org.librecellar.data.model.WinePriceTier
import org.librecellar.data.repository.ConnectionTestResult
import org.librecellar.data.repository.WineRepository
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
    val currentPassword by repository.password.collectAsState()
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
    var passwordInput by remember(currentPassword) { mutableStateOf(currentPassword) }
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
                        imageVector = Icons.Default.Cloud,
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

            // Tab Switcher: Connection Settings vs Price Range Tiers vs API Reference
            TabRow(
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
                    text = { Text("API Reference", fontWeight = FontWeight.SemiBold) }
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
                                    imageVector = if (isNonSsl) Icons.Default.NoEncryption else Icons.Default.Https,
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
                                            imageVector = Icons.Default.Warning,
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
                                            imageVector = Icons.Default.Warning,
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
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = CellarGold, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "API Authentication Credentials",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Provided credentials are transmitted via HTTP Basic Auth header for secured endpoints.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = usernameInput,
                            onValueChange = { usernameInput = it },
                            label = { Text("Username") },
                            placeholder = { Text("Leave blank if API is open") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = CellarGold)
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
                            placeholder = { Text("API password / secret") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = CellarGold)
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
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
                            Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = CellarGold, modifier = Modifier.size(20.dp))
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
                                        imageVector = Icons.Default.Warning,
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
                                Icon(imageVector = Icons.Default.People, contentDescription = null, tint = CellarGold, modifier = Modifier.size(20.dp))
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
                            Icon(imageVector = Icons.Default.Payments, contentDescription = null, tint = CellarGold, modifier = Modifier.size(20.dp))
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
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
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
                                imageVector = if (result.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
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
                // Tab 2: Backend Server Reference Implementation
                Text(
                    text = "Backend Server Reference Implementation with HTTP Basic Auth:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = CellarGold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Run this file with 'uvicorn main:app --host 0.0.0.0 --port 8000 --reload' on your server.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0B0E)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = getFastApiPythonSnippet(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color(0xFFE0E0E0),
                        modifier = Modifier.padding(12.dp)
                    )
                }
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
                    imageVector = Icons.Default.Warning,
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
                        text = "Transmitting data over plaintext HTTP means that your API username, password, cellar configurations, and wine inventory are transmitted without encryption across the network. Anyone on your local Wi-Fi or router can intercept or tamper with this traffic.",
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
                            imageVector = Icons.Default.Payments,
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
                        imageVector = Icons.Default.Info,
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
                        Color(android.graphics.Color.parseColor(tier.colorHex))
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

private fun getFastApiPythonSnippet(): String {
    return """
from fastapi import FastAPI, Depends, HTTPException, status, Response
from fastapi.security import HTTPBasic, HTTPBasicCredentials
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, ConfigDict
from typing import Optional, List
import secrets
from sqlalchemy import String, Integer, Float, create_engine, select
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column, Session, sessionmaker
import uvicorn

# --- Database Setup (SQLAlchemy 2.1) ---
DATABASE_URL = "sqlite:///./librecellar.db"
# For PostgreSQL: "postgresql+psycopg2://user:password@localhost:5432/librecellar"

engine = create_engine(
    DATABASE_URL,
    connect_args={"check_same_thread": False} if "sqlite" in DATABASE_URL else {},
    echo=False
)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

class Base(DeclarativeBase):
    pass

class WineModel(Base):
    __tablename__ = "wines"

    id: Mapped[str] = mapped_column(String(36), primary_key=True)
    name: Mapped[str] = mapped_column(String, nullable=False)
    vintage: Mapped[int] = mapped_column(Integer, nullable=False)
    country: Mapped[Optional[str]] = mapped_column(String, nullable=True)
    region: Mapped[Optional[str]] = mapped_column(String, nullable=True)
    varietal: Mapped[Optional[str]] = mapped_column(String, nullable=True)
    producer: Mapped[Optional[str]] = mapped_column(String, nullable=True)
    purchase_date: Mapped[Optional[str]] = mapped_column(String, nullable=True)
    alcohol_percent: Mapped[Optional[float]] = mapped_column(Float, nullable=True)
    size: Mapped[Optional[str]] = mapped_column(String, nullable=True, default="0.75L")
    tasting_rating: Mapped[Optional[int]] = mapped_column(Integer, nullable=True)
    comments: Mapped[Optional[str]] = mapped_column(String, nullable=True)
    purchase_price: Mapped[Optional[float]] = mapped_column(Float, nullable=True)
    purchase_location: Mapped[Optional[str]] = mapped_column(String, nullable=True)
    photo_url: Mapped[Optional[str]] = mapped_column(String, nullable=True)
    barcode: Mapped[Optional[str]] = mapped_column(String, nullable=True, index=True)
    price_tier: Mapped[Optional[str]] = mapped_column(String, nullable=True)
    quantity: Mapped[int] = mapped_column(Integer, default=1, nullable=False)
    location_id: Mapped[Optional[str]] = mapped_column(String(36), nullable=True)
    slot_row: Mapped[Optional[int]] = mapped_column(Integer, nullable=True)
    slot_column: Mapped[Optional[int]] = mapped_column(Integer, nullable=True)
    slot_label: Mapped[Optional[str]] = mapped_column(String, nullable=True)
    wine_type: Mapped[str] = mapped_column(String, default="RED", nullable=False)

class CellarLocationModel(Base):
    __tablename__ = "cellar_locations"

    id: Mapped[str] = mapped_column(String(36), primary_key=True)
    name: Mapped[str] = mapped_column(String, nullable=False)
    rows: Mapped[int] = mapped_column(Integer, default=4, nullable=False)
    columns: Mapped[int] = mapped_column(Integer, default=6, nullable=False)
    description: Mapped[Optional[str]] = mapped_column(String, nullable=True)
    temperature_celsius: Mapped[Optional[float]] = mapped_column(Float, nullable=True)
    humidity_percent: Mapped[Optional[float]] = mapped_column(Float, nullable=True)
    color_hex: Mapped[Optional[str]] = mapped_column(String, default="#6B1D2F", nullable=True)

class SupplierModel(Base):
    __tablename__ = "suppliers"

    id: Mapped[str] = mapped_column(String(36), primary_key=True)
    name: Mapped[str] = mapped_column(String, nullable=False)
    type: Mapped[str] = mapped_column(String, default="Wine Merchant", nullable=False)
    city: Mapped[Optional[str]] = mapped_column(String, nullable=True)
    country: Mapped[Optional[str]] = mapped_column(String, nullable=True)
    website: Mapped[Optional[str]] = mapped_column(String, nullable=True)
    notes: Mapped[Optional[str]] = mapped_column(String, nullable=True)
    date_added: Mapped[Optional[str]] = mapped_column(String, nullable=True)

Base.metadata.create_all(bind=engine)

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

# --- Pydantic Schemas (FastAPI DTOs) ---
class Wine(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: str
    name: str
    vintage: int
    country: Optional[str] = None
    region: Optional[str] = None
    varietal: Optional[str] = None
    producer: Optional[str] = None
    purchase_date: Optional[str] = None
    alcohol_percent: Optional[float] = None
    size: Optional[str] = "0.75L"
    tasting_rating: Optional[int] = None
    comments: Optional[str] = None
    purchase_price: Optional[float] = None
    purchase_location: Optional[str] = None
    photo_url: Optional[str] = None
    barcode: Optional[str] = None
    price_tier: Optional[str] = None
    quantity: int = 1
    location_id: Optional[str] = None
    slot_row: Optional[int] = None
    slot_column: Optional[int] = None
    slot_label: Optional[str] = None
    wine_type: str = "RED"

class CellarLocation(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: str
    name: str
    rows: int = 4
    columns: int = 6
    description: Optional[str] = None
    temperature_celsius: Optional[float] = None
    humidity_percent: Optional[float] = None
    color_hex: Optional[str] = "#6B1D2F"

class Supplier(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: str
    name: str
    type: str = "Wine Merchant"
    city: Optional[str] = None
    country: Optional[str] = None
    website: Optional[str] = None
    notes: Optional[str] = None
    date_added: Optional[str] = None

class AssignSlotRequest(BaseModel):
    wine_id: str
    location_id: Optional[str] = None
    slot_row: Optional[int] = None
    slot_column: Optional[int] = None

class DecrementInventoryResponse(BaseModel):
    wine_id: str
    previous_quantity: int
    remaining_quantity: int
    message: str

# --- FastAPI App Setup ---
app = FastAPI(title="LibreCellar Wine API", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

security = HTTPBasic()

def authenticate(credentials: HTTPBasicCredentials = Depends(security)):
    correct_username = secrets.compare_digest(credentials.username, "admin")
    correct_password = secrets.compare_digest(credentials.password, "cellar123")
    if not (correct_username and correct_password):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect username or password",
            headers={"WWW-Authenticate": "Basic"},
        )
    return credentials.username

# --- Health Check ---
@app.get("/api/health")
def health():
    return {"status": "ok", "app": "LibreCellar Server"}

# --- Wine Endpoints ---
@app.get("/api/wines", response_model=List[Wine])
def get_wines(user: str = Depends(authenticate), db: Session = Depends(get_db)):
    return db.scalars(select(WineModel)).all()

@app.get("/api/wines/{id}", response_model=Wine)
def get_wine(id: str, user: str = Depends(authenticate), db: Session = Depends(get_db)):
    wine = db.scalar(select(WineModel).where(WineModel.id == id))
    if not wine:
        raise HTTPException(status_code=404, detail="Wine not found")
    return wine

@app.get("/api/wines/barcode/{barcode}", response_model=Wine)
def get_wine_by_barcode(barcode: str, user: str = Depends(authenticate), db: Session = Depends(get_db)):
    wine = db.scalar(select(WineModel).where(WineModel.barcode == barcode))
    if not wine:
        raise HTTPException(status_code=404, detail="Wine with this barcode not found")
    return wine

@app.post("/api/wines", response_model=Wine, status_code=201)
def create_wine(wine: Wine, user: str = Depends(authenticate), db: Session = Depends(get_db)):
    existing = db.scalar(select(WineModel).where(WineModel.id == wine.id))
    if existing:
        raise HTTPException(status_code=409, detail="Wine ID already exists")
    wine_row = WineModel(**wine.model_dump())
    db.add(wine_row)
    db.commit()
    db.refresh(wine_row)
    return wine_row

@app.put("/api/wines/{id}", response_model=Wine)
def update_wine(id: str, wine: Wine, user: str = Depends(authenticate), db: Session = Depends(get_db)):
    existing = db.scalar(select(WineModel).where(WineModel.id == id))
    if not existing:
        raise HTTPException(status_code=404, detail="Wine not found")
    for field, value in wine.model_dump().items():
        setattr(existing, field, value)
    db.commit()
    db.refresh(existing)
    return existing

@app.delete("/api/wines/{id}", status_code=204)
def delete_wine(id: str, user: str = Depends(authenticate), db: Session = Depends(get_db)):
    existing = db.scalar(select(WineModel).where(WineModel.id == id))
    if not existing:
        raise HTTPException(status_code=404, detail="Wine not found")
    db.delete(existing)
    db.commit()
    return Response(status_code=status.HTTP_204_NO_CONTENT)

@app.post("/api/wines/{wine_id}/decrement", response_model=DecrementInventoryResponse)
def decrement_inventory(wine_id: str, user: str = Depends(authenticate), db: Session = Depends(get_db)):
    wine = db.scalar(select(WineModel).where(WineModel.id == wine_id))
    if not wine:
        raise HTTPException(status_code=404, detail="Wine not found")
    prev_qty = wine.quantity
    wine.quantity = max(0, prev_qty - 1)
    db.commit()
    return DecrementInventoryResponse(
        wine_id=wine_id,
        previous_quantity=prev_qty,
        remaining_quantity=wine.quantity,
        message="Stock quantity decremented"
    )

# --- Cellar Locations & Racks Endpoints ---
@app.get("/api/cellar/locations", response_model=List[CellarLocation])
def get_locations(user: str = Depends(authenticate), db: Session = Depends(get_db)):
    return db.scalars(select(CellarLocationModel)).all()

@app.post("/api/cellar/locations", response_model=CellarLocation, status_code=201)
def create_location(loc: CellarLocation, user: str = Depends(authenticate), db: Session = Depends(get_db)):
    existing = db.scalar(select(CellarLocationModel).where(CellarLocationModel.id == loc.id))
    if existing:
        raise HTTPException(status_code=409, detail="Location ID already exists")
    loc_row = CellarLocationModel(**loc.model_dump())
    db.add(loc_row)
    db.commit()
    db.refresh(loc_row)
    return loc_row

@app.put("/api/cellar/locations/{id}", response_model=CellarLocation)
def update_location(id: str, loc: CellarLocation, user: str = Depends(authenticate), db: Session = Depends(get_db)):
    existing = db.scalar(select(CellarLocationModel).where(CellarLocationModel.id == id))
    if not existing:
        raise HTTPException(status_code=404, detail="Location not found")
    for field, value in loc.model_dump().items():
        setattr(existing, field, value)
    db.commit()
    db.refresh(existing)
    return existing

@app.delete("/api/cellar/locations/{id}", status_code=204)
def delete_location(id: str, user: str = Depends(authenticate), db: Session = Depends(get_db)):
    existing = db.scalar(select(CellarLocationModel).where(CellarLocationModel.id == id))
    if not existing:
        raise HTTPException(status_code=404, detail="Location not found")
    db.delete(existing)
    db.commit()
    return Response(status_code=status.HTTP_204_NO_CONTENT)

@app.post("/api/cellar/locations/{id}/assign-slot", response_model=Wine)
def assign_slot(id: str, req: AssignSlotRequest, user: str = Depends(authenticate), db: Session = Depends(get_db)):
    wine = db.scalar(select(WineModel).where(WineModel.id == req.wine_id))
    if not wine:
        raise HTTPException(status_code=404, detail="Wine not found")
    wine.location_id = req.location_id
    wine.slot_row = req.slot_row
    wine.slot_column = req.slot_column
    if req.slot_row is not None and req.slot_column is not None:
        wine.slot_label = f"Row {req.slot_row + 1}, Slot {req.slot_column + 1}"
    else:
        wine.slot_label = None
    db.commit()
    db.refresh(wine)
    return wine

# --- Suppliers / Merchants Endpoints ---
@app.get("/api/suppliers", response_model=List[Supplier])
def get_suppliers(user: str = Depends(authenticate), db: Session = Depends(get_db)):
    return db.scalars(select(SupplierModel)).all()

@app.post("/api/suppliers", response_model=Supplier, status_code=201)
def create_supplier(sup: Supplier, user: str = Depends(authenticate), db: Session = Depends(get_db)):
    existing = db.scalar(select(SupplierModel).where(SupplierModel.id == sup.id))
    if existing:
        raise HTTPException(status_code=409, detail="Supplier ID already exists")
    sup_row = SupplierModel(**sup.model_dump())
    db.add(sup_row)
    db.commit()
    db.refresh(sup_row)
    return sup_row

@app.put("/api/suppliers/{id}", response_model=Supplier)
def update_supplier(id: str, sup: Supplier, user: str = Depends(authenticate), db: Session = Depends(get_db)):
    existing = db.scalar(select(SupplierModel).where(SupplierModel.id == id))
    if not existing:
        raise HTTPException(status_code=404, detail="Supplier not found")
    for field, value in sup.model_dump().items():
        setattr(existing, field, value)
    db.commit()
    db.refresh(existing)
    return existing

@app.delete("/api/suppliers/{id}", status_code=204)
def delete_supplier(id: str, user: str = Depends(authenticate), db: Session = Depends(get_db)):
    existing = db.scalar(select(SupplierModel).where(SupplierModel.id == id))
    if not existing:
        raise HTTPException(status_code=404, detail="Supplier not found")
    db.delete(existing)
    db.commit()
    return Response(status_code=status.HTTP_204_NO_CONTENT)

# --- Bottle Pictures API (per-user subfolder & UUIDv7 naming) ---
import os, time, uuid
from fastapi import UploadFile, File
from fastapi.responses import FileResponse

IMAGE_DIR = os.getenv("LIBRECELLAR_IMAGE_DIR", "/path/to/librecellar/data/images")

def generate_uuidv7() -> str:
    timestamp_ms = int(time.time() * 1000)
    rand_bytes = os.urandom(10)
    time_high = timestamp_ms & 0xFFFFFFFFFFFF
    rand_a = int.from_bytes(rand_bytes[0:2], "big") & 0x0FFF
    rand_b = int.from_bytes(rand_bytes[2:10], "big") & 0x3FFFFFFFFFFFFFFF
    msb = (time_high << 16) | (0x7 << 12) | rand_a
    lsb = (0x2 << 62) | rand_b
    return str(uuid.UUID(int=(msb << 64) | lsb))

@app.post("/api/images/upload")
async def upload_image(
    file: UploadFile = File(...),
    user: str = Depends(authenticate)
):
    # Stored under a subfolder named after the authenticated user, renamed to UUIDv7.<extension>.
    # Only the filename is saved in the database.
    user_folder = os.path.join(IMAGE_DIR, user)
    os.makedirs(user_folder, exist_ok=True)
    ext = os.path.splitext(file.filename)[1].lower() if file.filename else ".jpg"
    if not ext:
        ext = ".jpg"
    filename = f"{generate_uuidv7()}{ext}"
    with open(os.path.join(user_folder, filename), "wb") as f:
        f.write(await file.read())
    return {"filename": filename, "url": f"/api/images/{filename}"}

@app.get("/api/images/{filename}")
def get_image(filename: str, user: str = Depends(authenticate)):
    file_path = os.path.join(IMAGE_DIR, user, os.path.basename(filename))
    if not os.path.isfile(file_path):
        raise HTTPException(status_code=404, detail="Image not found")
    return FileResponse(file_path)

@app.delete("/api/images/{filename}", status_code=204)
def delete_image(filename: str, user: str = Depends(authenticate)):
    file_path = os.path.join(IMAGE_DIR, user, os.path.basename(filename))
    if os.path.isfile(file_path):
        os.remove(file_path)
    return Response(status_code=status.HTTP_204_NO_CONTENT)
    return Response(status_code=status.HTTP_204_NO_CONTENT)

if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
""".trimIndent()
}

