package com.example.data.remote

import com.example.data.remote.model.WordPressImageCptDto
import com.example.data.remote.model.WordPressMediaDto
import com.example.data.remote.model.WordPressPostDto
import com.example.data.remote.model.WordPressVideoCptDto
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

    @GET("images")
    suspend fun getImageAlbums(
        @Query("_embed") embed: Boolean = true,
        @Query("per_page") perPage: Int = 50,
        @Query("page") page: Int = 1
    ): List<WordPressImageCptDto>

    @GET("images/{id}")
    suspend fun getImageAlbumById(
        @Path("id") id: Long,
        @Query("_embed") embed: Boolean = true
    ): WordPressImageCptDto

    @GET("videos")
    suspend fun getVideos(
        @Query("_embed") embed: Boolean = true,
        @Query("per_page") perPage: Int = 50,
        @Query("page") page: Int = 1
    ): List<WordPressVideoCptDto>

    @GET("videos/{id}")
    suspend fun getVideoById(
        @Path("id") id: Long,
        @Query("_embed") embed: Boolean = true
    ): WordPressVideoCptDto

    @GET("media")
    suspend fun getMedia(
        @Query("per_page") perPage: Int = 40,
        @Query("page") page: Int = 1,
        @Query("include") include: String? = null
    ): List<WordPressMediaDto>
}

