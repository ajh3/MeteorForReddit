
package com.aaronhalbert.nosurfforreddit.data.repository.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("ALL")
public class Child {
    @SerializedName("kind")
    @Expose
    private String kind;

    @SerializedName("data")
    @Expose
    private Data_ data;

    public String getKind() {
        return kind;
    }

    public Data_ getData() {
        return data;
    }
}
