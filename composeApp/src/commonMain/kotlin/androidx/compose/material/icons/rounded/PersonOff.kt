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

public val Icons.Rounded.PersonOff: ImageVector
    get() {
        if (_personOff != null) {
            return _personOff!!
        }
        _personOff = materialIcon(name = "Rounded.PersonOff") {
            materialPath {
                moveTo(8.65f, 5.82f)
                curveTo(9.36f, 4.72f, 10.6f, 4.0f, 12.0f, 4.0f)
                curveToRelative(2.21f, 0.0f, 4.0f, 1.79f, 4.0f, 4.0f)
                curveToRelative(0.0f, 1.4f, -0.72f, 2.64f, -1.82f, 3.35f)
                lineTo(8.65f, 5.82f)
                close()
                moveTo(20.0f, 17.17f)
                curveToRelative(-0.02f, -1.1f, -0.63f, -2.11f, -1.61f, -2.62f)
                curveToRelative(-0.54f, -0.28f, -1.13f, -0.54f, -1.77f, -0.76f)
                lineTo(20.0f, 17.17f)
                close()
                moveTo(20.49f, 20.49f)
                lineTo(3.51f, 3.51f)
                curveToRelative(-0.39f, -0.39f, -1.02f, -0.39f, -1.41f, 0.0f)
                lineToRelative(0.0f, 0.0f)
                curveToRelative(-0.39f, 0.39f, -0.39f, 1.02f, 0.0f, 1.41f)
                lineToRelative(8.18f, 8.18f)
                curveToRelative(-1.82f, 0.23f, -3.41f, 0.8f, -4.7f, 1.46f)
                curveTo(4.6f, 15.08f, 4.0f, 16.11f, 4.0f, 17.22f)
                lineTo(4.0f, 20.0f)
                horizontalLineToRelative(13.17f)
                lineToRelative(1.9f, 1.9f)
                curveToRelative(0.39f, 0.39f, 1.02f, 0.39f, 1.41f, 0.0f)
                lineToRelative(0.0f, 0.0f)
                curveTo(20.88f, 21.51f, 20.88f, 20.88f, 20.49f, 20.49f)
                close()
            }
        }
        return _personOff!!
    }

private var _personOff: ImageVector? = null
