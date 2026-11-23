package com.pico.welcomespace.domain.usecase

import com.pico.welcomespace.domain.model.FloorPlanRoom
import com.pico.welcomespace.domain.model.VirtualRoomPlan

enum class FloorPlanInputSource {
    UPLOAD,
    CAMERA
}

data class RecognizedFloorPlan(
    val plan: VirtualRoomPlan,
    val source: FloorPlanInputSource
)

class RecognizeFloorPlanUseCase {
    /**
     * 当前为演示流程：不执行真实 CV 识别，无论上传文件还是拍照，
     * 都返回同一份示例户型（模拟数据），随后走同一提取流程。
     */
    fun recognize(source: FloorPlanInputSource): RecognizedFloorPlan =
        RecognizedFloorPlan(
            plan = FloorPlanRoom.simulatedSamplePlan,
            source = source
        )
}
