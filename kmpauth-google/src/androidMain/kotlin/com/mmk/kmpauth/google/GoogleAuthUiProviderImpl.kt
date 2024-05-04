package com.mmk.kmpauth.google

import android.content.Context
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

internal class GoogleAuthUiProviderImpl(
    private val activityContext: Context,
    private val credentialManager: CredentialManager,
    private val credentials: GoogleAuthCredentials,
) :
    GoogleAuthUiProvider {
    override suspend fun signIn(): GoogleAuthResult {
        return try {
            val credential = credentialManager.getCredential(
                context = activityContext,
                request = getCredentialRequest()
            ).credential
            getGoogleAuthResultFromCredential(credential)
        } catch (e: GetCredentialException) {
            println("GoogleAuthUiProvider error: ${e.message}")
            GoogleAuthResult.Failure(
                GoogleAuthException.GetCredential(
                    type = e.type,
                    message = e.errorMessage?.toString(),
                    cause = e,
                )
            )
        }
    }

    private fun getGoogleAuthResultFromCredential(credential: Credential): GoogleAuthResult {
        return when {
            credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                try {
                    val googleIdTokenCredential =
                        GoogleIdTokenCredential.createFrom(credential.data)
                    val user = GoogleUser(
                        id = googleIdTokenCredential.id,
                        idToken = googleIdTokenCredential.idToken,
                        accessToken = null,
                        displayName = googleIdTokenCredential.displayName ?: "",
                        profilePicUrl = googleIdTokenCredential.profilePictureUri?.toString()
                    )
                    GoogleAuthResult.Success(user)
                } catch (e: GoogleIdTokenParsingException) {
                    println("GoogleAuthUiProvider Received an invalid google id token response: ${e.message}")
                    GoogleAuthResult.Failure(GoogleAuthException.InvalidIdToken)
                }
            }

            else -> GoogleAuthResult.Failure(GoogleAuthException.InvalidCredential)
        }
    }

    private fun getCredentialRequest(): GetCredentialRequest {
        return GetCredentialRequest.Builder()
            .addCredentialOption(getGoogleIdOption(serverClientId = credentials.serverId))
            .build()
    }

    private fun getGoogleIdOption(serverClientId: String): GetGoogleIdOption {
        return GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(true)
            .setServerClientId(serverClientId)
            .build()
    }
}