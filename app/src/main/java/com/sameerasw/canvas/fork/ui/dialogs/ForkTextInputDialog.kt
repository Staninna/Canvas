package com.sameerasw.canvas.fork.ui.dialogs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.sameerasw.canvas.R

@Composable
fun ForkTextInputDialog(
    visible: Boolean,
    isEditing: Boolean,
    currentValue: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return
    val focus = remember { FocusRequester() }
    val canConfirm = currentValue.isNotBlank()
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(painterResource(R.drawable.rounded_text_fields_24), contentDescription = null) },
        title = {
            Text(stringResource(if (isEditing) R.string.text_dialog_edit_title else R.string.text_dialog_add_title))
        },
        text = {
            OutlinedTextField(
                value = currentValue,
                onValueChange = onValueChange,
                placeholder = { Text(stringResource(R.string.text_dialog_placeholder)) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { if (canConfirm) onConfirm() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus)
            )
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = canConfirm) {
                Text(stringResource(if (isEditing) R.string.action_save else R.string.action_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
    LaunchedEffect(Unit) { focus.requestFocus() }
}
