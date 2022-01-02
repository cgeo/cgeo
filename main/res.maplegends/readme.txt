## Alternative resource folders for map theme legend icons

- each map theme gets its own subfolder
  - elevate for OpenAndroMaps Elevate theme
  - fzk for Freizeitkarte base theme
  - fzk-outdoor for Freizeitkarte outdoor themes

- file naming conventions
  - generally use filename from server
  - replace all "-" by "_" (as "-" are not allowed in resource names)
  - add prefix to all files, determined by source:
    - "elevate_" for "elevate"/"elements" themes
    - "fzk_" for "freizeitkarte" theme
    - "fzk_outdoor_" for "freizeitkarte outdoor contrast / soft" themes

- sources
  - many symbols are loaded from the theme files (esp. all POI symbols),
  - others are downloaded from the online legends (webpage),
    with permissions from the respective owners)
    - https://www.openandromaps.org/kartenlegende/elevation-hike-theme
    - https://www.freizeitkarte-osm.de/android/en/legende-freizeitkarte.html
    - https://www.freizeitkarte-osm.de/android/en/legende-outdoor-contrast.html
