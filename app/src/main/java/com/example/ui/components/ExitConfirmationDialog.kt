package com.example.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun ExitConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "ออกจากแอป?") },
        text = { Text(text = "คุณแน่ใจหรือไม่ว่าต้องการออกจากแอปพลิเคชัน?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("ตกลง")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ยกเลิก")
            }
        }
    )
}
