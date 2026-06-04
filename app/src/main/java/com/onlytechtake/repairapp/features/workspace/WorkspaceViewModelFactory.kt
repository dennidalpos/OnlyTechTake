package com.onlytechtake.repairapp.features.workspace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.onlytechtake.repairapp.data.repositories.AdminUserRepository
import com.onlytechtake.repairapp.data.repositories.BackupRepository
import com.onlytechtake.repairapp.data.repositories.CatalogRepository
import com.onlytechtake.repairapp.data.repositories.CustomerRepository
import com.onlytechtake.repairapp.data.repositories.DataResetRepository
import com.onlytechtake.repairapp.data.repositories.OrderMediaRepository
import com.onlytechtake.repairapp.data.repositories.OrderWorkflowRepository
import com.onlytechtake.repairapp.data.repositories.SignatureRepository
import com.onlytechtake.repairapp.data.repositories.TemplateRepository
import com.onlytechtake.repairapp.domain.auth.AuthRepository
import com.onlytechtake.repairapp.domain.workspace.CreateIntakeOrderUseCase

class WorkspaceViewModelFactory(
    private val authRepository: AuthRepository,
    private val adminUserRepository: AdminUserRepository,
    private val customerRepository: CustomerRepository,
    private val catalogRepository: CatalogRepository,
    private val orderRepository: OrderWorkflowRepository,
    private val orderMediaRepository: OrderMediaRepository,
    private val signatureRepository: SignatureRepository,
    private val templateRepository: TemplateRepository,
    private val backupRepository: BackupRepository,
    private val dataResetRepository: DataResetRepository,
    private val createIntakeOrderUseCase: CreateIntakeOrderUseCase,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(WorkspaceViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return WorkspaceViewModel(
            authRepository = authRepository,
            adminUserRepository = adminUserRepository,
            customerRepository = customerRepository,
            catalogRepository = catalogRepository,
            orderRepository = orderRepository,
            orderMediaRepository = orderMediaRepository,
            signatureRepository = signatureRepository,
            templateRepository = templateRepository,
            backupRepository = backupRepository,
            dataResetRepository = dataResetRepository,
            createIntakeOrderUseCase = createIntakeOrderUseCase,
        ) as T
    }
}
