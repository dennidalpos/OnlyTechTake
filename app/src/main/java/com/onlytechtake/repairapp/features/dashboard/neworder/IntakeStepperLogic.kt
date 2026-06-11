package com.onlytechtake.repairapp.features.dashboard.neworder

const val LAST_INTAKE_STEP = 5
const val SUBMIT_INTAKE_STEP = 6

fun completedStepAfterEdit(completedStep: Int, step: Int): Int =
    minOf(completedStep, step - 1).coerceAtLeast(0)

fun completedStepAfterAdvance(completedStep: Int, step: Int): Int =
    maxOf(completedStep, step).coerceIn(0, LAST_INTAKE_STEP)

fun currentStepAfterAdvance(step: Int): Int =
    (step + 1).coerceAtMost(SUBMIT_INTAKE_STEP)

fun isIntakeSubmitUnlocked(completedStep: Int): Boolean =
    completedStep >= LAST_INTAKE_STEP

fun selectedCategoryAfterClick(currentCategoryId: String?, clickedCategoryId: String): String? =
    if (currentCategoryId == clickedCategoryId) null else clickedCategoryId

fun completedStepAfterCategorySelection(completedStep: Int): Int =
    completedStepAfterEdit(completedStep, 2)
