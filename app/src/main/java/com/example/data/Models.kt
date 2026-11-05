package com.example.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class UserProfile(
    @DocumentId val userId: String = "",
    val fullName: String = "",
    val gender: String = "",
    val dob: String = "",
    val email: String = "",
    val profilePicture: String = "",
    val availableForCall: Boolean = true,
    val earningRate: Int = 4,
    val isAdmin: Boolean = false,
    val isFounder: Boolean = false,
    val createdAt: Timestamp? = null
)

data class PublicProfile(
    @DocumentId val userId: String = "",
    val displayName: String = "",
    val gender: String = "",
    val age: Int = 22,
    val profilePicture: String = "",
    val availableForCall: Boolean = true,
    val earningRate: Int = 4
)

data class FounderRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val assignedAt: Timestamp? = null
)

data class MembershipInfo(
    @DocumentId val userId: String = "",
    val tier: String = "none",
    val status: String = "inactive",
    val activatedAt: Timestamp? = null,
    val paymentRef: String = ""
)

data class Conversation(
    @DocumentId val id: String = "",
    val participants: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val lastMessage: String = "",
    val updatedAt: Timestamp? = null
)

data class Message(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Timestamp? = null,
    val readStatus: Boolean = false
)

data class CallRecord(
    @DocumentId val id: String = "",
    val callerId: String = "",
    val calleeId: String = "",
    val callerName: String = "",
    val calleeName: String = "",
    val callType: String = "audio",
    val status: String = "ringing",
    val startTime: Timestamp? = null,
    val endTime: Timestamp? = null,
    val durationSeconds: Long = 0L,
    val earnedPaise: Long = 0L
)

data class Wallet(
    @DocumentId val userId: String = "",
    val balancePaise: Long = 0L,
    val pendingPaise: Long = 0L,
    val lifetimePaise: Long = 0L
)

data class WalletTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = "credit", // "credit" or "debit"
    val title: String = "",
    val amountPaise: Long = 0L,
    val timestamp: Timestamp? = null,
    val status: String = "success"
)

data class BankAccount(
    @DocumentId val userId: String = "",
    val accountHolderName: String = "",
    val bankName: String = "",
    val accountNumber: String = "",
    val ifscCode: String = "",
    val status: String = "pending",
    val submittedAt: Timestamp? = null
)

data class WithdrawalRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val amountPaise: Long = 0L,
    val status: String = "pending",
    val requestedAt: Timestamp? = null
)

data class UserReport(
    @DocumentId val id: String = "",
    val reporterId: String = "",
    val reportedUserId: String = "",
    val reason: String = "",
    val timestamp: Timestamp? = null
)

data class BlockRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val blockedUserId: String = "",
    val timestamp: Timestamp? = null
)

data class AdminAuditLog(
    @DocumentId val id: String = "",
    val adminId: String = "",
    val action: String = "",
    val targetUserId: String = "",
    val timestamp: Timestamp? = null
)
