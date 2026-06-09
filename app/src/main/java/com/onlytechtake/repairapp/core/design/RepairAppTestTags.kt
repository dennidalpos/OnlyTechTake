package com.onlytechtake.repairapp.core.design

object RepairAppTestTags {
    const val AuthUsernameField = "auth_username_field"
    const val AuthPasswordField = "auth_password_field"
    const val AuthSubmitButton = "auth_submit_button"

    const val NewOrderCustomerSearchField = "new_order_customer_search_field"
    const val NewOrderInsertCustomerButton = "new_order_insert_customer_button"
    const val NewOrderFirstNameField = "new_order_first_name_field"
    const val NewOrderLastNameField = "new_order_last_name_field"
    const val NewOrderPhoneField = "new_order_phone_field"
    const val NewOrderEmailField = "new_order_email_field"
    const val NewOrderSaveCustomerButton = "new_order_save_customer_button"
    const val NewOrderStep1NextButton = "new_order_step_1_next_button"
    const val NewOrderStep2NextButton = "new_order_step_2_next_button"
    const val NewOrderProblemSummaryField = "new_order_problem_summary_field"
    const val NewOrderProblemDetailsField = "new_order_problem_details_field"
    const val SignatureCanvas = "signature_canvas"

    const val StatusResultSummaryField = "status_result_summary_field"
    const val StatusNoteField = "status_note_field"
    const val StatusDialogConfirmButton = "status_dialog_confirm_button"

    fun dashboardTab(destination: String) = "dashboard_tab_${destination.lowercase()}"

    fun categoryButton(categoryId: String) = "new_order_category_$categoryId"

    fun orderCard(orderId: String) = "order_card_$orderId"

    fun statusAction(actionName: String) = "order_status_action_${actionName.lowercase()}"

    fun closureReason(reasonName: String) = "closure_reason_${reasonName.lowercase()}"
}
