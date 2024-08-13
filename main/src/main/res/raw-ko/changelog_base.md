### 지도
- 신규: 캐시 정보 시트에서 "개인 메모 편집"
- 수정: 단일 캐시 매핑 시 자점 필터링되지 않음(Unified Map)
- 신규: 사용자 정의 타일 공급자 지원
- 수정: 설정 대화상자 여닫기 후 지도 데이터 새로 고침(Unified Map)
- 신규: 건물 2D/3D(Unified Map OSM 지도) 토글 표시
- 신규: 팝업에서 캐시 저장소/새로고침이 백그라운드로 이동됨
- 변경: 좌표 검색: 현재 위치가 아닌 목표까지 방향 및 거리 표시
- New: Graphical D/T indicator in cache info sheet
- Fix: Compass rose hidden when filterbar is visible (UnifiedMap)

### 캐시 상세정보
- 신규: 이미지 탭의 "개인 메모"에 링크된 이미지 표시
- 변경: 캐시 세부정보 및 추적아이템 세부정보에서 길게 누르는 작업을 단순화합니다.
- 신규: 로그 이미지의 원활한 스케일링
- 변경: "로그" 아이콘을 연필에서 스마일 아이콘으로 변경
- 변경: "목록 편집" 아이콘을 연필에서 목록 및 연필로 변경
- Fix: vanity function failing on long strings
- Fix: Wrong parsing priority in formula backup
- Change: Allow larger integer ranges in formulas (disallow usage of negation)
- New: Allow user-stored cache images on creating/editing log
- Fix: Spoiler images no longer being loaded (website change)

### 일반
- 신규: 어드벤처 랩의 발견 상태를 수동 또는 자동으로 설정하도록 전환합니다.
- 신규: 목록 선택 대화 상자: 이름에 ":"가 포함된 캐시 목록 자동 그룹화
- 변경: OSM Nominatum을 보조 지오코더로 사용하고, MapQuest 지오코더를 대체합니다(더 이상 작동하지 않음)
- 변경: 통합 브라우저를 v1.7.5로 업데이트
- 신규: 가져오기 시 트랙에서 표고 정보 읽기
- 신규: API to Locus에서 캐시 크기 "가상" 지원
- 수정: 대상 위치와의 거리로 더 이상 정렬되지 않은 위치에 대한 검색 결과
- 신규: "정정된 좌표" 필터
- Change: Updated targetSDK to 34 to comply with upcoming Play Store requirements
- New: Added "none"-entry to selection of routing profiles
- Change: Improve description for "maintenance" function (remove orphaned data)
- New: Show warnings when HTTP error 429 occurs (Too many requests)
- Fix: Flickering on cache list refresh
- New: Allow display of passwords in connector configuration
- Fix: Search for geokretys no longer working when using trackingcodes
