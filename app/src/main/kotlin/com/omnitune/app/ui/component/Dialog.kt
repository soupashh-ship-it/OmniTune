/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.omnitune.app.constants.ListThumbnailSize

@Composable
fun DefaultDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    buttons: (@Composable RowScope.() -> Unit)? = null,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    contentScrollable: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .imePadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.heightIn(max = maxHeight),
                shape = AlertDialogDefaults.shape,
                color = AlertDialogDefaults.containerColor,
                tonalElevation = AlertDialogDefaults.TonalElevation,
            ) {
                Column(
                    modifier = modifier.padding(24.dp),
                ) {
                    val bodyModifier =
                        if (contentScrollable) {
                            Modifier
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState())
                        } else {
                            Modifier
                        }

                    Column(
                        horizontalAlignment = horizontalAlignment,
                        modifier = bodyModifier,
                    ) {
                        if (icon != null) {
                            CompositionLocalProvider(LocalContentColor provides AlertDialogDefaults.iconContentColor) {
                                Box(Modifier.align(Alignment.CenterHorizontally)) {
                                    icon()
                                }
                            }

                            Spacer(Modifier.height(16.dp))
                        }
                        if (title != null) {
                            CompositionLocalProvider(LocalContentColor provides AlertDialogDefaults.titleContentColor) {
                                ProvideTextStyle(MaterialTheme.typography.headlineSmall) {
                                    Box(
                                        Modifier.align(if (icon == null) Alignment.Start else Alignment.CenterHorizontally)
                                    ) {
                                        title()
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))
                        }

                        content()
                    }

                    if (buttons != null) {
                        Spacer(Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {
                                ProvideTextStyle(
                                    value = MaterialTheme.typography.labelLarge
                                ) {
                                    buttons()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ListDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.padding(24.dp),
            shape = AlertDialogDefaults.shape,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = modifier.padding(vertical = 24.dp),
            ) {
                LazyColumn(content = content)
            }
        }
    }
}

@Composable
fun TextFieldDialog(
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    initialValue: String = "",
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else 10,
    isInputValid: (String) -> Boolean = { it.isNotEmpty() },
    onDone: (String) -> Unit = {},
    textFields: List<Pair<String, TextFieldValue>>? = null,
    onTextFieldsChange: ((Int, TextFieldValue) -> Unit)? = null,
    onDoneMultiple: ((List<String>) -> Unit)? = null,
    onDismiss: () -> Unit,
    extraContent: (@Composable () -> Unit)? = null,
) {
    val legacyFieldState = remember(initialValue) {
        mutableStateOf(TextFieldValue(initialValue))
    }
    val keyboardController = LocalSoftwareKeyboardController.current

    DefaultDialog(
        onDismiss = onDismiss,
        modifier = modifier,
        icon = icon,
        title = title,
        contentScrollable = true,
        buttons = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }

            val isValid = textFields?.all { isInputValid(it.second.text) }
                ?: isInputValid(legacyFieldState.value.text)

            TextButton(
                enabled = isValid,
                onClick = {
                    if (textFields != null && onDoneMultiple != null) {
                        onDoneMultiple(textFields.map { it.second.text })
                    } else {
                        onDone(legacyFieldState.value.text)
                    }
                    keyboardController?.hide()
                    onDismiss()
                },
            ) {
                Text(text = "OK")
            }
        },
    ) {
        Column {
            if (textFields != null) {
                textFields.forEachIndexed { index, (label, value) ->
                    TextField(
                        value = value,
                        onValueChange = { onTextFieldsChange?.invoke(index, it) },
                        placeholder = { Text(label) },
                        singleLine = singleLine,
                        maxLines = maxLines,
                        colors = OutlinedTextFieldDefaults.colors(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (onDoneMultiple != null) {
                                    onDoneMultiple(textFields.map { it.second.text })
                                    keyboardController?.hide()
                                    onDismiss()
                                }
                            },
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                    )
                }
            } else {
                TextField(
                    value = legacyFieldState.value,
                    onValueChange = { legacyFieldState.value = it },
                    placeholder = placeholder,
                    singleLine = singleLine,
                    maxLines = maxLines,
                    colors = OutlinedTextFieldDefaults.colors(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            onDone(legacyFieldState.value.text)
                            keyboardController?.hide()
                            onDismiss()
                        },
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }

            extraContent?.invoke()
        }
    }
}
