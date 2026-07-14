package com.akustom15.mint.library.security

sealed class LicenseState {
    object Checking : LicenseState()
    object Valid : LicenseState()
    // Añadir un flag para indicar si es invalidación por piratería
    class Invalid(val reason: String, val isPiracyRelated: Boolean = false) : LicenseState()
    class Error(val message: String) : LicenseState()
} 
