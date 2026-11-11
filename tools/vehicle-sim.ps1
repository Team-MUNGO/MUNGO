<#
  POCO 주행 테스트 도우미 (에뮬레이터 차량 값 바꾸기)

  Pleos Virtual Console에 "속도" 조작이 없어서, 속도는 이 스크립트로 넣는다.
  (기어 · 브레이크 · 조향 · 창문 · 온도는 Virtual Console에서 바꿔도 된다)

  사용법 (PowerShell, POCO 폴더에서):
    .\tools\vehicle-sim.ps1 speed 60        # 속도 60km/h
    .\tools\vehicle-sim.ps1 gear D          # 기어 D (P R N D)
    .\tools\vehicle-sim.ps1 hardbrake       # 60 → 20km/h 1초 만에 = 급제동 1회
    .\tools\vehicle-sim.ps1 accel           # 20 → 50km/h 1초 만에 = 급가속 1회
    .\tools\vehicle-sim.ps1 cruise 40 30    # 40km/h로 30초 유지 (창문 알림 테스트용)
    .\tools\vehicle-sim.ps1 drive           # 짧은 주행 시나리오: D → 출발 → 급제동 → 급가속 → 정지 → P
#>
param(
    [Parameter(Position = 0)][string]$Command = "help",
    [Parameter(Position = 1)][string]$Value = "",
    [Parameter(Position = 2)][string]$Seconds = "10"
)

$adb = if ($env:ANDROID_HOME) { Join-Path $env:ANDROID_HOME "platform-tools\adb.exe" } else { "C:\Android\Sdk\platform-tools\adb.exe" }

# Android 표준 차량 속성 ID (VehiclePropertyIds)
$PERF_VEHICLE_SPEED = "0x11600207"   # m/s (float)
$GEAR_SELECTION = "0x11400400"       # int: P=4, R=2, N=1, D=8

function Set-SpeedKmh([double]$kmh) {
    $mps = [math]::Round($kmh / 3.6, 3)
    & $adb shell cmd car_service inject-vhal-event $PERF_VEHICLE_SPEED $mps | Out-Null
    Write-Host ("속도 {0} km/h" -f $kmh)
}

function Set-Gear([string]$gear) {
    $code = switch ($gear.ToUpper()) { "P" { 4 } "R" { 2 } "N" { 1 } "D" { 8 } default { throw "기어는 P R N D 중 하나" } }
    & $adb shell cmd car_service inject-vhal-event $GEAR_SELECTION $code | Out-Null
    Write-Host "기어 $($gear.ToUpper())"
}

# 속도를 [from]→[to]로 [ms] 동안 바꾼다 (5Hz)
function Ramp([double]$from, [double]$to, [int]$ms) {
    $steps = [math]::Max(1, [int]($ms / 200))
    for ($i = 0; $i -le $steps; $i++) {
        Set-SpeedKmh ($from + ($to - $from) * $i / $steps)
        Start-Sleep -Milliseconds 200
    }
}

function Hold([double]$kmh, [int]$sec) {
    $end = (Get-Date).AddSeconds($sec)
    while ((Get-Date) -lt $end) { Set-SpeedKmh $kmh; Start-Sleep -Milliseconds 500 }
}

switch ($Command.ToLower()) {
    "speed" { Set-SpeedKmh ([double]$Value) }
    "gear" { Set-Gear $Value }
    "hardbrake" { Hold 60 2; Ramp 60 20 1000; Hold 20 2 }
    "accel" { Hold 20 2; Ramp 20 50 1000; Hold 50 2 }
    "cruise" { Hold ([double]$Value) ([int]$Seconds) }
    "drive" {
        Set-Gear D; Start-Sleep 2
        Ramp 0 50 6000; Hold 50 5        # 부드러운 출발
        Ramp 50 20 1000; Hold 20 3       # 급제동
        Ramp 20 50 1000; Hold 50 5       # 급가속
        Ramp 50 0 6000; Start-Sleep 1    # 부드러운 정지
        Set-Gear P                       # → 도착 리포트(주행 종료)
    }
    default { Get-Help $MyInvocation.MyCommand.Path -Detailed | Out-Host; Write-Host "예) .\tools\vehicle-sim.ps1 speed 60" }
}
