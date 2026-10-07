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

public val Icons.Rounded.LinkOff: ImageVector
    get() {
        if (_linkOff != null) {
            return _linkOff!!
        }
        _linkOff = materialIcon(name = "Rounded.LinkOff") {
            materialPath {
                moveTo(14.0f, 9.0f)
                horizontalLineToRelative(2.87f)
                curveToRelative(1.46f, 0.0f, 2.8f, 0.98f, 3.08f, 2.42f)
                curveToRelative(0.31f, 1.64f, -0.74f, 3.11f, -2.22f, 3.48f)
                lineToRelative(1.53f, 1.53f)
                curveToRelative(1.77f, -0.91f, 2.95f, -2.82f, 2.7f, -5.01f)
                curveTo(21.68f, 8.86f, 19.37f, 7.0f, 16.79f, 7.0f)
                horizontalLineTo(14.0f)
                curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                curveTo(13.0f, 8.55f, 13.45f, 9.0f, 14.0f, 9.0f)
                close()
                moveTo(3.51f, 3.51f)
                curveToRelative(-0.39f, -0.39f, -1.02f, -0.39f, -1.41f, 0.0f)
                curveToRelative(-0.39f, 0.39f, -0.39f, 1.02f, 0.0f, 1.41f)
                lineToRelative(2.64f, 2.64f)
                curveToRelative(-1.77f, 0.91f, -2.95f, 2.82f, -2.7f, 5.01f)
                curveTo(2.32f, 15.14f, 4.63f, 17.0f, 7.21f, 17.0f)
                horizontalLineTo(10.0f)
                curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                curveToRelative(0.0f, -0.55f, -0.45f, -1.0f, -1.0f, -1.0f)
                horizontalLineTo(7.13f)
                curveToRelative(-1.46f, 0.0f, -2.8f, -0.98f, -3.08f, -2.42f)
                curveTo(3.74f, 10.94f, 4.8f, 9.47f, 6.27f, 9.1f)
                lineToRelative(2.12f, 2.12f)
                curveTo(8.16f, 11.41f, 8.0f, 11.68f, 8.0f, 12.0f)
                curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                horizontalLineToRelative(1.17f)
                lineToRelative(8.9f, 8.9f)
                curveToRelative(0.39f, 0.39f, 1.02f, 0.39f, 1.41f, 0.0f)
                curveToRelative(0.39f, -0.39f, 0.39f, -1.02f, 0.0f, -1.41f)
                lineTo(3.51f, 3.51f)
                close()
                moveTo(14.0f, 11.0f)
                lineToRelative(1.71f, 1.71f)
                curveTo(15.89f, 12.53f, 16.0f, 12.28f, 16.0f, 12.0f)
                curveToRelative(0.0f, -0.55f, -0.45f, -1.0f, -1.0f, -1.0f)
                horizontalLineTo(14.0f)
                close()
            }
        }
        return _linkOff!!
    }

private var _linkOff: ImageVector? = null
