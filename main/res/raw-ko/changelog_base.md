### 일반
- 변경: c:geo에서 가장 많이 사용되는 화면에 직접 액세스할 수 있는 하단 탐색 도입, 이전 메인 화면 교체
- 현재 안드로이드 사양으로 설정을 리팩터링했습니다(진행 중입니다. 잠시만 기다려 주십시오)

### 지도
- 수정: 여러 트랙이 포함된 GPX 파일을 로드할 때 별도의 연결되지 않은 트랙으로 표시됨
- 변경: GPX 트랙 파일을 로드할 때 자동으로 트랙 표시 활성화

### 캐시 목록
- 신규: 다음 20개 캐시를 선택하는 옵션
- 신규: 속성 개요(캐시 관리 => 속성 개요 참조)

### 캐시 상세정보
- 신규: 현재 캐시 좌표를 지오체커에 제공 (지오체커에서 지원하는 경우)

### 추가 사항
- New: Quick-load geocodes from clipboard text in mainscreen search
- New: Added support for 5 log templates
- New: Make Settings => View Settings filterable
- New: Added GC Wizard to useful apps list
- Change: Removed barcode scanner from useful apps list and from mainscreen
- Change: Removed BRouter from useful apps list (you can still use both external and internal navigation)
- Fix: Avoid repeated update checks for maps/routing tiles with interval=0
- Fix: Optimize support to autofill passwords from external password store apps in settings
