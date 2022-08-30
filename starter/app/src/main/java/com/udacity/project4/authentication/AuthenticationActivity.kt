package com.udacity.project4.authentication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityAuthenticationBinding
import com.udacity.project4.locationreminders.RemindersActivity


/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
class AuthenticationActivity : AppCompatActivity() {
    private val TAG = "AuthenticationActivity"
    private val viewModel by viewModels<AuthViewModel>()

    /**
     * onCreate
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityAuthenticationBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_authentication)
        binding.lifecycleOwner = this
        //Implement the create account and sign in using FirebaseUI, use sign in using email and sign in using Google
        viewModel.authenticationState.observe(this, Observer { authState ->
            //If the user was authenticated, send him to RemindersActivity
            if (authState == null) {
                Log.d(TAG, "onCreate: authState is null")
                return@Observer
            }
            when (authState) {
                AuthViewModel.AuthenticationState.AUTHENTICATED -> {
                    Log.d(TAG, "onCreate: user was authenticated")
                    val intent = Intent(this, RemindersActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                AuthViewModel.AuthenticationState.UNAUTHENTICATED -> Log.d(
                    TAG,
                    "onCreate: user was unauthenticated"
                )
                AuthViewModel.AuthenticationState.INVALID_AUTHENTICATION -> Log.d(
                    TAG,
                    "onCreate: user was invalid authentication"
                )
            }

        })

        binding.loginBtn.setOnClickListener {
            val providers = arrayListOf(
                AuthUI.IdpConfig.EmailBuilder().build(), AuthUI.IdpConfig.GoogleBuilder().build()
            )
            startActivityForResult(
                AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(
                    providers
                ).build(), SIGN_IN_RESULT_CODE
            )
        }
    }

    /**
     * on Activity Result
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SIGN_IN_RESULT_CODE) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                Log.d(
                    TAG,
                    "Logged in successfully with user ${FirebaseAuth.getInstance().currentUser?.displayName}"
                )
            } else {
                Log.d(TAG, "Login unsuccessful with error ${response?.error?.errorCode}")
            }
        }
    }

    companion object {
        const val SIGN_IN_RESULT_CODE = 1001
    }
}
