package com.angelhack.nellie.model;

import java.util.List;

/**
 * Created by wimagguc on 20/06/15.
 */
public class FaceResponse {

    private List<Face> face;
    private List<Object> additional_information;

    public List<Face> getFace() {
        return face;
    }

    public void setFace(List<Face> face) {
        this.face = face;
    }

    public void setAdditional_information(List<Object> additional_information) {
        this.additional_information = additional_information;
    }

    public List<Object> getAdditional_information() {
        return additional_information;
    }
}
