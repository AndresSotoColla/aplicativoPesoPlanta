package com.example.aplicativopesoplanta.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aplicativopesoplanta.ui.theme.LightBeige

@Composable
fun HomeScreen(
    onNavigateToForm: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onExport: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBeige)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Muestreo Peso Planta",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 48.dp)
            )

            HomeButton(
                text = "Nuevo Muestreo",
                icon = Icons.Default.Add,
                color = Color.Black,
                onClick = onNavigateToForm
            )

            Spacer(modifier = Modifier.height(16.dp))

            HomeButton(
                text = "Ver Historial",
                icon = Icons.Default.History,
                color = Color.Black,
                onClick = onNavigateToHistory
            )

            Spacer(modifier = Modifier.height(16.dp))

            HomeButton(
                text = "Descargar Información",
                icon = Icons.Default.Download,
                color = Color.Black,
                onClick = onExport
            )
        }
    }
}

@Composable
fun HomeButton(
    text: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = color,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
