# mungo 앱을 "처음 설치한 상태"로 되돌린다 (시연 영상 · 첫 실행 테스트용)
# 펫 프로필 · 이동 기록 · 권한 · 설정이 모두 지워진다.
# 사용법 (PowerShell, 저장소 폴더에서):  .\tools\reset-first-run.ps1
# 그다음 화면 아래 독의 mungo(발바닥) 아이콘을 누르면 인트로 영상부터 시작한다.
# (Pleos 에뮬레이터는 명령어로 앱을 열면 화면에 안 보여서 직접 누른다)

$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$package = "com.example.poco"
$driverUser = 10   # Pleos 에뮬레이터의 운전자 사용자

& $adb shell pm clear --user $driverUser $package
if ($LASTEXITCODE -ne 0) {
    Write-Host "지우기 실패 - 에뮬레이터가 켜져 있는지 확인하세요 (adb devices)"
    exit 1
}
Write-Host "처음 실행 상태로 되돌렸어요. 독의 mungo 아이콘을 누르세요."