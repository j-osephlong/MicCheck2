package com.jlong.miccheck.billing

import android.app.Activity
import android.app.Application
import android.content.Context.MODE_PRIVATE
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch


class Billing private constructor(
    application: Application,
    private val defaultScope: CoroutineScope,
    inAppSKUs: Array<String>?,
    setProValue: (Boolean) -> Unit
) : PurchasesUpdatedListener, BillingClientStateListener {

    private var billingClient: BillingClient
    private var application: Application
    private var setProValue: (Boolean) -> Unit

    private val inAppSKUs: List<String>?

    private val skuStateMap: MutableMap<String, MutableStateFlow<SkuState>> = HashMap()
    private val skuDetailsMap: MutableMap<String, MutableStateFlow<SkuDetails?>> = HashMap()


    private enum class SkuState {
        SKU_STATE_UNPURCHASED, SKU_STATE_PENDING, SKU_STATE_PURCHASED, SKU_STATE_PURCHASED_AND_ACKNOWLEDGED
    }

    init {
        this.inAppSKUs = inAppSKUs?.toList() ?: ArrayList()
        this.application = application
        this.setProValue = setProValue

        // TODO: add knownInAppSKUs to the flow

        billingClient = BillingClient.newBuilder(application)
            .setListener(this)
            .enablePendingPurchases()
            .build()
        billingClient.startConnection(this)

        addSkuFlows(this.inAppSKUs)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, list: MutableList<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> if (null != list) {
//                TODO: handlePurchase(list)
                return
            } else Log.d(TAG, "Null Purchase List Returned from OK response!")
            BillingClient.BillingResponseCode.USER_CANCELED ->
                Log.i(TAG, "onPurchasesUpdated: User canceled the purchase")
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED ->
                Log.i(TAG, "onPurchasesUpdated: The user already owns this item")
            BillingClient.BillingResponseCode.DEVELOPER_ERROR -> Log.e(
                TAG,
                "onPurchasesUpdated: Developer error means that Google Play " +
                        "does not recognize the configuration. If you are just " +
                        "getting started, make sure you have configured the " +
                        "application correctly in the Google Play Console. " +
                        "The SKU product ID must match and the APK" +
                        "you are using must be signed with release keys."
            )
            else ->
                Log.d(
                    TAG,
                    "BillingResult [" + billingResult.responseCode + "]: " +
                            billingResult.debugMessage
                )
        }
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        val responseCode = billingResult.responseCode
        val debugMessage = billingResult.debugMessage
        Log.d(TAG, "onBillingSetupFinished: $responseCode $debugMessage")
        when (responseCode) {
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
                BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
                BillingClient.BillingResponseCode.SERVICE_TIMEOUT,
                BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> {
                    Log.i(TAG, "Offline, resorting to shared preferences.")
                    setProValue(application.getSharedPreferences("micCheck", MODE_PRIVATE).getBoolean("is_pro", false))
                }
            BillingClient.BillingResponseCode.OK -> {
                Log.i(TAG, "Online.")
                defaultScope.launch {
                    querySkuDetailsAsync()
                    restorePurchases()
                }
            }
        }
    }

    override fun onBillingServiceDisconnected() {
        TODO("Not yet implemented")
    }

    private fun isSignatureValid(purchase: Purchase): Boolean {
        return BillingSecurity.verifyPurchase(purchase.originalJson, purchase.signature)
    }

    private fun handlePurchase(purchases: List<Purchase>?) {
        if (null != purchases) {
            for (purchase in purchases) {
                // Global check to make sure all purchases are signed correctly.
                // This check is best performed on your server.
                val purchaseState = purchase.purchaseState
                if (purchaseState == Purchase.PurchaseState.PURCHASED) {
                    if (!isSignatureValid(purchase)) {
                        Log.e(
                            TAG,
                            "Invalid signature. Check to make sure your " +
                                    "public key is correct."
                        )
                        continue
                    }
                    // only set the purchased state after we've validated the signature.
                    setSkuStateFromPurchase(purchase)

                    if (!purchase.isAcknowledged) {
                        defaultScope.launch {
                            for (sku in purchase.skus) {
                                // Acknowledge item and change its state
                                val billingResult = billingClient.acknowledgePurchase(
                                    AcknowledgePurchaseParams.newBuilder()
                                        .setPurchaseToken(purchase.purchaseToken)
                                        .build()
                                )
                                if (billingResult.responseCode !=
                                    BillingClient.BillingResponseCode.OK) {
                                    Log.e(
                                        TAG,
                                        "Error acknowledging purchase: ${purchase.skus}"
                                    )
                                } else {
                                    // purchase acknowledged
                                    val skuStateFlow = skuStateMap[sku]
                                    skuStateFlow?.tryEmit(
                                        SkuState.SKU_STATE_PURCHASED_AND_ACKNOWLEDGED
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // purchase not purchased
                    setSkuStateFromPurchase(purchase)
                }
            }
        } else {
            Log.d(TAG, "Empty purchase list.")
        }
    }

    private fun setSkuStateFromPurchase(purchase: Purchase) {
        if (purchase.skus.isEmpty()) {
            Log.e(TAG, "Empty list of skus")
            return
        }

        for (sku in purchase.skus) {
            val skuState = skuStateMap[sku]
            if (null == skuState) {
                Log.e(
                    TAG,
                    "Unknown SKU " + sku + ". Check to make " +
                            "sure SKU matches SKUS in the Play developer console."
                )
                continue
            }

            when (purchase.purchaseState) {
                Purchase.PurchaseState.PENDING -> {
                    skuState.tryEmit(SkuState.SKU_STATE_PENDING)
                    setProValue(false)
                }
                Purchase.PurchaseState.UNSPECIFIED_STATE -> {
                    skuState.tryEmit(SkuState.SKU_STATE_UNPURCHASED)
                    setProValue(false)
                }
                Purchase.PurchaseState.PURCHASED -> {
                    if (purchase.isAcknowledged) {
                        skuState.tryEmit(SkuState.SKU_STATE_PURCHASED_AND_ACKNOWLEDGED)
                    } else {
                        skuState.tryEmit(SkuState.SKU_STATE_PURCHASED)
                    }
                    Log.i(TAG, "Sku ${purchase.skus} is ack? ${purchase.isAcknowledged} ")
                    setProValue(true)
                }
                else -> {
                    Log.e(
                        TAG,
                        "Purchase in unknown state: " + purchase.purchaseState
                    )
                    setProValue(false)
                }
            }
        }
    }

    private fun addSkuFlows(skuList: List<String>?) {
        if (null == skuList) {
            Log.e(
                TAG,
                "addSkuFlows: " +
                        "SkuList is either null or empty."
            )
        }
        for (sku in skuList!!) {
            val skuState = MutableStateFlow(SkuState.SKU_STATE_UNPURCHASED)
            val details = MutableStateFlow<SkuDetails?>(null)
            // this initialization calls querySkuDetailsAsync() when the first
            //  subscriber appears
            details.subscriptionCount.map { count ->
                count > 0
            } // map count into active/inactive flag
                .distinctUntilChanged()
                .onEach { isActive -> // configure an action
                    if (isActive) {
                        querySkuDetailsAsync()
                    }
                }
                .launchIn(defaultScope) // launch it inside defaultScope

            skuStateMap[sku] = skuState
            skuDetailsMap[sku] = details
        }
    }

    private suspend fun querySkuDetailsAsync() {
        if (!inAppSKUs.isNullOrEmpty()) {
            val skuDetailsResult = billingClient.querySkuDetails(
                SkuDetailsParams.newBuilder()
                    .setType(BillingClient.SkuType.INAPP)
                    .setSkusList(inAppSKUs.toMutableList())
                    .build()
            )
            // Process the result
            onSkuDetailsResponse(
                skuDetailsResult.billingResult,
                skuDetailsResult.skuDetailsList
            )
        }
    }

    private suspend fun restorePurchases() {
        val purchasesResult =
            billingClient
                .queryPurchasesAsync(BillingClient.SkuType.INAPP)
        val billingResult = purchasesResult.billingResult
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            handlePurchase(purchasesResult.purchasesList)
        }
    }

    fun launchBillingFlow(activity: Activity, sku: String) {
        val skuDetails = skuDetailsMap[sku]?.value
        if (null != skuDetails) {
            val flowParams = BillingFlowParams.newBuilder()
                .setSkuDetails(skuDetails)
                .build()
            billingClient.launchBillingFlow(activity, flowParams)
        }
        Log.e(TAG, "SkuDetails not found for: $sku")
    }

    /**
     * This is called right after [querySkuDetailsAsync]. It gets all the skus available
     * and get the details for all of them.
     */
    private fun onSkuDetailsResponse(billingResult: BillingResult, skuDetailsList: List<SkuDetails>?) {
        val responseCode = billingResult.responseCode
        val debugMessage = billingResult.debugMessage
        when (responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                Log.i(TAG, "onSkuDetailsResponse: $responseCode $debugMessage")
                if (skuDetailsList == null || skuDetailsList.isEmpty()) {
                    Log.e(
                        TAG,
                        "onSkuDetailsResponse: " +
                                "Found null or empty SkuDetails. " +
                                "Check to see if the SKUs you requested are correctly published " +
                                "in the Google Play Console."
                    )
                } else {
                    for (skuDetails in skuDetailsList) {
                        val sku = skuDetails.sku
                        val detailsMutableFlow = skuDetailsMap[sku]
                        detailsMutableFlow?.tryEmit(skuDetails) ?: Log.e(TAG, "Unknown sku: $sku")
                    }
                }
            }
        }
    }

    /**
     * The title of our SKU from SkuDetails.
     * @param SKU to get the title from
     * @return title of the requested SKU as an observable
     * */
    fun getSkuTitle(sku: String): Flow<String> {
        val skuDetailsFlow = skuDetailsMap[sku]!!
        return skuDetailsFlow.mapNotNull { skuDetails ->
            skuDetails?.title
        }
    }

    fun getSkuPrice(sku: String): Flow<String> {
        val skuDetailsFlow = skuDetailsMap[sku]!!
        return skuDetailsFlow.mapNotNull { skuDetails ->
            skuDetails?.price
        }
    }

    fun getSkuDescription(sku: String): Flow<String> {
        val skuDetailsFlow = skuDetailsMap[sku]!!
        return skuDetailsFlow.mapNotNull { skuDetails ->
            skuDetails?.description
        }
    }

    companion object {
        @Volatile
        private var singleton: Billing? = null

        @JvmStatic
        fun getInstance(
            application: Application,
            defaultScope: CoroutineScope,
            inAppSKUs: Array<String>,
            setProValue: (Boolean) -> Unit
        ) = singleton ?: synchronized(this) {
            singleton ?: Billing(
                application,
                defaultScope,
                inAppSKUs,
                setProValue
            ).also { singleton = it }
        }
    }

    private val TAG = "MicCheckBilling"
}