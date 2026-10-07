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

public val Icons.Rounded.Science: ImageVector
    get() {
        if (_science != null) {
            return _science!!
        }
        _science = materialIcon(name = "Rounded.Science") {
            materialPath {
                moveTo(20.54f, 17.73f)
                lineTo(15.0f, 11.0f)
                verticalLineTo(5.0f)
                horizontalLineToRelative(1.0f)
                curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                reflectiveCurveToRelative(-0.45f, -1.0f, -1.0f, -1.0f)
                horizontalLineTo(8.0f)
                curveTo(7.45f, 3.0f, 7.0f, 3.45f, 7.0f, 4.0f)
                reflectiveCurveToRelative(0.45f, 1.0f, 1.0f, 1.0f)
                horizontalLineToRelative(1.0f)
                verticalLineToRelative(6.0f)
                lineToRelative(-5.54f, 6.73f)
                curveTo(3.14f, 18.12f, 3.0f, 18.56f, 3.0f, 19.0f)
                curveToRelative(0.01f, 1.03f, 0.82f, 2.0f, 2.0f, 2.0f)
                horizontalLineTo(19.0f)
                curveToRelative(1.19f, 0.0f, 2.0f, -0.97f, 2.0f, -2.0f)
                curveTo(21.0f, 18.56f, 20.86f, 18.12f, 20.54f, 17.73f)
                close()
            }
        }
        return _science!!
    }

private var _science: ImageVector? = null
