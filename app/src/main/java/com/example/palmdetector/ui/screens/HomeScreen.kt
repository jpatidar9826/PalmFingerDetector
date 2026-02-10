package com.example.palmdetector.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.palmdetector.ui.components.AppCard
import com.example.palmdetector.ui.components.LargeStatusIcon
import com.example.palmdetector.ui.components.PrimaryButton
import com.example.palmdetector.ui.components.ScreenHeader
import com.example.palmdetector.viewmodel.BiometricViewModel

@Composable
fun HomeScreen(
    viewModel: BiometricViewModel,
    onAddBiometric: () -> Unit,
    onVerify: () -> Unit
) {
    val isRegistered by viewModel.isBiometricRegistered.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshState()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            LargeStatusIcon(icon = Icons.Default.Fingerprint)
            Spacer(modifier = Modifier.height(24.dp))

            ScreenHeader(
                title = "Palm ID",
                subtitle = "Secure Biometric Verification"
            )

            Spacer(modifier = Modifier.height(48.dp))

            AppCard(
                onClick = onAddBiometric
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Register Hand",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Capture Palm & Fingers",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                PrimaryButton(
                    text = "Start Registration",
                    onClick = onAddBiometric
                )
            }

            Spacer(modifier = Modifier.height(24.dp))


            AppCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = if (isRegistered) com.example.palmdetector.ui.theme.SuccessGreen else Color.Gray,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Verify Identity",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isRegistered) MaterialTheme.colorScheme.onSurface else Color.Gray
                        )
                        Text(
                            text = if (isRegistered) "Ready to Scan" else "Registration Required",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isRegistered) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))


                PrimaryButton(
                    text = "Verify Now",
                    onClick = onVerify,
                    enabled = isRegistered,
                    isSuccess = isRegistered
                )
            }
        }
    }
}