package com.example.interview.screen.roomdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.interview.model.Comment
import com.example.interview.model.SyncState
import com.example.interview.viewmodel.send
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomDetailScreen(
    viewModel: RoomDetailViewModel = koinViewModel()
) {
    val viewState = viewModel.viewState
    val room = viewState.room

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(room?.name ?: "Room") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.send(RoomDetailViewModel.Action.GoBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.send(RoomDetailViewModel.Action.EditRoom) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Room")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (room == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Panorama Section
                item {
                    PanoSection(
                        hasPano = room.pano != null && room.pano.syncState != SyncState.DELETED,
                        onAddPano = {
                            // For simplicity, add dummy pano data
                            viewModel.send(RoomDetailViewModel.Action.AddPano(byteArrayOf(1, 2, 3)))
                        },
                        onRemovePano = {
                            viewModel.send(RoomDetailViewModel.Action.RemovePano)
                        }
                    )
                }

                // Comments Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Comments (${room.comments.size})",
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(onClick = { viewModel.send(RoomDetailViewModel.Action.AddComment) }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Comment")
                        }
                    }
                }

                // Comments List
                if (room.comments.isEmpty()) {
                    item {
                        Text(
                            text = "No comments yet. Tap + to add one.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(room.comments, key = { it.id }) { comment ->
                        CommentCard(
                            comment = comment,
                            onEdit = { viewModel.send(RoomDetailViewModel.Action.EditComment(comment.id)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PanoSection(
    hasPano: Boolean,
    onAddPano: () -> Unit,
    onRemovePano: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (hasPano) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("360° Panorama Added")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = onRemovePano) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Remove")
                        }
                    }
                }
            } else {
                Button(onClick = onAddPano) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add 360° Panorama")
                }
            }
        }
    }
}

@Composable
private fun CommentCard(
    comment: Comment,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = comment.text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Comment")
            }
        }
    }
}
