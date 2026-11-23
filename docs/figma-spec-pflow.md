# 户型流程（HF）Figma 实现规格

> 来源：Figma 画布 `"UI设计（v2)" #2302:39362`（MCP dump 解析）。本文档只覆盖户型流程画板：HF00 与 HF / P01–P19。
> 文中坐标一律为「相对父级」px；HF 画板整体为 0.932 缩放稿（894.72 ≈ 960×0.932），**实现时建议按 1.2 节换算成设计基准值**（如 16.776→18、22.368→24），或直接按实测值 1:1 使用。
> 设计说明（GLOBAL_VARS）：8px 网格 · 主间距 24 · 控件高 44（本稿实测，原生 target 48）· 按钮胶囊圆角 28（实测 19.572）；主文字 #292722、次文字 #59554D、底色 #FAF8F2、强调 #F3BC54；中文 Noto Sans SC 32/24/20/18/16 行高 150%；语气简洁生活化，不用 emoji、感叹号。

---

## 1. HF00 全局 tokens（#2316:628 + GLOBAL_VARS）

### 1.1 颜色 tokens（解引用后）

| 引用 ref | 值 | 用途 |
|---|---|---|
| fill_ea14ac0d | `#292722` | 主文字、房间名、尺寸标注 |
| fill_97d170e1 | `#59554D` | 次文字 |
| fill_1541ea47 | `#FAF8F2` | 输入框值区底色、画板底色 |
| fill_9d686c3f | `#29211A` | P01 标题/正文（深棕） |
| fill_f7814091 | `#292621` | toast、弹窗标题/正文 |
| fill_a4af3634 | `#382B1A` | 按钮文字、图标色 |
| fill_658ab2fa | `#FFFFFF` | 导航 Secondary 按钮文字 |
| fill_4d6d13ef | `#785510` | 画布选中对象高亮（墙/门/窗） |
| fill_c23f1518 | `#6E6E6E` | 墙体默认色 |
| fill_7fb5e2d0 | `#9D978C` | 输入框描边（弱） |
| fill_8bd4187f | `#B8B8B8` | 辅助灰 |
| fill_fef61db7 | `#D9D9D9` | 窗块底色 |
| fill_02f5f8b8 | `#292929` | 窗块描边 |
| fill_31d0491e | `#F7F7F7` | 门块底色 |
| fill_dd1d6abd | `#F8B856` | 备用琥珀 |
| fill_4009dbd8 | `rgba(255,191,74,0.9)` | **Primary 按钮底**（≈#FFBF4A@90%，设计注记为 #F3BC54） |
| fill_ef0f56d0 | `rgba(255,250,240,0.2)` | **Secondary 按钮底**（玻璃白 20%） |
| fill_80687602 | `#FFFFFF` | 按钮描边 |
| fill_1196e503 | `rgba(255,250,237,0.68)` | 画布面板底 |
| fill_164b936d | `rgba(255,253,245,0.98)` | 就近工具面板 / 弹窗底 |
| fill_026f1936 | `rgba(89,85,77,0.4)` | 弹窗描边 |
| （内联） | `rgba(255,250,237,0.58)` | 主工作窗口 Glass 底 |
| （内联） | `rgba(255,250,237,0.56)` | 品牌标题衬底 |
| （内联） | `rgba(255,255,255,0.65)` | Glass / 衬底描边 |
| fill_34dc0314 / fill_b4c0e48c | `#000000` / `transparent` | — |

### 1.2 字体 tokens（Noto Sans SC；行高≈1.5）

| style ref | 规格 | 用途 |
|---|---|---|
| style_5130f734 | Bold 22.368 左 | P03/P12 大标题（基准 24） |
| style_30918599 | Bold 22.368 居中 | P12/P14 居中标题 |
| style_25ac21be | Regular 19.572 左 | 弹窗标题（基准 21） |
| style_584302b9 | Medium 16.776 左 | 面板标题/正文（基准 18） |
| style_a1f6d880 | Medium 16.776 居中 | 窗口标题、按钮文字 |
| style_25f0aad0 | Regular 13.98 左 | 输入值、正文（基准 15） |
| style_2417e783 | Regular 13.98 左 | 弹窗正文 |
| style_011a18a5 | Medium 12.582 居中 | 小按钮文字（基准 13.5→13） |
| style_d4a8cfb4 | Medium 12.582 左 | 画布房间名、提示行 |
| style_8db31afb | Regular 11.184 左 | 输入标签、小注（基准 12） |
| style_507f00ea | Regular 11.184 | toast |
| style_3397f92f | SF Pro Display Medium 24.03 | HF00 品牌字 |
| style_67ab9ecb | Darker Grotesque Bold 40.19，字距 -0.02em | HF00 标注大数字 |
| 基准（GLOBAL） | title 32 Bold / body 20 / label 18 / note 16 | 设计基准（未缩放） |

### 1.3 效果 tokens

| ref | 定义 | 用于 |
|---|---|---|
| effect_46dd438c | 投影 0 / 8.39 / 22.37 `rgba(31,20,8,0.2)` + 背景模糊 15.38 | Glass、标题衬底、画布、就近工具面板、弹窗 |
| effect_f2ef19db | 内投影 4.79/4.79/7.19 `rgba(255,255,255,0.4)` + 内投影 -4.79/-4.79/7.19 `rgba(255,255,255,0.25)` + 背景模糊 13.98 | 所有按钮 |
| effect_e938cfd3 | 内投影 5.59/5.59/25.16 `rgba(255,255,255,0.3)` + 内投影 -5.59/-5.59/15.38 `rgba(255,255,255,0.2)` + 背景模糊 22.37 | HF00 大面板（备用） |

### 1.4 按钮四态（HF00 实测）

按钮通用：高 **44**；胶囊圆角 **19.572**（≈28 设计值 ×0.7 参考）；row 布局；带图标时 padding 左 27.96 / 右 5.59、图标-文字 gap 11.18、图标 16.78×16.78；**宽度随文字自适应**（实测 121.4–150.98）。文字 Medium 16.776（大按钮）或 12.582（导航/面板小按钮）。

| 态 | 底色 | 描边 | 文字/图标 | 其它 |
|---|---|---|---|---|
| Primary（fill_4009dbd8） | `rgba(255,191,74,0.9)` | 无 | `#382B1A` | 默认主操作 |
| Secondary / Default（fill_ef0f56d0） | `rgba(255,250,240,0.2)` | `#FFFFFF` 1px | `#382B1A`（导航内 `#FFFFFF`） | effect_f2ef19db |
| Disabled | 同 Secondary | 同 Secondary | 同色，整体 **opacity 0.45** | 图标变体 house-add Disabled |
| Danger | 同 Secondary | 同 Secondary | 同色 | 固定 Delete 图标（无红色） |
| Selected（设计中无独立实例） | — | — | — | 建议在 Secondary 上加琥珀描边/底色表达 |

### 1.5 其他组件规格

- **IH/Input/Labeled numeric**：125.82×67.1；标签 Regular 11.184 `#292722`（顶部）；值区 125.82×39.14，底 `#FAF8F2`、描边 `#9D978C` 1px、圆角 8.388，值 Regular 13.98 左对齐（≈11 内边距）。
- **IH/PlanHandle（48px hit area）**：白色圆形手柄，命中区 48（0.7 缩放实测 33.55×33.55），中心对齐选中对象锚点，成对出现在选中对象两端。
- **就近工具面板**：宽 454.35；高 146.79（标题+说明+1 行按钮）或 190.13（含输入行）；底 `rgba(255,253,245,0.98)`、圆角 16.776、effect_46dd438c；内容 padding 16.78；按钮行 y=97.86（146.79 高）或 y=134.21（190.13 高），按钮 125.82×44，横向位置 x=16.78 / 153.08 / 289.39（间距 10.48）；输入行 y=44.74，x=16.78 / 156.58 / 296.38。
- **确认弹窗（EL-03cc084f）**：503.28×209.7，位于窗口内 (195.72,195.72)；底 `rgba(255,253,245,0.98)`、描边 `rgba(89,85,77,0.4)` 1px、圆角 16.776、投影+模糊；标题 Regular 19.572 `#292621`；正文 Regular 13.98；按钮行 y=148.19：Secondary 145.39×44 @ (173.35,148.19) + Primary 150.98×44 @ (329.93,148.19)，按钮内可带 16.78 箭头图标。
- **toast**：Regular 11.184 `#292621`，窗口内左下 @ (44.74,508.87)，宽度自适应（实测 279.6×13）。
- **画布元素**（P02 起，827.62×405.42 面板内）：
  - 墙：`#6E6E6E`，厚 5.59；外横墙 489.3×5.59、右竖墙 5.59×257.23、内竖墙 5.59×160.77、内横墙 223.68×5.59。
  - 房间名/尺寸标注：Medium 12.582 `#292722`（"客厅/卧室/厨房"、"6.00 m"、"4.00 m"）。
  - 窗（未选中）：97.86×8.39，底 `#D9D9D9`、描边 `#292929`、圆角 16.776。
  - 门（未选中）：53.12×53.12，底 `#F7F7F7`、描边 `#6E6E6E`（含开门弧示意）。
  - 选中态：对象变 `#785510` + 两端 PlanHandle。
- **底部导航 pills**：145.39×44，间距 11.19；左=我的户型（Primary）、右=选家具（Secondary）；图标 16.78 在文字左侧（Home / Couch）。

### 1.6 缩放换算（0.932 → 基准）

实测 → 建议值：22.368→24、19.572→21、16.776→18、13.98→15、12.582→13（或 13.5）、11.184→12、8.388→9、33.55→36、44（不变）、52×49 LOGO→56×52。窗口整体 894.72×671.04（≈960×720 基准）。

---

## 2. 共用窗口骨架（P01–P19）

画板：1280×832，背景 IMAGE `imageRef=0648fb4c33ade0754b35c0d7a30711e0f0b8fb52`（scaleMode=COVER）。

窗口 `Window / 850px reference scale`：**894.72×671.04 @ (192.64, 72)**（画板内居中）。

从上到下：

1. **品牌标题衬底** 849.98×50.33 @ (22.37,11.18)：底 `rgba(255,250,237,0.56)`、描边 `rgba(255,255,255,0.65)` 0.7px、圆角 16.776、effect_46dd438c。内含 LOGO 图 52×49 @ (10.99,1.82)（`imageRef=059e49ff-a5b7-4d23-ba08-71e5c53b36e0`）与窗口标题文本 "Immerse Home  /  {页面名}"，643.08×25 @ (125.82,22.37)，Medium 16.776 `#292722`。
2. **主工作窗口 Glass** 849.98×486.5 @ (22.37,67.1)：底 `rgba(255,250,237,0.58)`、同描边、圆角 16.776、effect_46dd438c。承载：画布面板（827.62×405.42 @ (33.55,78.29)，底 `rgba(255,250,237,0.68)` 圆角 11.184）、就近工具面板、流程提示、居中状态列。
3. **底部操作条**（Window 内 y=503.28）：上传户型图 Secondary 146.79×44 @ (522.85,503.28)（图标 File upload @ (534.04,514.46)）；生成模拟房间 Primary 146.79×44 @ (682.22,503.28)（图标 house-add @ (696.36,516)；禁用时 opacity 0.45）。
4. **底部导航**（Window 底 y≈591–594，或画板级 y=664.75）：我的户型 Primary 145.39×44 + 选家具 Secondary 145.39×44，图标 Home / Couch 16.78。
5. **就近工具面板**（可无）：标准位 @ (216.69,274.01)（高 190.13）或 @ (216.69,307.56)（高 146.79）。
6. **模态弹窗**（P15–P19）：居中 (195.72,195.72)，503.28×209.7。
7. **toast**（可无）：左下 (44.74,508.87)。

各页窗口标题文案（衬底文本 "Immerse Home  /  ×××"）：
P01 上传你的户型 · P02 选择户型来源 · P03 正在处理户型 · P04 校准真实尺寸 · P05 直接在图上编辑 · P06 编辑墙体 · P07 添加与连接墙 · P08 编辑门 · P09 编辑窗 · P10 检查户型问题 · P11 删除与撤销 · P12 生成模拟房间 · P13 模拟房间已就绪 · P14 异常与恢复 · P15–P19 直接在图上编辑（弹窗态复用编辑页）。

**全局交互规则**
- 「生成模拟房间」禁用条件：P01（未上传）、P04（校准未完成）、P10（存在问题未修正）；P02/P05/P06/P08/P09/P15–P19 启用；P03/P12/P13/P14 无底部操作条（P13 仅导航）。
- toast 出现：P05「已保存到本机」（保存成功）；P15/P16/P18「本机会话草稿」；P17「尚未保存，请重试」；P19「本机会话草稿」。
- 弹窗统一双按钮：左 Secondary（取消向）/ 右 Primary（继续向）；文案见各页。
- 门宽合法区间 **0.10–2.40 m**（P19）；墙默认厚 0.20 / 高 2.70（P06 示例值）。

---

## 3. 逐页规格

### P01 上传你的户型 #2316:656
- 结构：衬底标题 → 左侧上传面板（335.52×251.64 @ (67.1,145.39)，底 `rgba(255,250,237,0.68)`、描边 `rgba(255,255,255,0.65)` 0.7px、圆角 19.572、effect_46dd438c，IMAGE 空态图）→ 右侧文案区 → 底部流程提示 → 底部操作条 → 导航。
- 文案（逐字）：
  - "让家的轮廓，
变成立体空间"（Bold 25.164 `#29211A`，363.48×76 @ (447.36,145.39)）
  - "上传户型图，在图上调整墙、门和窗。
确认尺寸后，生成可以进入的模拟房间。"（Regular 13.98，363.48×42 @ (447.36,240.46)）
  - "支持 JPG / PNG · 建议使用清晰的正视平面图"（Regular 11.184，363.48×17 @ (447.36,307.56)）
  - "上传平面图    →    图上编辑    →    生成房间"（Medium 13.98，760.51×27.96 @ (67.1,419.4)）
  - 按钮："上传户型图"（Secondary）、"生成模拟房间"（**Disabled**，opacity 0.45）
- 图标：File upload（上传按钮）、house-add Disabled（生成按钮，opacity 0.45）。
- 交互：点击上传进入 P02（选择来源）；无画布。

### P02 选择户型来源 #2316:679
- 结构：衬底标题 → 画布面板（含示例户型：6 面墙 `#6E6E6E`、房间名 客厅/卧室/厨房、尺寸 "6.00 m"/"4.00 m"、窗块、门块）→ 就近工具面板「上传户型图」→ 底部操作条（两按钮均启用）→ 导航。
- 面板（454.35×146.79 @ (216.69,307.56)）文案：
  - "上传户型图"（Medium 16.776）
  - "选择清晰的 JPG / PNG；相机不可用时仍可选文件。"（Medium 12.582）
  - 按钮：选择文件 @ (16.78,97.86)、拍照上传 @ (153.08,97.86)、使用示例 @ (289.39,97.86)（均 Secondary 125.82×44）
- 图标：File upload ×2（前两个按钮内）。
- 交互：选择文件/拍照 → P03；使用示例 → 跳过处理直接 P05。

### P03 正在处理户型 #2316:729
- 结构：衬底标题 → 居中状态列「处理中」@ (111.84,174.75) 宽 671.04（相对 Window）→ 导航（**无画布、无底部操作条**）。
- 文案：
  - "正在提取墙体与门窗…"（Bold 22.368）
  - "上传完成 → 提取轮廓 → 检查开口
请稍候，完成后可以直接在图上修正。"（Medium 16.776）
  - 按钮 "取消处理"（Secondary 146.79×44，列内 @ (363.48,356.49)，图标 Arrow Left @ (374.66,367.67)）
- 交互：完成后 → P04；取消 → 回 P02。

### P04 校准真实尺寸 #2316:748
- 结构：衬底标题 → 画布面板（顶墙选中高亮 `#785510` + 2×PlanHandle @ (109.04,48.93)/(598.34,48.93)）→ 就近工具面板 → 底部操作条（生成按钮 **Disabled**）→ 导航。
- 面板「就近工具 / 先确认一段已知长度」（454.35×190.13 @ (216.69,274.01)）：
  - 标题 "校准真实尺寸"（Medium 16.776 @ (16.78,11.18)）
  - 输入：标签 "已知长度（米）"，值 "6.00"（125.82×67.1 @ (16.78,44.74)）
  - 按钮：取消 Secondary 128.62×44 @ (16.78,134.21)（Arrow Left @ (27.96,145.39)）、确认尺寸 Primary 128.62×44 @ (156.58,134.21)（Arrow Right @ (167.76,145.39)）
- 交互：确认 → P05；取消 → 回 P02。

### P05 直接在图上编辑（基础态）#2316:810
- 结构：衬底标题 → 画布面板（默认示例户型，无选中）→ 提示行 → toast → 底部操作条（均启用）→ 导航（Group @ (286.36,591)）。
- 文案：
  - "点击墙或门窗开始编辑 · 长按空白添加墙 · 拖空白移动视图"（Medium 12.582，768.9×19 @ (48.93,447.36)）
  - toast "已保存到本机"（Regular 11.184 @ (44.74,508.87)）
- 交互：点墙/门/窗 → P06/P08/P09；长按空白 → P07；此页无工具面板。

### P06 编辑墙体 #2316:845
- 结构：画布（顶墙选中 `#785510` + PlanHandles）→ 面板「就近工具 / 墙体已选中  ·  长 6.00 m」（454.35×190.13 @ (216.69,274.01)）→ 底部操作条 → 导航。
- 面板文案与控件：
  - 标题 "编辑墙体"
  - 输入：长度（米）=6.00 @ (16.78,44.74)、墙高（米）=2.70 @ (156.58,44.74)、厚度（米）=0.20 @ (296.38,44.74)
  - 按钮：添加门 Secondary、添加窗 Secondary、删除墙 Danger 128.62×44 @ (296.38,134.21)（Delete 图标 @ (307.56,145.39)）
- 图标：Delete。

### P07 添加与连接墙 #2316:921
- 结构：画布（新墙选中 + PlanHandles @ (109.04,48.93)/(598.34,48.93)）→ 面板「就近工具 / 添加墙体」（454.35×146.79 @ (216.69,307.56)）→ 底部操作条 → 导航。
- 面板文案：
  - "添加墙体"（Medium 16.776）
  - "点起点，再点终点；连接已有端点可闭合房间。"（Medium 12.582）
  - 按钮：完成绘制 Secondary 125.82×44 @ (16.78,97.86)、取消 Secondary（含 Arrow Left @ (177.67,112.44)）
- 交互：完成绘制 → 回 P05。

### P08 编辑门 #2316:974
- 结构：画布（门区选中段 53.12×5.59 @ (391.44,167.76) `#785510` + PlanHandles @ (374.66,153.78)/(427.79,153.78)）→ 面板「就近工具 / 门  ·  宽 0.90 m / 高 2.10 m」（454.35×190.13 @ (216.69,274.01)）→ 底部操作条 → 导航。
- 面板文案与控件：
  - 标题 "编辑门"
  - 输入：宽度（米）=0.90、高度（米）=2.10（x=16.78 / 156.58，y=44.74）
  - 按钮：切换开向 Secondary @ (16.78,134.21)、确认修改 Primary 128.62×44 @ (156.58,134.21)（Arrow Right @ (167.76,145.39)）、删除门 Danger @ (296.38,134.21)（Delete @ (307.56,145.39)）
- 备注：设计稿面板内输入行标签残留模板字样「长度（米）」与示例值 6.00/2.10，实现以标题口径（宽度/高度）为准。
- 约束：宽 0.10–2.40 m，超出 → P19 弹窗。

### P09 编辑窗 #2316:1046
- 结构：画布（窗选中 + PlanHandles @ (185.93,46.13)/(283.79,46.13)）→ 面板「就近工具 / 窗  ·  宽 1.50 m / 高 1.20 m」（454.35×190.13 @ (216.69,274.01)）→ 底部操作条 → 导航。
- 面板文案与控件：
  - 标题 "编辑窗"
  - 输入：宽度（米）=1.50、高度（米）=1.20、窗台高（米）=0.90（x=16.78 / 156.58 / 296.38，y=44.74）
  - 按钮：确认修改 Primary @ (16.78,134.21)（Arrow Right 18×18 @ (39.67,147.77)）、删除窗 Danger @ (156.58,134.21)（Delete @ (185.67,147.99)）
- 图标：Arrow Right、Delete。

### P10 检查户型问题 #2316:1120
- 结构：画布（含问题对象选中 + PlanHandles）→ 面板「就近工具 / 有 2 处需要修正」（454.35×146.79 @ (216.69,307.56)）→ 底部操作条（生成按钮 **Disabled**）→ 导航。
- 面板文案：
  - "有 2 处需要修正"（Medium 16.776）
  - "墙体未闭合；门窗发生重叠。请先修正标记位置。"（Medium 12.582）
  - 按钮：定位断点 Secondary 125.82×44 @ (16.78,97.86)、定位重叠 Secondary 121.4×44 @ (153.08,97.86)
- 交互：全部修正后底部「生成模拟房间」转可用。

### P11 删除与撤销 #2316:1173
- 结构：画布（待删对象选中 + PlanHandles）→ 面板「就近工具 / 删除这面墙？」（454.35×146.79 @ (216.69,307.56)）→ 底部操作条 → 导航。
- 面板文案：
  - "删除这面墙？"（Medium 16.776）
  - "这面墙上的 1 扇门也会移除。删除后可撤销。"（Medium 12.582）
  - 按钮：取消 Secondary @ (16.78,97.86)（Arrow Left）、确认删除 Danger @ (153.08,97.86)（Delete）
- 交互：确认删除后可撤销（撤销入口在编辑页，设计稿未单独画）。

### P12 生成模拟房间 #2316:1226
- 结构：衬底标题 → 居中状态列「生成中」@ (90.87,207) 宽 699（相对 Window）→ 导航（**无画布、无底部操作条**）。
- 文案：
  - "正在搭建你的房间"（Bold 22.368 居中）
  - "检查结构  ✓    构建墙体与开口  …    准备空间
生成期间保留当前编辑结果。"（Medium 16.776）
  - 按钮 "取消生成"（Secondary 146.79×44 @ (395.36,435)，图标 house-add @ (406.54,446.18)）
- 交互：完成 → P13；取消 → 回 P05。

### P13 模拟房间已就绪 #2316:1245
- 结构：衬底标题 → 画布（户型渲染图，SVG 模板 EL-ba4dd7a7）→ 面板「就近工具 / 房间已生成」（454.35×146.79 @ (216.69,349.5)）→ 导航（**无底部操作条**）。
- 面板文案：
  - "房间已生成"（Medium 16.776）
  - "尺寸已校准 · 可以进入空间并开始选家具。"（Medium 12.582）
  - 按钮：返回编辑 Secondary @ (16.78,97.86)（Arrow Left @ (43.67,111.5)）、进入房间 Secondary @ (153.08,97.86)（house-add @ (176.67,110.5)）、去选家具 Secondary @ (289.39,97.86)（Couch @ (312.67,111.5)）
- 交互：进入房间 → 切换 3D Stage；去选家具 → 电商 tab；返回编辑 → P05。

### P14 异常与恢复 #2316:1288
- 结构：衬底标题 → 居中状态列「异常状态」@ (83.88,125.82) 宽 699 → 导航（**无画布、无底部操作条**）。
- 文案：
  - "暂时无法生成房间"（Bold 22.368 居中）
  - "当前编辑已保留，请重试或返回检查户型。

上传失败：格式不支持 → 重新选择
提取失败：图片不清晰 → 重传 / 使用示例
空间不可用：返回平面预览 → 稍后重试"（Medium 16.776）
  - 按钮：返回编辑 Secondary 146.79×44 @ (279.6,433.38)（Arrow Left @ (312.36,447)）、重新尝试 Primary 146.79×44 @ (447.36,433.38)
- 交互：重试 → P12；返回编辑 → P05。

### P15 替换当前户型？ #2319:764
- 底层：P05 编辑页（画布 + 提示行 + 底部操作条 + 导航 + toast "本机会话草稿"）。
- 弹窗（503.28×209.7 @ (195.72,195.72)）：
  - 标题 "替换当前户型？"（Regular 19.572 `#292621`）
  - 正文 "当前编辑将保留为本机草稿。上传新图会替换当前画布。"（Regular 13.98）
  - 按钮：保留当前 Secondary 145.39×44 @ (173.35,148.19)、上传新图 Primary 150.98×44 @ (329.93,148.19)（File upload 图标）

### P16 恢复上次编辑？ #2319:801
- 底层同 P05（toast "本机会话草稿"）。弹窗：
  - 标题 "恢复上次编辑？"
  - 正文 "发现上次保存的户型草稿，可以恢复墙体及门窗修改。"
  - 按钮：稍后恢复 Secondary、恢复草稿 Primary（Arrow Right 图标）

### P17 保存失败 #2319:838
- 底层同 P05（toast "尚未保存，请重试"）。弹窗：
  - 标题 "保存失败"
  - 正文 "修改仍保留在当前会话。请不要退出应用，重试保存。"
  - 按钮：继续编辑 Secondary、重试保存 Primary（Arrow Down 图标）

### P18 当前已离线 #2319:875
- 底层同 P05（toast "本机会话草稿"）。弹窗：
  - 标题 "当前已离线"
  - 正文 "可以继续本地编辑。在线识别暂不可用，原图已保留。"
  - 按钮：使用示例 Secondary、重试连接 Primary（Arrow Down 图标）

### P19 门宽超出可用墙段 #2319:912
- 底层同 P05（toast "本机会话草稿"）。弹窗：
  - 标题 "门宽超出可用墙段"
  - 正文 "请输入 0.10–2.40 米，或选择更长的墙。当前输入未丢失。"
  - 按钮：取消修改 Secondary、返回输入 Primary（Arrow Left 图标）
- 触发条件：P08 门宽输入越界。

---

## 4. SVG 图标汇总清单

原始矢量不在 dump 中，需从 Figma 导出（或复用工程现有图标）。模板 ref → 建议文件名：

| 模板/节点 | 名称 | 尺寸（实测/基准） | 出现画板 | 建议文件名 |
|---|---|---|---|---|
| EL-f8ee5e01 | File upload | 16.78 / 18 | 底部操作条、P02×2、P15 | ic_file_upload.svg |
| EL-ecc52381 | house-add | 16.78 / 18 | 生成模拟房间按钮、P12、P13 | ic_house_add.svg |
| EL-f512af3a | house-add（Disabled 变体） | 16.78 | P01（可用 opacity 0.45 代替） | ic_house_add_disabled.svg |
| EL-a478287e / EL-5d871a02 / EL-22aa996f / EL-448bd0ec / EL-e818454f | Home（各画板重复导出变体） | 16.78 / 18 | 全部导航 | ic_nav_home.svg |
| EL-617f1d1e / EL-34a301b2 / EL-8adaaa2b | Couch（变体） | 16.78 / 18 | 全部导航、P13 | ic_nav_couch.svg |
| （内联）+ EL-62041fd7 | Arrow Left | 16.78 / 18 | P03/P04/P07/P11/P13/P14/P19 | ic_arrow_left.svg |
| EL-571720d3 | Arrow Right | 16.78 / 18 | P04/P08/P09/P16 | ic_arrow_right.svg |
| （内联） | Arrow Down | 16.98 / 18 | P17/P18 | ic_arrow_down.svg |
| EL-e20494ce | Delete | 16.78 / 18 | P06/P08/P09/P11 | ic_delete.svg |
| EL-d3071566 / EL-8a3ce2b4 | IH/PlanHandle/48px hit area | 33.55 / 48 命中区 | P04/P06/P07/P10/P11 画布 | plan_handle.svg |
| IMAGE 059e49ff-a5b7-4d23-ba08-71e5c53b36e0 | LOGO | 52×49 | 顶栏 | logo_immerse_home.png |
| IMAGE 0648fb4c33ade0754b35c0d7a30711e0f0b8fb52 | 画板背景 | 1280×832 COVER | 全部 | bg_stage.jpg |

图标底色：画布/按钮内深棕 `#382B1A`；导航 Secondary 内白色。

---

## 5. 工程现状核对（E:\IH\Immerse-Home-New）

### 5.1 页面路由方式
- **入口** `Main.kt`：`mainApp(scope) = with(scope) { DefaultWindowContainer { PicoTheme { HomeStage() } }; Stage("FurnitureRoom") { PicoTheme { RoomStage() } } }` —— 双 Stage：主 UI 窗口（HomeStage）+ 3D 房间窗口（RoomStage，ui/shop 包）。
- `HomeStage()` 仅包装 `HomeScreen()`。
- **HomeScreen**：`when (state.selectedTab)` 切换 —— `HomeTab.SHOP → ShopScreen(onNavigateToPlanner = { viewModel.onEvent(HomeEvent.TabSelected(HomeTab.PLANNER)) })`；`HomeTab.PLANNER → RoomPlannerScreen(onNavigateToShop = { viewModel.onEvent(HomeEvent.TabSelected(HomeTab.SHOP)) })`。底部 pill 导航由私有组件 `HomeBottomTabs(selectedTab, onTabSelected)` + `RoundTabButton(tab, selected, onClick)` 实现。**无导航库，纯状态切换**；HomeTab 默认 `SHOP`。
- 进入户型编辑：电商页回调 `onNavigateToPlanner` → 切 tab；`RoomPlannerScreen` 内部再按 `PlannerStep` 切换 Upload/Calibrate/Edit/Result/Busy 五个步骤组件（UploadStep/CalibrateStep/EditStep/ResultStep/BusyStep），弹窗用 `PlannerDialogOverlay`。

### 5.2 ViewModel / UiState 模式
- `HomeViewModel : ViewModel()`：`MutableStateFlow<HomeUiState>` + `onEvent(HomeEvent)`；`HomeEvent.TabSelected(tab)` 唯一事件（`_state.update { it.copy(selectedTab = ...) }`）。
- `RoomPlannerViewModel : ViewModel()`：同样 `MutableStateFlow<RoomPlannerUiState>` + `RoomPlannerEvent`（约 20 个事件：上传/校准确认、选择工具、选中墙/门/窗、尺寸提交、添加门/窗、切换开向、删除、撤销、取消、弹窗确认/取消、重试保存/连接等）；门宽 clamp **0.10f..2.40f**（与 P19 弹窗文案一致）；维护撤销栈（VirtualRoomPlan 历史副本）；`buildPlanIssues(plan)` 检查墙体未闭合/门窗重叠（对应 P10）。
- 单向数据流：UI → event → ViewModel 更新 StateFlow → Compose `collectAsStateWithLifecycle`。

### 5.3 工程内可复用组件
- `GlassPanel`（玻璃卡容器）、`PlannerPrimaryButton` / `PlannerSecondaryButton`（primaryButtonColors/secondaryButtonColors）、`PlannerDialogOverlay` + `DialogHeading`（弹窗）、`FloorPlanCanvas` + `CanvasTransform`（户型画布，绘制墙/门窗/标注）、`InternalBottomTabs`、`FooterActions`、`ToolSelectorRow`、`EditActionRow`、`LoadingRing`、`UploadIllustration`、`ImmerseHomeLogo`。
- 电商侧：`ShopGlassPanel`、`ShopPrimaryButton`、`ShopSecondaryButton`、`ShopSectionCard`、`ShopHeader`/`ShopSidebar`（ShopNavigation）、`FeaturedProduct`、`MetricCard`、`RoomStage`（3D）。

### 5.4 可用 SpatialUI 组件 / 修饰符清单
依赖（`libs.versions.toml`）：`pico-spatial-bom = "6.0.0"` + `com.pico.spatial.ui:design / foundation / platform`（无版本）。Gradle 缓存中存在 design 0.13.3 / 6.0.0 等 AAR，以下组件名经 AAR 类枚举验证。

**已在两工程实际 import 验证**：
- `com.pico.spatial.ui.design`：`PicoTheme`、`Material`(+`MaterialDefaults`)、`Button`(+`ButtonDefaults`)、`IconButton`(+`IconButtonDefaults`)、`NumberField`(+`NumberFieldDefaults`)、`Text`、`ToggleButton`(+`ToggleButtonDefaults`)。
- `com.pico.spatial.ui.foundation.dsl`：`DefaultWindowContainer`、`Stage`、`SpatialAppScope`。
- 参考工程（E:\IH\ImmerserHome---VR-Furniture-Planning-based-on-Pico）另使用：`com.pico.spatial.ui.foundation.layout.SpatialRow`、`com.pico.spatial.ui.foundation.material.backgroundMaterial`（材质修饰符）、`com.pico.spatial.ui.foundation.modifier.zOffset`（深度偏移）。

**design 6.0.0 全量公开组件**（类枚举归纳）：Button、IconButton、ToggleButton、ToggleIconButton、Chip/Chips、Option、Checkbox、Switch、Slider、NumberField、TextField、SearchField、Link、Icon、Text、Divider、ListItem、Badge、PageControl、CircularProgressIndicator / LinearProgressIndicator、ScrollIndicator、SegmentControl、Stepper、DatePicker / DateRangePicker、TimePicker、WheelPicker、Menu/MenuItem（menu 子包）、SideNavigation、TitleBar、TabBar、AlertDialog / Sheet / Subwindow / SpatialPopup / Snackbar / Coachmark（windows 子包）、PicoTheme/Typography + tokens（ColorTokens、TypeScaleTokens、DimensionTokens、MotionTokens）。

**foundation 6.0.0**：
- dsl：SpatialAppScope、Stage、DefaultWindowContainer、WindowContainer、Placement、Form、Immersion、Brightness、WorldScale、VolumeAlignment、ContainerProperties。
- 布局/3D：Box3D（Box3DScope）、SpatialAlignDepth/DepthAlignment、SpatialPadding3D、`zOffset`（SpatialZOffset）、LayoutElement3D、rotate3D/scale3D（effect3d）。
- 材质：`backgroundMaterial(...)`（material 包，配合 platform.Material）。
- 手势：SpatialTapGestureDetector、SpatialDragGestureDetector、SpatialRotateGestureDetector、SpatialScaleGestureDetector、SpatialTransformGestureDetector（含对应 *Value）。
- hover：spatialHoverEffect* / disableSpatialHoverEffect / SpatialHoverEffectGroup。
- 其它：SpatialHittestProvider、Vibrant、Augment*、WindowMaterials、ExperimentalSpatialUIApi。

**platform 6.0.0**：Material、LengthConvert/PhysicalLengthConverter、SpatialNavigator、SUIStage/SUIWindowContainer、StageScope/WindowContainerScope、StageStyle、WindowPriority、WindowContainerSizeController、ContainerResizeType/Restriction、SpatialWindowType/AnimationType、UpperLimbRenderMode、SpatialAudioEffectPlayer、SpatialHandControllerHaptic、ViewPoint/VolumeViewPointManager。

### 5.5 现有户型相关状态
- **RoomPlannerSession**（object，Compose 全局状态）：`var activePlan: VirtualRoomPlan by mutableStateOf(FloorPlanRoom.demoPlan)`（private set）、`var revision: Int by mutableIntStateOf(0)`、`fun applyPlan(plan)` —— 3D Stage 通过该单例读取当前户型与版本号。
- **FloorPlanRoom**（domain/model，object）：常量 `PlanWidthMeters=4f`、`PlanDepthMeters=5f`、`RoomHeightMeters=2.5f`、`WallThickness=0.1f`、`PlacementPadding=0.08f`；`demoPlan`：4×5m 标准房间，4 面墙（north/south/west/east，墙高 2.5、厚 0.1）+ 房间地面 surface，**无 openings**；`choosePlacement(product,x,z)` 做家具放置约束；`scaledPlan(footprintScale 0.6..1.8, wallHeight 2.2..3.6)` 缩放房间。
- **模型**：`VirtualRoomPlan(name, widthMeters, depthMeters, walls: List<RoomWall>, surfaces, openings: List<RoomOpening>=[], ceilingHeight=2.5f)`；`RoomWall(id, centerX, centerZ, width, depth, height=2.7f)`；`RoomOpening(id, label, kind: DOOR|WINDOW, centerX, centerZ, width, depth)`。坐标约定：`stageX = planX - 4/2`，`stageZ = 5/2 - planY`（平面图左上原点 → 房间中心原点，Z 轴翻转）。
- **RoomPlannerUiState**：`PlannerStep { UPLOAD("上传你的户型"), CALIBRATE("校准真实尺寸"), EDIT("编辑户型"), RESULT("模拟房间已就绪"), BUSY("正在处理户型") }`（与 P01/P04/P05/P13/P03 一一对应）；`PlannerTool { SELECT, CREATE_WALL, PLACE_DOOR, PLACE_WINDOW }`；`PlannerSelectionType { WALL, DOOR, WINDOW }`；`PlannerSelection(type, id, widthMeters, heightMeters)`；`PlannerDialog` 五枚举（REPLACE_PLAN/RESTORE_DRAFT/SAVE_FAILED/OFFLINE/DOOR_TOO_WIDE），title/confirm/dismiss 文案与 Figma P15–P19 弹窗一致。
- **实现建议落点**：新增/改造按 `PlannerStep` 扩展；画布复用 `FloorPlanCanvas`；弹窗复用 `PlannerDialogOverlay`；上传按钮态（禁用逻辑）、toast、就近工具面板可从本规格 1.4/2/3 节直接映射；图标资源按第 4 节清单落 `res/drawable`。
