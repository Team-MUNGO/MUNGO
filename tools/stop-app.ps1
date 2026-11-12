# mungo 앱을 완전히 끈다 (데이터는 그대로)
# 사용법 (PowerShell, 저장소 폴더에서):  .\tools\restart-app.ps1
# 그다음 독의 mungo(발바닥) 아이콘을 누르면 인트로 영상부터 다시 시작한다.
# (Pleos 에뮬레이터는 명령어로 앱을 열면 화면에 안 보여서 직접 누른다)

$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$package = "com.example.poco"
$driverUser = 10   # Pleos 에뮬레이터의 운전자 사용자

& $adb shell am force-stop --user $driverUser $package
Write-Host "앱을 껐어요. 독의 mungo 아이콘을 누르면 인트로부터 다시 시작해요."