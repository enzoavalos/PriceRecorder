package com.example.pricerecorder.theme

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Shapes
import androidx.compose.ui.unit.dp

val PriceRecorderShapes = Shapes(
    medium = RoundedCornerShape(20.dp),
    large = CutCornerShape(topEnd = 50.dp, bottomStart = 50.dp)
)