package com.g2.chatroom.feature.home

import androidx.lifecycle.ViewModel
import com.g2.chatroom.model.Channel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {
    private val firebaseDatabase = Firebase.database
    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels = _channels.asStateFlow()

    // New properties for search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    private val _searchResults = MutableStateFlow<List<Channel>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    // Get current user ID
    private val currentUserId = Firebase.auth.currentUser?.uid

    init {
        // Load user's channels instead of all channels
        loadUserChannels()
    }

    // Load only channels the user has interacted with
    private fun loadUserChannels() {
        currentUserId?.let { userId ->
            firebaseDatabase.getReference("user_channels")
                .child(userId)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val channelIds = mutableListOf<String>()
                        snapshot.children.forEach { data ->
                            data.key?.let { channelIds.add(it) }
                        }
                        fetchChannelDetails(channelIds)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Handle error
                    }
                })
        }
    }

    // Fetch details for specific channel IDs
    private fun fetchChannelDetails(channelIds: List<String>) {
        if (channelIds.isEmpty()) {
            _channels.value = emptyList()
            return
        }

        val list = mutableListOf<Channel>()
        var fetchedCount = 0

        channelIds.forEach { channelId ->
            firebaseDatabase.getReference("channel").child(channelId).get()
                .addOnSuccessListener { snapshot ->
                    snapshot.key?.let { key ->
                        snapshot.value?.toString()?.let { name ->
                            list.add(Channel(key, name))
                        }
                    }

                    fetchedCount++
                    if (fetchedCount == channelIds.size) {
                        _channels.value = list
                    }
                }
        }
    }

    // Search for channels
    fun searchChannels(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        firebaseDatabase.getReference("channel").get().addOnSuccessListener {
            val results = mutableListOf<Channel>()
            it.children.forEach { data ->
                val channelName = data.value.toString()
                if (channelName.contains(query, ignoreCase = true)) {
                    results.add(Channel(data.key!!, channelName))
                }
            }
            _searchResults.value = results
        }
    }

    // Add channel to user's list when they message in it
    fun addChannelToUserList(channelId: String) {
        currentUserId?.let { userId ->
            firebaseDatabase.getReference("user_channels")
                .child(userId)
                .child(channelId)
                .setValue(true)
        }
    }

    // Create a new channel and add it to user's list
    fun addChannel(name: String) {
        val key = firebaseDatabase.getReference("channel").push().key
        key?.let { channelId ->
            firebaseDatabase.getReference("channel").child(channelId).setValue(name)
                .addOnSuccessListener {
                    // Add to user's list of channels
                    addChannelToUserList(channelId)
                }
        }
    }
}