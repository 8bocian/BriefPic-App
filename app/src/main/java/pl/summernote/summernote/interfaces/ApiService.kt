package pl.summernote.summernote.interfaces

import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @Multipart
    @POST("summarize")
    suspend fun uploadImage(
        @Part("text") text: RequestBody,
        @Part("length") length: RequestBody
    ): String
}