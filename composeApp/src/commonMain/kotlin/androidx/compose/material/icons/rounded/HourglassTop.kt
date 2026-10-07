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

public val Icons.Rounded.HourglassTop: ImageVector
    get() {
        if (_hourglassTop != null) {
            return _hourglassTop!!
        }
        _hourglassTop = materialIcon(name = "Rounded.HourglassTop") {
            materialPath {
                moveTo(8.0f, 2.0f)
                curveTo(6.9f, 2.0f, 6.0f, 2.9f, 6.0f, 4.0f)
                lineToRelative(0.01f, 3.18f)
                curveToRelative(0.0f, 0.53f, 0.21f, 1.03f, 0.58f, 1.41f)
                lineTo(10.0f, 12.0f)
                lineToRelative(-3.41f, 3.43f)
                curveToRelative(-0.37f, 0.37f, -0.58f, 0.88f, -0.58f, 1.41f)
                lineTo(6.0f, 20.0f)
                curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(8.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                verticalLineToRelative(-3.16f)
                curveToRelative(0.0f, -0.53f, -0.21f, -1.04f, -0.58f, -1.41f)
                lineTo(14.0f, 12.0f)
                lineToRelative(3.41f, -3.4f)
                curveTo(17.79f, 8.22f, 18.0f, 7.71f, 18.0f, 7.18f)
                verticalLineTo(4.0f)
                curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                horizontalLineTo(8.0f)
                close()
                moveTo(16.0f, 16.91f)
                verticalLineTo(19.0f)
                curveToRelative(0.0f, 0.55f, -0.45f, 1.0f, -1.0f, 1.0f)
                horizontalLineTo(9.0f)
                curveToRelative(-0.55f, 0.0f, -1.0f, -0.45f, -1.0f, -1.0f)
                verticalLineToRelative(-2.09f)
                curveToRelative(0.0f, -0.27f, 0.11f, -0.52f, 0.29f, -0.71f)
                lineTo(12.0f, 12.5f)
                lineToRelative(3.71f, 3.71f)
                curveTo(15.89f, 16.39f, 16.0f, 16.65f, 16.0f, 16.91f)
                close()
            }
        }
        return _hourglassTop!!
    }

private var _hourglassTop: ImageVector? = null
