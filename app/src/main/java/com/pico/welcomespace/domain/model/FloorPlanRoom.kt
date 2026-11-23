package com.pico.welcomespace.domain.model

import kotlin.math.max
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

enum class RoomSurfaceKind {
    FLOOR,
    TABLE
}

data class RoomSurface(
    val id: String,
    val label: String,
    val kind: RoomSurfaceKind,
    val centerX: Float,
    val centerZ: Float,
    val width: Float,
    val depth: Float,
    val heightY: Float
)

data class RoomWall(
    val id: String,
    val centerX: Float,
    val centerZ: Float,
    val width: Float,
    val depth: Float,
    val height: Float = 2.7f
)

enum class RoomOpeningKind {
    DOOR,
    WINDOW
}

data class RoomOpening(
    val id: String,
    val label: String,
    val kind: RoomOpeningKind,
    val centerX: Float,
    val centerZ: Float,
    val width: Float,
    val depth: Float,
    val heightMeters: Float = 2.1f,
    val sillMeters: Float = 0f,
    val swingFlipped: Boolean = false
)

data class RoomPlacementTarget(
    val surface: RoomSurface,
    val x: Float,
    val y: Float,
    val z: Float
)

data class VirtualRoomPlan(
    val name: String,
    val widthMeters: Float,
    val depthMeters: Float,
    val walls: List<RoomWall>,
    val surfaces: List<RoomSurface>,
    val openings: List<RoomOpening> = emptyList(),
    val ceilingHeight: Float = 2.5f
)

object FloorPlanRoom {
    val demoPlan: VirtualRoomPlan = VirtualRoomPlan(
        name = "4 × 5 米标准房间",
        widthMeters = PlanWidthMeters,
        depthMeters = PlanDepthMeters,
        walls = listOf(
            // 墙厚向外延伸，室内净空不被墙体占用。
            RoomWall("north", 0f, -(PlanDepthMeters + WallThickness) / 2,
                PlanWidthMeters + 2 * WallThickness, WallThickness, RoomHeightMeters),
            RoomWall("south", 0f, (PlanDepthMeters + WallThickness) / 2,
                PlanWidthMeters + 2 * WallThickness, WallThickness, RoomHeightMeters),
            RoomWall("west", -(PlanWidthMeters + WallThickness) / 2, 0f,
                WallThickness, PlanDepthMeters, RoomHeightMeters),
            RoomWall("east", (PlanWidthMeters + WallThickness) / 2, 0f,
                WallThickness, PlanDepthMeters, RoomHeightMeters)
        ),
        surfaces = listOf(
            RoomSurface("room-floor", "房间地面", RoomSurfaceKind.FLOOR,
                0f, 0f, PlanWidthMeters, PlanDepthMeters, 0f)
        ),
        ceilingHeight = RoomHeightMeters
    )

    /**
     * 示例户型（模拟数据，非真实识别结果）：6 × 4 米，六面墙，
     * 客厅 / 卧室 / 厨房三个房间，含 2 扇窗与 3 扇门。
     */
    val simulatedSamplePlan: VirtualRoomPlan = VirtualRoomPlan(
        name = "示例户型 · 6 × 4 米（模拟）",
        widthMeters = 6f,
        depthMeters = 4f,
        walls = listOf(
            RoomWall("sample-north", 0f, -2.06f, 6.24f, 0.12f, 2.7f),
            RoomWall("sample-south", 0f, 2.06f, 6.24f, 0.12f, 2.7f),
            RoomWall("sample-west", -3.06f, 0f, 0.12f, 4f, 2.7f),
            RoomWall("sample-east", 3.06f, 0f, 0.12f, 4f, 2.7f),
            RoomWall("sample-divider-v", 1.9f, -0.7f, 0.12f, 2.6f, 2.7f),
            RoomWall("sample-divider-h", -0.55f, 0.6f, 4.9f, 0.12f, 2.7f)
        ),
        surfaces = listOf(
            RoomSurface("sample-room-living", "客厅", RoomSurfaceKind.FLOOR, -0.55f, -0.7f, 4.9f, 2.6f, 0f),
            RoomSurface("sample-room-kitchen", "厨房", RoomSurfaceKind.FLOOR, -0.55f, 1.3f, 4.9f, 1.4f, 0f),
            RoomSurface("sample-room-bed", "卧室", RoomSurfaceKind.FLOOR, 2.45f, 0f, 1.1f, 4f, 0f)
        ),
        openings = listOf(
            RoomOpening("sample-window-living", "客厅窗", RoomOpeningKind.WINDOW,
                -1.5f, -2.06f, 1.5f, 0.12f, heightMeters = 1.2f, sillMeters = 0.9f),
            RoomOpening("sample-window-kitchen", "厨房窗", RoomOpeningKind.WINDOW,
                -3.06f, 1.3f, 0.12f, 0.9f, heightMeters = 1.2f, sillMeters = 0.9f),
            RoomOpening("sample-door-entry", "入户门", RoomOpeningKind.DOOR,
                0.8f, 2.06f, 0.9f, 0.12f),
            RoomOpening("sample-door-kitchen", "厨房门", RoomOpeningKind.DOOR,
                0.8f, 0.6f, 0.9f, 0.12f),
            RoomOpening("sample-door-bed", "卧室门", RoomOpeningKind.DOOR,
                1.9f, -0.4f, 0.12f, 0.9f)
        ),
        ceilingHeight = 2.7f
    )

    /**
     * 含 2 处问题的示例数据（模拟）：东墙断开导致墙体未闭合 +
     * 入户门与客厅窗发生重叠，用于 P10 检查户型问题的演示。
     */
    val simulatedIssuePlan: VirtualRoomPlan = simulatedSamplePlan.copy(
        name = "示例户型 · 待修正（模拟）",
        walls = simulatedSamplePlan.walls.map { wall ->
            if (wall.id == "sample-east") {
                wall.copy(centerZ = 1f, depth = 2f)
            } else {
                wall
            }
        },
        openings = simulatedSamplePlan.openings +
            RoomOpening("sample-door-duplicate", "重叠门", RoomOpeningKind.DOOR,
                -1f, -2.06f, 0.9f, 0.12f)
    )

    fun choosePlacement(
        product: ShopProduct,
        x: Float,
        z: Float,
        plan: VirtualRoomPlan = demoPlan,
        yawRadians: Float = 0f,
        size: PhysicalSize = product.physicalSize
    ): RoomPlacementTarget? {
        if (!x.isFinite() || !z.isFinite() || !yawRadians.isFinite()) return null
        val kind = if (product.prefersTable && plan.surfaces.any { it.kind == RoomSurfaceKind.TABLE }) {
            RoomSurfaceKind.TABLE
        } else {
            RoomSurfaceKind.FLOOR
        }
        val halfX = (abs(cos(yawRadians)) * size.width + abs(sin(yawRadians)) * size.depth) / 2f
        val halfZ = (abs(sin(yawRadians)) * size.width + abs(cos(yawRadians)) * size.depth) / 2f
        return plan.surfaces
            .asSequence()
            .filter { it.kind == kind }
            .mapNotNull { surface ->
                if (surface.heightY + size.height > plan.ceilingHeight) return@mapNotNull null
                val halfWidth = surface.width / 2f - halfX - PlacementPadding
                val halfDepth = surface.depth / 2f - halfZ - PlacementPadding
                if (halfWidth <= 0f || halfDepth <= 0f) return@mapNotNull null
                val clampedX = x.coerceIn(surface.centerX - halfWidth, surface.centerX + halfWidth)
                val clampedZ = z.coerceIn(surface.centerZ - halfDepth, surface.centerZ + halfDepth)
                val distanceX = x - clampedX
                val distanceZ = z - clampedZ
                val distanceSquared = distanceX * distanceX + distanceZ * distanceZ
                distanceSquared to RoomPlacementTarget(
                    surface = surface,
                    x = clampedX,
                    y = surface.heightY,
                    z = clampedZ
                )
            }
            .minByOrNull { it.first }
            ?.second
    }

    fun scaledPlan(
        footprintScale: Float,
        wallHeight: Float,
        source: VirtualRoomPlan = demoPlan
    ): VirtualRoomPlan {
        val scale = footprintScale.coerceIn(0.6f, 1.8f)
        val safeWallHeight = wallHeight.coerceIn(2.2f, 3.6f)
        return source.copy(
            name = "${source.name} · ${(scale * 100).toInt()}%",
            widthMeters = source.widthMeters * scale,
            depthMeters = source.depthMeters * scale,
            ceilingHeight = safeWallHeight,
            walls = source.walls.map { wall ->
                wall.copy(
                    centerX = wall.centerX * scale,
                    centerZ = wall.centerZ * scale,
                    width = wall.width * scale,
                    depth = wall.depth * scale,
                    height = safeWallHeight
                )
            },
            surfaces = source.surfaces.map { surface ->
                surface.copy(
                    centerX = surface.centerX * scale,
                    centerZ = surface.centerZ * scale,
                    width = surface.width * scale,
                    depth = surface.depth * scale,
                    heightY = if (surface.kind == RoomSurfaceKind.TABLE) {
                        surface.heightY
                    } else {
                        0f
                    }
                )
            },
            openings = source.openings.map { opening ->
                opening.copy(
                    centerX = opening.centerX * scale,
                    centerZ = opening.centerZ * scale,
                    width = opening.width * scale,
                    depth = opening.depth * scale
                )
            }
        )
    }

    private fun wall(id: String, x1: Float, y1: Float, x2: Float, y2: Float): RoomWall {
        val sx1 = stageX(x1)
        val sz1 = stageZ(y1)
        val sx2 = stageX(x2)
        val sz2 = stageZ(y2)
        return RoomWall(
            id = id,
            centerX = (sx1 + sx2) / 2f,
            centerZ = (sz1 + sz2) / 2f,
            width = max(kotlin.math.abs(sx2 - sx1), WallThickness),
            depth = max(kotlin.math.abs(sz2 - sz1), WallThickness)
        )
    }

    private fun surface(
        id: String,
        label: String,
        kind: RoomSurfaceKind,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        heightY: Float = 0f
    ): RoomSurface {
        val sx1 = stageX(x1)
        val sx2 = stageX(x2)
        val sz1 = stageZ(y1)
        val sz2 = stageZ(y2)
        return RoomSurface(
            id = id,
            label = label,
            kind = kind,
            centerX = (sx1 + sx2) / 2f,
            centerZ = (sz1 + sz2) / 2f,
            width = kotlin.math.abs(sx2 - sx1),
            depth = kotlin.math.abs(sz2 - sz1),
            heightY = heightY
        )
    }

    private fun opening(
        id: String,
        label: String,
        kind: RoomOpeningKind,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float
    ): RoomOpening {
        val sx1 = stageX(x1)
        val sx2 = stageX(x2)
        val sz1 = stageZ(y1)
        val sz2 = stageZ(y2)
        return RoomOpening(
            id = id,
            label = label,
            kind = kind,
            centerX = (sx1 + sx2) / 2f,
            centerZ = (sz1 + sz2) / 2f,
            width = max(kotlin.math.abs(sx2 - sx1), WallThickness),
            depth = max(kotlin.math.abs(sz2 - sz1), WallThickness)
        )
    }

    private fun stageX(planX: Float): Float = planX - PlanWidthMeters / 2f

    private fun stageZ(planY: Float): Float = PlanDepthMeters / 2f - planY

    private const val PlanWidthMeters = 4f
    private const val PlanDepthMeters = 5f
    private const val RoomHeightMeters = 2.5f
    private const val WallThickness = 0.1f
    private const val PlacementPadding = 0.08f
}
