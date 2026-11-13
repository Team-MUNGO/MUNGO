# mungo

반려견을 태우고 운전하는 사람을 위한 Pleos Connect 차량용 앱입니다.
2026 현대자동차그룹 차량용 앱 개발 공모전에 출품했습니다.

강아지는 차가 흔들리거나 안이 더워져도 말을 못 합니다. mungo는 운전하는 동안 차량 데이터를 보고 대신 알려 줍니다.
급하게 서거나 커브를 세게 돌면 알려 주고, 반려견 쪽 창문이 열려 있거나 실내가 더우면 음성으로 물어본 뒤 대답에 맞춰 창문과 에어컨을 조절합니다.
운전 중에는 화면을 만지지 않아도 되도록 대부분 말로 주고받습니다.

패키지 이름(`com.example.poco`)은 기존 앱 식별자를 유지합니다. 코드의 클래스·함수·테마 이름과 앱 표기는 Mungo/mungo로 통일했습니다.

## 할 수 있는 것

**반려견 프로필**
처음 실행하면 이름, 견종, 나이, 몸무게와 성향을 입력합니다.
몸무게로 크기를 나누고, 견종으로 단두종인지 봅니다.
멀미를 한다고 체크하면 작은 흔들림부터 알려 주고, 겁이 많다고 하면 음성 안내를 줄입니다.
여러 마리를 등록할 수 있습니다.

**탑승 준비**
출발 전에 함께 탈 반려견과 앉을 자리를 고르고 체크리스트를 확인합니다.
카시트와 하네스는 직접 체크하고, 창문과 실내 온도는 차에서 읽어 와 자동으로 체크합니다.
기어가 P일 때만 열립니다.

**주행 중**
급가속, 급제동, 급커브, 과속방지턱을 감지해서 컴포트 점수(100점 시작)를 매깁니다.
내비에 급커브나 과속방지턱이 잡혀 있으면 미리 알려 줍니다.
창문이 열렸거나 온도가 반려견 기준을 벗어나면 "닫을까요?"라고 묻고, "응"이나 "조금만"처럼 대답하는 대로 처리합니다.

**휴식과 음성 질문**
정해 둔 간격(기본 2시간)이 다 되어 가면 쉴 만한 곳을 찾아 경유지로 넣어 줍니다.
운전하면서 궁금한 걸 물어보면 지금 차 상태를 보고 짧게 답합니다(Gemini).

**이동 기록과 마이펫**
주행마다 점수와 있었던 일을 남기고, 도착 후에는 멀미 증상을 적어 둘 수 있습니다.
마이펫에서 반려견 정보를 고치거나 추가, 삭제합니다.

## 기술 스택

- Kotlin, Jetpack Compose(Material3), Hilt, Coroutines/Flow
- Room, DataStore
- Pleos Connect SDK, Android Car API
- Firebase Cloud Functions + Gemini API, Google Maps (음성 질문, 휴식 장소 검색)
- ktlint, JUnit

minSdk 28, targetSdk 37, JDK 17. 앱 영역이 가로라서 가로로 고정했습니다.

## 코드 구조

```
app/src/main/java/com/example/poco
├── core          테마, 공용 컴포넌트, 유틸
├── data          Room, Pleos SDK, Car API, repository
├── domain        점수 계산, 견종별 기준, 음성 대답 해석
└── presentation  화면별 Compose + ViewModel
```

화면은 ViewModel만 알고, ViewModel은 repository를 통해 데이터를 가져옵니다.
Pleos SDK는 콜백 방식이라 `data/pleos/datasource`에서 suspend 함수나 Flow로 바꿔 두었습니다. SDK 초기화는 `PleosSdkManager`가 처음 쓸 때 한 번 합니다.

## 사용하는 Pleos SDK

| SDK | 버전 | 용도 |
|---|---|---|
| Vehicle | 2.3.2 | 기어, 창문, 공조 온도, 브레이크, 조향 |
| NaviHelper | 3.0.5 | 경로의 급커브·과속방지턱, 경로 시작과 도착, 경유지 추가 |
| TextToSpeech | 3.3.1 | 음성 안내 |
| SpeechToText | 3.3.1 | 대답 듣기, 음성 질문 |
| FusedLocation | 1.1.3 | 진행 방향, 휴식 장소 검색 |

속도는 Android Car API(`PERF_VEHICLE_SPEED`)로 읽습니다.
SDK 문서: [Connect SDK](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/), [API Reference](https://document.pleos.ai/api-reference/connect-sdk-pleos)

## 빌드

Android Studio로 열면 됩니다. Pleos SDK는 `settings.gradle.kts`에 적어 둔 Pleos Maven 저장소에서 받아 옵니다.

터미널에서 빌드할 때 기본 JDK가 17보다 낮으면 Android Studio에 들어 있는 JDK를 지정하세요.

```bash
JAVA_HOME="C:\Program Files\Android\Android Studio\jbr" ./gradlew assembleDebug
```

음성 질문 기능은 서버 주소(`gradle.properties`의 `POCO_ASSISTANT_URL`)와 Firebase 키(`local.properties`의 `POCO_FIREBASE_API_KEY`)가 있어야 켜집니다. 없어도 나머지 기능은 그대로 됩니다. 서버 쪽 설정은 [functions/README.md](functions/README.md)에 있습니다.

## 에뮬레이터에서 돌려 보기

Pleos Connect 에뮬레이터(API 34)에서는 운전자 계정이 user 10이라 거기에 설치해야 합니다.

```bash
adb install -r --user 10 app/build/outputs/apk/debug/app-debug.apk
```

`am start`로 실행하면 차량 화면에 안 뜨는 경우가 있어서, 아래 독에 있는 mungo 아이콘을 눌러 여는 게 편합니다.

- 처음 설치한 상태로 되돌리기: `.\tools\reset-first-run.ps1`
- 앱 완전히 종료: `.\tools\stop-app.ps1`
- 기어, 창문, 온도 등 차량 값 바꾸기: `adb forward tcp:8088 tcp:8088` 후 http://127.0.0.1:8088 (Pleos Virtual Console)
- 마이크: 에뮬레이터 설정의 Microphone에서 host audio input 켜기

에뮬레이터 이미지에 따라 Pleos 음성 엔진이 안 뜨는 경우가 있습니다. 음성과 내비 연동은 실제 기기에서 확인해야 합니다.
`adb root`는 차량 화면이 깨지니 쓰지 마세요.

테스트는 `./gradlew testDebugUnitTest`로 돌립니다.

## 같이 작업할 때

- 브랜치 이름은 `feat/#이슈번호-작업명`, 커밋은 `feat/#이슈번호: 설명` 형식으로 씁니다.
- 커밋 전에 `./gradlew ktlintFormat`을 돌려 주세요.
- main에는 직접 올리지 않고, PR을 올려 리뷰를 받은 뒤 합칩니다.

디자인: [Figma](https://www.figma.com/design/g7UnrDclVk077ot6Hyi64i/)
