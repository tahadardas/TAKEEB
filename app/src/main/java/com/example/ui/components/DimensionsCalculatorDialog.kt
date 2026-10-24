package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CalcTarget
import com.example.ui.TruckViewModel
import java.util.Locale

@Composable
fun DimensionsCalculatorDialog(
    viewModel: TruckViewModel,
    modifier: Modifier = Modifier
) {
    val showDialog by viewModel.showCalcDialog.collectAsState()
    if (!showDialog) return

    val target by viewModel.calcTarget.collectAsState()
    val length by viewModel.calcLength.collectAsState()
    val width by viewModel.calcWidth.collectAsState()
    val height by viewModel.calcHeight.collectAsState()
    val resultVolume by viewModel.calcResultVolume.collectAsState()

    val targetTitle = if (target == CalcTarget.HEAD) "لحساب كمية الرأس" else "لحساب كمية الذنب"

    AlertDialog(
        onDismissRequest = { viewModel.closeCalcDialog() },
        icon = {
            Icon(
                imageVector = Icons.Default.Calculate,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "حاسبة الأبعاد ($targetTitle)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "أدخل أبعاد الصندوق بالمتر (طول × عرض × ارتفاع):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = length,
                    onValueChange = { viewModel.calcLength.value = it },
                    label = { Text("الطول (متر)") },
                    placeholder = { Text("مثال: 5.5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calc_length_input")
                )

                OutlinedTextField(
                    value = width,
                    onValueChange = { viewModel.calcWidth.value = it },
                    label = { Text("العرض (متر)") },
                    placeholder = { Text("مثال: 2.4") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calc_width_input")
                )

                OutlinedTextField(
                    value = height,
                    onValueChange = { viewModel.calcHeight.value = it },
                    label = { Text("الارتفاع (متر)") },
                    placeholder = { Text("مثال: 1.2") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calc_height_input")
                )

                Spacer(modifier = Modifier.height(4.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "الناتج التكعيبي:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = String.format(Locale.US, "%.2f م³", resultVolume),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.applyCalcResult() },
                enabled = resultVolume > 0.0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("apply_calc_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text("تطبيق الناتج")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = { viewModel.closeCalcDialog() },
                modifier = Modifier.testTag("cancel_calc_button")
            ) {
                Icon(Icons.Default.Close, contentDescription = null)
                Spacer(modifier = Modifier.padding(horizontal = 2.dp))
                Text("إلغاء")
            }
        }
    )
}
