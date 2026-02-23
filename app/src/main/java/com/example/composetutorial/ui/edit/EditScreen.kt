package com.example.composetutorial.ui.edit

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.composetutorial.ui.login.TopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(viewModel: EditViewModel, onSaveSuccess: () -> Unit, onCancel: () -> Unit) {
    val name by viewModel.name.collectAsState()
    val description by viewModel.description.collectAsState()
    val date by viewModel.date.collectAsState()
    val value by viewModel.value.collectAsState()
    val flag by viewModel.flag.collectAsState()

    // Animation states for staggered appearance
    var fieldsVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { fieldsVisible = true }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = {
                            AnimatedContent(
                                    targetState =
                                            if (viewModel.isEditing) "Edit Item" else "Add Item",
                                    transitionSpec = {
                                        fadeIn() + slideInVertically() togetherWith
                                                fadeOut() + slideOutVertically()
                                    },
                                    label = "title_animation"
                            ) { title -> Text(title) }
                        },
                        colors =
                                TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        titleContentColor = MaterialTheme.colorScheme.primary
                                )
                )
            }
    ) { innerPadding ->
        Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Animated form fields
            AnimatedFormField(visible = fieldsVisible, delay = 0) {
                OutlinedTextField(
                        value = name ?: "",
                        onValueChange = viewModel::onNameChange,
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                )
            }

            AnimatedFormField(visible = fieldsVisible, delay = 50) {
                OutlinedTextField(
                        value = description ?: "",
                        onValueChange = viewModel::onDescriptionChange,
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                )
            }

            // Date picker section
            val datePickerState = rememberDatePickerState()
            var showDatePicker by remember { mutableStateOf(false) }

            if (showDatePicker) {
                DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(
                                    onClick = {
                                        datePickerState.selectedDateMillis?.let { millis ->
                                            val formattedDate =
                                                    java.text.SimpleDateFormat(
                                                                    "yyyy-MM-dd",
                                                                    java.util.Locale.getDefault()
                                                            )
                                                            .format(java.util.Date(millis))
                                            viewModel.onDateChange(formattedDate)
                                        }
                                        showDatePicker = false
                                    }
                            ) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                        }
                ) { DatePicker(state = datePickerState) }
            }

            AnimatedFormField(visible = fieldsVisible, delay = 100) {
                OutlinedTextField(
                        value = date ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Date") },
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) { Text("Select") }
                        },
                        modifier = Modifier.fillMaxWidth()
                )
            }

            AnimatedFormField(visible = fieldsVisible, delay = 150) {
                OutlinedTextField(
                        value = value,
                        onValueChange = viewModel::onValueChange,
                        label = { Text("Nr") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                )
            }

            AnimatedFormField(visible = fieldsVisible, delay = 200) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = flag, onCheckedChange = viewModel::onFlagChange)
                    Text(text = "Done")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Animated buttons
            AnimatedFormField(visible = fieldsVisible, delay = 250) {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Animated Save button
                    AnimatedButton(
                            onClick = { viewModel.saveItem(onSaveSuccess) },
                            modifier = Modifier.weight(1f)
                    ) { Text("Save") }

                    // Animated Delete button (only when editing)
                    AnimatedVisibility(
                            visible = viewModel.isEditing,
                            enter = scaleIn() + fadeIn(),
                            exit = scaleOut() + fadeOut()
                    ) {
                        Button(
                                onClick = { viewModel.deleteItem(onSuccess = onSaveSuccess) },
                                colors =
                                        ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error
                                        )
                        ) { Text("Delete") }
                    }

                    OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedFormField(visible: Boolean, delay: Int, content: @Composable () -> Unit) {
    val alpha by
            animateFloatAsState(
                    targetValue = if (visible) 1f else 0f,
                    animationSpec =
                            tween(
                                    durationMillis = 300,
                                    delayMillis = delay,
                                    easing = FastOutSlowInEasing
                            ),
                    label = "form_alpha"
            )

    val translation by
            animateFloatAsState(
                    targetValue = if (visible) 0f else 30f,
                    animationSpec =
                            tween(
                                    durationMillis = 300,
                                    delayMillis = delay,
                                    easing = FastOutSlowInEasing
                            ),
                    label = "form_translation"
            )

    Box(
            modifier =
                    Modifier.graphicsLayer {
                        this.alpha = alpha
                        translationY = translation
                    }
    ) { content() }
}

@Composable
private fun AnimatedButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        content: @Composable RowScope.() -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by
            animateFloatAsState(
                    targetValue = if (isPressed) 0.95f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "button_scale"
            )

    Button(
            onClick = {
                isPressed = true
                onClick()
            },
            modifier = modifier.scale(scale),
            content = content
    )
}
