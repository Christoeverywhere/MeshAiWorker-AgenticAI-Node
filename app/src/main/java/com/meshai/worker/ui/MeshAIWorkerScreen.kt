package com.meshai.worker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshAIWorkerScreen(viewModel: WorkerViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MeshAI Worker") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Distributed Worker",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            ConnectionStatusCard(uiState.workerState, uiState.errorMessage)

            Spacer(modifier = Modifier.height(24.dp))

            OrchestratorConfigCard(
                url = uiState.orchestratorUrl,
                onUrlChange = { viewModel.updateOrchestratorUrl(it) },
                onConnect = { viewModel.connect() },
                onDisconnect = { viewModel.disconnect() },
                state = uiState.workerState
            )

            Spacer(modifier = Modifier.height(24.dp))

            DeviceInfoCard(uiState.nodeInfo, uiState.lastHeartbeat)
        }
    }
}

@Composable
fun ConnectionStatusCard(state: WorkerState, message: String) {
    val color = when (state) {
        WorkerState.CONNECTED -> Color(0xFF4CAF50)
        WorkerState.CONNECTING -> Color(0xFFFFC107)
        WorkerState.DISCONNECTED -> Color.Gray
        WorkerState.ERROR -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(12.dp),
                    shape = MaterialTheme.shapes.small,
                    color = color
                ) {}
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = state.name,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    fontSize = 18.sp
                )
            }
            if (message.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = message, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun OrchestratorConfigCard(
    url: String,
    onUrlChange: (String) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    state: WorkerState
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Orchestrator Configuration", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                label = { Text("Orchestrator URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = state == WorkerState.DISCONNECTED || state == WorkerState.ERROR
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (state == WorkerState.CONNECTED || state == WorkerState.CONNECTING) {
                    Button(onClick = onDisconnect, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                        Text("Disconnect")
                    }
                } else {
                    Button(onClick = onConnect) {
                        Text(if (state == WorkerState.ERROR) "Reconnect" else "Connect")
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceInfoCard(nodeInfo: com.meshai.worker.model.NodeInfo?, lastHeartbeat: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Device Information", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            InfoRow("Node ID", nodeInfo?.nodeId ?: "N/A")
            InfoRow("Device", nodeInfo?.deviceName ?: "N/A")
            InfoRow("Android", nodeInfo?.operatingSystem ?: "N/A")
            InfoRow("RAM", "${nodeInfo?.ramMb ?: 0} MB")
            InfoRow("CPU Cores", "${nodeInfo?.cpuCores ?: 0}")
            InfoRow("Battery", "${nodeInfo?.batteryPercent ?: 0}%")
            InfoRow("Capabilities", nodeInfo?.capabilities?.joinToString(", ") ?: "N/A")
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            
            InfoRow("Last Heartbeat", lastHeartbeat)
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.secondary)
        Text(text = value, fontWeight = FontWeight.Medium)
    }
}
