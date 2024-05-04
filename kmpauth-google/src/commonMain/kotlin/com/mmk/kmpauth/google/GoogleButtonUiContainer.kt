package com.mmk.kmpauth.google

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mmk.kmpauth.core.UiContainerScope
import com.mmk.kmpauth.google.AuthorizationCredentialsResult.AccessAlreadyGranted
import com.mmk.kmpauth.google.AuthorizationCredentialsResult.AccessShouldBeGranted
import kotlinx.coroutines.launch

/**
 * GoogleButton Ui Container Composable that handles all sign-in functionality.
 * Make sure you create [GoogleAuthUiProvider] instance using [GoogleAuthProvider.create]
 * before invoking below composable function.
 *
 * Child of this Composable can be any view or Composable function.
 * You need to call [UiContainerScope.onClick] function on your child view's click function.
 *
 * [onGoogleSignInResult] callback will return [GoogleUser] or null if sign-in was unsuccessful.
 *
 * Example Usage:
 * ```
 * //Google Sign-In with Custom Button and authentication without Firebase
 * GoogleButtonUiContainer(onGoogleSignInResult = { googleUser ->
 *     val idToken = googleUser?.idToken // Send this idToken to your backend to verify
 * }) {
 *     Button(onClick = { this.onClick() }) { Text("Google Sign-In(Custom Design)") }
 * }
 *
 * ```
 *
 */
@Composable
public fun GoogleButtonUiContainer(
    modifier: Modifier = Modifier,
    onGoogleSignInResult: (GoogleAuthResult) -> Unit,
    content: @Composable UiContainerScope.() -> Unit,
) {
    val googleAuthProvider = GoogleAuthProvider.get()
    val googleAuthUiProvider = googleAuthProvider.getUiProvider()
    val coroutineScope = rememberCoroutineScope()
    val updatedOnResultFunc by rememberUpdatedState(onGoogleSignInResult)

    var grantAccessAndGoogleAuthResultPair by remember {
        mutableStateOf<Pair<AccessShouldBeGranted, GoogleAuthResult.Success>?>(null)
    }

    grantAccessAndGoogleAuthResultPair?.also { (granted, success) ->
        val launcher = granted.rememberLauncherForIntent { credentials ->
            val result = credentials?.accessToken?.let { accessToken ->
                val user = success.user.copy(accessToken = accessToken)
                GoogleAuthResult.Success(user = user)
            } ?: success
            updatedOnResultFunc(result)
            // Avoid another request in case the function is recomposed
            grantAccessAndGoogleAuthResultPair = null
        }

        LaunchedEffect(launcher) {
            launch {
                launcher.launch()
            }.invokeOnCompletion {
                if (it != null) {
                    // Ensure the result is still sent with the access token set
                    // to the default value.
                    updatedOnResultFunc(success)
                }
            }
        }
    }

    val credentialsRetriever = AuthorizationCredentialsRetriever.get()

    val uiContainerScope = remember {
        object : UiContainerScope {
            override fun onClick() {
                println("GoogleUiButtonContainer is clicked")
                coroutineScope.launch {
                    when (val authResult = googleAuthUiProvider.signIn()) {
                        is GoogleAuthResult.Failure -> onGoogleSignInResult(authResult)
                        is GoogleAuthResult.Success -> {
                            when (val result =
                                credentialsRetriever.getAuthorizationCredentials(authResult.user.id)) {
                                is AccessAlreadyGranted -> {
                                    val accessToken = result.credentials.accessToken
                                    val user = authResult.user.copy(accessToken = accessToken)
                                    updatedOnResultFunc(GoogleAuthResult.Success(user = user))
                                }

                                is AccessShouldBeGranted -> {
                                    grantAccessAndGoogleAuthResultPair = result to authResult
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    Box(modifier = modifier) { uiContainerScope.content() }

}