package com.onlytechtake.repairapp.core.di

import android.content.Context
import androidx.room.withTransaction
import com.onlytechtake.repairapp.data.local.dao.UserDao
import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase
import com.onlytechtake.repairapp.data.orders.OrderDocumentFileStorage
import com.onlytechtake.repairapp.data.preferences.OwnerPreferences
import com.onlytechtake.repairapp.domain.repositories.CatalogRepository
import com.onlytechtake.repairapp.domain.repositories.CustomerRepository
import com.onlytechtake.repairapp.domain.repositories.OrderDocumentRepository
import com.onlytechtake.repairapp.domain.repositories.OrderMediaRepository
import com.onlytechtake.repairapp.domain.repositories.OrderRepository
import com.onlytechtake.repairapp.domain.repositories.SignatureRepository
import com.onlytechtake.repairapp.domain.repositories.UserRepository
import com.onlytechtake.repairapp.domain.usecases.ChangeOrderStatusUseCase
import com.onlytechtake.repairapp.domain.usecases.CreateIntakeOrderUseCase
import com.onlytechtake.repairapp.domain.usecases.GenerateOrderStatusPdfUseCase
import com.onlytechtake.repairapp.domain.usecases.RecordCustomerFeedbackUseCase
import com.onlytechtake.repairapp.domain.usecases.RecordDiagnosisResultUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    @Provides
    @Singleton
    fun provideCreateIntakeOrderUseCase(
        @ApplicationContext context: Context,
        customerRepository: CustomerRepository,
        orderRepository: OrderRepository,
        orderMediaRepository: OrderMediaRepository,
        signatureRepository: SignatureRepository,
        database: RepairAppDatabase,
    ): CreateIntakeOrderUseCase =
        CreateIntakeOrderUseCase(
            context = context,
            customerRepository = customerRepository,
            orderRepository = orderRepository,
            orderMediaRepository = orderMediaRepository,
            signatureRepository = signatureRepository,
        ) { block -> database.withTransaction { block() } }

    @Provides
    @Singleton
    fun provideGenerateOrderStatusPdfUseCase(
        @ApplicationContext context: Context,
        workflowRepository: OrderRepository,
        catalogRepository: CatalogRepository,
        documentRepository: OrderDocumentRepository,
        userRepository: UserRepository,
        fileStorage: OrderDocumentFileStorage,
        ownerPreferences: OwnerPreferences,
    ): GenerateOrderStatusPdfUseCase =
        GenerateOrderStatusPdfUseCase(
            context = context,
            workflowRepository = workflowRepository,
            catalogRepository = catalogRepository,
            documentRepository = documentRepository,
            userRepository = userRepository,
            fileStorage = fileStorage,
            ownerPreferences = ownerPreferences,
        )

    @Provides
    @Singleton
    fun provideChangeOrderStatusUseCase(
        orderRepository: OrderRepository,
    ): ChangeOrderStatusUseCase = ChangeOrderStatusUseCase(orderRepository)

    @Provides
    @Singleton
    fun provideRecordDiagnosisResultUseCase(
        orderRepository: OrderRepository,
    ): RecordDiagnosisResultUseCase = RecordDiagnosisResultUseCase(orderRepository)

    @Provides
    @Singleton
    fun provideRecordCustomerFeedbackUseCase(
        orderRepository: OrderRepository,
    ): RecordCustomerFeedbackUseCase = RecordCustomerFeedbackUseCase(orderRepository)
}
