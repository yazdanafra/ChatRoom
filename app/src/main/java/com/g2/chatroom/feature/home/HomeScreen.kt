package com.g2.chatroom.feature.home

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.g2.chatroom.AppID
import com.g2.chatroom.AppSign
import com.g2.chatroom.MainActivity
import com.g2.chatroom.feature.chat.CallButton
import com.g2.chatroom.ui.theme.DarkGrey
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.zegocloud.uikit.prebuilt.call.invite.widget.ZegoSendCallInvitationButton
import androidx.compose.material3.ButtonDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalActivity.current as MainActivity
    LaunchedEffect(Unit) {
        Firebase.auth.currentUser?.let {
            context.initZegoService(
                appID = AppID,
                appSign = AppSign,
                userID = it.email!!,
                userName = it.email!!
            )
        }
    }
    val viewModel = hiltViewModel<HomeViewModel>()
    val channels = viewModel.channels.collectAsState()
    val searchQuery = viewModel.searchQuery.collectAsState()
    val searchResults = viewModel.searchResults.collectAsState()
    val showSearchResults = remember { mutableStateOf(false) }
    val addChannel = remember {
        mutableStateOf(false)
    }
    val sheetState = rememberModalBottomSheetState()
    Scaffold(
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xC1006E46))
                    .clickable {
                        addChannel.value = true
                    }) {
                Text(
                    text = "Add Channel", modifier = Modifier.padding(16.dp), color = Color.White
                )
            }
        }, containerColor = Color.White
    ) {
        Box(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
        ) {
            LazyColumn {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Messages",
                            color = Color.Gray,
                            style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Black)
                        )
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.Gray,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable {
                                    navController.navigate("settings")
                                }
                        )
                    }
                }

                item {
                    TextField(
                        value = searchQuery.value,
                        onValueChange = {
                            viewModel.searchChannels(it)
                            showSearchResults.value = it.isNotBlank()
                        },
                        placeholder = { Text(text = "Search...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(40.dp)),
                        textStyle = TextStyle(color = Color.LightGray),
                        colors = TextFieldDefaults.colors().copy(
                            focusedContainerColor = Color(0xC1006E46),
                            unfocusedContainerColor = Color(0xC1006E46),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedPlaceholderColor = Color.White,
                            unfocusedPlaceholderColor = Color.White,
                            focusedIndicatorColor = Color.White
                        ),
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search, contentDescription = null
                            )
                        })

                }

                // Show search results or user's channels
                if (showSearchResults.value && searchResults.value.isNotEmpty()) {
                    item {
                        Text(
                            text = "Search Results",
                            color = Color.Gray,
                            style = TextStyle(fontSize = 16.sp),
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                        )
                    }

                    items(searchResults.value) { channel ->
                        Column {
                            ChannelItem(
                                channelName = channel.name,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                                false,
                                onClick = {
                                    navController.navigate("chat/${channel.id}&${channel.name}")
                                },
                                onCall = {})
                        }
                    }
                }

               else if (channels.value.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillParentMaxSize() // make this Box as tall as the LazyColumn
                                .padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "You don't have any active channels right now.\n" +
                                        "Either search for one, or create one using the \"Add Channel\" button down below.",
                                textAlign = TextAlign.Center,
                                style = TextStyle(fontSize = 16.sp, color = Color.Gray)
                            )
                        }
                    }
                }

                else {
                    // Show user's channels (ones they've created or messaged in)
                    items(channels.value) { channel ->
                        Column {
                            ChannelItem(
                                channelName = channel.name,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                                false,
                                onClick = {
                                    navController.navigate("chat/${channel.id}&${channel.name}")
                                },
                                onCall = {})
                        }
                    }
                }
            }
        }
    }



    if (addChannel.value) {
        ModalBottomSheet(onDismissRequest = { addChannel.value = false }, sheetState = sheetState) {
            AddChannelDialog {
                viewModel.addChannel(it)
                addChannel.value = false
            }
        }
    }

}


@Composable
fun ChannelItem(
    channelName: String,
    modifier: Modifier,
    shouldShowCallButtons: Boolean = false,
    onClick: () -> Unit,
    onCall: (ZegoSendCallInvitationButton) -> Unit,
    useRoundedCorners: Boolean = true
) {
    val shape = if (useRoundedCorners) RoundedCornerShape(16.dp) else RectangleShape
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xC1006E46))
    )
    {
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clickable {
                    onClick()
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(Color.Yellow.copy(alpha = 0.3f))

            ) {
                Text(
                    text = channelName[0].uppercase(),
                    color = Color.White,
                    style = TextStyle(fontSize = 35.sp),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )
            }


            Text(text = channelName, modifier = Modifier.padding(8.dp), color = Color.White)
        }
        if (shouldShowCallButtons) {
            Row(
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                CallButton(isVideoCall = true, onCall)
                CallButton(isVideoCall = false, onCall)
            }
        }
    }
}

@Composable
fun AddChannelDialog(onAddChannel: (String) -> Unit) {
    val channelName = remember {
        mutableStateOf("")
    }
    Column(
        modifier = Modifier.padding(8.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Add Channel")
        Spacer(modifier = Modifier.padding(8.dp))
        TextField(value = channelName.value, onValueChange = {
            channelName.value = it
        }, label = { Text(text = "Channel Name") }, singleLine = true)
        Spacer(modifier = Modifier.padding(8.dp))
        Button(onClick = { onAddChannel(channelName.value) }, modifier = Modifier.fillMaxWidth(),            colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xC1006E46),           // سبز ملایم تار
            contentColor = Color.White                    // نوشته سفید
        )) {
            Text(text = "Add")
        }
    }
}
