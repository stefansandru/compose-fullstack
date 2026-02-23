package com.example.composetutorial.ui.list

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.composetutorial.data.model.Item
import com.example.composetutorial.ui.login.TopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
        viewModel: ListViewModel,
        onItemClick: (Int) -> Unit,
        onAddClick: () -> Unit,
        onLogout: () -> Unit
) {
    val items by viewModel.items.collectAsState()
    val isLoading = items.isEmpty()

    // Animated FAB scale
    val fabScale by
            animateFloatAsState(
                    targetValue = if (isLoading) 0f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "fab_scale"
            )

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Items List") },
                        colors =
                                TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        titleContentColor = MaterialTheme.colorScheme.primary
                                ),
                        actions = {
                            TextButton(
                                    onClick = {
                                        viewModel.logout()
                                        onLogout()
                                    }
                            ) { Text("Logout") }
                        }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = onAddClick, modifier = Modifier.scale(fabScale)) {
                    Icon(Icons.Default.Add, contentDescription = "Add Item")
                }
            }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Loading animation
            AnimatedVisibility(
                    visible = isLoading,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading items...")
                }
            }

            // Items list with staggered animation
            AnimatedVisibility(visible = !isLoading, enter = fadeIn(animationSpec = tween(300))) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                        AnimatedItemRow(
                                item = item,
                                index = index,
                                onClick = { onItemClick(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedItemRow(item: Item, index: Int, onClick: () -> Unit) {
    // Staggered animation delay based on index
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }

    val animatedAlpha by
            animateFloatAsState(
                    targetValue = if (visible) 1f else 0f,
                    animationSpec =
                            tween(
                                    durationMillis = 300,
                                    delayMillis = index * 50, // Stagger effect
                                    easing = FastOutSlowInEasing
                            ),
                    label = "alpha_$index"
            )

    val animatedTranslation by
            animateFloatAsState(
                    targetValue = if (visible) 0f else 50f,
                    animationSpec =
                            tween(
                                    durationMillis = 300,
                                    delayMillis = index * 50,
                                    easing = FastOutSlowInEasing
                            ),
                    label = "translation_$index"
            )

    Card(
            modifier =
                    Modifier.fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .graphicsLayer {
                                alpha = animatedAlpha
                                translationY = animatedTranslation
                            }
                            .clickable(onClick = onClick),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = item.name ?: "No Name", style = MaterialTheme.typography.titleMedium)
            Text(text = item.description ?: "", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                        text = "Date: ${item.date ?: "N/A"}",
                        style = MaterialTheme.typography.labelSmall
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Value: ${item.value}", style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.weight(1f))

                // Animated offline badge
                AnimatedVisibility(
                        visible = item.isDirty,
                        enter = scaleIn() + fadeIn(),
                        exit = scaleOut() + fadeOut()
                ) {
                    Text(
                            text = "Offline",
                            color = Color.Red,
                            style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

// Keep original ItemRow for compatibility
@Composable
fun ItemRow(item: Item, onClick: () -> Unit) {
    AnimatedItemRow(item = item, index = 0, onClick = onClick)
}
