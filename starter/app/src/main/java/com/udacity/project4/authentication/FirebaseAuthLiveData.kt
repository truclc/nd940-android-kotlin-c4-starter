package com.udacity.project4.authentication

import android.util.Log
import androidx.lifecycle.LiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser


class FirebaseUserLiveData : LiveData<FirebaseUser?>() {

    private val TAG = "FirebaseUserLiveData"
    private val firebaseAuth = FirebaseAuth.getInstance()

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        value = firebaseAuth.currentUser
    }

    /**
     * onActive
     * Once this object has an active observer, start observing the FirebaseAuth state to see if a user is currently logged in.
     */
    override fun onActive() {
        Log.d(TAG, "onActive: ")
        firebaseAuth.addAuthStateListener(authStateListener)
    }

    /**
     * onInactive
     * When this object has no active observers, stop observing the FirebaseAuth state to avoid memory leaks.
     */
    override fun onInactive() {
        Log.d(TAG, "onInactive: ")
        firebaseAuth.removeAuthStateListener(authStateListener)
    }
}
