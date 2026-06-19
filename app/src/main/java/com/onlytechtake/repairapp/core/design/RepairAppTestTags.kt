package com.onlytechtake.repairapp.core.design

object RepairAppTestTags {
    const val DashboardTabs = "dashboard_tabs"
    const val LogoutButton = "logout_button"
    const val HomeNewOrderButton = "home_new_order_button"
    const val OrdersFilterButton = "orders_filter_button"
    const val DashboardDiscardChangesButton = "dashboard_discard_changes_button"

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
    const val NewOrderNotesField = "new_order_notes_field"
    const val SignatureCanvas = "signature_canvas"
    const val IntakeMediaTakePhotoButton = "intake_media_take_photo_button"
    const val IntakeMediaAttachFileButton = "intake_media_attach_file_button"
    const val IntakeMediaCloseCameraButton = "intake_media_close_camera_button"
    const val IntakeSubmitButton = "intake_submit_button"
    const val IntakeStepperStep = "intake_stepper_step"

    const val StatusResultSummaryField = "status_result_summary_field"
    const val StatusNoteField = "status_note_field"
    const val StatusDialogConfirmButton = "status_dialog_confirm_button"
    const val GenericConfirmButton = "generic_confirm_button"
    const val QuoteAcceptedToggle = "quote_accepted_toggle"
    const val QuoteRejectedToggle = "quote_rejected_toggle"
    const val QuoteConfirmButton = "quote_confirm_button"
    const val DiagnosisRepairableToggle = "diagnosis_repairable_toggle"
    const val DiagnosisNotRepairableToggle = "diagnosis_not_repairable_toggle"
    const val DiagnosisEstimateField = "diagnosis_estimate_field"

    const val OrderStatusLabel = "order_status_label"
    const val OrderDetailLazyColumn = "order_detail_lazy_column"
    const val OrderDetailOrderReference = "order_detail_order_reference"
    const val OrderDetailCustomerReference = "order_detail_customer_reference"
    const val OrderDetailStatusStepper = "order_detail_status_stepper"
    const val OrderActionSection = "order_action_section"
    const val OrderWhatsAppButton = "order_whatsapp_button"
    const val OrderWhatsAppPreviewPanel = "order_whatsapp_preview_panel"
    const val OrderWhatsAppOpenButton = "order_whatsapp_open_button"
    const val OrderWhatsAppSentButton = "order_whatsapp_sent_button"
    const val OrderWhatsAppClosePreviewButton = "order_whatsapp_close_preview_button"
    const val OrderSignatureSection = "order_signature_section"

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
    const val CustomerDetailCloseButton = "customer_detail_close_button"
    const val CustomerDetailEditButton = "customer_detail_edit_button"
    const val CustomerSearchField = "customer_search_field"

    const val ConfigHubCategoriesButton = "config_hub_categories_button"
    const val ConfigHubTemplatesButton = "config_hub_templates_button"
    const val ConfigHubTemplateMappingButton = "config_hub_template_mapping_button"
    const val ConfigHubWhatsappButton = "config_hub_whatsapp_button"
    const val ConfigHubStorageButton = "config_hub_storage_button"
    const val ConfigHubBackupButton = "config_hub_backup_button"
    const val ConfigHubAdminButton = "config_hub_admin_button"
    const val ConfigHubAboutButton = "config_hub_about_button"
    const val ConfigBackButton = "config_back_button"
    const val ConfigCategoryNameField = "config_category_name_field"
    const val ConfigCategoryDescriptionField = "config_category_description_field"
    const val ConfigCategoryNewButton = "config_category_new_button"
    const val ConfigCategorySaveButton = "config_category_save_button"
    const val ConfigCategoryDeleteButton = "config_category_delete_button"
    const val ConfigTemplateCodeField = "config_template_code_field"
    const val ConfigTemplateTitleField = "config_template_title_field"
    const val ConfigTemplateBodyField = "config_template_body_field"
    const val ConfigTemplateNewButton = "config_template_new_button"
    const val ConfigTemplateSaveButton = "config_template_save_button"
    const val ConfigBackupReminderSwitch = "config_backup_reminder_switch"
    const val ConfigBackupIntervalField = "config_backup_interval_field"
    const val ConfigBackupRunZipButton = "config_backup_run_zip_button"
    const val BackupRestoreZipButton = "backup_restore_zip_button"
    const val BackupCloseButton = "backup_close_button"
    const val AdminCreateOperatorButton = "admin_create_operator_button"
    const val AdminDeleteAllOrdersButton = "admin_delete_all_orders_button"
    const val AdminTotalResetButton = "admin_total_reset_button"

    fun dashboardTab(destination: String) = "dashboard_tab_${destination.lowercase()}"

    fun categoryButton(categoryId: String) = "new_order_category_$categoryId"

    fun orderCard(orderId: String) = "order_card_$orderId"

    fun orderCardOrderReference(orderId: String) = "order_card_order_reference_$orderId"

    fun orderCardCustomerReference(orderId: String) = "order_card_customer_reference_$orderId"

    fun orderCardStatusStepper(orderId: String) = "order_card_status_stepper_$orderId"

    fun orderStatusStepperStep(step: Int) = "order_status_stepper_step_$step"

    fun orderCardStatusStep(
        orderId: String,
        step: Int,
    ) = "order_card_status_step_${orderId}_$step"

    fun orderDetailStatusStep(step: Int) = "order_detail_status_step_$step"

    fun statusAction(actionName: String) = "order_status_action_${actionName.lowercase()}"

    fun closureReason(reasonName: String) = "closure_reason_${reasonName.lowercase()}"

    fun intakeStepperStep(step: Int) = "${IntakeStepperStep}_$step"
}
