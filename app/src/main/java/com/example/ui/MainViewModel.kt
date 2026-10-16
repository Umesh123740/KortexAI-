package com.example.ui

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose

import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.FirebaseException
import java.util.concurrent.TimeUnit
import android.app.Activity

import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class MainViewModel(private val repository: AppRepository) : ViewModel() {

    fun signInWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        viewModelScope.launch {
            _isAuthenticating.value = true
            try {
                auth.signInWithCredential(credential).await()
            } catch (e: Exception) {
                _authError.value = e.localizedMessage
            } finally {
                _isAuthenticating.value = false
            }
        }
    }

    private val auth = Firebase.auth
    
    private val _verificationId = MutableStateFlow<String?>(null)
    private val _resendToken = MutableStateFlow<PhoneAuthProvider.ForceResendingToken?>(null)
    
    private val _isOtpSent = MutableStateFlow(false)
    val isOtpSent = _isOtpSent.asStateFlow()
    
    private val _authError = MutableStateFlow<String?>(null)
    val authError = _authError.asStateFlow()

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating = _isAuthenticating.asStateFlow()

    private val _cooldown = MutableStateFlow(0)
    val cooldown = _cooldown.asStateFlow()

    private var cooldownJob: Job? = null
    val authState: StateFlow<FirebaseUser?> = callbackFlow<FirebaseUser?> {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), auth.currentUser)

    val userProfile: StateFlow<UserProfile?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allDoubts: StateFlow<List<Doubt>> = repository.allDoubts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val today = dateFormatter.format(Date())

    val todayGoals: StateFlow<List<Goal>> = repository.getGoalsForDate(today)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allStreaks: StateFlow<List<Streak>> = repository.allStreaks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _solutionState = MutableStateFlow<String?>(null)
    val solutionState: StateFlow<String?> = _solutionState.asStateFlow()

    private val _isSolving = MutableStateFlow(false)
    val isSolving: StateFlow<Boolean> = _isSolving.asStateFlow()

    private var solveJob: Job? = null

    init {
        viewModelScope.launch {
            repository.userProfile.first()?.let { 
                // Profile exists
            } ?: run {
                // Initialize with some goals for new users
                repository.insertGoal(Goal(title = "Solve 1 Math doubt", date = today))
                repository.insertGoal(Goal(title = "Study Science for 15 min", date = today))
            }
            
            // Initial mock notifications
            _notifications.value = listOf(
                Notification(1, "Don't break your streak!", "You haven't solved a doubt today yet.", "Streak"),
                Notification(2, "Your doubt has been solved", "The answer to Quadratic Equations is ready.", "DoubtDetail"),
                Notification(3, "Study buddy request", "Priya invited you to a study group.", "Models")
            )
        }
    }

    fun solveDoubt(question: String, subject: String, imageBitmap: Bitmap? = null) {
        solveJob?.cancel()
        solveJob = viewModelScope.launch {
            _isSolving.value = true
            val profile = userProfile.value
            val result = repository.solveDoubt(
                question = question,
                subject = subject,
                className = profile?.className ?: "Class 10",
                board = profile?.board ?: "CBSE",
                language = profile?.language ?: "Hinglish",
                isPro = profile?.isPro ?: false,
                imageBitmap = imageBitmap
            )
            _solutionState.value = result
            
            // Save to database
            repository.insertDoubt(
                Doubt(
                    subject = subject,
                    question = question,
                    solution = result,
                    status = "Pending", // Initially pending, user marks as solved
                    imagePath = if (imageBitmap != null) "attached_image" else null
                )
            )
            
            _isSolving.value = false
        }
    }

    fun markAsSolved(doubt: Doubt) {
        viewModelScope.launch {
            repository.insertDoubt(doubt.copy(status = "Solved"))
            repository.insertStreak(Streak(date = today))
            
            // Try to check off a goal for today
            todayGoals.value.firstOrNull { !it.isCompleted }?.let { goal ->
                repository.updateGoal(goal.copy(isCompleted = true))
            }
        }
    }

    fun stopSolving() {
        solveJob?.cancel()
        _isSolving.value = false
        _solutionState.value = "Problem solving cancelled."
    }

    fun clearSolution() {
        _solutionState.value = null
    }

    private val _isNewUser = MutableStateFlow<Boolean?>(null)
    val isNewUser = _isNewUser.asStateFlow()

    fun checkUserExists() {
        viewModelScope.launch {
            val user = auth.currentUser
            if (user != null) {
                val profile = repository.getFirestoreProfile(user.uid)
                if (profile != null) {
                    // Returning user, sync to local Room
                    repository.insertUserProfile(profile)
                    _isNewUser.value = false
                } else {
                    // New user
                    _isNewUser.value = true
                }
            }
        }
    }

    fun saveProfile(name: String, language: String, board: String, className: String, mobileNumber: String, isOnboarded: Boolean = true) {
        viewModelScope.launch {
            val user = auth.currentUser ?: return@launch
            val profile = UserProfile(
                userId = user.uid,
                name = name,
                language = language,
                board = board,
                className = className,
                mobileNumber = mobileNumber,
                isOnboarded = isOnboarded
            )
            repository.insertUserProfile(profile)
            repository.syncProfileToFirestore(profile)
            _isNewUser.value = false
        }
    }

    fun signOut() {
        auth.signOut()
        _isOtpSent.value = false
        _verificationId.value = null
        _authError.value = null
    }

    fun sendOtp(phoneNumber: String, activity: Activity) {
        if (phoneNumber.length < 10) {
            _authError.value = "Please enter a valid 10-digit number"
            return
        }
        
        _isAuthenticating.value = true
        _authError.value = null
        
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                viewModelScope.launch {
                    try {
                        auth.signInWithCredential(credential).await()
                        _isAuthenticating.value = false
                    } catch (e: Exception) {
                        _authError.value = e.localizedMessage
                        _isAuthenticating.value = false
                    }
                }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                _authError.value = e.localizedMessage
                _isAuthenticating.value = false
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                _verificationId.value = verificationId
                _resendToken.value = token
                _isOtpSent.value = true
                _isAuthenticating.value = false
                startCooldown()
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber("+91$phoneNumber") // Assuming India as per context
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyOtp(code: String) {
        val vId = _verificationId.value ?: return
        _isAuthenticating.value = true
        val credential = PhoneAuthProvider.getCredential(vId, code)
        viewModelScope.launch {
            try {
                auth.signInWithCredential(credential).await()
                _isAuthenticating.value = false
            } catch (e: Exception) {
                _authError.value = "Invalid OTP. Please try again."
                _isAuthenticating.value = false
            }
        }
    }

    fun resendOtp(phoneNumber: String, activity: Activity) {
        val token = _resendToken.value ?: return
        _isAuthenticating.value = true
        
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                viewModelScope.launch {
                    auth.signInWithCredential(credential).await()
                    _isAuthenticating.value = false
                }
            }
            override fun onVerificationFailed(e: FirebaseException) {
                _authError.value = e.localizedMessage
                _isAuthenticating.value = false
            }
            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                _verificationId.value = verificationId
                _resendToken.value = token
                _isAuthenticating.value = false
                startCooldown()
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber("+91$phoneNumber")
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .setForceResendingToken(token)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun startCooldown() {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            _cooldown.value = 60
            while (_cooldown.value > 0) {
                delay(1000)
                _cooldown.value -= 1
            }
        }
    }

    fun updateProfilePicture(uri: String) {
        viewModelScope.launch {
            userProfile.value?.let {
                repository.updateUserProfile(it.copy(profilePicture = uri))
            }
        }
    }

    fun setPro(isPro: Boolean) {
        viewModelScope.launch {
            userProfile.value?.let {
                repository.updateUserProfile(it.copy(isPro = isPro))
            }
        }
    }

    fun toggleGoal(goal: Goal) {
        viewModelScope.launch {
            repository.updateGoal(goal.copy(isCompleted = !goal.isCompleted))
        }
    }

    private val _generatedInfographic = MutableStateFlow<Bitmap?>(null)
    val generatedInfographic: StateFlow<Bitmap?> = _generatedInfographic.asStateFlow()

    private val _isGeneratingVisual = MutableStateFlow(false)
    val isGeneratingVisual: StateFlow<Boolean> = _isGeneratingVisual.asStateFlow()

    fun generateInfographic(topic: String, subject: String) {
        viewModelScope.launch {
            _isGeneratingVisual.value = true
            _generatedInfographic.value = null
            val result = repository.generateInfographic(topic, subject)
            _generatedInfographic.value = result
            _isGeneratingVisual.value = false
            
            if (result != null) {
                // Add a notification that your visual is ready
                val newNotif = Notification(
                    id = (_notifications.value.maxOfOrNull { it.id } ?: 0) + 1,
                    title = "Visual Ready!",
                    body = "Your infographic on $topic is generated.",
                    route = "Visuals"
                )
                _notifications.value = listOf(newNotif) + _notifications.value
            }
        }
    }

    fun clearGeneratedVisual() {
        _generatedInfographic.value = null
    }
}
