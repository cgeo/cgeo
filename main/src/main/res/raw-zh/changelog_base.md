### 常规版本说明

**全屏显示 (Edge to Edge)**

由于 Play 商店政策，我们更新了此版本 c:geo 适配的 Android API 级别，并更改了一些屏幕布局程序。 可能会带来一些未知的副作用，特别是在一些新的安卓版本上。 如果您在使用此版本的 c:geo 时遇到任何问题，请在 [GitHub](https://github.com/cgeo/cgeo) 上反馈，或发送电子邮件至 [support@cgeo.org](mailto:support@cgeo.org)

**旧版地图**

正如 2025.07.17 和 2025.12.01 版本中所宣布的，我们最终移除了地图的旧版实现。 您将自动切换到新的统一地图 (UnifiedMap)，除了以下一些新功能外，您应该不会感觉到差异：
- Map rotation for OpenStreetMap based maps (online + offline)
- Cluster popup for Google Maps
- Hide map sources you don't need
- Elevation chart for routes and tracks
- Switch between lists directly from map
- "Driving mode" for OpenStreetMap based maps
- 长按轨迹/个人路线以查看更多选项

### 地图
- 新增：路线优化会缓存已计算的数据
- 新增：启用实时模式时保持当前设定目标的航点可见
- 新增：长按导航线开启海拔图 (统一地图)
- 新增：在地图上显示生成的航点
- 新增：按距离排序下载藏点
- 修复：个人路线项目重复的问题
- 新增：支持 Motorider 主题 (仅限 VTM)
- 新增：NoMap 瓦片提供商 (不显示地图，仅显示藏点等)
- 变更：历史轨迹点连接的最大距离降至 500 米 (可配置)
- 新增：允许将 KML 文件作为轨迹导入 (例如：追踪物行程)
- 新增：即使藏点尚未保存，也提供设置藏点图标的选项
- 新增：海拔图信息栏，显示剩余距离、爬升和下降高度
- 新增：在航点弹出窗口中显示航点坐标
- 修复：切换语言后，地图快速设置可能会为清空的路线配置文件显示按钮“1”/“2”
- 新增：导入轨迹时计算缺失的海拔数据 (如果已下载海拔数据)
- 修复：瓦片下载器在某些条件下停止的问题 (仅限 OpenStreetMap 在线地图)
- 新增：条件藏点标记
- 新增：显示导航提示 (箭头 + 距离)
- Change: Reduce memory requirements of map activity

### 藏点详细信息
- 新增：识别公式中的额外字符：–, ⋅, ×
- 新增：刷新藏点时保留自己记录的时间戳
- 新增：可选的罗盘微型视图 (见 设置 => 藏点详细信息 => 在藏点详情视图中显示方向)
- 新增：在“好友/自己”标签页显示宝主的记录
- 变更：“好友/自己”标签页显示该标签页内的记录计数，而非全局计数
- 变更：改进了变量和航点标签页的页眉
- 修复：显示了两个“删除记录”项目的问题
- 修复：旋转屏幕时 c:geo 在藏点详情页崩溃的问题
- 变更：“添加新航点”的布局更紧凑
- 新增：可选择以“原始”尺寸加载 geocaching.com 藏点的图片
- 新增：变量视图现在可以过滤
- 新增：在航点列表中可视化显示计算坐标的溢出
- 新增：航点列表菜单项，可将特定类型的航点标记为已访问
- 新增：追踪物记录占位符 (藏点名称、藏点编号、用户)
- 变更：移除了指向已过时的 WhereYouGo 播放器的链接。 内置 Wherigo 播放器现在是 Wherigo 的默认播放器。
- 修复：航点计算器引导模式中缺失快速切换开关的问题
- 新增：支持范围的聚合函数：add/sum, min/minimum, max/maximum, cnt/count, avg/average, multiply/product/prod
- 修复：Opencaching 平台 DNF 状态处理不正确的问题
- 新增：与在线记录合并后删除离线记录
- 新增：删除带有离线记录的藏点时显示确认提示
- 新增：从“全部”列表中删除所有藏点时显示确认提示
- 新增：允许在用户自定义藏点的描述文本中使用 Markdown 格式
- 变更：在添加用户图片前先保存藏点
- 修复：加载直接嵌入在描述文本中的图片时崩溃的问题
- 新增：在记录视图中显示自己的收藏点 (Geocaching.com + 离线记录)
- 新增：发送记录现在在后台完成
- Fix: Inventory hidden on logging under certain conditions
- New: Averaging of coordinates on creating waypoint / setting coordinates for user-defined caches

### Wherigo 播放器
- 新增：Wherigo 离线翻译
- 新增：改进了按钮处理
- 新增：状态自动保存
- 新增：可在手机主屏幕创建 Wherigo 播放器快捷方式的选项
- Fix: Missing/wrong media files lead to error

### 通用
- 新增：记录藏点后的分享选项
- 变更：不再为自己的藏点显示“需要维护”或“需要归档”选项
- 修复：恢复备份可能会导致内部存储和后续备份中出现重复的轨迹文件
- 变更：移除了对 Twitter 的引用
- 新增：在清理和恢复备份时删除孤立的轨迹文件
- 新增：尝试向书签列表添加过多藏点时发出警告
- 新增：关注/取消关注列表功能
- 新增：提供使用 Google 翻译或 DeepL 应用进行离线翻译的选项 (如果已安装)
- 新增：删除搜索历史记录项
- 变更：移除 GCVote (服务已停止)
- 新增：藏点详情页面的彩色工具栏
- 新增：可选择多个书签列表/口袋查询 (PQ) 进行下载
- 新增：预览书签列表
- 变更：将最低 Android 版本要求提高到 Android 8
- 新增：为新安装提供默认快速按钮
- 修复：范围输入对话框中的标题被截断的问题
- 修复：每夜版更新通知指向常规 APK，即使是 FOSS 变体版也是如此
- 新增：日期过滤器的“忽略年份”选项
- 新增：使待下载列表中的远程 URI 可点击
- 变更：新安装默认使用系统设置主题
- 新增：GPX 导出：导出原始航点时写入 GSAK Lat/LonBeforeCorrect 注释
- 新增：从地图上的列表中删除藏点时显示撤销栏
- 修复：收藏点百分比过滤器崩溃的问题
- 新增：使简单列表更容易作为父列表使用
- 变更：日历条目使用本地时区 (设备的，而非 Event 的)，而非 UTC
- 修复：部分文本忽略语言切换的问题
- 修复：新安装时“使用英制设置”未正确初始化的问题
- 变更：使用 Bergamot 开源离线翻译模块替换闭源的 Google ML Kit 翻译器
- 变更：新的表情符号选择器
