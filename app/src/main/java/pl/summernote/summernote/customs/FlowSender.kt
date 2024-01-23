package pl.summernote.summernote.customs

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class FlowSender {
    fun sendFlowInformation(name: String, uuid: String, type: String){
        val client = OkHttpClient()

        val requestBody = FormBody.Builder()
            .add("name", name)
            .add("uuid", uuid)
            .build()
        Log.d("DUPAJAJ", uuid)
        val request = Request.Builder()
            .url("https://www.briefpic.me/php/flow.php")
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d("DUPAJAJ", " ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("DUPAJAJ", " $responseBody, $name")
            }
        })
    }
}