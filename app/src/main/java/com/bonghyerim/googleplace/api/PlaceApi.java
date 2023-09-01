package com.bonghyerim.googleplace.api;

import com.bonghyerim.googleplace.model.PlaceList;

import retrofit2.Call;
import retrofit2.http.GET;

import retrofit2.http.Query;

public interface PlaceApi {

    @GET("/maps/api/place/nearbysearch/json")
    Call<PlaceList> getPlaceList(@Query("language") String language,
                                 @Query("location") String location,
                                 @Query("radius") int radius,
                                 @Query("key") String key,
                                 @Query("keyword") String keyword);

}