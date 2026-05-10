package com.gramaangana.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Booking(
    @DocumentId val id: String = "",
    val requesterName: String = "",
    val requesterPhone: String = "",
    val purpose: String = "",
    val eventType: String = "",
    val date: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val attendeeCount: Int = 0,
    val status: String = "PENDING",
    val notes: String = "",
    val createdAt: Timestamp? = null,
    val keyHolder: String = ""
)

data class MaintenanceItem(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val amountNeeded: Double = 0.0,
    val amountRaised: Double = 0.0,
    val category: String = "OTHER",
    val status: String = "OPEN",
    val imageUrl: String = "",
    val createdAt: Timestamp? = null
)

data class Pledge(
    val pledgerName: String = "",
    val amount: Double = 0.0,
    val message: String = "",
    val timestamp: Timestamp? = null
)

data class CommunityEvent(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val date: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val eventType: String = "",
    val organizer: String = "",
    val contactPhone: String = "",
    val isFeatured: Boolean = false
)
