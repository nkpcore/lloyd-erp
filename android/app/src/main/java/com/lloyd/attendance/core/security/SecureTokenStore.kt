package com.lloyd.attendance.core.security

interface SecureTokenStore {
    fun getAccessToken(): String?
    fun setAccessToken(token: String?)
    fun getRefreshToken(): String?
    fun setRefreshToken(token: String?)
    fun getVerifiedStudentId(): Int
    fun setVerifiedStudentId(id: Int)
    fun clearTokens()
    fun hasValidRefreshToken(): Boolean
}
