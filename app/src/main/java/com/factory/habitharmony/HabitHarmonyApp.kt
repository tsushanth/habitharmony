package com.factory.habitharmony

import android.app.Application
import com.factory.habitharmony.billing.BillingManager
import com.factory.habitharmony.billing.PremiumManager
import com.factory.habitharmony.data.database.HabitDatabase
import com.factory.habitharmony.data.repository.HabitRepository

class HabitHarmonyApp : Application() {

    val database: HabitDatabase by lazy { HabitDatabase.getInstance(this) }
    val habitRepository: HabitRepository by lazy { HabitRepository(database.habitDao()) }

    lateinit var billingManager: BillingManager
        private set

    lateinit var premiumManager: PremiumManager
        private set

    override fun onCreate() {
        super.onCreate()
        billingManager = BillingManager(this)
        premiumManager = PremiumManager(this, billingManager)
        billingManager.startConnection()
    }

    override fun onTerminate() {
        billingManager.endConnection()
        super.onTerminate()
    }
}
