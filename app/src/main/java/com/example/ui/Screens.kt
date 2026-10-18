package com.example.ui

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable data object Splash : Screen
    @Serializable data object Onboarding : Screen
    @Serializable data object Login : Screen
    @Serializable data class PhoneAuth(val isSignUp: Boolean) : Screen
    @Serializable data object OTP : Screen
    @Serializable data object Setup : Screen
    @Serializable data object Home : Screen
    @Serializable data object Chat : Screen
    @Serializable data object AskDoubt : Screen
    @Serializable data object Solution : Screen
    @Serializable data object MyDoubts : Screen
    @Serializable data object Streak : Screen
    @Serializable data object Profile : Screen
    @Serializable data object Models : Screen
    @Serializable data object Notifications : Screen
    @Serializable data class DoubtDetail(val doubtId: Long) : Screen
    @Serializable data object StudyGroups : Screen
    @Serializable data object Visuals : Screen
    @Serializable data object Library : Screen
}
