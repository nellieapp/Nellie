package com.angelhack.nellie.api;

import retrofit.RestAdapter;
import com.angelhack.nellie.model.Face;

import java.util.List;

/**
 * Created by wimagguc on 20/06/15.
 */
public final class IdolHelper {

    // TODO use singleton pattern instead

    private IdolHelper() {}

    public static IdolService getIdolService() {
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint("https://api.idolondemand.com/1/api")
                .build();

        IdolService service = restAdapter.create(IdolService.class);

        return service;
    }

    public static List<Face> listFaces() {
        return IdolHelper.getIdolService().listFaces("9c882743-ad02-4437-a32f-5f300742e085", "http://startalk.nhlrc.ucla.edu/startalk/images/group-photo.jpg").getFace();
    }

}
