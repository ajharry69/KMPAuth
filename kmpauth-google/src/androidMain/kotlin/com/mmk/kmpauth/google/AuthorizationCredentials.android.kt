package com.mmk.kmpauth.google


import android.accounts.Account
import android.app.Activity
import android.content.Context
import android.os.CancellationSignal
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


internal class AuthorizationCredentialsRetrieverImpl(private val context: Context) :
    AuthorizationCredentialsRetriever {

    override suspend fun getAuthorizationCredentials(selectedAccountId: String?): AuthorizationCredentialsResult {
        val result = getGoogleAuthorizationRequestResult(selectedAccountId)
        return if (!result.hasResolution()) {
            // Access already granted, continue with user action
            AuthorizationCredentialsResult.AccessAlreadyGranted(result.getAuthorizationCredentials())
        } else {
            // Access needs to be granted by the user
            object : AuthorizationCredentialsResult.AccessShouldBeGranted {
                @Composable
                override fun rememberLauncherForIntent(onCredentials: (AuthorizationCredentials?) -> Unit): Launcher<Unit> {
                    val launcher = rememberLauncherForActivityResult(StartIntentSenderForResult()) {
                        val credentials = if (it.resultCode != Activity.RESULT_OK) null else {
                            Identity.getAuthorizationClient(context)
                                .getAuthorizationResultFromIntent(it.data)
                                .getAuthorizationCredentials()
                        }
                        onCredentials(credentials)
                    }

                    return remember(launcher) {
                        Launcher {
                            launcher.launch(
                                IntentSenderRequest.Builder(result.pendingIntent!!)
                                    .build(),
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun getGoogleAuthorizationRequestResult(accountName: String?): AuthorizationResult {
        val requestedScopes = listOf(Scopes.EMAIL, Scopes.PROFILE, Scopes.OPEN_ID)
            .map(::Scope)

        val authorizationRequest = AuthorizationRequest.builder()
            .setRequestedScopes(requestedScopes)
            .setAccount(Account(accountName, "com.google"))
            .build()

        return withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                val cancellationSignal = CancellationSignal()

                continuation.invokeOnCancellation { cancellationSignal.cancel() }

                Identity.getAuthorizationClient(context)
                    .authorize(authorizationRequest)
                    .addOnSuccessListener(continuation::resume)
                    .addOnFailureListener(continuation::resumeWithException)
                    .addOnCanceledListener(continuation::cancel)
            }
        }
    }

    private fun AuthorizationResult.getAuthorizationCredentials(): AuthorizationCredentials {
        return AuthorizationCredentials(
            accessToken = accessToken,
            serverAuthCode = serverAuthCode,
            grantedScopes = grantedScopes,
        )
    }
}
