package com.example.choreboo_habittrackerfriend.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.example.choreboo_habittrackerfriend.ui.components.SnackbarType
import com.example.choreboo_habittrackerfriend.ui.components.StitchSnackbar
import com.example.choreboo_habittrackerfriend.ui.components.showStitchSnackbar
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.example.choreboo_habittrackerfriend.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    onAuthSuccess: (onboardingComplete: Boolean) -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    // Credential Manager — replaces legacy Google Sign-In launcher
    val credentialManager = remember { CredentialManager.create(context) }
    val webClientId = stringResource(R.string.default_web_client_id)

    fun launchGoogleSignIn() {
        scope.launch {
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                val result = credentialManager.getCredential(context, request)
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                val idToken = googleIdTokenCredential.idToken
                val photoUrl = googleIdTokenCredential.profilePictureUri?.toString()
                viewModel.signInWithGoogle(idToken, photoUrl)
            } catch (e: GetCredentialException) {
                snackbarHostState.showStitchSnackbar(
                    message = context.getString(R.string.auth_error_google_failed),
                    type = SnackbarType.Error,
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AuthEvent.AuthSuccess -> onAuthSuccess(event.onboardingComplete)
                is AuthEvent.ShowError -> snackbarHostState.showStitchSnackbar(message = context.getString(event.messageRes), type = SnackbarType.Error)
                is AuthEvent.ShowMessage -> {
                    val msg = if (event.formatArg != null) context.getString(event.messageRes, event.formatArg)
                              else context.getString(event.messageRes)
                    snackbarHostState.showStitchSnackbar(message = msg, type = SnackbarType.Info)
                }
            }
        }
    }

    // Forgot password dialog
    if (formState.showForgotPassword) {
        AlertDialog(
            onDismissRequest = { viewModel.toggleForgotPassword(false) },
            title = { Text(stringResource(R.string.auth_reset_password_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.auth_reset_email_body,
                        formState.email.ifBlank { stringResource(R.string.auth_reset_email_placeholder) },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.sendPasswordReset() }) {
                    Text(stringResource(R.string.auth_send_reset_email))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.toggleForgotPassword(false) }) {
                    Text(stringResource(R.string.auth_cancel))
                }
            },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) { data -> StitchSnackbar(data) } },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Spacer(modifier = Modifier.height(40.dp))

                // Egg hero
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🥚", fontSize = 44.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    stringResource(R.string.app_name).substringBefore(" -"),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Sign In / Sign Up animated subtitle
                AnimatedContent(
                    targetState = formState.isSignUp,
                    transitionSpec = {
                        (fadeIn() + slideInVertically { it / 2 }) togetherWith
                            (fadeOut() + slideOutVertically { -it / 2 })
                    },
                    label = "auth_subtitle",
                ) { isSignUp ->
                    Text(
                        if (isSignUp) stringResource(R.string.auth_create_account) else stringResource(R.string.auth_welcome_back),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Form card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .padding(24.dp),
                ) {
                    Column {
                        // Email field
                        OutlinedTextField(
                            value = formState.email,
                            onValueChange = viewModel::onEmailChange,
                            label = { Text(stringResource(R.string.auth_email_label)) },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null)
                            },
                            isError = formState.emailError != null,
                            supportingText = formState.emailError?.let { err -> { Text(stringResource(err.messageRes)) } },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next,
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Password field
                        var passwordVisible by rememberSaveable { mutableStateOf(false) }
                        OutlinedTextField(
                            value = formState.password,
                            onValueChange = viewModel::onPasswordChange,
                            label = { Text(stringResource(R.string.auth_password_label)) },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null)
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        if (passwordVisible) Icons.Default.VisibilityOff
                                        else Icons.Default.Visibility,
                                        contentDescription = if (passwordVisible) stringResource(R.string.auth_hide_password) else stringResource(R.string.auth_show_password),
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None
                            else PasswordVisualTransformation(),
                            isError = formState.passwordError != null,
                            supportingText = formState.passwordError?.let { err -> { Text(stringResource(err.messageRes)) } },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    viewModel.submit()
                                }
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                        )

                        // Forgot password (sign-in mode only)
                        AnimatedVisibility(visible = !formState.isSignUp) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                TextButton(
                                    onClick = { viewModel.toggleForgotPassword(true) },
                                    modifier = Modifier.align(Alignment.CenterEnd),
                                ) {
                                    Text(
                                        stringResource(R.string.auth_forgot_password),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Submit button
                        Button(
                            onClick = { viewModel.submit() },
                            enabled = !formState.isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            if (formState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            } else {
                                AnimatedContent(
                                    targetState = formState.isSignUp,
                                    label = "submit_label",
                                ) { isSignUp ->
                                    Text(
                                        if (isSignUp) stringResource(R.string.auth_create_account_button) else stringResource(R.string.auth_sign_in_button),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Divider
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        "  ${stringResource(R.string.auth_or_divider)}  ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Google Sign-In button
                OutlinedButton(
                    onClick = { launchGoogleSignIn() },
                    enabled = !formState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    Text("G", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        stringResource(R.string.auth_continue_with_google),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Privacy policy notice
                val uriHandler = LocalUriHandler.current
                TextButton(
                    onClick = {
                        uriHandler.openUri("https://www.notion.so/elihebdon/Privacy-Policy-for-Choreboo-Habit-Tracker-Friend-3306b7634ff3805bad3ac4306bd087a8")
                    },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                ) {
                    Text(
                        stringResource(R.string.auth_privacy_policy),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Toggle sign-in / sign-up
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AnimatedContent(
                        targetState = formState.isSignUp,
                        label = "toggle_label",
                    ) { isSignUp ->
                        Text(
                            if (isSignUp) stringResource(R.string.auth_already_have_account) else stringResource(R.string.auth_dont_have_account),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(
                        onClick = viewModel::toggleMode,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    ) {
                        AnimatedContent(
                            targetState = formState.isSignUp,
                            label = "toggle_action",
                        ) { isSignUp ->
                            Text(
                                if (isSignUp) stringResource(R.string.auth_sign_in_button) else stringResource(R.string.auth_sign_up_button),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }

            // Syncing overlay — shown after auth succeeds while cloud data is pulled
            // Placed AFTER main content so it renders on top and blocks interaction
            if (formState.isSyncing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.auth_syncing_data),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
