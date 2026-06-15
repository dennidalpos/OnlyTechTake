package com.onlytechtake.repairapp.core.design

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.components.AppIconButton

@Composable
fun PasswordVisibilityButton(
    visible: Boolean,
    onVisibleChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppIconButton(
        icon = if (visible) PasswordVisibleIcon else PasswordHiddenIcon,
        contentDescription = stringResource(
            if (visible) R.string.auth_hide_password else R.string.auth_show_password,
        ),
        onClick = { onVisibleChange(!visible) },
        modifier = modifier,
    )
}

private val PasswordVisibleIcon: ImageVector = ImageVector.Builder(
    name = "PasswordVisible",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        fill = null,
    ) {
        moveTo(2f, 12f)
        curveTo(5f, 7f, 8.5f, 5f, 12f, 5f)
        reflectiveCurveTo(19f, 7f, 22f, 12f)
        curveTo(19f, 17f, 15.5f, 19f, 12f, 19f)
        reflectiveCurveTo(5f, 17f, 2f, 12f)
        close()
        moveTo(9f, 12f)
        arcToRelative(3f, 3f, 0f, true, true, 6f, 0f)
        arcToRelative(3f, 3f, 0f, true, true, -6f, 0f)
    }
}.build()

private val PasswordHiddenIcon: ImageVector = ImageVector.Builder(
    name = "PasswordHidden",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        fill = null,
    ) {
        moveTo(3f, 3f)
        lineTo(21f, 21f)
        moveTo(10.6f, 10.6f)
        arcToRelative(3f, 3f, 0f, false, false, 3.8f, 3.8f)
        moveTo(9.9f, 5.2f)
        curveTo(10.6f, 5.1f, 11.3f, 5f, 12f, 5f)
        reflectiveCurveTo(19f, 7f, 22f, 12f)
        curveTo(21.2f, 13.4f, 20.3f, 14.5f, 19.2f, 15.5f)
        moveTo(14.1f, 18.7f)
        curveTo(13.4f, 18.9f, 12.7f, 19f, 12f, 19f)
        curveTo(8.5f, 19f, 5f, 17f, 2f, 12f)
        curveTo(3.1f, 10.2f, 4.3f, 8.8f, 5.7f, 7.7f)
    }
}.build()
