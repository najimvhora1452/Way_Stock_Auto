package com.example.data

data class AdminDeviceEntity(
    val email: String = "",
    val displayName: String = "",
    val isAutoLaunchEnabled: Boolean = false,
    val registeredAt: Long = 0L,
    val lastActiveAt: Long = 0L,
    val isSuperAdmin: Boolean = false
)

data class MasterSecurityConfig(
    val masterAdminEmail: String = "najimvhora1452@gmail.com",
    val masterPin: String = "1234",
    val lastModifiedBy: String = "najimvhora1452@gmail.com",
    val lastModifiedAt: Long = 0L
)
