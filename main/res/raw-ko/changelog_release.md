### Advanced filtering system
- Introducing a new filtering system to c:geo, supporting flexible, combinable and storable filters
- Available in both cache lists and map view
- New "Search by filter" function

### Map
- New: On creating a user-defined cache while displaying a map from a list: Offer user to store new cache in current list (instead of default list for user-defined caches)
- New: Separate "own" and "found" filters in map quick settings
- Change: Additionally show cache name in poup details

### Cache details
- New: Make use of google translate in-app translation popup
- New: Allow changing the assigned icon in cache details popup via long click (stored caches only)

### Downloader
- Change: Downloads will now completely happen in background, a notification is shown
- Change: Files downloaded successfully will automatically overwrite existing files having the same name
- Change: If a map requires a certain theme which is not installed yet, c:geo will automatically download and install that theme as well

### Other
- Change: We've completely reworked the internal technical aspects c:geo theming to be able to make use of some more modern components provided by Android. This will have a couple of side-effects, some of them unintended. Please report any errors or glitches either on our [GitHub page](https://www.github.com/cgeo/cgeo/issues) or by contacting support.
- New: Support day / night mode from system (optional)
- New: Download bookmark lists from geocaching.com - see "Lists / pocket queries" in main menu
- New: Ignore capability for geocaching.su
- Change: Removed no longer maintained RMAPS navigation app
- 수정: 개인 메모에서 이름은 같지만 좌표가 다른 지점 추출
- 수정: 공식을 사용하여 지점에 대한 사용자 메모를 추출하는 버그
- 수정: 완료된 공식의 좌표 대신 공식을 PN으로 내보내기
- 수정: 백업 재설치 및 복원 후 오프라인지도 및 테마 폴더가 잘못됨
- 수정: 트랙/경로를 업데이트 할 수 없었음
- 수정: 라이트 테마의 다운로더에 대한 테마 오류
