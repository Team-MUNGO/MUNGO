package com.example.poco.presentation.record

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.poco.core.driving.DrivingSettings
import com.example.poco.presentation.design.IconBadge
import com.example.poco.presentation.design.Mg
import com.example.poco.presentation.design.MgCard
import com.example.poco.presentation.design.MgIcon
import com.example.poco.presentation.design.MgIcons
import com.example.poco.presentation.design.PrimaryButton
import com.example.poco.presentation.session.PetSettingsForm
import com.example.poco.presentation.session.component.formatTemperature
import com.example.poco.presentation.session.component.intervalText
import com.example.poco.presentation.session.component.seatShort

/** 설정 (rail tab): rest interval entry and voice guidance. */
@Composable
fun SettingsRoute(onNavigate: (RailTab) -> Unit, onOpenInterval: () -> Unit, viewModel: RestSettingsViewModel = hiltViewModel()) {
    val interval by viewModel.interval.collectAsStateWithLifecycle()
    val voice by viewModel.voice.collectAsStateWithLifecycle()
    RailScaffold(RailTab.SETTINGS, onNavigate) {
        Text("설정", style = Mg.bold(38, 51), color = Mg.Ink)
        Spacer(Modifier.height(32.dp))
        SettingRow(MgIcons.Cup, "휴식 간격", "출발 또는 재출발 시각부터 ${intervalText(interval)}마다 쉬어 가요. 휴식 20분 전에 맞춤 제안을 받아요.", Modifier.clickable(onClick = onOpenInterval)) {
            Text(intervalText(interval), style = Mg.bold(26, 35), color = Mg.OrangeText)
            Spacer(Modifier.width(16.dp))
            MgIcon(MgIcons.ChevronRight, Mg.Faint, 24.dp)
        }
        Spacer(Modifier.height(20.dp))
        SettingRow(MgIcons.Speaker, "음성 안내", "끄면 안내를 화면에만 보여 주고, 나중에 다시 읽어 주지 않아요.") {
            Switch(voice, viewModel::setVoice, colors = SwitchDefaults.colors(checkedTrackColor = Mg.Orange))
        }
    }
}

@Composable
private fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit
) {
    MgCard(Modifier.fillMaxWidth().height(150.dp).then(modifier), padding = PaddingValues(horizontal = 36.dp, vertical = 24.dp)) {
        Row(Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            IconBadge(icon, size = 66.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = Mg.semi(26, 35), color = Mg.Ink)
                Text(body, style = Mg.Body, color = Mg.Muted)
            }
            trailing()
        }
    }
}

/** 마이펫 (rail tab): the pet profile used when a trip starts. */
@Composable
fun PetProfileRoute(onNavigate: (RailTab) -> Unit, viewModel: PetProfileViewModel = hiltViewModel()) {
    val saved by viewModel.profile.collectAsStateWithLifecycle()
    var draft by remember { mutableStateOf<DrivingSettings?>(null) }
    var savedNotice by remember { mutableStateOf(false) }
    RailScaffold(RailTab.PET, onNavigate) {
        Text("마이펫", style = Mg.bold(38, 51), color = Mg.Ink)
        Spacer(Modifier.height(32.dp))
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(26.dp)) {
            MgCard(Modifier.weight(1f), padding = PaddingValues(40.dp)) {
                val s = saved
                Text(s?.petName ?: "아직 등록하지 않았어요", style = Mg.bold(34, 45), color = Mg.Ink)
                Spacer(Modifier.height(20.dp))
                if (s != null) {
                    ProfileLine("탑승 위치", seatShort(s.seat))
                    ProfileLine("기준 온도", "${formatTemperature(s.minTemperatureC)}~${formatTemperature(s.maxTemperatureC)}°C")
                    ProfileLine("몸무게", s.weightTenths?.let { "${it / 10}.${it % 10}kg · ${s.petSize?.label}견" } ?: "미입력")
                }
                Spacer(Modifier.weight(1f))
                Text("주행을 시작할 때 이 정보로 온도·창문·휴식 안내를 맞춰요.", style = Mg.Body, color = Mg.Muted)
            }
            MgCard(Modifier.weight(1f), padding = PaddingValues(40.dp)) {
                Text("정보 수정", style = Mg.semi(26, 35), color = Mg.Ink)
                Spacer(Modifier.height(16.dp))
                PetSettingsForm(saved, Modifier.weight(1f)) {
                    draft = it
                    savedNotice = false
                }
                Spacer(Modifier.height(16.dp))
                PrimaryButton(
                    if (savedNotice) "저장했어요" else "저장하기",
                    {
                        draft?.let {
                            viewModel.save(it)
                            savedNotice = true
                        }
                    },
                    Modifier.fillMaxWidth().height(90.dp),
                    enabled = draft != null && draft != saved
                )
            }
        }
    }
}

@Composable
private fun ProfileLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = Mg.Body, color = Mg.Muted)
        Text(value, style = Mg.semi(23, 30), color = Color(0xFF191F28))
    }
}
