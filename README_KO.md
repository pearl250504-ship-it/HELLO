# Little Companion (Android)

업로드된 캐릭터 그림을 화면 위에 띄우는 Android 네이티브 프로젝트입니다.

## 현재 포함된 기능
- 다른 앱 위에 표시하는 오버레이 캐릭터
- 캐릭터를 손가락으로 드래그해 이동
- 화면 안에서 천천히 자동 이동
- 앱에서 표시 켜기/끄기
- 캐릭터 크기 조절 및 위치 초기화
- 실행 중 알림에서 캐릭터 끄기

## APK 빌드 방법
이 실행 환경에는 Android SDK/Gradle 빌드 도구가 없어 서명된 APK를 직접 컴파일하지 못했습니다.
대신 Android Studio에서 열 수 있는 프로젝트와 GitHub Actions 빌드 워크플로를 넣었습니다.

### Android Studio
1. ZIP 압축을 풉니다.
2. Android Studio에서 `little_companion` 폴더를 엽니다.
3. Gradle 동기화를 마칩니다.
4. Build > Build Bundle(s) / APK(s) > Build APK(s)를 선택합니다.
5. APK는 `app/build/outputs/apk/debug/app-debug.apk`에 생성됩니다.

### GitHub Actions
1. 새 GitHub 저장소를 만들고 이 프로젝트 파일을 올립니다.
2. Actions 탭에서 `Build Android APK`를 실행합니다.
3. 완료된 실행 항목의 Artifacts에서 `Little-Companion-debug-apk`를 내려받습니다.

## 설치 및 첫 실행
1. APK를 Android 기기에 설치합니다.
2. 앱을 열고 '다른 앱 위에 표시 권한 설정'을 누릅니다.
3. 설정에서 Little Companion의 '다른 앱 위에 표시 허용'을 켭니다.
4. 앱으로 돌아와 '캐릭터 표시 켜기'를 누릅니다.

Android 버전과 제조사 설정에 따라 배터리 최적화/백그라운드 실행 제한이 동작에 영향을 줄 수 있습니다.
이 프로젝트는 첫 프로토타입이며, 잠금 화면/재부팅 후 자동 시작은 아직 포함하지 않았습니다.
