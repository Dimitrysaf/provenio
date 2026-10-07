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

public val Icons.Rounded.Speed: ImageVector
    get() {
        if (_speed != null) {
            return _speed!!
        }
        _speed = materialIcon(name = "Rounded.Speed") {
            materialPath {
                moveTo(19.46f, 10.0f)
                arcToRelative(1.0f, 1.0f, 0.0f, false, false, -0.07f, 1.0f)
                arcToRelative(7.55f, 7.55f, 0.0f, false, true, 0.52f, 1.81f)
                arcToRelative(8.0f, 8.0f, 0.0f, false, true, -0.69f, 4.73f)
                arcToRelative(1.0f, 1.0f, 0.0f, false, true, -0.89f, 0.53f)
                horizontalLineTo(5.68f)
                arcToRelative(1.0f, 1.0f, 0.0f, false, true, -0.89f, -0.54f)
                arcTo(8.0f, 8.0f, 0.0f, false, true, 13.0f, 6.06f)
                arcToRelative(7.69f, 7.69f, 0.0f, false, true, 2.11f, 0.56f)
                arcToRelative(1.0f, 1.0f, 0.0f, false, false, 1.0f, -0.07f)
                arcToRelative(1.0f, 1.0f, 0.0f, false, false, -0.17f, -1.76f)
                arcTo(10.0f, 10.0f, 0.0f, false, false, 3.35f, 19.0f)
                arcToRelative(2.0f, 2.0f, 0.0f, false, false, 1.72f, 1.0f)
                horizontalLineToRelative(13.85f)
                arcToRelative(2.0f, 2.0f, 0.0f, false, false, 1.74f, -1.0f)
                arcToRelative(10.0f, 10.0f, 0.0f, false, false, 0.55f, -8.89f)
                arcToRelative(1.0f, 1.0f, 0.0f, false, false, -1.75f, -0.11f)
                close()
            }
            materialPath {
                moveTo(10.59f, 12.59f)
                arcToRelative(2.0f, 2.0f, 0.0f, false, false, 2.83f, 2.83f)
                lineToRelative(5.66f, -8.49f)
                close()
            }
        }
        return _speed!!
    }

private var _speed: ImageVector? = null
