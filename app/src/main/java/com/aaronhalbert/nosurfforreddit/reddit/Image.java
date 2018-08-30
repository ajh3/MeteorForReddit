
package com.aaronhalbert.nosurfforreddit.reddit;

import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Image {

    @SerializedName("source")
    @Expose
    private Source source;

    @SerializedName("id")
    @Expose
    private String id;

    public Source getSource() {
        return source;
    }


    public String getId() {
        return id;
    }



}
