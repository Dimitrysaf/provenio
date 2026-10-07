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

public val Icons.Rounded.FilterAltOff: ImageVector
    get() {
        if (_filterAltOff != null) {
            return _filterAltOff!!
        }
        _filterAltOff = materialIcon(name = "Rounded.FilterAltOff") {
            materialPath {
                moveTo(19.79f, 5.61f)
                curveTo(20.3f, 4.95f, 19.83f, 4.0f, 19.0f, 4.0f)
                horizontalLineTo(6.83f)
                lineToRelative(7.97f, 7.97f)
                lineTo(19.79f, 5.61f)
                close()
            }
            materialPath {
                moveTo(20.49f, 20.49f)
                lineTo(3.51f, 3.51f)
                curveToRelative(-0.39f, -0.39f, -1.02f, -0.39f, -1.41f, 0.0f)
                curveToRelative(-0.39f, 0.39f, -0.39f, 1.02f, 0.0f, 1.41f)
                lineTo(10.0f, 13.0f)
                verticalLineToRelative(5.0f)
                curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                reflectiveCurveToRelative(2.0f, -0.9f, 2.0f, -2.0f)
                verticalLineToRelative(-1.17f)
                lineToRelative(5.07f, 5.07f)
                curveToRelative(0.39f, 0.39f, 1.02f, 0.39f, 1.41f, 0.0f)
                reflectiveCurveTo(20.88f, 20.88f, 20.49f, 20.49f)
                close()
            }
        }
        return _filterAltOff!!
    }

private var _filterAltOff: ImageVector? = null
