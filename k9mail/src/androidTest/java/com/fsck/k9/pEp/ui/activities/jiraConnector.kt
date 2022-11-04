package com.fsck.k9.pEp.ui.activities

import android.util.Log
import okhttp3.OkHttpClient
import retrofit2.Call;
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import retrofit2.http.*;


interface jiraConnector {

    val headers: Headers

    suspend fun rawJSON(jsonObjectString: String, auth: String) {
        val retrofit = ServiceBuilder.buildService(APIInterface::class.java)
        val obj = RequestModel(jsonObjectString, auth, headers)

        //val requestBody = jsonObjectString.toRequestBody("application/json".toMediaTypeOrNull())

        retrofit.requestLogin(obj).enqueue(
            object : Callback<ResponseClass> {
                override fun onResponse(
                    call: Call<ResponseClass>,
                    response: Response<ResponseClass>
                ) {
                    Log.d("Pretty Printed JSON :", "prettyJson")
                }
                override fun onFailure(call: Call<ResponseClass>, t: Throwable) {
                    Log.e("RETROFIT_ERROR", "Message")
                }
            }
        )
    }
}

interface APIInterface {

    @Headers(
        "Accept: application/json",
        "Content-type:application/json"
    )
    //@Headers(value = ["Accept: application/json",
    //    "Content-type:application/json"])

    @POST("api/v1/create") //201 when created
    suspend fun requestLogin(@Body requestModel: RequestModel) : Call<ResponseClass>
}

object ServiceBuilder {
    private val client = OkHttpClient.Builder().build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://pep.foundation/jira/") //
        .addConverterFactory(GsonConverterFactory.create())
        .client(client)
        .build()
    fun<T> buildService(service: Class<T>): T{
        return retrofit.create(service)
    }
}

class ResponseClass (
    val message: String
)

data class RequestModel(
    val report: String,
    val auth: String,
    val headers: Headers,
)



