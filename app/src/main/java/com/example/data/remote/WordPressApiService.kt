package com.example.data.remote

import com.example.data.remote.model.WordPressMediaDto
import com.example.data.remote.model.WordPressPostDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface WordPressApiService {

    @GET("posts")
    suspend fun getPosts(
        @Query("_embed") embed: Boolean = true,
        @Query("per_page") perPage: Int = 20,
        @Query("page") page: Int = 1
    ): List<WordPressPostDto>

    @GET("posts/{id}")
    suspend fun getPostById(
        @Path("id") id: Long,
        @Query("_embed") embed: Boolean = true
    ): WordPressPostDto

    @GET("media")
    suspend fun getMedia(
        @Query("per_page") perPage: Int = 40,
        @Query("page") page: Int = 1
    ): List<WordPressMediaDto>
}

