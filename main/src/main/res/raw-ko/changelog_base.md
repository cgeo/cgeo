### 통합지도
내부적으로 "통합지도"이라고 불리는 완전히 새로운 지도 구현에 오신 것을 환영합니다. 이는 완전히 새로운 지도 구현을 위해 c:geo 팀이 거의 2년 동안 작업한 결과입니다. 우리가 이것을 시작한 이유는 일부 코드 부분이 10년이상 되었기 때문에 기존 지도 구현을 유지 관리하고 기능별로 동기화를 유지하기가 점점 더 어려워졌기 때문입니다.

통합지도를 사용하여 우리는 내부 아키텍처를 현대화하고 통합하는 동시에 모든 다양한 지도 유형에서 동일한 사용자 경험을 얻으려고 노력했습니다.

통합지도는 기본적으로 기존 지도 구현의 모든 기능을 (거의) 제공하지만 몇 가지 추가 기능도 제공합니다.

- OpenStreetMap 기반 지도의 지도 회전(온라인 및 오프라인)
- OpenStreetMap 기반 지도에 대한 부분 축척
- Google 지도에 대한 클러스터 팝업
- 필요 없는 지도 원본 숨기기
- 노선 및 경로 입면도(노선 시 누름)

통합지도는 현재 베타 상태에 도달하여 모든 야간 사용자를 위한 기본 지도로 만들기로 결정했습니다.

모든 것이 작동해야 하지만 여전히 버그가 있을 수 있습니다. 필요한 경우 이전 지도 구현과 새 지도 구현을 전환할 수 있습니다(설정 - 지도 소스 참조). 하지만 새 지도 구현을 시도해 보시기 바랍니다. 지원([support@cgeo.org ](mailto:support@cgeo.org)) 또는 [c:geo on GitHub](github.com/cgeo/cgeo/issues)에서 발견된 버그를 보고해 주세요. 모든 피드백을 환영합니다!

---

더 많은 변경 사항:

### 지도
- 신규: 다운로드 관리자에서 기존 다운로드 강조 표시
- 신규: 지점 아이콘에 캐시 찾음 상태 표시
- 신규: 캐시와 지점을 선으로 연결하는 길게 누르기 옵션
- Change: Show cache/waypoint details in a non-blocking way

### 캐시 상세정보
- 변경: "전환 말하기"는 이제 실제 전환입니다.
- Change: Increased maximum log length for geocaching.com
- Fix: Cannot upload longer personal notes on opencaching sites

### 일반
- 신규: 다운로더 알림을 누르면 "대기 중인 다운로드" 보기가 열립니다.
- 변경: 배경화면을 배경으로 사용하는 데 더 이상 READ_EXTERNAL_STORAGE 권한이 필요하지 않습니다.
- New: Two column-layout for settings in landscape mode
