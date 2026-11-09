# ieo-mobile

이에오 차주용 **휴대폰 앱** (Android, Kotlin/Compose). 차량 앱(`ieo-app/android`)과 같은 서버(`ieo-server`)를 쓴다.

## 1차 범위

| 기능 | API |
|---|---|
| 시작·번호·6칸 인증번호·약관·기본 정보·가입 완료, 이미 가입된 계정 창 (차주만, 시안 M01·V01~V07·V09) | `POST /api/auth/phone/code`, `/phone/verify`, `/phone/login`, `/signup` |
| 차량 QR 승인 (카메라, 기본 카메라 딥링크 `ieo://car-login?code=`, 코드 직접 입력) | `GET /api/car-login/requests/{code}`, `POST .../approve`, `.../reject` |
| 내 차량 추가·삭제·기본 지정 | `/api/vehicles` |
| 내 정보: 이름 바꾸기, 로그아웃, 탈퇴 (시안 M11) | `PATCH /api/users/me`, `POST /api/users/me/withdraw` |
| 로그인된 차량·원격 로그아웃, 동의 관리, 사업자 정보 | `/api/users/me/car-sessions`, `/consents`, `/business` |

| 오더 찾기, 상세, 배차 요청·철회, 신고 (M08·M09) | `/api/orders`, `/api/dispatch-requests` |
| 내 배차, 다른 앱 배차 등록(4단계), 취소 | `/api/trips` |
| 수익: 월 순수익, 6개월 그래프, 날짜별 장부, 못 받은 돈, 받음 표시, 운임 고치기, 경비·운행 추가 (M10) | `/api/ledger`, `/api/expenses` |

화면 기준은 [UX.md](UX.md)를 따른다. 운행 진행(출발·도착)은 차량 앱이 맡는다.

디자인은 아티팩트 "Ieo 홈 목록 시안"의 모바일 앱 페이지를 따른다. 구글 로그인(버튼만 있고 눌러도 동작하지 않는다)은 앱용 키가 준비되면 더한다. 약관 전문은 출시 전에 넣는다.

## 구조

`core`(서버 연결·로그인 유지, 차량 앱에서 복사) → `domain`(모델, 저장소 인터페이스) → `data`(서버 구현) → `ui`(화면·ViewModel), `di/AppContainer`에서 손으로 연결한다.

## 빌드와 시험

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

- `./gradlew testDebugUnitTest`는 휴대폰 크기(390×844) 화면도 `app/build/screens`에 PNG로 그린다. 폰 없이 화면을 볼 수 있다.
- 디버그 빌드의 서버 주소는 에뮬레이터에서 본 내 컴퓨터(`http://10.0.2.2:8080/`)다.
- USB로 연결한 폰은 `adb reverse tcp:8080 tcp:8080` 뒤 `-PieoDebugApiBaseUrl=http://localhost:8080/`로 빌드한다.
- 배포 빌드 주소는 `-PieoApiBaseUrl=https://.../`로 넘긴다.
