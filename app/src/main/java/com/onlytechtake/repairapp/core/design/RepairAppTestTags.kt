package com.onlytechtake.repairapp.core.design

object RepairAppTestTags {
    const val DashboardTabs = "dashboard_tabs"

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
    const val QuoteAcceptedToggle = "quote_accepted_toggle"
    const val QuoteRejectedToggle = "quote_rejected_toggle"
    const val QuoteConfirmButton = "quote_confirm_button"

    const val CustomerNewButton = "customer_new_button"
    const val CustomerPrivateToggle = "customer_private_toggle"
    const val CustomerBusinessToggle = "customer_business_toggle"
    const val CustomerFirstNameField = "customer_first_name_field"
    const val CustomerLastNameField = "customer_last_name_field"
    const val CustomerCompanyNameField = "customer_company_name_field"
    const val CustomerVatNumberField = "customer_vat_number_field"
    const val CustomerTaxCodeField = "customer_tax_code_field"
    const val CustomerPhoneField = "customer_phone_field"
    const val CustomerEmailField = "customer_email_field"
    const val CustomerAddressField = "customer_address_field"
    const val CustomerNotesField = "customer_notes_field"
    const val CustomerSaveButton = "customer_save_button"
    const val CustomerConfirmSaveButton = "customer_confirm_save_button"

    const val ConfigHubCategoriesButton = "config_hub_categories_button"
    const val ConfigHubTemplatesButton = "config_hub_templates_button"
    const val ConfigBackButton = "config_back_button"
    const val ConfigCategoryNameField = "config_category_name_field"
    const val ConfigCategoryDescriptionField = "config_category_description_field"
    const val ConfigCategorySortOrderField = "config_category_sort_order_field"
    const val ConfigCategoryNewButton = "config_category_new_button"
    const val ConfigCategorySaveButton = "config_category_save_button"
    const val ConfigTemplateCodeField = "config_template_code_field"
    const val ConfigTemplateTitleField = "config_template_title_field"
    const val ConfigTemplateBodyField = "config_template_body_field"
    const val ConfigTemplateSaveButton = "config_template_save_button"

    fun dashboardTab(destination: String) = "dashboard_tab_${destination.lowercase()}"

    fun categoryButton(categoryId: String) = "new_order_category_$categoryId"

    fun orderCard(orderId: String) = "order_card_$orderId"

    fun statusAction(actionName: String) = "order_status_action_${actionName.lowercase()}"

    fun closureReason(reasonName: String) = "closure_reason_${reasonName.lowercase()}"
}
