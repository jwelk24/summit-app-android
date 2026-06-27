package com.summit.android.service

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface PlaidApi {
    @POST("/api/link/token/create")
    suspend fun createLinkToken(@Body body: Map<String, String>): LinkTokenResponse

    @POST("/api/item/public_token/exchange")
    suspend fun exchangePublicToken(@Body body: Map<String, String>): ExchangeResponse

    @GET("/api/accounts")
    suspend fun getAccounts(@Header("X-Plaid-Access-Token") accessToken: String): AccountsResponse

    @POST("/api/transactions/sync")
    suspend fun syncTransactions(
        @Header("X-Plaid-Access-Token") accessToken: String,
        @Body body: SyncBody
    ): SyncResponse
}

data class LinkTokenResponse(val linkToken: String, val hostedLinkUrl: String)
data class ExchangeResponse(val accessToken: String, val itemId: String)
data class AccountsResponse(val item: PlaidItem, val accounts: List<PlaidAccount>)
data class PlaidItem(val item_id: String, val institution_id: String?)
data class PlaidAccount(
    val account_id: String,
    val name: String,
    val official_name: String?,
    val mask: String?,
    val type: String,
    val subtype: String?,
    val balances: Balances
)
data class Balances(val available: Double?, val current: Double?, val iso_currency_code: String?)

data class SyncBody(val cursor: String?)
data class SyncResponse(
    val added: List<PlaidTransaction>,
    val modified: List<PlaidTransaction>,
    val removed: List<RemovedTransaction>,
    val nextCursor: String?
)
data class PlaidTransaction(
    val transaction_id: String,
    val account_id: String,
    val amount: Double,
    val date: String,
    val name: String
)
data class RemovedTransaction(val transaction_id: String)

object PlaidService {
    private const val BASE_URL = "http://10.0.2.2:8080" // Android emulator localhost

    val api: PlaidApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(PlaidApi::class.java)
}
