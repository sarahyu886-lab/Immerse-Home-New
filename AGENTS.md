# WelcomeSpace - PICO Spatial SDK Shared Space Project

## 项目概述

这是一个基于 PICO Spatial SDK 的家具展示项目，参考 GitHub 案例 [PICO-Spatial-SDK-Sample-WelcomeSpace](https://github.com/Pico-Developer/PICO-Spatial-SDK-Sample-WelcomeSpace/tree/main) 创建。
保留二维 DefaultWindowContainer 与独立 FurnitureRoom Stage，禁止嵌套。

## 技术栈

- **平台**: Android (PICO OS)
- **语言**: Kotlin
- **SDK**: PICO Spatial SDK 6.0.0（与当前模拟器固件兼容）
- **容器**: 平面 WindowContainer (Shared Space)
- **UI**: SpatialUI (`com.pico.spatial.ui.*`)

## 项目结构

```
app/src/main/java/com/pico/welcomespace/
├── Main.kt                          # 应用入口
├── content/
│   └── HomeStage.kt                 # 电商页面入口
├── ui/shop/                         # 商品页面、状态与组件
├── domain/                          # 商品模型与筛选用例
├── data/repository/                 # 本地演示商品数据
├── platform/
│   ├── LaunchActivity.kt            # 启动 Activity
│   └── SpatialApplication.kt        # Spatial 应用基类
└── assets/
    ├── box.usdz                     # 原有 3D 资源
    └── sofa.glb                     # 电商页面中的 3D 沙发
```

## 关键文件说明

### Main.kt
应用入口点，使用 `DefaultWindowContainer { PicoTheme { HomeStage() } }`，
以及平级 `Stage("FurnitureRoom") { PicoTheme { RoomStage() } }`。

### content/HomeStage.kt
保留原有入口名称，经 HomeScreen 转接家具页和我的户型页。电商页包含搜索、分类、收藏、
购物袋计数和六件家具预览；商品和价格仅用于本地演示，
没有真实订单或支付。

### 标准房间与家具
- `domain/model/FloorPlanRoom.kt`：净尺寸 4×5×2.5 m，四墙封闭，无门窗。
- `ui/planner/components/StandardRoomOverview.kt`：同源比例鸟瞰，旧识别编辑流程不再作为界面入口。
- `ui/shop/FurnitureScene.kt`：底部中心实例节点、模型等比缩放、可重新抓取和绕 Y 轴旋转。
- `domain/usecase/NormalizeFurnitureSizeUseCase.kt`：单位转换和尺寸校准分别处理，比例偏差超过25%拒绝。
- `tools/generate_demo_assets.py`：生成地毯、落地灯、电视柜及标准房间，使用 bbox 工具校验；不修改旧模型。
- `app/src/main/assets/profiles/`：构建期资产记录。不是完整运行时外部模型导入系统。

### 本机 Tripo 配置
Key 只填写 `.private/tripo.json`，由 `tools/tripo_gateway.py` 读取。
禁止注入 BuildConfig、assets 或提交 Git。代理默认不允许付费生成，Android 上传/入库尚未接通。
详见 `本轮更新与材质接入说明.md`。不自动提交或推送代码。

### 本轮验证边界
36 项 JVM 单测、2 项本地代理离线测试及构建通过；模拟器可安装启动并显示房间。
真机射线拖入、旋转及重复抓取仍需人工验收，不得仅凭编译通过声称手势验证完成。

### platform/LaunchActivity.kt
应用启动 Activity，处理权限和初始化。

### platform/SpatialApplication.kt
Spatial 应用基类，配置 SDK 和应用级设置。

## 构建与运行

### 构建 APK
```bash
./gradlew assembleDebug
```

### 安装到设备
```bash
./gradlew installDebug
```

### 运行测试
```bash
./gradlew :app:testDebugUnitTest
```

## UI 规范

### 强制要求
- **使用 SpatialUI**: 所有 2D UI 必须使用 `com.pico.spatial.ui.*`
- **PicoTheme 包裹**: 根组件必须包裹在 `PicoTheme { ... }` 中
- **禁用 Material**: 禁止使用 `androidx.compose.material` 或 `Material3`

### 主题角色
- 颜色: `PicoTheme.colorScheme.<role>`
- 字体: `PicoTheme.typography.<role>`

## 扩展方向

1. **添加更多 3D 模型**: 将 .usdz/.gltf 文件放入 assets 目录
2. **扩展电商页面**: 修改 `ui/shop/` 中的 Compose 页面与组件
3. **添加交互**: 实现手势识别、射线交互等
4. **网络功能**: 添加多人协作、数据同步

## 参考资料

- [PICO Spatial SDK 官方文档](https://developer.picoxr.com/)
- [WelcomeSpace 示例源码](https://github.com/Pico-Developer/PICO-Spatial-SDK-Sample-WelcomeSpace/tree/main)
- [SpatialUI 设计指南](https://developer.picoxr.com/docs/spatial-ui)

## 注意事项

1. 开发时需要 PICO 设备或模拟器
2. 确保设备已启用开发者模式
3. 某些功能可能需要特定的 PICO OS 版本
4. 构建前请检查 `local.properties` 中的 SDK 路径配置
5. `Stage` 和 `WindowContainer` 是不同的根容器，不要在当前窗口内嵌套 `Stage`
