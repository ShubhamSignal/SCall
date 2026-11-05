package com.example.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.GoogleAuthProvider
import com.example.R
import com.example.data.*
import com.example.repository.SClabRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed interface AuthState {
    object Idle : AuthState
    object Loading : AuthState
    object Authenticated : AuthState
    data class Error(val message: String) : AuthState
}

class SClabViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SClabRepository(application)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _membership = MutableStateFlow<MembershipInfo?>(null)
    val membership: StateFlow<MembershipInfo?> = _membership.asStateFlow()

    private val _wallet = MutableStateFlow<Wallet>(Wallet())
    val wallet: StateFlow<Wallet> = _wallet.asStateFlow()

    private val _publicProfiles = MutableStateFlow<List<PublicProfile>>(emptyList())
    val publicProfiles: StateFlow<List<PublicProfile>> = _publicProfiles.asStateFlow()

    private val _walletTransactions = MutableStateFlow<List<WalletTransaction>>(emptyList())
    val walletTransactions: StateFlow<List<WalletTransaction>> = _walletTransactions.asStateFlow()

    init {
        checkCurrentUser()
    }

    fun checkCurrentUser() {
        val user = Firebase.auth.currentUser
        if (user != null) {
            _authState.value = AuthState.Authenticated
            loadUserData(user.uid)
        } else {
            _authState.value = AuthState.Idle
        }
    }

    fun loadUserData(uid: String) {
        viewModelScope.launch {
            try {
                val profile = repository.getUserProfile(uid)
                _userProfile.value = profile

                if (profile != null) {
                    repository.checkAndAssignFounderMembership(uid)
                    _userProfile.value = repository.getUserProfile(uid)
                }

                val mem = repository.getMembership(uid)
                _membership.value = mem

                val w = repository.getWallet(uid)
                _wallet.value = w

                viewModelScope.launch {
                    repository.observePublicProfiles().collect { list ->
                        _publicProfiles.value = list
                    }
                }

                viewModelScope.launch {
                    repository.observeWalletTransactions(uid).collect { txs ->
                        _walletTransactions.value = txs
                    }
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Failed to load user data")
            }
        }
    }

    fun registerUser(fullName: String, gender: String, dob: String, email: String, pass: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = Firebase.auth.createUserWithEmailAndPassword(email, pass).await()
                val uid = result.user?.uid ?: throw IllegalStateException("User creation failed")

                val profile = UserProfile(
                    userId = uid,
                    fullName = fullName,
                    gender = gender,
                    dob = dob,
                    email = email,
                    profilePicture = "https://picsum.photos/seed/$uid/200",
                    availableForCall = true,
                    earningRate = 4,
                    isAdmin = false,
                    isFounder = false
                )
                repository.saveUserProfile(profile)
                repository.checkAndAssignFounderMembership(uid)

                _authState.value = AuthState.Authenticated
                loadUserData(uid)
                onComplete(true)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Registration failed")
                onComplete(false)
            }
        }
    }

    fun loginUser(email: String, pass: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                Firebase.auth.signInWithEmailAndPassword(email, pass).await()
                val uid = Firebase.auth.currentUser?.uid ?: throw IllegalStateException("Login failed")
                _authState.value = AuthState.Authenticated
                loadUserData(uid)
                onComplete(true)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Login failed")
                onComplete(false)
            }
        }
    }

    fun loginWithGoogle(context: Context, activity: Activity, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val clientId = context.getString(R.string.default_web_client_id)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setServerClientId(clientId)
                    .setAutoSelectEnabled(false)
                    .setFilterByAuthorizedAccounts(false)
                    .build()

                val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
                val credentialManager = CredentialManager.create(context)
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential
                if (credential is CustomCredential) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    Firebase.auth.signInWithCredential(authCredential).await()
                    val uid = Firebase.auth.currentUser?.uid ?: throw IllegalStateException("Google sign in failed")

                    val existing = repository.getUserProfile(uid)
                    if (existing == null) {
                        val firebaseUser = Firebase.auth.currentUser
                        val profile = UserProfile(
                            userId = uid,
                            fullName = firebaseUser?.displayName ?: "Google User",
                            gender = "Male",
                            dob = "2000-01-01",
                            email = firebaseUser?.email ?: "",
                            profilePicture = firebaseUser?.photoUrl?.toString() ?: "https://picsum.photos/seed/$uid/200",
                            availableForCall = true,
                            earningRate = 4
                        )
                        repository.saveUserProfile(profile)
                    }
                    repository.checkAndAssignFounderMembership(uid)
                    _authState.value = AuthState.Authenticated
                    loadUserData(uid)
                    onComplete(true)
                } else {
                    _authState.value = AuthState.Idle
                    onComplete(false)
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Google Sign-In failed")
                onComplete(false)
            }
        }
    }

    fun logout() {
        Firebase.auth.signOut()
        _authState.value = AuthState.Idle
        _userProfile.value = null
        _membership.value = null
    }

    fun activatePaidMembership(tier: String, paymentRef: String, rate: Int, onComplete: () -> Unit) {
        val uid = Firebase.auth.currentUser?.uid ?: return
        viewModelScope.launch {
            repository.activatePaidMembership(uid, tier, paymentRef, rate)
            loadUserData(uid)
            onComplete()
        }
    }
}
