/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.material.icons.rounded

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

public val Icons.Rounded.Timer: ImageVector
    get() {
        if (_timer != null) {
            return _timer!!
        }
        _timer = materialIcon(name = "Rounded.Timer") {
            materialPath {
                moveTo(10.0f, 3.0f)
                horizontalLineToRelative(4.0f)
                curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                curveToRelative(0.0f, -0.55f, -0.45f, -1.0f, -1.0f, -1.0f)
                horizontalLineToRelative(-4.0f)
                curveTo(9.45f, 1.0f, 9.0f, 1.45f, 9.0f, 2.0f)
                curveTo(9.0f, 2.55f, 9.45f, 3.0f, 10.0f, 3.0f)
                close()
            }
            materialPath {
                moveTo(19.03f, 7.39f)
                lineToRelative(0.75f, -0.75f)
                curveToRelative(0.38f, -0.38f, 0.39f, -1.01f, 0.0f, -1.4f)
                curveToRelative(0.0f, 0.0f, -0.01f, -0.01f, -0.01f, -0.01f)
                curveToRelative(-0.39f, -0.39f, -1.01f, -0.38f, -1.4f, 0.0f)
                lineToRelative(-0.75f, 0.75f)
                curveTo(16.07f, 4.74f, 14.12f, 4.0f, 12.0f, 4.0f)
                curveToRelative(-4.8f, 0.0f, -8.88f, 3.96f, -9.0f, 8.76f)
                curveTo(2.87f, 17.84f, 6.94f, 22.0f, 12.0f, 22.0f)
                curveToRelative(4.98f, 0.0f, 9.0f, -4.03f, 9.0f, -9.0f)
                curveTo(21.0f, 10.88f, 20.26f, 8.93f, 19.03f, 7.39f)
                close()
                moveTo(13.0f, 13.0f)
                curveToRelative(0.0f, 0.55f, -0.45f, 1.0f, -1.0f, 1.0f)
                reflectiveCurveToRelative(-1.0f, -0.45f, -1.0f, -1.0f)
                verticalLineTo(9.0f)
                curveToRelative(0.0f, -0.55f, 0.45f, -1.0f, 1.0f, -1.0f)
                reflectiveCurveToRelative(1.0f, 0.45f, 1.0f, 1.0f)
                verticalLineTo(13.0f)
                close()
            }
        }
        return _timer!!
    }

private var _timer: ImageVector? = null
