package com.example.composetutorial

import android.app.Application
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.composetutorial.connectivity.NetworkConnectivityObserver
import com.example.composetutorial.data.ItemRepository
import com.example.composetutorial.data.local.AppDatabase
import com.example.composetutorial.data.local.UserPreferencesRepository
import com.example.composetutorial.data.remote.ApiInterface
import com.example.composetutorial.data.remote.ItemWebSocket
import com.example.composetutorial.util.NotificationHelper
import com.example.composetutorial.worker.SyncWorker
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ComposeTutorialApplication : Application() {

        // Singleton instance of the repository
        lateinit var container: AppContainer

        override fun onCreate() {
                super.onCreate()
                logMapsApiKeyPresence()
                container = AppContainer(this)
                setupWorkManager()
        }

        /**
         * Simple debug check for Step 0 of the Maps assignment.
         *
         * This will log whether the `com.google.android.geo.API_KEY` meta-data is present
         * and what resource ID/value it resolves to. Check Logcat with:
         *   TAG: "ComposeTutorialApp"
         */
        private fun logMapsApiKeyPresence() {
                val tag = "ComposeTutorialApp"
                try {
                        val appInfo =
                                packageManager.getApplicationInfo(
                                        packageName,
                                        android.content.pm.PackageManager.GET_META_DATA
                                )
                        val metaData = appInfo.metaData
                        val key = metaData?.getString("com.google.android.geo.API_KEY")

                        if (key.isNullOrEmpty()) {
                                Log.w(
                                        tag,
                                        "Maps Step0: com.google.android.geo.API_KEY is MISSING or empty in AndroidManifest.xml"
                                )
                        } else {
                                Log.i(
                                        tag,
                                        "Maps Step0: Found com.google.android.geo.API_KEY -> \"$key\" (this may be a @string/ reference)."
                                )
                        }
                } catch (e: Exception) {
                        Log.e(tag, "Maps Step0: Error while reading API key meta-data", e)
                }
        }

        private fun setupWorkManager() {
                val constraints =
                        Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

                val syncRequest =
                        PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.MINUTES)
                                .setConstraints(constraints)
                                .build()

                WorkManager.getInstance(this)
                        .enqueueUniquePeriodicWork(
                                "SyncWorker",
                                ExistingPeriodicWorkPolicy.KEEP,
                                syncRequest
                        )
        }
}

// Simple manual dependency injection container
class AppContainer(private val context: android.content.Context) {

        val database by lazy { AppDatabase.getDatabase(context) }

        val userPrefs by lazy { UserPreferencesRepository(context) }

        val connectivityObserver by lazy { NetworkConnectivityObserver(context) }

        val notificationHelper by lazy { NotificationHelper(context) }

        private val baseUrl = "http://10.0.2.2:3000/"

        private val logging =
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }

        private val client = OkHttpClient.Builder().addInterceptor(logging).build()

        private val retrofit =
                Retrofit.Builder()
                        .baseUrl(baseUrl)
                        .addConverterFactory(GsonConverterFactory.create())
                        .client(client)
                        .build()

        val apiObserver: ApiInterface by lazy { retrofit.create(ApiInterface::class.java) }

        val webSocket by lazy { ItemWebSocket(baseUrl) }

        val repository: ItemRepository by lazy {
                ItemRepository(
                        apiObserver,
                        webSocket,
                        database.itemDao(),
                        userPrefs,
                        notificationHelper,
                        connectivityObserver
                )
        }
}
