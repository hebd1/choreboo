package com.choreboo.app.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import com.choreboo.app.ui.components.SnackbarType
import com.choreboo.app.ui.components.StitchSnackbar
import com.choreboo.app.ui.components.showStitchSnackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.choreboo.app.R
import com.choreboo.app.ui.components.ChorebooTopAppBar
import com.choreboo.app.ui.components.PremiumBadge
import com.choreboo.app.ui.components.ProfileAvatar
import com.choreboo.app.ui.util.findActivity
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onSignOut: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val soundEnabled by viewModel.soundEnabled.collectAsStateWithLifecycle()
    val totalPoints by viewModel.totalPoints.collectAsStateWithLifecycle()
    val profilePhotoUri by viewModel.profilePhotoUri.collectAsStateWithLifecycle()
    val googlePhotoUrl by viewModel.googlePhotoUrl.collectAsStateWithLifecycle()
    val isResetting by viewModel.isResetting.collectAsStateWithLifecycle()
    val currentDisplayName by viewModel.currentDisplayName.collectAsStateWithLifecycle()
    val isUpdatingName by viewModel.isUpdatingName.collectAsStateWithLifecycle()
    val isGoogleUser = viewModel.isGoogleUser
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showSignOutDialog by rememberSaveable { mutableStateOf(false) }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    var resetPassword by rememberSaveable { mutableStateOf("") }
    var resetPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var showPhotoOptionsDialog by rememberSaveable { mutableStateOf(false) }
    var showInviteDialog by rememberSaveable { mutableStateOf(false) }
    var showJoinDialog by rememberSaveable { mutableStateOf(false) }
    var showManageMembersDialog by rememberSaveable { mutableStateOf(false) }
    var showLeaveHouseholdDialog by rememberSaveable { mutableStateOf(false) }
    var showCreateHouseholdDialog by rememberSaveable { mutableStateOf(false) }
    var showEditNameDialog by rememberSaveable { mutableStateOf(false) }
    var editNameText by rememberSaveable { mutableStateOf("") }

    // Credential Manager — used for Google re-authentication before account reset.
    // Not wrapped in remember{} so it is never tied to a stale Activity context after
    // a configuration change; CredentialManager.create() is cheap to call per-composition.
    val credentialManager = CredentialManager.create(context)
    val webClientId = stringResource(R.string.default_web_client_id)

    fun launchGoogleReauth() {
        scope.launch {
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(true)
                    .setServerClientId(webClientId)
                    .build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                val result = credentialManager.getCredential(context, request)
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                viewModel.resetAccount(googleIdToken = googleIdTokenCredential.idToken)
            } catch (e: GetCredentialException) {
                snackbarHostState.showStitchSnackbar(
                    message = context.getString(R.string.settings_msg_google_signin_failed),
                    type = SnackbarType.Error,
                )
            }
        }
    }

    // Household state
    val currentHousehold by viewModel.currentHousehold.collectAsStateWithLifecycle()
    val householdMembers by viewModel.householdMembers.collectAsStateWithLifecycle()
    val isCreatingHousehold by viewModel.isCreatingHousehold.collectAsStateWithLifecycle()
    val isJoiningHousehold by viewModel.isJoiningHousehold.collectAsStateWithLifecycle()
    val isLeavingHousehold by viewModel.isLeavingHousehold.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val isRestoringPurchases by viewModel.isRestoringPurchases.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.SignedOut -> onSignOut()
                is SettingsEvent.AccountReset -> onSignOut()
                is SettingsEvent.ShowError -> {
                    scope.launch {
                        val msg = if (event.formatArg != null)
                            context.getString(event.messageRes, event.formatArg)
                        else
                            context.getString(event.messageRes)
                        snackbarHostState.showStitchSnackbar(message = msg, type = SnackbarType.Error)
                    }
                }
                is SettingsEvent.ShowSuccess -> {
                    showEditNameDialog = false
                    scope.launch {
                        val msg = if (event.formatArg != null)
                            context.getString(event.messageRes, event.formatArg)
                        else
                            context.getString(event.messageRes)
                        snackbarHostState.showStitchSnackbar(message = msg, type = SnackbarType.Success)
                    }
                }
                is SettingsEvent.ShowRawError -> {
                    scope.launch {
                        snackbarHostState.showStitchSnackbar(message = event.message, type = SnackbarType.Error)
                    }
                }
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            viewModel.onProfilePhotoPicked(uri)
        }
    }

    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { if (!isUpdatingName) showEditNameDialog = false },
            title = { Text(stringResource(R.string.settings_change_username_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value = editNameText,
                        onValueChange = { if (it.length <= 30) editNameText = it },
                        label = { Text(stringResource(R.string.settings_username_label)) },
                        singleLine = true,
                        enabled = !isUpdatingName,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() },
                        ),
                    )
                    if (editNameText.length >= 20) {
                        Text(
                            text = "${editNameText.length}/30",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (editNameText.length == 30) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateDisplayName(editNameText)
                    },
                    enabled = !isUpdatingName && editNameText.trim().isNotBlank(),
                ) {
                    if (isUpdatingName) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_saving))
                    } else {
                        Text(stringResource(R.string.settings_save_button))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditNameDialog = false },
                    enabled = !isUpdatingName,
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text(stringResource(R.string.settings_sign_out_dialog_title)) },
            text = {
                Text(
                    stringResource(R.string.settings_sign_out_dialog_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        viewModel.signOut()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.settings_sign_out_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    // Reset Account (Dev) confirmation dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = {
                showResetDialog = false
                resetPassword = ""
                resetPasswordVisible = false
            },
            title = { Text(stringResource(R.string.settings_reset_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.settings_reset_dialog_body_header),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        stringResource(R.string.settings_reset_dialog_data_list),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        stringResource(R.string.settings_reset_reregister_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (isGoogleUser) {
                        Text(
                            stringResource(R.string.settings_reset_google_note),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error,
                        )
                    } else {
                        OutlinedTextField(
                            value = resetPassword,
                            onValueChange = { resetPassword = it },
                            label = { Text(stringResource(R.string.settings_password_label)) },
                            placeholder = { Text(stringResource(R.string.settings_reset_dialog_password_label)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { resetPasswordVisible = !resetPasswordVisible }) {
                                    Icon(
                                        if (resetPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (resetPasswordVisible)
                                            stringResource(R.string.settings_hide_password)
                                        else
                                            stringResource(R.string.settings_show_password),
                                    )
                                }
                            },
                            visualTransformation = if (resetPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done,
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.error,
                                focusedLabelColor = MaterialTheme.colorScheme.error,
                            ),
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        if (isGoogleUser) {
                            launchGoogleReauth()
                        } else {
                            val pwd = resetPassword
                            resetPassword = ""
                            resetPasswordVisible = false
                            viewModel.resetAccount(password = pwd)
                        }
                    },
                    enabled = isGoogleUser || resetPassword.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Icon(
                        Icons.Default.DeleteForever,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.settings_reset_everything))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    resetPassword = ""
                    resetPasswordVisible = false
                }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    if (showPhotoOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoOptionsDialog = false },
            title = { Text(stringResource(R.string.settings_change_profile_photo)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            showPhotoOptionsDialog = false
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly,
                                ),
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_choose_from_gallery), modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
                    }
                    if (!profilePhotoUri.isNullOrBlank()) {
                        TextButton(
                            onClick = {
                                showPhotoOptionsDialog = false
                                viewModel.clearProfilePhoto()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_remove_custom_photo), modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPhotoOptionsDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    // Create Household dialog
    if (showCreateHouseholdDialog) {
        var householdName by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateHouseholdDialog = false },
            title = { Text(stringResource(R.string.settings_create_household_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        stringResource(R.string.settings_create_household_body),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedTextField(
                        value = householdName,
                        onValueChange = { if (it.length <= 50) householdName = it },
                        placeholder = { Text(stringResource(R.string.settings_household_name_placeholder)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = if (householdName.length > 35) {
                            {
                                Text(
                                    text = "${householdName.length}/50",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (householdName.length >= 50)
                                        MaterialTheme.colorScheme.error
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(end = 8.dp),
                                )
                            }
                        } else null,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() },
                        ),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (householdName.isNotBlank()) {
                            viewModel.createHousehold(householdName.trim())
                            showCreateHouseholdDialog = false
                        }
                    },
                    enabled = householdName.isNotBlank() && !isCreatingHousehold,
                ) {
                    if (isCreatingHousehold) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(stringResource(R.string.settings_create_button))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateHouseholdDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    // Join Household dialog
    if (showJoinDialog) {
        var inviteCode by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showJoinDialog = false },
            title = { Text(stringResource(R.string.settings_join_household_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        stringResource(R.string.settings_join_code_body),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedTextField(
                        value = inviteCode,
                        onValueChange = { if (it.length <= 8) inviteCode = it.uppercase() },
                        placeholder = { Text(stringResource(R.string.settings_invite_code_placeholder)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() },
                        ),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inviteCode.length == 8) {
                            viewModel.joinHousehold(inviteCode.trim())
                            showJoinDialog = false
                        }
                    },
                    enabled = inviteCode.length == 8 && !isJoiningHousehold,
                ) {
                    if (isJoiningHousehold) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(stringResource(R.string.settings_join_button))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    // Invite code display dialog (for existing household)
    if (showInviteDialog) {
        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            title = { Text(stringResource(R.string.settings_invite_code_label)) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(R.string.settings_invite_code_share),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                    ) {
                        Text(
                            text = currentHousehold?.inviteCode ?: "------",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 4.sp,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showInviteDialog = false }) {
                    Text(stringResource(R.string.common_done))
                }
            },
        )
    }

    // Manage Members dialog
    if (showManageMembersDialog) {
        AlertDialog(
            onDismissRequest = { showManageMembersDialog = false },
            title = { Text(stringResource(R.string.settings_household_members)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (householdMembers.isEmpty()) {
                        Text(
                            stringResource(R.string.settings_no_members),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        householdMembers.forEach { member ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = member.displayName.take(1).uppercase(),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = member.displayName,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    if (member.email != null) {
                                        Text(
                                            text = member.email,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showManageMembersDialog = false }) {
                    Text(stringResource(R.string.common_done))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showManageMembersDialog = false
                        showLeaveHouseholdDialog = true
                    },
                ) {
                    Text(
                        stringResource(R.string.settings_leave_household),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
        )
    }

    // Leave Household confirmation dialog
    if (showLeaveHouseholdDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveHouseholdDialog = false },
            title = { Text(stringResource(R.string.settings_leave_household_dialog_title)) },
            text = {
                Text(
                    stringResource(R.string.settings_leave_household_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLeaveHouseholdDialog = false
                        viewModel.leaveHousehold()
                    },
                    enabled = !isLeavingHousehold,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    if (isLeavingHousehold) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onError,
                        )
                    } else {
                        Text(stringResource(R.string.settings_leave_button))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveHouseholdDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            ChorebooTopAppBar(
                profilePhotoUri = profilePhotoUri,
                googlePhotoUrl = googlePhotoUrl,
                totalPoints = totalPoints,
                pointsContentDescription = stringResource(R.string.settings_points_cd),
            ) {
                Text(
                    stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                PremiumBadge(isPremium = isPremium)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) { StitchSnackbar(it) } },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Appearance section
            SettingsSectionHeader(
                icon = { Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                label = stringResource(R.string.settings_appearance_section),
            )
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        stringResource(R.string.settings_appearance_vibe),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Custom pill segmented control
                    val options = listOf("system", "light", "dark")
                    val labels = listOf(
                        stringResource(R.string.settings_theme_system),
                        stringResource(R.string.settings_theme_light),
                        stringResource(R.string.settings_theme_dark),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(4.dp),
                    ) {
                        Row {
                            options.forEachIndexed { index, option ->
                                val isSelected = themeMode == option
                                Surface(
                                    onClick = { viewModel.setThemeMode(option) },
                                    shape = CircleShape,
                                    color = if (isSelected) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent,
                                    shadowElevation = if (isSelected) 2.dp else 0.dp,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(
                                        text = labels[index],
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Subscription section
            SettingsSectionHeader(
                icon = { Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                label = stringResource(R.string.settings_subscription_section),
            )
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    if (isPremium) {
                        // Premium active state
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(28.dp),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.settings_premium_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    stringResource(R.string.settings_premium_active),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(
                            onClick = {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    android.net.Uri.parse("https://play.google.com/store/account/subscriptions"),
                                )
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.settings_manage_subscription))
                        }
                    } else {
                        // Free tier — upgrade CTA
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text("✨", style = MaterialTheme.typography.titleLarge)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.settings_upgrade_premium),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    stringResource(R.string.settings_premium_price),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.settings_premium_features_list),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                val activity = context.findActivity()
                                if (activity != null) {
                                    viewModel.launchPremiumPurchase(activity)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.WorkspacePremium, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_start_free_trial))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(
                            onClick = { viewModel.restorePurchases() },
                            enabled = !isRestoringPurchases,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (isRestoringPurchases) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.settings_restoring))
                            } else {
                                Text(stringResource(R.string.settings_restore_purchases))
                            }
                        }
                    }
                }
            }

            // Household section
            SettingsSectionHeader(
                icon = { Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                label = stringResource(R.string.settings_household_section),
            )
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    if (currentHousehold != null) {
                        // Household name header
                        Text(
                            text = currentHousehold?.name ?: stringResource(R.string.settings_my_household_fallback),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Invite to Household
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                .clickable { showInviteDialog = true }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                Icons.Default.PersonAdd,
                                contentDescription = stringResource(R.string.settings_invite_household_cd),
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                stringResource(R.string.settings_invite_code_label),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Manage Housemates
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                .clickable { showManageMembersDialog = true }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                Icons.Default.Groups,
                                contentDescription = stringResource(R.string.settings_manage_housemates_cd),
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                stringResource(R.string.settings_manage_housemates),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                            )
                        }

                    } else {
                        // No household — show create/join options
                        Text(
                            text = stringResource(R.string.settings_no_household),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.settings_no_household_body_full),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Create Household button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                .clickable(enabled = !isCreatingHousehold && !isJoiningHousehold) {
                                    showCreateHouseholdDialog = true
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            if (isCreatingHousehold) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(
                                    Icons.Default.Home,
                                    contentDescription = stringResource(R.string.settings_create_household_cd),
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Text(
                                stringResource(R.string.settings_create_household),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Join Household button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                .clickable(enabled = !isCreatingHousehold && !isJoiningHousehold) {
                                    showJoinDialog = true
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            if (isJoiningHousehold) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(
                                    Icons.Default.PersonAdd,
                                    contentDescription = stringResource(R.string.settings_join_household_icon_cd),
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Text(
                                stringResource(R.string.settings_join_with_code),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            // Sound section
            SettingsSectionHeader(
                icon = { Icon(Icons.Default.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                label = stringResource(R.string.settings_sound_section),
            )
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_sound_effects), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            stringResource(R.string.settings_sound_effects_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = soundEnabled, onCheckedChange = { viewModel.setSoundEnabled(it) })
                }
            }

            // Account section
            SettingsSectionHeader(
                icon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                label = stringResource(R.string.settings_account_section),
            )
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Profile photo section
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .clickable { showPhotoOptionsDialog = true }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ProfileAvatar(
                            profilePhotoUri = profilePhotoUri,
                            googlePhotoUrl = googlePhotoUrl,
                            size = 48.dp,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.settings_profile_photo),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                stringResource(R.string.settings_tap_to_change),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(
                            Icons.Default.PhotoCamera,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Username row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .clickable {
                                editNameText = currentDisplayName
                                showEditNameDialog = true
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.settings_username_label),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                currentDisplayName.ifBlank { stringResource(R.string.settings_tap_to_set_username) },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (currentDisplayName.isBlank()) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                        }
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(R.string.settings_edit_username_cd),
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Email display
                    if (viewModel.currentUserEmail != null) {
                        Column {
                            Text(
                                stringResource(R.string.settings_email_label),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                viewModel.currentUserEmail ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Sign out button
                    Button(
                        onClick = { showSignOutDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_sign_out_button))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Dev reset button
                    Button(
                        onClick = { showResetDialog = true },
                        enabled = !isResetting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (isResetting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_resetting))
                        } else {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_reset_account_button))
                        }
                    }
                }
            }

        }

    }
}

@Composable
private fun SettingsSectionHeader(
    icon: @Composable () -> Unit,
    label: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 4.dp),
    ) {
        icon()
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
