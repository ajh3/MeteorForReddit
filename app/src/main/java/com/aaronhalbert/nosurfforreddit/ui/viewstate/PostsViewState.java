package com.aaronhalbert.nosurfforreddit.ui.viewstate;

import java.util.ArrayList;

/* data structure to hold cleaned/transformed post data from the Reddit API */

public class PostsViewState {
    private static final int INITIAL_CAPACITY = 25;
    public final ArrayList<PostDatum> postData;
    public final boolean[] hasBeenClicked;

    public PostsViewState() {
        postData = new ArrayList<>(INITIAL_CAPACITY);
        hasBeenClicked = new boolean[INITIAL_CAPACITY];

        // make sure initial *size* (not just capacity) of postData == INITIAL_CAPACITY
        // so we can start set()ing its elements right away from the ViewModel
        for (int i = 0; i < INITIAL_CAPACITY; i++) {
            postData.add(new PostDatum());
        }
    }

    public static class PostDatum {
        public boolean isSelf;
        public int score;
        public int numComments;
        public String id;
        public String title;
        public String author;
        public String subreddit;
        public String url;
        public String thumbnailUrl;
        public String imageUrl;
        public String selfTextHtml;
        public boolean isNsfw;
        public String permalink;
    }
}
