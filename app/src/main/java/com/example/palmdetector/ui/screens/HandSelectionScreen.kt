package com.example.palmdetector.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.palmdetector.model.BiometricStep
import com.example.palmdetector.model.CaptureState
import com.example.palmdetector.model.HandType
import com.example.palmdetector.ui.components.AppCard
import com.example.palmdetector.ui.components.PrimaryButton
import com.example.palmdetector.ui.components.ScreenHeader
import com.example.palmdetector.ui.theme.SuccessGreen
import com.example.palmdetector.viewmodel.BiometricViewModel

@Composable
fun HandSelectionScreen(
    viewModel: BiometricViewModel,
    onStepClick: (HandType, BiometricStep) -> Unit
) {
    LaunchedEffect(Unit) {
        viewModel.refreshState()
    }

    val leftHandState by viewModel.leftHandState.collectAsState()
    val rightHandState by viewModel.rightHandState.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ScreenHeader(
                title = "Biometric Status",
                subtitle = "Track your capture progress"
            )

            HandStatusCard(
                handType = HandType.LEFT,
                handState = leftHandState,
                onClick = {
                    val nextStep = getNextStep(leftHandState)
                    onStepClick(HandType.LEFT, nextStep)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            HandStatusCard(
                handType = HandType.RIGHT,
                handState = rightHandState,
                onClick = {
                    val nextStep = getNextStep(rightHandState)
                    onStepClick(HandType.RIGHT, nextStep)
                }
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun HandStatusCard(
    modifier: Modifier = Modifier,
    handType: HandType,
    handState: Map<BiometricStep, CaptureState>,
    onClick: () -> Unit
) {
    val nextStep = getNextStep(handState)
    val isAllComplete = handState.values.all { it.isCompleted }

    AppCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = if (handType == HandType.LEFT) "LEFT HAND" else "RIGHT HAND",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isAllComplete) "All Captured" else "In Progress",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isAllComplete) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Go",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val steps = listOf(
                BiometricStep.PALM,
                BiometricStep.THUMB,
                BiometricStep.INDEX,
                BiometricStep.MIDDLE,
                BiometricStep.RING,
                BiometricStep.LITTLE
            )

            steps.forEach { step ->
                val isCompleted = handState[step]?.isCompleted == true
                StepStatusItem(
                    stepName = step.displayName,
                    isCompleted = isCompleted
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        PrimaryButton(
            text = if (isAllComplete) "Review Capture" else "Scan ${nextStep.displayName}",
            onClick = onClick,
            isSuccess = isAllComplete
        )
    }
}

@Composable
fun StepStatusItem(
    stepName: String,
    isCompleted: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    if (isCompleted) SuccessGreen else Color.Transparent
                )
                .border(
                    width = 2.dp,
                    color = if (isCompleted) SuccessGreen else MaterialTheme.colorScheme.outline.copy(
                        alpha = 0.3f
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Done",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = stepName,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isCompleted) FontWeight.Bold else FontWeight.Normal,
            color = if (isCompleted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.weight(1f))

        if (isCompleted) {
            Text(
                text = "SAVED",
                style = MaterialTheme.typography.labelSmall,
                color = SuccessGreen,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

fun getNextStep(state: Map<BiometricStep, CaptureState>): BiometricStep {
    val order = listOf(
        BiometricStep.PALM,
        BiometricStep.THUMB,
        BiometricStep.INDEX,
        BiometricStep.MIDDLE,
        BiometricStep.RING,
        BiometricStep.LITTLE
    )

    for (step in order) {
        if (state[step]?.isCompleted != true) {
            return step
        }
    }
    return BiometricStep.PALM
}