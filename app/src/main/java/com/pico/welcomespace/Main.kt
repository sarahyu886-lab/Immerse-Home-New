package com.pico.welcomespace

import com.pico.welcomespace.content.HomeStage
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.foundation.dsl.DefaultWindowContainer
import com.pico.spatial.ui.foundation.dsl.Stage
import com.pico.spatial.ui.foundation.dsl.SpatialAppScope
import com.pico.welcomespace.ui.shop.RoomStage

fun mainApp(scope: SpatialAppScope) =
    with(scope) {
        DefaultWindowContainer {
            PicoTheme {
                HomeStage()
            }
        }
        Stage("FurnitureRoom") {
            PicoTheme {
                RoomStage()
            }
        }
    }
