package com.onlytechtake.repairapp.features.dashboard

internal const val LAST_INTAKE_STEP = 5
internal const val SUBMIT_INTAKE_STEP = 6

internal fun completedStepAfterEdit(completedStep: Int, step: Int): Int =
    minOf(completedStep, step - 1).coerceAtLeast(0)

internal fun completedStepAfterAdvance(completedStep: Int, step: Int): Int =
    maxOf(completedStep, step).coerceIn(0, LAST_INTAKE_STEP)

internal fun currentStepAfterAdvance(step: Int): Int =
    (step + 1).coerceAtMost(SUBMIT_INTAKE_STEP)

internal fun isIntakeSubmitUnlocked(completedStep: Int): Boolean =
    completedStep >= LAST_INTAKE_STEP

internal fun selectedCategoryAfterClick(currentCategoryId: String?, clickedCategoryId: String): String? =
    if (currentCategoryId == clickedCategoryId) null else clickedCategoryId

internal fun completedStepAfterCategorySelection(completedStep: Int): Int =
    completedStepAfterEdit(completedStep, 2)
