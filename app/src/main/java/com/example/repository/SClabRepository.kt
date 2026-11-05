package com.example.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.*
import com.google.firebase.Timestamp
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class SClabRepository(private val context: Context) {
    private val db = FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
    private val auth = Firebase.auth

    val currentUserId: String?
        get() = auth.currentUser?.uid

    suspend fun getUserProfile(userId: String): UserProfile? {
        val doc = db.collection("users").document(userId).get().await()
        return doc.toObject(UserProfile::class.java)
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        val uid = currentUserId ?: throw IllegalStateException("Not authenticated")
        db.collection("users").document(uid).set(profile.copy(userId = uid, createdAt = profile.createdAt ?: Timestamp.now())).await()
        
        val publicProfile = PublicProfile(
            userId = uid,
            displayName = profile.fullName,
            gender = profile.gender,
            age = 22,
            profilePicture = profile.profilePicture,
            availableForCall = profile.availableForCall,
            earningRate = profile.earningRate
        )
        db.collection("publicProfiles").document(uid).set(publicProfile).await()
    }

    suspend fun checkAndAssignFounderMembership(userId: String): Boolean {
        val founderRef = db.collection("founderMembership").document("global")
        val membershipRef = db.collection("memberships").document(userId)
        val userRef = db.collection("users").document(userId)

        return try {
            db.runTransaction { transaction ->
                val founderSnapshot = transaction.get(founderRef)
                if (!founderSnapshot.exists() || founderSnapshot.getString("userId").isNullOrBlank()) {
                    val founderData = mapOf(
                        "userId" to userId,
                        "assignedAt" to FieldValue.serverTimestamp()
                    )
                    transaction.set(founderRef, founderData)

                    val membershipData = mapOf(
                        "userId" to userId,
                        "tier" to "founder_free",
                        "status" to "active",
                        "activatedAt" to FieldValue.serverTimestamp(),
                        "paymentRef" to "FREE_FOUNDER"
                    )
                    transaction.set(membershipRef, membershipData)
                    transaction.update(userRef, "isFounder", true, "earningRate", 4)
                    true
                } else {
                    false
                }
            }.await()
        } catch (e: Exception) {
            Log.e("Repository", "Founder assignment transaction failed", e)
            false
        }
    }

    suspend fun getMembership(userId: String): MembershipInfo? {
        val doc = db.collection("memberships").document(userId).get().await()
        return doc.toObject(MembershipInfo::class.java)
    }

    suspend fun activatePaidMembership(userId: String, tier: String, paymentRef: String, rate: Int) {
        val membershipData = MembershipInfo(
            userId = userId,
            tier = tier,
            status = "active",
            activatedAt = Timestamp.now(),
            paymentRef = paymentRef
        )
        db.collection("memberships").document(userId).set(membershipData).await()
        db.collection("users").document(userId).update("earningRate", rate).await()
    }

    suspend fun getWallet(userId: String): Wallet {
        val doc = db.collection("wallets").document(userId).get().await()
        return doc.toObject(Wallet::class.java) ?: Wallet(userId = userId, balancePaise = 0L, pendingPaise = 0L, lifetimePaise = 0L)
    }

    fun observeWalletTransactions(userId: String): Flow<List<WalletTransaction>> = callbackFlow {
        val listener = db.collection("wallets").document(userId).collection("transactions")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(WalletTransaction::class.java) }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    fun observePublicProfiles(): Flow<List<PublicProfile>> = callbackFlow {
        val uid = currentUserId
        val listener = db.collection("publicProfiles")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(PublicProfile::class.java) }
                        .filter { it.userId != uid }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }
}
