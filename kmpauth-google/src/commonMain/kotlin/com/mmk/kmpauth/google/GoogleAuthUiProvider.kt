package com.mmk.kmpauth.google

/**
 * Provider class for Google Authentication UI part. a.k.a [signIn]
 */
public interface GoogleAuthUiProvider {

    /**
     * Opens Sign In with Google UI, and returns [GoogleUser]
     * if sign-in was successful, otherwise, null
     * @return returns GoogleUser or null(if sign-in was not successful)
     */
    public suspend fun signIn(): GoogleAuthResult
}

public sealed interface GoogleAuthResult {
    public data class Success(val user: GoogleUser) : GoogleAuthResult
    public data class Failure(val error: GoogleAuthException) : GoogleAuthResult
}

public sealed class GoogleAuthException(
    override val message: String?,
    override val cause: Throwable? = null,
) : Throwable(message = message, cause = cause) {
    public data object MisconfiguredProvider : GoogleAuthException("Misconfigured provider")
    public data object InvalidCredential : GoogleAuthException("Invalid credential")
    public data object InvalidIdToken : GoogleAuthException("Invalid ID token")
    public data class GetCredential(
        val type: String,
        override val message: String?,
        override val cause: Throwable?,
    ) : GoogleAuthException(message = message, cause = cause)
}