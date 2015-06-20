package com.angelhack.nellie.api;

import com.angelhack.nellie.model.Face;
import com.angelhack.nellie.model.FaceResponse;

import java.util.List;

import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;

/**
 * Created by wimagguc on 20/06/15.
 */
public interface IdolService {

    @FormUrlEncoded
    @POST("/sync/detectfaces/v1")
    FaceResponse listFaces(@Field("apikey") String apiKey,
                           @Field("url") String url);

}
