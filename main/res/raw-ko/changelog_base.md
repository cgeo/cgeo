### 캐시 상세정보

- 신규: 재설계된 좌표 계산기 (지원 기능)
- 변경: 지점 계산을 위한 변수가 이제 캐시 전역입니다.
- 신규: 캐시 세부정보의 변수 탭
- 신규: 범위가 있는 공식 및 변수를 사용하여 지점 생성
- 신규: 오프라인 로그용 로그 템플릿
- 신규: 로그 템플릿 메뉴에 \[location\] 추가
- 신규: 로그 텍스트 선택 허용
- 수정: Android 12의 특정 조건에서 루프로 이어지는 GC 체커 링크
- 신규: 설명 텍스트 끝에 지오체커 버튼 추가 (해당되는 경우)
- 신규: 캐시 메뉴에 '브라우저 로그인' 옵션 추가

### 캐시 목록

- 신규: 고급 상태 필터에 "사용자 정의 지점 있음" 옵션 추가
- New: Allow inclusion of caches without D/T in filter
- 수정: 거리 정렬 순서에 따라 모든 위치 변경 시 캐시 목록 재정렬

### 지도

- New: Map theming for Google Maps
- New: Map scaling options for OpenStreetMap (see theme options)
- 변경: 설정 => 지도 => 지도를 길게 누르면 이제 캐시 지도에서도 길게 누름이 활성화/비활성화됩니다(현재 캐시에 대한 새 지점 생성과 관련됨).
- 변경: 영구보관된 캐시에 대한 거리 원을 표시하지 않음
- 수정: 특정 조건에서 OpenStreetMap 지도의 충돌
- Fix: Routing becoming unresponsive when many routing tiles are installed

### 일반

- 신규: 자동으로 백업 수행(선택 사항)
- 수정: 완료된 다운로드 가져오기 재개
- 신규: 구성 가능한 빠른 실행 버튼을 홈 화면에 추가했습니다. 설정 => 외관 디자인
- 신규: BRouter v1.6.3으로 내부 라우팅 업데이트
- New: Limit the need of repetitive back key usage by starting a new activity stack when changing to another part of the app
- New: Add setting to decrypt the cache hint by default (instead of only when tapping on it)