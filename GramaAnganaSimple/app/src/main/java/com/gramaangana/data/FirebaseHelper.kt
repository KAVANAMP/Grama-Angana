package com.gramaangana.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.gramaangana.model.Booking
import com.gramaangana.model.CommunityEvent
import com.gramaangana.model.MaintenanceItem
import com.gramaangana.model.Pledge
import kotlinx.coroutines.tasks.await

object FirebaseHelper {

    private val db: FirebaseFirestore get() = Firebase.firestore

    // ── Bookings ────────────────────────────────────────────────

    suspend fun getBookings(): List<Booking> {
        return try {
            val snapshot = db.collection("bookings").get().await()
            snapshot.documents.mapNotNull { doc ->
                try {
                    Booking(
                        id = doc.id,
                        requesterName = doc.getString("requesterName") ?: "",
                        requesterPhone = doc.getString("requesterPhone") ?: "",
                        purpose = doc.getString("purpose") ?: "",
                        eventType = doc.getString("eventType") ?: "",
                        date = doc.getString("date") ?: "",
                        startTime = doc.getString("startTime") ?: "",
                        endTime = doc.getString("endTime") ?: "",
                        attendeeCount = (doc.getLong("attendeeCount") ?: 0L).toInt(),
                        status = doc.getString("status") ?: "PENDING",
                        notes = doc.getString("notes") ?: "",
                        keyHolder = doc.getString("keyHolder") ?: ""
                    )
                } catch (e: Exception) { null }
            }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getBookingsByDate(date: String): List<Booking> {
        return try {
            val snapshot = db.collection("bookings")
                .whereEqualTo("date", date).get().await()
            snapshot.documents.mapNotNull { doc ->
                try {
                    Booking(
                        id = doc.id,
                        requesterName = doc.getString("requesterName") ?: "",
                        requesterPhone = doc.getString("requesterPhone") ?: "",
                        purpose = doc.getString("purpose") ?: "",
                        eventType = doc.getString("eventType") ?: "",
                        date = doc.getString("date") ?: "",
                        startTime = doc.getString("startTime") ?: "",
                        endTime = doc.getString("endTime") ?: "",
                        attendeeCount = (doc.getLong("attendeeCount") ?: 0L).toInt(),
                        status = doc.getString("status") ?: "PENDING",
                        notes = doc.getString("notes") ?: "",
                        keyHolder = doc.getString("keyHolder") ?: ""
                    )
                } catch (e: Exception) { null }
            }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun submitBooking(booking: Booking): Result<String> {
        return try {
            // Check for conflicts
            val existing = db.collection("bookings")
                .whereEqualTo("date", booking.date)
                .whereEqualTo("status", "APPROVED")
                .get().await()

            val hasConflict = existing.documents.any { doc ->
                val start = doc.getString("startTime") ?: "00:00"
                val end = doc.getString("endTime") ?: "00:00"
                !(booking.endTime <= start || booking.startTime >= end)
            }

            if (hasConflict) {
                Result.failure(Exception("This time slot is already booked!"))
            } else {
                val data = hashMapOf(
                    "requesterName" to booking.requesterName,
                    "requesterPhone" to booking.requesterPhone,
                    "purpose" to booking.purpose,
                    "eventType" to booking.eventType,
                    "date" to booking.date,
                    "startTime" to booking.startTime,
                    "endTime" to booking.endTime,
                    "attendeeCount" to booking.attendeeCount,
                    "status" to "PENDING",
                    "notes" to booking.notes,
                    "keyHolder" to ""
                )
                val ref = db.collection("bookings").add(data).await()
                Result.success(ref.id)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Maintenance ─────────────────────────────────────────────

    suspend fun getMaintenanceItems(): List<MaintenanceItem> {
        return try {
            val snapshot = db.collection("maintenance_items").get().await()
            snapshot.documents.mapNotNull { doc ->
                try {
                    MaintenanceItem(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        amountNeeded = doc.getDouble("amountNeeded") ?: 0.0,
                        amountRaised = doc.getDouble("amountRaised") ?: 0.0,
                        category = doc.getString("category") ?: "OTHER",
                        status = doc.getString("status") ?: "OPEN",
                        imageUrl = doc.getString("imageUrl") ?: ""
                    )
                } catch (e: Exception) { null }
            }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun addPledge(itemId: String, pledge: Pledge): Result<Unit> {
        return try {
            val ref = db.collection("maintenance_items").document(itemId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(ref)
                val current = snapshot.getDouble("amountRaised") ?: 0.0
                transaction.update(ref, "amountRaised", current + pledge.amount)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addMaintenanceItem(item: MaintenanceItem): Result<String> {
        return try {
            val data = hashMapOf(
                "title" to item.title,
                "description" to item.description,
                "amountNeeded" to item.amountNeeded,
                "amountRaised" to 0.0,
                "category" to item.category,
                "status" to "OPEN",
                "imageUrl" to ""
            )
            val ref = db.collection("maintenance_items").add(data).await()
            Result.success(ref.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Events ──────────────────────────────────────────────────

    suspend fun getEvents(): List<CommunityEvent> {
        return try {
            val snapshot = db.collection("community_events").get().await()
            snapshot.documents.mapNotNull { doc ->
                try {
                    CommunityEvent(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        date = doc.getString("date") ?: "",
                        startTime = doc.getString("startTime") ?: "",
                        endTime = doc.getString("endTime") ?: "",
                        eventType = doc.getString("eventType") ?: "",
                        organizer = doc.getString("organizer") ?: "",
                        contactPhone = doc.getString("contactPhone") ?: "",
                        isFeatured = doc.getBoolean("isFeatured") ?: false
                    )
                } catch (e: Exception) { null }
            }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getTodayEvents(today: String): List<CommunityEvent> {
        return try {
            val snapshot = db.collection("community_events")
                .whereEqualTo("date", today).get().await()
            snapshot.documents.mapNotNull { doc ->
                try {
                    CommunityEvent(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        date = doc.getString("date") ?: "",
                        startTime = doc.getString("startTime") ?: "",
                        endTime = doc.getString("endTime") ?: "",
                        eventType = doc.getString("eventType") ?: "",
                        organizer = doc.getString("organizer") ?: "",
                        contactPhone = doc.getString("contactPhone") ?: "",
                        isFeatured = doc.getBoolean("isFeatured") ?: false
                    )
                } catch (e: Exception) { null }
            }
        } catch (e: Exception) { emptyList() }
    }
}
