package pl.summernote.summernote.interfaces

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import pl.summernote.summernote.dataclasses.FlaskResponse
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @Multipart
    @POST("summarize")
    suspend fun uploadImage(
        @Part image: MultipartBody.Part,
        @Part("points") points: RequestBody,
        @Part("ratio") ratio: RequestBody
    ): String
}