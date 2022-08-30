package com.udacity.project4.authentication

import androidx.lifecycle.map
import androidx.lifecycle.ViewModel

/**
 * Authentication ViewModel class
 */
class AuthViewModel : ViewModel() {
    enum class AuthenticationState {
        AUTHENTICATED, UNAUTHENTICATED, INVALID_AUTHENTICATION
    }

    val authenticationState = FirebaseUserLiveData().map { user ->
        if (user != null) {
            AuthenticationState.AUTHENTICATED
        } else {
            AuthenticationState.UNAUTHENTICATED
        }
    }
}
