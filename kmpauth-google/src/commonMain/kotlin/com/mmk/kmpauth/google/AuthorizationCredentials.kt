package com.mmk.kmpauth.google

import androidx.compose.runtime.Composable
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.di.KMPKoinComponent
import org.koin.core.component.get

internal fun interface Launcher<T> {
    suspend fun launch(): T
}

internal data class AuthorizationCredentials(
    val accessToken: String?,
    val serverAuthCode: String?,
    val grantedScopes: List<String>,
)

internal sealed interface AuthorizationCredentialsResult {
    interface AccessShouldBeGranted : AuthorizationCredentialsResult {
        @Composable
        fun rememberLauncherForIntent(
            onCredentials: (AuthorizationCredentials?) -> Unit,
        ): Launcher<Unit>
    }

    data class AccessAlreadyGranted(
        val credentials: AuthorizationCredentials,
    ) : AuthorizationCredentialsResult
}

internal fun interface AuthorizationCredentialsRetriever {
    suspend fun getAuthorizationCredentials(selectedAccountId: String?): AuthorizationCredentialsResult

    companion object {
        internal fun get(): AuthorizationCredentialsRetriever {
            return AuthorizationCredentialsRetrieverImpl.get()
        }
    }

    @OptIn(KMPAuthInternalApi::class)
    private object AuthorizationCredentialsRetrieverImpl : KMPKoinComponent() {
        fun get(): AuthorizationCredentialsRetriever {
            try {
                return (this as KMPKoinComponent).get()
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Make sure you invoked GoogleAuthProvider #create method with providing credentials")
            }
        }
    }
}
