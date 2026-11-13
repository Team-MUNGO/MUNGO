# POCO 텍스트 음성 AI 서버

앱의 Pleos SpeechToText 결과 텍스트 → HTTPS Cloud Functions → Gemini 텍스트 generateContent → 앱 화면 및 Pleos TextToSpeech. Gemini 오디오/Live API, Pleos LLM, Android 음성 인식·합성·직접 마이크 캡처는 사용하지 않는다. 앱에는 Firebase SDK나 Gemini 키가 필요하지 않다.

Firebase 프로젝트 `mungo-8be3c`의 `asia-northeast3`에 배포되어 있다(함수 URL: `https://asia-northeast3-mungo-8be3c.cloudfunctions.net/assistantApi`, 런타임 계정 `poco-assistant-runtime`). 휴게소 카탈로그는 비어 있으므로 휴게소 안내는 CATALOG_UNAVAILABLE로 종료한다. 오류를 가짜 응답으로 대체하지 않는다.

## 심사위원 설치 방법 (개발팀 도움 불필요)

1. 배포용 APK를 Pleos 기기/에뮬레이터에 설치한다: `adb install -r app-debug.apk`.
2. 앱 실행 → 우측 상단 **주행 시작** → 반려견 이름·탑승 위치·허용 온도 입력 → 위치 권한 허용.
3. P단 정차 상태에서 **AI 연결** → "위 정보 전송에 동의합니다" 체크 → **연결**. 연결 코드는 필요 없다. 앱이 Firebase 익명 인증으로 자동 로그인한다.
4. **음성으로 질문** → Pleos STT로 질문 → 서버가 Gemini로 차량 상태 기반 답변 생성 → 주행 화면 답변 카드 표시 → Pleos TTS 출력.

APK 빌드(개발팀): 함수 URL은 루트 `gradle.properties`의 `POCO_ASSISTANT_URL`에 들어 있어 `./gradlew :app:assembleDebug`(Android Studio 실행 포함)만으로 연결된다. 다른 서버로 붙을 때만 `-PPOCO_ASSISTANT_URL=...` 또는 `~/.gradle/gradle.properties`로 덮어쓴다. Firebase Web API 키는 `local.properties`의 `POCO_FIREBASE_API_KEY=` 또는 `-PPOCO_FIREBASE_API_KEY=`로 주입한다(Firebase 앱 `POCO Android`, `firebase apps:sdkconfig ANDROID 1:899311377531:android:ddce974a0ff8163d9216de`). 이 키는 공개 프로젝트 식별자이며 비밀이 아니다. Gemini 키는 APK에 들어가지 않는다.

에뮬레이터 주의: 확인한 `Pleos_Connect_v2.1.0` 에뮬레이터 이미지에서는 Pleos 온디바이스 STT/TTS 엔진의 모델 로딩이 실패한다(STT `KenLMWrapper: unk is not defined`, TTS `Failed to load MetaSpace byte pair encoder` → `ON_DEVICE_ENGINE_ERROR`). SERVER 모드는 Pleos 클라이언트 자격 증명이 없는 앱에서 `Client credentials missing`으로 실패한다. 이 환경에서는 음성 질문을 시작할 수 없다. 앱 결함이 아닌 엔진/이미지 상태이며 실제 Pleos 기기 또는 정상 모델이 설치된 이미지에서 확인해야 한다.

## 구조화 AI 엔드포인트 `/assist` (Gemini + Google Maps)

같은 함수(`assistantApi`)의 `/assist` 경로. 인증(Firebase ID 토큰 또는 개발 세션)과 사용자·IP·프로젝트 한도는 `/answer`와 동일하며, 기존 `/answer`는 호환을 위해 유지한다.

- `USER_QUESTION`: 앱은 질문과 최소 차량 문맥(점수, 휴식까지 남은 분, 측정 온도·상태, 창문, 반려견 크기)만 보낸다. Gemini가 의미로 의도를 판정한다(`ANSWER`/`PLACE_SEARCH`, 문자열 매칭 없음). 장소 검색이 필요하면 서버가 `LOCATION_REQUIRED`와 검색어를 돌려주고, 앱은 그때만 위치 권한·위치를 받아 재요청한다. 일반 질문에는 Maps를 호출하지 않는다.
- `REST_RECOMMENDATION`: 앱이 휴식 20분 전(사용자 간격 기준, 주기당 1회)에 자동 호출. 서버는 타이머를 갖지 않는다.
- 응답: `requestId, eventId, type, status(OK|NO_RESULTS|LOCATION_REQUIRED), displayText, ttsText, places[{placeId,name,latitude,longitude,address,mapsUrl,canAddWaypoint}], recommendedPlaceId, searchQuery, sources{text:'gemini', places:'google_maps'}`. 장소 이름·ID·좌표는 Maps 결과에서만 만들고 Gemini는 문구와 후보 ID 선택만 한다. 목록에 없는 ID, ETA·시간 주장은 검증된 템플릿 문구로 대체한다. 좌표는 Gemini에 보내지 않는다. 주소는 Essentials 응답에 없으므로 `null`.
- 지도: "Maps Experience agent tools"(구 Maps Grounding Lite) REST `POST https://mapstools.googleapis.com/v1:searchText`, `X-Goog-Api-Key`=`GOOGLE_MAPS_API_KEY` Secret. `pageSize=5`, 확장 정보 미요청 → Essentials SKU만 사용(Pro 미사용). 반경 20km location bias, `ko`/`KR`. 주변 후보이며 경로상 위치를 보장하지 않는다.
- 비용/한도(공식 문서 확인, 2026-11): Essentials는 월 10,000건 이후 1,000건당 $7, Pro는 월 5,000건 이후 1,000건당 $14(미사용). 프로젝트 쿼터 300 QPM. 서버는 Firestore로 Maps 호출을 일 300건(`maps-<day>`)으로 별도 제한한다. 무료 사용량을 무제한으로 가정하지 말고 Cloud 예산 알림을 설정한다.
- 표기 의무: Maps 근거 장소는 화면에 "Google Maps" 출처를 함께 표시한다(맞춤 제안 카드에 표시). 음성 인터페이스 공개 고지·컴패니언 UI 요구는 출시 전 검토 항목이다.
- 실시간 확인 스크립트(키 비출력): `scripts/smoke-anonymous.js`(익명 인증 경로).

## 인증 방식

- **기본(심사·배포)**: Firebase Authentication 익명 로그인. 앱은 Google Play 서비스 의존 없이 Identity Toolkit REST API(`accounts:signUp`, `securetoken` 갱신)로 ID 토큰을 받는다. 익명 refresh token만 앱의 no-backup 저장소에 보관해 설치당 하나의 uid를 유지하고, 1시간짜리 ID 토큰은 메모리에만 두고 만료 전 자동 갱신한다.
- **서버 검증**: `/answer`는 `Authorization: Bearer <Firebase ID token>`을 Admin SDK `verifyIdToken`으로 검증(서명·audience=`mungo-8be3c`·만료)한다. 검증 실패는 401이며 Gemini를 호출하지 않는다.
- **개발용 일회용 연결 코드**: 기존 `/session` 초대 코드 → 1시간 세션 경로는 그대로 유지된다. `POCO_FIREBASE_API_KEY` 없이 빌드한 APK는 코드 입력 화면을 보여 준다.
- **사용량 제한**(Firestore transaction): 익명 사용자 uid당 5회/분·60회/시, HMAC된 IP당 120회/시, 개발 세션 기기당 5회/분·60회/시, 프로젝트 전체 500회/일. uid·IP 원문은 저장하지 않는다.

## App Check 검토 (현재 미적용)

Play Integrity 공급자는 Google Play 서비스와 Play 배포/인증 기기를 전제로 하는데, Pleos(AAOS) 기기와 에뮬레이터의 지원 여부가 확인되지 않았다. 사이드로드 APK를 심사위원이 직접 설치하는 방식이라 강제 적용 시 정상 심사 사용자가 차단될 위험이 크다. 디버그 공급자는 기기별 토큰을 수동 등록해야 해 "개발팀 도움 없는 설치" 요건과 충돌한다. 그래서 지금은 적용하지 않는다.

미적용 상태의 남용 위험: 공개 Web API 키로 누구나 익명 계정을 만들어 ID 토큰을 받고 `/answer`를 호출할 수 있다(앱 외부 스크립트 포함). 완화 수단은 uid·IP·프로젝트 일일 한도, Firebase 익명 계정 생성의 IP당 기본 제한, 질문 500자/응답 300자, 차량 상태 형식 검증, maxInstances 2다. 이는 비용 상한을 보장하지 않으므로 Gemini API 할당량·예산 알림을 함께 설정하고 심사 종료 후 익명 인증 비활성화 또는 함수 비활성화를 권장한다. 공개 출시 시에는 App Check(Play Integrity 또는 Pleos 플랫폼의 기기 증명) 강제가 필요하다.


## 배포 전 준비

1. 운영자가 Firebase/Google Cloud 프로젝트, 결제(Functions에 필요), Firestore Native database를 준비한다. 적절한 지역·IAM·개인정보 정책을 검토한다. 이 저장소는 계정/결제/프로젝트를 자동 생성하지 않는다.
2. 지원되는 최신 Node 22 LTS를 사용한다. 로컬 Node 22.9에서는 jwks-rsa의 ESM 의존성 로딩이 실패하므로 22.12 이상을 사용한다. Node 22.23.5에서 실제 함수 진입점 로딩과 서버 테스트 13개를 확인했다. `npm ci --prefix functions`로 lockfile 의존성을 설치한다. Firebase CLI는 운영자 환경에서 설치·인증한다. 서비스 계정 JSON을 앱이나 저장소에 넣지 않는다.
3. 루트에서 명시적인 `--project PROJECT_ID`를 사용한다. `firebase functions:secrets:set GEMINI_API_KEY --project PROJECT_ID`와 `firebase functions:secrets:set ASSISTANT_IP_SALT --project PROJECT_ID`로 각각 Gemini 키와 충분히 긴 난수 salt를 Secret Manager에 등록한다. 키는 명령 인자·채팅·CI 로그로 전달하지 않는다. 키는 Gemini API만 허용하고 프로젝트 예산/할당량도 설정한다.
4. `functions/.env.PROJECT_ID`에 `GEMINI_MODEL=현재_계정에서_지원되는_텍스트_모델_ID`를 지정한다. 모델 지원은 계정에서 확인한다. 같은 파일에 `ASSISTANT_SERVICE_ACCOUNT=전용런타임계정@PROJECT_ID.iam.gserviceaccount.com`을 설정한다. 해당 계정에는 Firestore 데이터 접근과 두 secret의 accessor 권한만 부여하고 기본 Editor 계정 사용을 피한다. 배포자는 이 계정을 사용할 권한과 Functions 배포 권한이 필요하다. `.env*`와 `.secret.local`은 git에서 제외된다. API 키는 이 파일에 저장하지 않는다.
5. 휴게소 데이터가 필요하면 아래 카탈로그를 준비한다. 비어 있으면 주변 검색은 CATALOG_UNAVAILABLE로 종료한다.
6. `npm test --prefix functions`, `npm run check --prefix functions` 후 Firestore 규칙은 `firebase deploy --only firestore:rules --project PROJECT_ID`로 별도 적용한다. 함수만 배포할 때는 현재 codebase 이름까지 지정해 `firebase deploy --only functions:poco-assistant:assistantApi --project PROJECT_ID`를 사용한다. Firestore rules는 모든 클라이언트 직접 접근을 차단한다. 기존 프로젝트에 적용할 때 기존 rules와 통합해야 한다. Functions만 배포하는 명령은 Firestore 규칙을 적용하지 않는다.
7. Firebase CLI가 출력하는 **실제 함수 HTTPS URL**을 앱에 설정한다. URL을 추측하지 않는다. 예: `./gradlew :app:assembleDebug -PPOCO_ASSISTANT_URL=https://실제함수호스트/assistantApi`. URL은 공개 설정이며 비밀 키가 아니다. HTTP/cleartext URL은 빌드에서 거부한다.
8. (개발용, 선택) 운영자 개발환경 ADC(`gcloud auth application-default login`) 또는 최소 권한 런타임 자격증명으로 `GOOGLE_CLOUD_PROJECT=PROJECT_ID node functions/scripts/create-invitation.js`를 실행한다. 출력은 비밀 일회용 코드이므로 운영자 터미널에서만 확인하고 로그/저장소에 남기지 않는다. 차량 앱에서 주행 세션을 시작하고 P단에서 AI 연결·정보 전송 동의 후 입력한다. 코드 유효시간 30분, 1회 사용, 발급 세션 1시간. 앱 재시작 시 재연결한다. 공개 상용 배포용 계정 관리·기기 attestation 대신 공모전의 제한된 기기 프로비저닝을 위한 방식이다.
9. 실제 Pleos 기기에서 STT → 서버 → Gemini → TTS, 권한 거부, 네트워크 단절, 세션 만료, 위치 최신성, Navi 결과를 검증한다. 심사팀에 연결 코드 발급 절차를 제공한다. 개인정보 처리방침에는 전송 목적·항목·수신자·보관·국외이전 및 Gemini 운영 계정의 실제 데이터 처리 조건을 명시한다.

`assistantInvitations`, `assistantSessions`, `assistantLimits`, `assistantBootstrap` 컬렉션의 `expiresAt`에 Firestore TTL을 설정한다. TTL은 삭제 지연이 있으므로 접근 허용 판단에는 사용하지 않으며 코드가 만료시각을 직접 검사한다. 서버 운영자는 세션 문서의 `revoked=true`로 세션을 폐기할 수 있다. 서비스 중단은 함수 비활성화 또는 Gemini 키 회수로 수행한다. HTTP 진입점은 공개지만 검증된 Firebase ID 토큰 또는 유효한 초대 세션 없이는 Gemini를 호출하지 못한다. 초대 발급은 Admin IAM만 가능하다.

## 개인정보·남용 방지

- 질문 500자, JSON 8KB, 응답 300자, HTTPS만 사용. 서버는 원본 음성을 받는 필드가 없으며 알 수 없는 필드를 거부한다.
- 위치는 휴게소 질문에만 보내며 30초 이내 측정값만 허용한다. 좌표는 후보 검색에 사용하고 Gemini에는 좌표 대신 검증된 후보 이름·시설만 전달한다. 일반 상태 질문에는 위치를 보내지 않는다.
- 질문·좌표·응답·인증값을 Firestore나 애플리케이션 로그에 저장하지 않는다. 인프라 access log 및 Gemini 측 처리는 별도 운영 정책을 따른다. IP는 HMAC 처리된 한도 키만 저장한다. 초대/세션 비밀값은 SHA-256 해시만 저장한다.
- 기기당 5회/분·60회/시, 프로젝트당 500회/일, IP hash당 초대 시도 10회/시를 Firestore transaction으로 제한한다. 시간 경계에서 두 구간의 요청이 가까이 발생할 수 있는 fixed-window 정책이다. 실패한 Gemini 요청도 예산을 소비한다. Firebase maxInstances는 비용의 절대 상한이 아니므로 예산 알림과 API quota를 함께 설정한다.
- 클라이언트 단기 토큰도 비밀이며 메모리에만 보관한다. 서버 키는 Secret Manager에서 함수에만 바인딩한다. 인증은 기기 attestation을 보장하지 않는다. 공개 출시에는 운영 계정·기기 등록 체계가 필요하다.
- AI 반환은 텍스트와 검증된 후보 ID뿐이다. 차량 제어 함수를 호출하지 않는다. 창문 경고가 우선하고 AI 인식/발화가 취소된다. UI TTS 실패는 텍스트 응답을 지우지 않는다.

## 휴게소 카탈로그

정확한 정보 없이 Gemini가 장소·시설·ETA를 만들지 않도록 운영자가 이용 조건을 확인한 공공/공식 데이터를 서버에 등록한다. 현재 실제 데이터는 포함하지 않았다. 한국도로공사 데이터는 [공식 포털](https://data.ex.co.kr/)에서 확보할 수 있다. API 키가 필요한 공공 서비스나 내비게이션의 POI ID를 추측하지 않는다.

JSON 필드:

- 루트: `sourceUrl`(원본 HTTPS URL), `updatedAt`(실제 확인 날짜 ISO), `license`(재사용 근거), `stops` 배열.
- 각 장소: `id`(카탈로그 내부 식별자), `name`, `latitude`, `longitude`, `address`, `facilities`(원문에서 확인된 시설만).
- 선택적 `navigation`: `{id, subId, provider}`. **실제 Navi 제공자의 확인된 POI 매핑**만 넣는다. 공공 데이터 행 번호나 임의 카탈로그 ID를 Navi POI ID로 사용하지 않는다. 매핑이 없으면 추천은 가능하지만 경유지 버튼은 비활성화한다. 좌표만/빈 POI ID만으로 경유지를 추가하는 지원 여부가 공개 문서에서 확인되지 않았기 때문이다.

`node functions/scripts/import-rest-stops.js /absolute/path/verified-catalog.json`로 검증 후 가져온다. 최신 확인일로부터 366일이 넘으면 검색을 중단한다. 반경 50km에서 직선거리 가까운 최대 3개를 제공한다. 실제 진입 방향·운영 상태·경로·도착시간을 보장하지 않는다. 반려견 시설이 확인되지 않으면 있다고 표시하지 않는다. 이는 실시간 장소/경로 서비스가 아닌 갱신된 카탈로그 기반 주변 후보 검색이다.

경유지는 사용자 버튼 입력에만 요청한다. 기존 안내가 활성이고 기존 경유지가 없는 경우에만 `addWaypoint`를 한 번 호출한다. 실제 `getWaypointInfo` 결과의 POI·좌표 일치로 성공을 확인한다. 12초 시간초과는 결과 불명확 상태로 표시하며 자동 재요청하거나 기존 경유지를 삭제하지 않는다. 사용자는 내비게이션에서 확인한다.

## 검증 범위

`functions/test`는 입력 검증, 모델 응답 검증, 비밀/좌표 미전달, 카탈로그 미지원, 초대 1회 사용·만료·동시 요청·예산 정책을 fake fetch/transaction store로 검증한다. 실제 Gemini·Firestore transaction·Secret Manager·IAM·Functions 배포 성공을 입증하지 않는다. 클라우드 E2E는 프로젝트가 준비된 후 수행한다.

Pleos FusedLocation 1.1.3 소스는 timestampMillis의 시간 기준을 명시하지 않아 앱은 최근 Unix milliseconds로 검증 가능한 값만 수용한다. 다른 기준이면 위치 unavailable로 종료하며 현재 시각으로 덮어쓰지 않는다. 실제 서비스의 시간 기준은 기기 검증 항목이다.

## 기준 문서

- [Pleos 보안 가이드](https://document.pleos.ai/docs/pleos-only/app-registration-guide/app-security-guide), [앱 제출](https://document.pleos.ai/docs/pleos-only/distribute/app-submission-guide)
- [Pleos STT](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/intro), [FusedLocation](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/FusedLocation/intro), [NaviHelper](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/intro)
- [Functions secret 설정](https://firebase.google.com/docs/functions/config-env), [Gemini 텍스트 API](https://ai.google.dev/api/generate-content)
