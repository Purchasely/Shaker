package com.purchasely.shaker.purchasely

import io.purchasely.ext.presentation.PLYPresentation

@JvmInline
value class PresentationHandle internal constructor(
    internal val presentation: PLYPresentation
)
