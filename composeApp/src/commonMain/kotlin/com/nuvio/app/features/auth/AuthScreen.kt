package com.nuvio.app.features.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.auth.AuthRepository
import com.nuvio.app.core.auth.DeviceLinkAuthRepository
import com.nuvio.app.core.auth.DeviceLinkAuthState
import com.nuvio.app.core.build.AppFeaturePolicy
import com.nuvio.app.shell.theme.RobotoFontFamily
import com.nuvio.app.shell.components.WindowBreakpoint
import com.nuvio.app.features.settings.AppBrandWordmark
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.compose_auth_continue_without_account
import nuvio.composeapp.generated.resources.compose_auth_create_account
import nuvio.composeapp.generated.resources.compose_auth_email
import nuvio.composeapp.generated.resources.compose_auth_hide_password
import nuvio.composeapp.generated.resources.compose_auth_or_separator
import nuvio.composeapp.generated.resources.compose_auth_password
import nuvio.composeapp.generated.resources.compose_auth_show_password
import nuvio.composeapp.generated.resources.compose_auth_sign_in
import nuvio.composeapp.generated.resources.compose_auth_sign_in_subtitle
import nuvio.composeapp.generated.resources.compose_auth_sign_up_subtitle
import nuvio.composeapp.generated.resources.compose_auth_store_locally
import nuvio.composeapp.generated.resources.compose_auth_tagline
import nuvio.composeapp.generated.resources.compose_auth_terms_link
import nuvio.composeapp.generated.resources.compose_auth_terms_prefix
import nuvio.composeapp.generated.resources.compose_auth_welcome_back
import org.jetbrains.compose.resources.stringResource
import com.nuvio.app.core.auth.ServerConnectionController

/** Widest the form itself is allowed to get, from the spec's recommended pane widths. */
private val FormMaxWidth = 360.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    modifier: Modifier = Modifier,
) {
    val authError by AuthRepository.error.collectAsStateWithLifecycle()
    val deviceLinkAuthState by DeviceLinkAuthRepository.state.collectAsStateWithLifecycle()
    val serverConnectionState by ServerConnectionController.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var isSignUp by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var showServerSheet by rememberSaveable { mutableStateOf(false) }
    var showOfficialServerDialog by rememberSaveable { mutableStateOf(false) }

    fun submitAuth() {
        if (email.isBlank() || password.length < 6 || isLoading) return
        DeviceLinkAuthRepository.cancel()
        isLoading = true
        focusManager.clearFocus(force = true)
        scope.launch {
            if (isSignUp) AuthRepository.signUpWithEmail(email, password)
            else AuthRepository.signInWithEmail(email, password)
            isLoading = false
        }
    }

    fun selectAuthMode(signUp: Boolean) {
        if (signUp == isSignUp) return
        DeviceLinkAuthRepository.cancel()
        isSignUp = signUp
        AuthRepository.clearError()
    }

    fun startDeviceLink() {
        if (isLoading) return
        focusManager.clearFocus(force = true)
        AuthRepository.clearError()
        DeviceLinkAuthRepository.start()
    }

    LaunchedEffect(serverConnectionState.activeServer.backendUrl) {
        DeviceLinkAuthRepository.cancel()
        if (!serverConnectionState.activeServer.isCustom) {
            showOfficialServerDialog = false
        }
    }

    DisposableEffect(Unit) {
        onDispose(DeviceLinkAuthRepository::cancel)
    }

    // Plain MaterialTheme leaves LocalUsingExpressiveTheme false, which makes every M3
    // component fall back to its pre-expressive defaults. MaterialExpressiveTheme is what
    // actually opts this subtree into Material 3 Expressive shapes, sizing and motion.
    //
    // The colour scheme is inherited untouched from the app theme, which is Material You's own
    // scheme on Android 12+ and Material's baseline scheme below that. Typography is the stock
    // M3 scale in Roboto, so every role lands on Material's sizes, weights and tracking.
    MaterialExpressiveTheme(
        colorScheme = MaterialTheme.colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = Typography(fontFamily = RobotoFontFamily),
    ) {
        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            // Breakpoints are defined on window width, so resolve it outside the Scaffold,
            // before any inset padding is subtracted.
            val breakpoint = WindowBreakpoint.forWidth(maxWidth)

            Scaffold(
                topBar = {
                    // The server switcher belongs in the scaffold's bar region rather than
                    // floating over the pane, which also hands the status-bar inset and the
                    // title ruler to the app bar.
                    if (AppFeaturePolicy.customServerConnectionsEnabled) {
                        TopAppBar(
                            title = {},
                            navigationIcon = {
                                ServerConnectionMenu(
                                    activeServer = serverConnectionState.activeServer,
                                    onUseOfficial = {
                                        ServerConnectionController.resetDiscovery()
                                        showOfficialServerDialog = true
                                    },
                                    onConnectCustom = {
                                        ServerConnectionController.resetDiscovery()
                                        showServerSheet = true
                                    },
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                            ),
                        )
                    }
                },
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { focusManager.clearFocus(force = true) })
                        },
                ) {
                    val form = @Composable {
                        AuthForm(
                            isSignUp = isSignUp,
                            email = email,
                            password = password,
                            passwordVisible = passwordVisible,
                            isLoading = isLoading,
                            authError = authError,
                            deviceLinkAuthState = deviceLinkAuthState,
                            deviceLinkEnabled = serverConnectionState.activeServer.capabilities.tvLogin,
                            onEmailChange = {
                                email = it
                                AuthRepository.clearError()
                            },
                            onPasswordChange = {
                                password = it
                                AuthRepository.clearError()
                            },
                            onPasswordVisibilityToggle = { passwordVisible = !passwordVisible },
                            onSubmit = ::submitAuth,
                            onSelectAuthMode = ::selectAuthMode,
                            onContinueWithoutAccount = {
                                focusManager.clearFocus(force = true)
                                DeviceLinkAuthRepository.cancel()
                                AuthRepository.signInAnonymously()
                            },
                            onStartDeviceLink = ::startDeviceLink,
                            onCancelDeviceLink = DeviceLinkAuthRepository::cancel,
                        )
                    }

                    if (breakpoint.isTwoPane) {
                        AuthTwoPaneLayout(
                            breakpoint = breakpoint,
                            isSignUp = isSignUp,
                            form = form,
                        )
                    } else {
                        AuthSinglePaneLayout(
                            breakpoint = breakpoint,
                            isSignUp = isSignUp,
                            form = form,
                        )
                    }
                }
            }
        }

        if (
            AppFeaturePolicy.customServerConnectionsEnabled &&
            showServerSheet &&
            serverConnectionState.discoveredServer == null
        ) {
            ServerConnectionSheet(
                state = serverConnectionState,
                onDiscover = ServerConnectionController::discover,
                onDismiss = {
                    showServerSheet = false
                    ServerConnectionController.resetDiscovery()
                },
            )
        }

        serverConnectionState.discoveredServer?.let { server ->
            if (AppFeaturePolicy.customServerConnectionsEnabled) {
                ServerTrustDialog(
                    server = server,
                    isSwitching = serverConnectionState.isSwitching,
                    switchFailure = serverConnectionState.switchFailure,
                    onConfirm = ServerConnectionController::connectDiscovered,
                    onDismiss = {
                        showServerSheet = false
                        ServerConnectionController.resetDiscovery()
                    },
                )
            }
        }

        if (AppFeaturePolicy.customServerConnectionsEnabled && showOfficialServerDialog) {
            OfficialServerDialog(
                isSwitching = serverConnectionState.isSwitching,
                switchFailure = serverConnectionState.switchFailure,
                onConfirm = ServerConnectionController::useOfficial,
                onDismiss = {
                    showOfficialServerDialog = false
                    ServerConnectionController.resetDiscovery()
                },
            )
        }
    }
}

/**
 * Compact and medium: one flexible pane holding brand, heading and form. The pane centres its
 * content while it fits and scrolls once it doesn't, so a raised keyboard never clips the form.
 */
@Composable
private fun AuthSinglePaneLayout(
    breakpoint: WindowBreakpoint,
    isSignUp: Boolean,
    form: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val paneHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = paneHeight)
                    .padding(horizontal = breakpoint.margin, vertical = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = FormMaxWidth)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AuthBrandLockup(logoHeight = 40.dp)

                    Spacer(modifier = Modifier.height(40.dp))

                    AuthHeading(isSignUp = isSignUp)

                    Spacer(modifier = Modifier.height(24.dp))

                    form()
                }
            }
        }
    }
}

/**
 * Expanded and up: a fixed-and-flexible two-pane layout. The brand pane flexes with the window
 * and the form sits in the fixed pane at the spec's default width, separated by the 24dp pane
 * spacer and framed by 24dp window margins.
 */
@Composable
private fun AuthTwoPaneLayout(
    breakpoint: WindowBreakpoint,
    isSignUp: Boolean,
    form: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = breakpoint.margin, vertical = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
        ) {
            AppBrandWordmark(
                contentDescription = null,
                modifier = Modifier.height(56.dp),
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = stringResource(Res.string.compose_auth_tagline),
                modifier = Modifier.widthIn(max = 440.dp),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(Res.string.compose_auth_sign_up_subtitle),
                modifier = Modifier.widthIn(max = 400.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.width(WindowBreakpoint.PaneSpacer))

        Column(
            modifier = Modifier
                .width(breakpoint.fixedPaneWidth)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
        ) {
            AuthHeading(isSignUp = isSignUp)

            Spacer(modifier = Modifier.height(24.dp))

            form()
        }
    }
}

@Composable
private fun AuthBrandLockup(
    logoHeight: Dp,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppBrandWordmark(
            contentDescription = null,
            modifier = Modifier.height(logoHeight),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(Res.string.compose_auth_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AuthHeading(isSignUp: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AnimatedContent(
            targetState = isSignUp,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "authHeading",
        ) { signUp ->
            Text(
                text = if (signUp) stringResource(Res.string.compose_auth_create_account)
                else stringResource(Res.string.compose_auth_welcome_back),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        AnimatedContent(
            targetState = isSignUp,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "authSubtitle",
        ) { signUp ->
            Text(
                text = if (signUp) stringResource(Res.string.compose_auth_sign_up_subtitle)
                else stringResource(Res.string.compose_auth_sign_in_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AuthForm(
    isSignUp: Boolean,
    email: String,
    password: String,
    passwordVisible: Boolean,
    isLoading: Boolean,
    authError: String?,
    deviceLinkAuthState: DeviceLinkAuthState,
    deviceLinkEnabled: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onSubmit: () -> Unit,
    onSelectAuthMode: (Boolean) -> Unit,
    onContinueWithoutAccount: () -> Unit,
    onStartDeviceLink: () -> Unit,
    onCancelDeviceLink: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        AuthModeTabs(
            isSignUp = isSignUp,
            onSelectAuthMode = onSelectAuthMode,
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(Res.string.compose_auth_email)) },
            leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
            singleLine = true,
            isError = authError != null,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(Res.string.compose_auth_password)) },
            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = onPasswordVisibilityToggle) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = stringResource(
                            if (passwordVisible) Res.string.compose_auth_hide_password
                            else Res.string.compose_auth_show_password,
                        ),
                    )
                }
            },
            singleLine = true,
            isError = authError != null,
            supportingText = authError?.let { errorText ->
                { Text(text = errorText) }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = { onSubmit() },
            ),
        )

        if (isSignUp) {
            Spacer(modifier = Modifier.height(8.dp))
            AuthTermsAcknowledgement(
                onTermsClick = { uriHandler.openUri("https://nuvio.tv/terms") },
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Default M3 button: small size, ButtonDefaults.MinHeight, ButtonDefaults.ContentPadding
        // and ButtonDefaults.shape. Nothing here overrides Material's sizing.
        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
        ) {
            if (isLoading) {
                LoadingIndicator(
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(
                    text = if (isSignUp) stringResource(Res.string.compose_auth_create_account)
                    else stringResource(Res.string.compose_auth_sign_in),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        AuthOrSeparator()

        Spacer(modifier = Modifier.height(24.dp))

        if (!isSignUp && deviceLinkEnabled) {
            DeviceLinkAuthSection(
                state = deviceLinkAuthState,
                enabled = !isLoading,
                onStart = onStartDeviceLink,
                onCancel = onCancelDeviceLink,
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        FilledTonalButton(
            onClick = onContinueWithoutAccount,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
        ) {
            Text(stringResource(Res.string.compose_auth_continue_without_account))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(Res.string.compose_auth_store_locally),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Sign in and create account are two peer destinations for the form below, which is what primary
 * tabs are for: 48dp container, a 1dp outline-variant divider on the bottom edge, and a 3dp
 * fully-rounded primary active indicator. Per the responsive-layout guidance the row takes a
 * fluid margin and aligns to the centre of the body region rather than bleeding to the window
 * edge.
 *
 * m3.material.io/components/tabs/specs
 */
@Composable
private fun AuthModeTabs(
    isSignUp: Boolean,
    onSelectAuthMode: (Boolean) -> Unit,
) {
    PrimaryTabRow(
        selectedTabIndex = if (isSignUp) 1 else 0,
        modifier = Modifier.fillMaxWidth(),
        containerColor = Color.Transparent,
    ) {
        Tab(
            selected = !isSignUp,
            onClick = { onSelectAuthMode(false) },
            text = { Text(stringResource(Res.string.compose_auth_sign_in)) },
            selectedContentColor = MaterialTheme.colorScheme.primary,
            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Tab(
            selected = isSignUp,
            onClick = { onSelectAuthMode(true) },
            text = { Text(stringResource(Res.string.compose_auth_create_account)) },
            selectedContentColor = MaterialTheme.colorScheme.primary,
            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AuthTermsAcknowledgement(
    onTermsClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.compose_auth_terms_prefix),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(
            onClick = onTermsClick,
            contentPadding = PaddingValues(horizontal = 8.dp),
        ) {
            Text(
                text = stringResource(Res.string.compose_auth_terms_link),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/**
 * A Material divider is one unbroken line — the spec measures it as thickness 1, length ∞, in
 * either the full-width or the 16dp inset variant. Splitting it into two segments around a label
 * is not a variant it defines, so the separation here is carried by the 24dp margins above and
 * below instead: "content may not require a divider line... using only the margin between items
 * is acceptable".
 *
 * m3.material.io/components/divider/specs
 */
@Composable
private fun AuthOrSeparator() {
    Text(
        text = stringResource(Res.string.compose_auth_or_separator).trim(),
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}
