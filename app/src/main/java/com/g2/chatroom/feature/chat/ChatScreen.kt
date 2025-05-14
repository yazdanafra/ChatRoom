package com.g2.chatroom.feature.chat

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.g2.chatroom.R
import com.g2.chatroom.feature.home.ChannelItem
import com.g2.chatroom.model.Message
import com.g2.chatroom.ui.theme.DarkGrey
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.zegocloud.uikit.prebuilt.call.invite.widget.ZegoSendCallInvitationButton
import com.zegocloud.uikit.service.defines.ZegoUIKitUser
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.util.Log
import android.widget.Toast



@Composable
fun ChatScreen(navController: NavController, channelId: String, channelName: String) {
    Scaffold(
        containerColor = Color.White
    ) {
        val viewModel: ChatViewModel = hiltViewModel()
        val chooserDialog = remember {
            mutableStateOf(false)
        }

        val cameraImageUri = remember {
            mutableStateOf<Uri?>(null)
        }

        val cameraImageLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture()
        ) { success ->
            if (success) {
                cameraImageUri.value?.let {
                    viewModel.sendImageMessage(it, channelId)
                }
            }
        }

        val imageLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let { viewModel.sendImageMessage(it, channelId) }
        }


        fun createImageUri(): Uri {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = ContextCompat.getExternalFilesDirs(
                navController.context, Environment.DIRECTORY_PICTURES
            ).first()
            return FileProvider.getUriForFile(
                navController.context,
                "${navController.context.packageName}.provider",
                File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
                    cameraImageUri.value = Uri.fromFile(this)
                })
        }

        val permissionLauncher =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    cameraImageLauncher.launch(createImageUri())
                }
            }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            LaunchedEffect(key1 = true) {
                viewModel.listenForMessages(channelId)
            }
            val messages = viewModel.message.collectAsState()
            ChatMessages(
                messages = messages.value,
                onSendMessage = { message ->
                    viewModel.sendMessage(channelId, message)
                },
                onImageClicked = {
                    chooserDialog.value = true
                },
                onDeleteMessage = { message ->
                    viewModel.deleteMessage(channelId, message)
                },
                channelName = channelName,
                viewModel = viewModel,
                channelID = channelId
            )
        }

        if (chooserDialog.value) {
            ContentSelectionDialog(onCameraSelected = {
                chooserDialog.value = false
                if (navController.context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    cameraImageLauncher.launch(createImageUri())
                } else {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }, onGallerySelected = {
                chooserDialog.value = false
                imageLauncher.launch("image/*")
            })
        }
    }
}


@Composable
fun ContentSelectionDialog(onCameraSelected: () -> Unit, onGallerySelected: () -> Unit) {
    AlertDialog(
        onDismissRequest = { },
        confirmButton = {
            TextButton(onClick = onCameraSelected) {
                Text(text = "Camera")
            }
        },
        dismissButton = {
            TextButton(onClick = onGallerySelected) {
                Text(text = "Gallery")
            }
        },
        title = { Text(text = "Select your source?") },
        text = { Text(text = "Would you like to pick an image from the gallery or use the") })
}

@Composable
fun ChatMessages(
    channelName: String,
    channelID: String,
    messages: List<Message>,
    onSendMessage: (String) -> Unit,
    onImageClicked: () -> Unit,
    onDeleteMessage: (Message) -> Unit,
    viewModel: ChatViewModel
) {
    val hideKeyboardController = LocalSoftwareKeyboardController.current

    val msg = remember {
        mutableStateOf("")
    }
    Column(modifier = Modifier.fillMaxSize()) {
        ChannelItem(
            channelName = channelName,
            Modifier,
            true,
            onClick = {},
            onCall = { callButton ->
                viewModel.getAllUserEmails(channelID) {
                    val list: MutableList<ZegoUIKitUser> = mutableListOf()
                    it.forEach { email ->
                        Firebase.auth.currentUser?.email?.let { em ->
                            if (email != em) {
                                list.add(
                                    ZegoUIKitUser(
                                        email, email
                                    )
                                )
                            }
                        }
                    }
                    callButton.setInvitees(list)
                }
            },
            false
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            reverseLayout = true
        ) {
            items(messages.reversed()) { message ->
                ChatBubble(
                    message = message,
                    onDeleteMessage = onDeleteMessage,
                    onSaveImage = { url, id -> viewModel.saveImageFromUrl(url, id) }
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1F8793))
                .padding(8.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                msg.value = ""
                onImageClicked()
            }) {
                Image(
                    painter = painterResource(id = R.drawable.attach), contentDescription = "attach"
                )
            }

            TextField(
                value = msg.value,
                onValueChange = { msg.value = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(text = "Type a message") },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    hideKeyboardController?.hide()
                }),
                colors = TextFieldDefaults.colors().copy(
                    focusedContainerColor = Color(0xFF1F8793),
                    unfocusedContainerColor = Color(0xFF1F8793),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.LightGray,
                    focusedPlaceholderColor = Color.White,
                    unfocusedPlaceholderColor = Color.LightGray
                )
            )
            IconButton(onClick = {
                if (msg.value.trim().isNotEmpty()) {
                    onSendMessage(msg.value)
                    msg.value = ""
                }
            }) {
                Image(painter = painterResource(id = R.drawable.send), contentDescription = "send")
            }
        }
    }
}

@Composable
fun ChatBubble(
    message: Message,
    onDeleteMessage: (Message) -> Unit,
    onSaveImage: (String, String?) -> Unit
) {
    val isCurrentUser = message.senderId == Firebase.auth.currentUser?.uid
    val bubbleColor = if (isCurrentUser) {
        Color(0xFF239BFC)
    } else {
        Color(0xFF1F8793)
    }

    // Format the timestamp
    val formattedTime = remember(message.createdAt) {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.createdAt))
    }

    // Show context menu only for text messages from current user
    val showContextMenu = remember { mutableStateOf(false) }
    val contextMenuPosition = remember { mutableStateOf(Offset.Zero) }
    val clipboardManager = LocalClipboardManager.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        val alignment = if (!isCurrentUser) Alignment.CenterStart else Alignment.CenterEnd
        Column(
            modifier = Modifier
                .padding(8.dp)
                .align(alignment),
            horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isCurrentUser) {
                    Image(
                        painter = painterResource(id = R.drawable.friend),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))


                }

                Column {
                    //show the user name
                    if (!isCurrentUser) {
                        Text(
                            text = message.senderName,
                            color = Color.Gray,
                            style = TextStyle(fontSize = 12.sp),
                            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                color = bubbleColor, shape = RoundedCornerShape(8.dp)
                            )
                            .padding(16.dp)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = { offset ->
                                        // Show for *any* message
                                        if (message.message != null || message.imageUrl != null) {
                                            contextMenuPosition.value = offset
                                            showContextMenu.value = true
                                        }
                                    }
                                )
                            }
                    ) {
                        if (message.imageUrl != null) {
                            AsyncImage(
                                model = message.imageUrl,
                                contentDescription = null,
                                modifier = Modifier.size(200.dp),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(text = message.message?.trim() ?: "", color = Color.White)
                        }
                    }
                }
            }

            // Display the context menu if showContextMenu is true
            if (showContextMenu.value && (message.imageUrl != null || !message.message.isNullOrBlank())) {
                DropdownMenu(
                    expanded = showContextMenu.value,
                    onDismissRequest = { showContextMenu.value = false },
                    modifier = Modifier.widthIn(max = 150.dp),
                    offset = DpOffset(
                        x = with(LocalDensity.current) { contextMenuPosition.value.x.toDp() },
                        y = 0.dp
                    )
                ) {
                    // 1) IMAGE MENU (highest priority)
                    if (message.imageUrl != null) {
                        DropdownMenuItem(
                            text = { Text("Save") },
                            onClick = {
                                Log.d("ChatBubble", "Save clicked for ${message.imageUrl}")
                                onSaveImage(message.imageUrl!!, message.id)
                                showContextMenu.value = false
                            }
                        )
                        if (isCurrentUser) {
                            Divider(color = Color.Gray.copy(alpha = 0.3f))
                            DropdownMenuItem(
                                text = { Text("Delete", color = Color.Red) },
                                onClick = {
                                    onDeleteMessage(message)
                                    showContextMenu.value = false
                                }
                            )
                        }
                    }
                    // 2) TEXT MENU (only if not blank)
                    else if (!message.message.isNullOrBlank()) {
                        DropdownMenuItem(
                            text = { Text("Copy") },
                            onClick = {
                                clipboardManager.setText(AnnotatedString(message.message!!.trim()))
                                showContextMenu.value = false
                            }
                        )
                        if (isCurrentUser) {
                            Divider(color = Color.Gray.copy(alpha = 0.3f))
                            DropdownMenuItem(
                                text = { Text("Delete", color = Color.Red) },
                                onClick = {
                                    onDeleteMessage(message)
                                    showContextMenu.value = false
                                }
                            )
                        }
                    }
                }
            }



            // Display timestamp
            Text(
                text = formattedTime,
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp, start = if (!isCurrentUser) 48.dp else 0.dp)
            )
        }
    }
}

@Composable
fun MessageContextMenu(
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        color = DarkGrey,
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            TextButton(
                onClick = {
                    onCopy()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Copy", color = Color.White)
            }

            Divider(color = Color.Gray.copy(alpha = 0.3f))

            TextButton(
                onClick = {
                    onDelete()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Delete", color = Color.Red)
            }
        }
    }
}

@Composable
fun CallButton(isVideoCall: Boolean, onClick: (ZegoSendCallInvitationButton) -> Unit) {
    AndroidView(factory = { context ->
        val button = ZegoSendCallInvitationButton(context)
        button.setIsVideoCall(isVideoCall)
        button.resourceID = "zego_data"
        button
    }, modifier = Modifier.size(50.dp)) { zegoCallButton ->
        zegoCallButton.setOnClickListener { _ -> onClick(zegoCallButton) }
    }
}

