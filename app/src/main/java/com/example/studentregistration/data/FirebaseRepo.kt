package com.example.studentregistration.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

object FirebaseRepo {
    // ✅ Auth
    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    // ✅ Realtime Database (RTDB) — trainer’s requirement
    val rtdb: DatabaseReference by lazy { FirebaseDatabase.getInstance().reference }

    // (Optional) Keep if other screens still use them
    val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }       // Firestore (optional)
    val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }      // Storage (for files)

    // ---------- RTDB helpers (use these from Activities) ----------

    /** Create/Update user at /users/{uid} */
    fun saveUserToRtdb(uid: String, userMap: Map<String, Any?>, onOk: (() -> Unit)? = null, onErr: ((Exception) -> Unit)? = null) {
        rtdb.child("users").child(uid).setValue(userMap)
            .addOnSuccessListener { onOk?.invoke() }
            .addOnFailureListener { e -> onErr?.invoke(e) }
    }

    /** Read user once from /users/{uid} */
    fun getUserOnce(uid: String, onOk: (Map<String, Any?>?) -> Unit, onErr: (Exception) -> Unit) {
        rtdb.child("users").child(uid).get()
            .addOnSuccessListener { snap -> @Suppress("UNCHECKED_CAST")
            onOk(snap.value as? Map<String, Any?>)
            }
            .addOnFailureListener { e -> onErr(e) }
    }

    /** Patch some fields at /users/{uid} */
    fun updateUser(uid: String, updates: Map<String, Any?>, onOk: (() -> Unit)? = null, onErr: ((Exception) -> Unit)? = null) {
        rtdb.child("users").child(uid).updateChildren(updates)
            .addOnSuccessListener { onOk?.invoke() }
            .addOnFailureListener { e -> onErr?.invoke(e) }
    }

    // ---------- (Optional) simple list helpers under user node ----------

    /** Push an item to a sub-list: /users/{uid}/{child}/autoId -> data */
    fun pushChild(uid: String, child: String, data: Map<String, Any?>, onOk: ((String) -> Unit)? = null, onErr: ((Exception) -> Unit)? = null) {
        val ref = rtdb.child("users").child(uid).child(child).push()
        ref.setValue(data)
            .addOnSuccessListener { onOk?.invoke(ref.key ?: "") }
            .addOnFailureListener { e -> onErr?.invoke(e) }
    }

    /** Read a full sub-list once: /users/{uid}/{child} */
    fun getChildListOnce(uid: String, child: String, onOk: (List<Map<String, Any?>>) -> Unit, onErr: (Exception) -> Unit) {
        rtdb.child("users").child(uid).child(child).get()
            .addOnSuccessListener { snap ->
                val list = mutableListOf<Map<String, Any?>>()
                for (c in snap.children) {
                    @Suppress("UNCHECKED_CAST")
                    val item = c.value as? Map<String, Any?>
                    if (item != null) list.add(item)
                }
                onOk(list)
            }
            .addOnFailureListener { e -> onErr(e) }
    }
}