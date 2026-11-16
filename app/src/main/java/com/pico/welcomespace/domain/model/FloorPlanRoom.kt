package com.pico.welcomespace.domain.model

import kotlin.math.max

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
    val depth: Float
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
    val openings: List<RoomOpening> = emptyList()
)

object FloorPlanRoom {
    val demoPlan: VirtualRoomPlan = VirtualRoomPlan(
        name = "134㎡四房两厅识别户型",
        widthMeters = PlanWidthMeters,
        depthMeters = PlanDepthMeters,
        walls = listOf(
            wall("outer-top-left-a", 0.8f, 0f, 1.35f, 0f),
            wall("outer-top-left-b", 2.95f, 0f, 3.8f, 0f),
            wall("outer-top-kitchen-a", 3.8f, 0.3f, 4.65f, 0.3f),
            wall("outer-top-kitchen-b", 6.0f, 0.3f, 6.8f, 0.3f),
            wall("outer-top-right-a", 6.8f, 0f, 7.35f, 0f),
            wall("outer-top-right-b", 8.85f, 0f, 9.6f, 0f),
            wall("outer-left-north", 0.8f, 0f, 0.8f, 3.2f),
            wall("outer-left-bath-a", 0.8f, 3.2f, 0.8f, 3.85f),
            wall("outer-left-bath-b", 0.8f, 4.45f, 0.8f, 5.0f),
            wall("outer-left-master-a", 0f, 5.0f, 0f, 6.0f),
            wall("outer-left-master-b", 0f, 6.8f, 0f, 10.95f),
            wall("outer-left-balcony", 0f, 11.45f, 0f, 11.85f),
            wall("outer-bottom-master-a", 0f, 11.85f, 0.35f, 11.85f),
            wall("outer-bottom-master-b", 2.95f, 11.85f, 3.6f, 11.85f),
            wall("outer-bottom-living-a", 3.6f, 11.35f, 5.05f, 11.35f),
            wall("outer-bottom-living-b", 6.45f, 11.35f, 7.9f, 11.35f),
            wall("outer-bottom-study-a", 7.9f, 11.85f, 8.35f, 11.85f),
            wall("outer-bottom-study-b", 10.55f, 11.85f, 11f, 11.85f),
            wall("outer-right-study", 11f, 7.3f, 11f, 11.85f),
            wall("outer-right-service-lower", 9.6f, 5.9f, 9.6f, 7.3f),
            wall("outer-right-entry-a", 9.6f, 3.2f, 9.6f, 3.65f),
            wall("outer-right-entry-b", 9.6f, 4.45f, 9.6f, 5.9f),
            wall("outer-right-north", 9.6f, 0f, 9.6f, 3.2f),
            wall("north-left-bottom-a", 0.8f, 3.2f, 2.25f, 3.2f),
            wall("north-left-bottom-b", 3.05f, 3.2f, 3.8f, 3.2f),
            wall("kitchen-bottom-a", 3.8f, 3.2f, 4.65f, 3.2f),
            wall("kitchen-bottom-b", 6.05f, 3.2f, 6.8f, 3.2f),
            wall("north-right-bottom-a", 6.8f, 3.2f, 7.25f, 3.2f),
            wall("north-right-bottom-b", 8.15f, 3.2f, 9.6f, 3.2f),
            wall("north-left-kitchen-divider", 3.8f, 0f, 3.8f, 3.2f),
            wall("kitchen-right-divider", 6.8f, 0.3f, 6.8f, 3.2f),
            wall("small-bath-right", 2.6f, 3.2f, 2.6f, 5.0f),
            wall("small-bath-bottom-a", 0.8f, 5.0f, 1.75f, 5.0f),
            wall("small-bath-bottom-b", 2.35f, 5.0f, 3.6f, 5.0f),
            wall("master-suite-top", 0f, 5.0f, 3.6f, 5.0f),
            wall("master-suite-bottom-a", 0f, 6.8f, 1.95f, 6.8f),
            wall("master-suite-bottom-b", 2.8f, 6.8f, 3.6f, 6.8f),
            wall("master-bedroom-top-a", 0f, 6.8f, 2.25f, 6.8f),
            wall("master-bedroom-top-b", 3.05f, 6.8f, 3.6f, 6.8f),
            wall("master-right", 3.6f, 6.8f, 3.6f, 11.35f),
            wall("living-left-lower", 3.6f, 6.8f, 3.6f, 11.35f),
            wall("living-right-lower", 7.9f, 7.3f, 7.9f, 11.35f),
            wall("service-left", 7.9f, 4.3f, 7.9f, 7.3f),
            wall("service-divider", 7.9f, 5.9f, 9.6f, 5.9f),
            wall("service-bottom-a", 7.9f, 7.3f, 8.35f, 7.3f),
            wall("service-bottom-b", 9.15f, 7.3f, 9.6f, 7.3f),
            wall("study-top-a", 7.9f, 7.3f, 8.35f, 7.3f),
            wall("study-top-b", 9.15f, 7.3f, 11f, 7.3f)
        ),
        surfaces = listOf(
            surface("dining-floor", "餐厅地面", RoomSurfaceKind.FLOOR, 2.6f, 3.2f, 7.9f, 6.8f),
            surface("living-floor", "起居室地面", RoomSurfaceKind.FLOOR, 3.6f, 6.8f, 7.9f, 11.35f),
            surface("master-bedroom-floor", "主卧地面", RoomSurfaceKind.FLOOR, 0f, 6.8f, 3.6f, 11.35f),
            surface("suite-bath-floor", "主卫地面", RoomSurfaceKind.FLOOR, 0f, 5.0f, 3.6f, 6.8f),
            surface("north-bedroom-floor", "卧室地面", RoomSurfaceKind.FLOOR, 0.8f, 0f, 3.8f, 3.2f),
            surface("kitchen-floor", "厨房地面", RoomSurfaceKind.FLOOR, 3.8f, 0.3f, 6.8f, 3.2f),
            surface("north-bedroom-a-floor", "卧室A地面", RoomSurfaceKind.FLOOR, 6.8f, 0f, 9.6f, 3.2f),
            surface("guest-bath-floor", "卫生间地面", RoomSurfaceKind.FLOOR, 0.8f, 3.2f, 2.6f, 5.0f),
            surface("service-floor", "家政间地面", RoomSurfaceKind.FLOOR, 7.9f, 4.3f, 9.6f, 5.9f),
            surface("bath-a-floor", "卫生间A地面", RoomSurfaceKind.FLOOR, 7.9f, 5.9f, 9.6f, 7.3f),
            surface("study-floor", "书房地面", RoomSurfaceKind.FLOOR, 7.9f, 7.3f, 11f, 11.35f),
            surface("dining-table", "餐厅桌面", RoomSurfaceKind.TABLE, 4.55f, 4.35f, 6.15f, 5.25f, 0.72f)
        ),
        openings = listOf(
            opening("entry-door", "入户门", RoomOpeningKind.DOOR, 9.6f, 3.65f, 9.6f, 4.45f),
            opening("north-bedroom-door", "卧室门", RoomOpeningKind.DOOR, 2.25f, 3.2f, 3.05f, 3.2f),
            opening("kitchen-door", "厨房推拉门", RoomOpeningKind.DOOR, 4.65f, 3.2f, 6.05f, 3.2f),
            opening("bedroom-a-door", "卧室A门", RoomOpeningKind.DOOR, 7.25f, 3.2f, 8.15f, 3.2f),
            opening("guest-bath-door", "卫生间门", RoomOpeningKind.DOOR, 1.75f, 5.0f, 2.35f, 5.0f),
            opening("suite-bath-door", "主卫门", RoomOpeningKind.DOOR, 1.95f, 6.8f, 2.8f, 6.8f),
            opening("master-bedroom-door", "主卧门", RoomOpeningKind.DOOR, 2.25f, 6.8f, 3.05f, 6.8f),
            opening("bath-a-door", "卫生间A门", RoomOpeningKind.DOOR, 8.35f, 7.3f, 9.15f, 7.3f),
            opening("study-door", "书房门", RoomOpeningKind.DOOR, 8.35f, 7.3f, 9.15f, 7.3f),
            opening("north-bedroom-window", "卧室窗", RoomOpeningKind.WINDOW, 1.35f, 0f, 2.95f, 0f),
            opening("kitchen-window", "厨房窗", RoomOpeningKind.WINDOW, 4.65f, 0.3f, 6.0f, 0.3f),
            opening("bedroom-a-window", "卧室A窗", RoomOpeningKind.WINDOW, 7.35f, 0f, 8.85f, 0f),
            opening("guest-bath-window", "卫生间窗", RoomOpeningKind.WINDOW, 0.8f, 3.85f, 0.8f, 4.45f),
            opening("suite-bath-window", "主卫窗", RoomOpeningKind.WINDOW, 0f, 6.0f, 0f, 6.8f),
            opening("master-bedroom-window", "主卧飘窗", RoomOpeningKind.WINDOW, 0.35f, 11.85f, 2.95f, 11.85f),
            opening("living-balcony-window", "起居室阳台窗", RoomOpeningKind.WINDOW, 5.05f, 11.35f, 6.45f, 11.35f),
            opening("study-window", "书房飘窗", RoomOpeningKind.WINDOW, 8.35f, 11.85f, 10.55f, 11.85f)
        )
    )

    fun choosePlacement(
        product: ShopProduct,
        x: Float,
        z: Float,
        plan: VirtualRoomPlan = demoPlan
    ): RoomPlacementTarget? {
        val kind = if (product.category == ShopCategory.LIGHTING) {
            RoomSurfaceKind.TABLE
        } else {
            RoomSurfaceKind.FLOOR
        }
        val footprintRadius = max(product.physicalSize.width, product.physicalSize.depth) / 2f
        return plan.surfaces
            .asSequence()
            .filter { it.kind == kind }
            .mapNotNull { surface ->
                val halfWidth = surface.width / 2f - footprintRadius - PlacementPadding
                val halfDepth = surface.depth / 2f - footprintRadius - PlacementPadding
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

    private const val PlanWidthMeters = 11f
    private const val PlanDepthMeters = 11.85f
    private const val WallThickness = 0.08f
    private const val PlacementPadding = 0.08f
}
