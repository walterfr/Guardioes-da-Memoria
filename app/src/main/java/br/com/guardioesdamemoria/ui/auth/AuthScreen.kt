package br.com.guardioesdamemoria.ui.auth

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.guardioesdamemoria.ui.theme.*
import br.com.guardioesdamemoria.viewmodel.LocationViewModel

@Composable
fun AuthScreen(
    viewModel: LocationViewModel,
    onAuthSuccess: (Boolean) -> Unit // isTeacher
) {
    var isTeacherSelection by remember { mutableStateOf<Boolean?>(null) }
    var name by remember { mutableStateOf("") }
    var school by remember { mutableStateOf("") }
    var generatedPin by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier.fillMaxSize().background(NightField),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "IDENTIFICAÇÃO",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            Text(
                text = "Como você deseja explorar o bairro?",
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(48.dp))

            if (isTeacherSelection == null) {
                // Seleção de Perfil
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ProfileCard(
                        title = "ALUNO",
                        icon = Icons.Default.School,
                        modifier = Modifier.weight(1f),
                        onClick = { isTeacherSelection = false }
                    )
                    ProfileCard(
                        title = "PROFESSOR",
                        icon = Icons.Default.Person,
                        modifier = Modifier.weight(1f),
                        onClick = { isTeacherSelection = true }
                    )
                }
            } else {
                // Formulário de Cadastro
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome Completo") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedTextColor = Color.White, focusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = school,
                        onValueChange = { school = it },
                        label = { Text("Escola / Instituição") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedTextColor = Color.White, focusedTextColor = Color.White)
                    )

                    if (isTeacherSelection == true) {
                        OutlinedTextField(
                            value = generatedPin ?: "",
                            onValueChange = { 
                                generatedPin = it
                                viewModel.setTeacherPin(it)
                            },
                            label = { Text("Definir PIN de Orientador (Ex: 2024)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(unfocusedTextColor = Color.White, focusedTextColor = Color.White),
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                        )
                        Text(
                            "Este PIN será solicitado para aprovar ou excluir relatos.",
                            color = Color.White.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    Button(
                        onClick = { onAuthSuccess(isTeacherSelection!!) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MemoryTeal),
                        enabled = name.isNotBlank() && (if (isTeacherSelection == true) !generatedPin.isNullOrBlank() else true)
                    ) {
                        Text("ENTRAR NA JORNADA", fontWeight = FontWeight.Black)
                    }

                    TextButton(onClick = { isTeacherSelection = null; generatedPin = null }) {
                        Text("Trocar Perfil", color = Color.White.copy(alpha = 0.4f))
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.height(180.dp).clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = MemoryTeal, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Black)
        }
    }
}
