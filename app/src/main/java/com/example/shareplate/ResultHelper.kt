package com.example.shareplate

import androidx.credentials.GetCredentialResponse
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

fun Result<*>.isSuccess(): Boolean {
    return this.isSuccess
}

fun Result<*>.getCredentialResponseOrNull(): GetCredentialResponse? {
    return this.getOrNull() as? GetCredentialResponse
}

fun Result<*>.exceptionOrNull(): Throwable? {
    return this.exceptionOrNull()
}