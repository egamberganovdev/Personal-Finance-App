package dev.egamberganov.finflow.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class ExchangeRateApiResponse(
    @Json(name = "result") val result: String?,
    @Json(name = "base_code") val baseCode: String?,
    @Json(name = "rates") val rates: Map<String, Double>?
)

interface ExchangeRateApi {
    @GET("v6/latest/{base}")
    suspend fun getLatestRates(@Path("base") base: String): ExchangeRateApiResponse
}

class ExchangeRateService(
    private val api: ExchangeRateApi = createDefaultApi()
) {
    suspend fun getExchangeRate(
        fromCurrency: String,
        toCurrency: String
    ): Result<Double> = withContext(Dispatchers.IO) {
        val from = fromCurrency.trim().uppercase()
        val to = toCurrency.trim().uppercase()

        if (from == to) {
            return@withContext Result.success(1.0)
        }

        try {
            val response = api.getLatestRates(from)
            if (response.result != "success" || response.rates == null) {
                return@withContext Result.failure(
                    Exception("Failed to fetch exchange rate for $from")
                )
            }

            val rate = response.rates[to]
            if (rate != null && rate > 0.0) {
                Result.success(rate)
            } else {
                Result.failure(Exception("Exchange rate for $to not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private fun createDefaultApi(): ExchangeRateApi {
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl("https://open.er-api.com/")
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()

            return retrofit.create(ExchangeRateApi::class.java)
        }
    }
}
