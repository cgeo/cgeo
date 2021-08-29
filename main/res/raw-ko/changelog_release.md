## Bugfix Release

### Design
- Increase font size for text input fields
- Increase font size for some compass elements
- Use font color with higher contrast in waypoint tab
- Make quick offline log check mark visible again
- Increase font size for coordinate input fields
- Respect system font size settings also on older Android versions (5,6 and 7)

### 캐시 상세정보
- Fix missing cache title if cache opened via geocode or link (website change)
- Fix missing cache description on some caches

### 추가 사항
- Show premium caches again in search results of basic members
- Fix further creation of user defined caches if some user defines caches have been loaded via GPX
- Use more common English abbreviation for traditional cache in cache type filter

## Feature Release 2021.08.15:

### 고급 필터링 시스템
- 유연하고 결합 가능하며 저장 가능한 필터를 지원하는 새로운 필터링 시스템을 c:geo에 소개합니다.
- 캐시 목록과 지도보기 모두에서 사용 가능
- 새로운 "필터로 검색" 기능

### 지도
- 신규: 목록에서 맵을 표시하는 동안 사용자 정의 캐시 생성시: 사용자가 현재 목록에 새 캐시를 저장하도록 제안 (사용자 정의 캐시의 기본 목록 대신)
- 신규: 지도 빠른 설정에서 "소유"및 "찾음" 필터 분리
- 변경: 팝업 세부 정보에 캐시 이름 추가

### 캐시 상세정보
- 신규: 구글 번역 인앱 번역 팝업 사용
- 신규: 긴 클릭을 통해 캐시 세부 사항 팝업에서 할당 된 아이콘 변경 허용 (저장된 캐시 만 해당)

### 다운로더
- 변경: 이제 다운로드가 백그라운드에서 완전히 이루어지며 알림이 표시됩니다.
- 변경: 성공적으로 다운로드된 파일은 동일한 이름을 가진 기존 파일을 자동으로 덮어 씁니다.
- 변경: 지도에 아직 설치되지 않은 특정 테마가 필요한 경우 c:geo는 해당 테마도 자동으로 다운로드하여 설치합니다.

### 추가 사항
- 변경: Android에서 제공하는 최신 구성 요소를 사용할 수 있도록 내부 기술 측면 c:geo 테마를 완전히 재 작업했습니다. 이것은 몇 가지 부작용이 있을 것이며 일부는 의도하지 않은 것입니다. [GitHub 페이지](https://www.github.com/cgeo/cgeo/issues) 또는 지원팀에 문의하여 오류나 결함을 신고 해주세요.
- 신규: 시스템에서 주간 / 야간 모드 지원 (옵션)
- 신규: geocaching.com에서 북마크 목록 다운로드 - 메인 메뉴의 "목록/포켓 쿼리" 참조
- 신규: geocaching.su에 대한 기능 무시
- 변경: 더 이상 유지 관리되지 않는 RMAPS 탐색 앱 제거
- 수정: 개인 메모에서 이름은 같지만 좌표가 다른 지점 추출
- 수정: 공식을 사용하여 지점에 대한 사용자 메모를 추출하는 버그
- 수정: 완료된 공식의 좌표 대신 공식을 PN으로 내보내기
- 수정: 백업 재설치 및 복원 후 오프라인지도 및 테마 폴더가 잘못됨
- 수정: 트랙/경로를 업데이트 할 수 없었음
- 수정: 라이트 테마의 다운로더에 대한 테마 오류
