package com.example.poco.presentation.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.poco.core.driving.DrivingSettings
import com.example.poco.core.driving.PetSeat
import com.example.poco.core.driving.parseWeightTenths
import com.example.poco.core.driving.petSizeFor
import com.example.poco.presentation.design.Mg

/** Pet profile fields shared by the trip setup dialog and the 마이펫 screen. Emits null while invalid. */
@Composable
fun PetSettingsForm(saved: DrivingSettings?, modifier: Modifier = Modifier, onChange: (DrivingSettings?) -> Unit) {
    var name by rememberSaveable { mutableStateOf(saved?.petName.orEmpty()) }
    var seat by rememberSaveable { mutableStateOf(saved?.seat) }
    var min by rememberSaveable { mutableStateOf(saved?.minTemperatureC?.toString().orEmpty()) }
    var max by rememberSaveable { mutableStateOf(saved?.maxTemperatureC?.toString().orEmpty()) }
    var weight by rememberSaveable {
        mutableStateOf(saved?.weightTenths?.let { "${it / 10}.${it % 10}" }.orEmpty())
    }
    val weightTenths = parseWeightTenths(weight)
    val settings = runCatching {
        require(weight.isBlank() || weightTenths != null)
        DrivingSettings(name.trim(), seat ?: error("좌석 미선택"), min.toFloat(), max.toFloat(), weightTenths)
    }.getOrNull()
    LaunchedEffect(settings) { onChange(settings) }
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(name, { name = it }, label = { Text("반려견 이름") }, singleLine = true)
        Text("반려견 탑승 위치", style = Mg.BodyStrong, color = Mg.Ink)
        PetSeat.entries.forEach { option ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(seat == option, { seat = option })
                Text(option.label, style = Mg.Body, color = Mg.Ink)
            }
        }
        OutlinedTextField(min, { min = it }, label = { Text("최저 허용 온도 (°C)") }, singleLine = true)
        OutlinedTextField(max, { max = it }, label = { Text("최고 허용 온도 (°C)") }, singleLine = true)
        Text("최저 온도가 최고 온도 이하가 되도록 입력해 주세요.", style = Mg.Caption, color = Mg.Muted)
        OutlinedTextField(weight, { weight = it }, label = { Text("몸무게 (kg, 0.1 단위)") }, singleLine = true)
        Text(
            weightTenths?.let { "${petSizeFor(it).label}견 · 10kg 미만 소형, 25kg 미만 중형, 25kg 이상 대형" }
                ?: if (weight.isBlank()) "몸무게를 입력하면 크기에 맞춰 추천해요" else "0.1kg 단위로 입력해 주세요",
            style = Mg.Caption,
            color = Mg.Muted
        )
    }
}
