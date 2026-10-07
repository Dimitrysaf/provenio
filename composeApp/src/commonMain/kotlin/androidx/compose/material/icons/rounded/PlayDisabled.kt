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

public val Icons.Rounded.PlayDisabled: ImageVector
    get() {
        if (_playDisabled != null) {
            return _playDisabled!!
        }
        _playDisabled = materialIcon(name = "Rounded.PlayDisabled") {
            materialPath {
                moveTo(2.1f, 3.51f)
                lineTo(2.1f, 3.51f)
                curveToRelative(-0.39f, 0.39f, -0.39f, 1.02f, 0.0f, 1.41f)
                lineToRelative(5.9f, 5.9f)
                verticalLineToRelative(6.35f)
                curveToRelative(0.0f, 0.79f, 0.87f, 1.27f, 1.54f, 0.84f)
                lineToRelative(3.45f, -2.2f)
                lineToRelative(6.08f, 6.08f)
                curveToRelative(0.39f, 0.39f, 1.02f, 0.39f, 1.41f, 0.0f)
                lineToRelative(0.0f, 0.0f)
                curveToRelative(0.39f, -0.39f, 0.39f, -1.02f, 0.0f, -1.41f)
                lineTo(3.51f, 3.51f)
                curveTo(3.12f, 3.12f, 2.49f, 3.12f, 2.1f, 3.51f)
                close()
                moveTo(17.68f, 12.84f)
                curveToRelative(0.62f, -0.39f, 0.62f, -1.29f, 0.0f, -1.69f)
                lineTo(9.54f, 5.98f)
                curveTo(9.27f, 5.81f, 8.97f, 5.79f, 8.7f, 5.87f)
                lineToRelative(7.75f, 7.75f)
                lineTo(17.68f, 12.84f)
                close()
            }
        }
        return _playDisabled!!
    }

private var _playDisabled: ImageVector? = null
