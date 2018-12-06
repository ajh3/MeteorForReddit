package com.aaronhalbert.nosurfforreddit.repository;

import com.aaronhalbert.nosurfforreddit.BuildConfig;
import com.aaronhalbert.nosurfforreddit.repository.redditschema.Listing;

import java.util.List;

import io.reactivex.Maybe;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Path;

interface RetrofitContentInterface {

    @Headers({BuildConfig.USER_AGENT})
    @GET("r/all/hot")
    Maybe<Listing> fetchAllPostsASync(
            @Header("Authorization") String authorization);

    @Headers({BuildConfig.USER_AGENT})
    @GET("hot")
    Call<Listing> fetchSubscribedPostsASync(
            @Header("Authorization") String authorization);

    @Headers({BuildConfig.USER_AGENT})
    @GET("comments/{article}")
    Call<List<Listing>> fetchPostCommentsASync(
            @Header("Authorization") String authorization,
            @Path("article") String article);
}
