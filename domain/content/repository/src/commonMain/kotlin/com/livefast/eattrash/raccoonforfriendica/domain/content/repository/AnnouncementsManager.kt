package com.livefast.eattrash.raccoonforfriendica.domain.content.repository

import kotlinx.coroutines.flow.StateFlow

interface AnnouncementsManager {
    val unreadCount: StateFlow<Int>

    suspend fun clearUnreadCount()

    suspend fun refreshUnreadCount()

    suspend fun decrementUnreadCount()
}
