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

public val Icons.Rounded.SearchOff: ImageVector
    get() {
        if (_searchOff != null) {
            return _searchOff!!
        }
        _searchOff = materialIcon(name = "Rounded.SearchOff") {
            materialPath {
                moveTo(15.5f, 14.0f)
                horizontalLineToRelative(-0.79f)
                lineToRelative(-0.28f, -0.27f)
                curveToRelative(1.2f, -1.4f, 1.82f, -3.31f, 1.48f, -5.34f)
                curveToRelative(-0.47f, -2.78f, -2.79f, -4.99f, -5.58f, -5.34f)
                curveTo(6.54f, 2.58f, 3.3f, 5.38f, 3.03f, 9.0f)
                horizontalLineToRelative(2.02f)
                curveToRelative(0.24f, -2.12f, 1.92f, -3.8f, 4.06f, -3.98f)
                curveTo(11.65f, 4.8f, 14.0f, 6.95f, 14.0f, 9.5f)
                curveToRelative(0.0f, 2.49f, -2.01f, 4.5f, -4.5f, 4.5f)
                curveToRelative(-0.17f, 0.0f, -0.33f, -0.03f, -0.5f, -0.05f)
                lineToRelative(0.0f, 2.02f)
                curveToRelative(0.0f, 0.0f, 0.0f, 0.0f, 0.01f, 0.01f)
                curveToRelative(1.8f, 0.13f, 3.47f, -0.47f, 4.72f, -1.55f)
                lineTo(14.0f, 14.71f)
                verticalLineToRelative(0.79f)
                lineToRelative(4.25f, 4.25f)
                curveToRelative(0.41f, 0.41f, 1.08f, 0.41f, 1.49f, 0.0f)
                lineToRelative(0.0f, 0.0f)
                curveToRelative(0.41f, -0.41f, 0.41f, -1.08f, 0.0f, -1.49f)
                lineTo(15.5f, 14.0f)
                close()
            }
            materialPath {
                moveTo(6.12f, 11.17f)
                lineTo(4.0f, 13.29f)
                lineToRelative(-2.12f, -2.12f)
                curveToRelative(-0.2f, -0.2f, -0.51f, -0.2f, -0.71f, 0.0f)
                lineToRelative(0.0f, 0.0f)
                curveToRelative(-0.2f, 0.2f, -0.2f, 0.51f, 0.0f, 0.71f)
                lineTo(3.29f, 14.0f)
                lineToRelative(-2.12f, 2.12f)
                curveToRelative(-0.2f, 0.2f, -0.2f, 0.51f, 0.0f, 0.71f)
                lineToRelative(0.0f, 0.0f)
                curveToRelative(0.2f, 0.2f, 0.51f, 0.2f, 0.71f, 0.0f)
                lineTo(4.0f, 14.71f)
                lineToRelative(2.12f, 2.12f)
                curveToRelative(0.2f, 0.2f, 0.51f, 0.2f, 0.71f, 0.0f)
                lineToRelative(0.0f, 0.0f)
                curveToRelative(0.2f, -0.2f, 0.2f, -0.51f, 0.0f, -0.71f)
                lineTo(4.71f, 14.0f)
                lineToRelative(2.12f, -2.12f)
                curveToRelative(0.2f, -0.2f, 0.2f, -0.51f, 0.0f, -0.71f)
                lineToRelative(0.0f, 0.0f)
                curveTo(6.63f, 10.98f, 6.32f, 10.98f, 6.12f, 11.17f)
                close()
            }
        }
        return _searchOff!!
    }

private var _searchOff: ImageVector? = null
