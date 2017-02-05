package me.li2.android.photogallery.model;

import android.net.Uri;
import android.util.Log;

public class GalleryItem {
    private int stableId; // DraggableItemAdapter requires stable ID
    private String title;
    private String id;
    private String url_s;
    private String owner;

    public GalleryItem() {
        stableId = 0;
        title = "";
        id = "";
        url_s = "";
        owner = "";
    }
    
    public String toString() {
        return title;
    }

    public int getStableId() {
        return stableId;
    }

    public void setStableId(int stableId) {
        this.stableId = stableId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url_s;
    }

    public void setUrl(String url) {
        url_s = url;
    }

    public String getOwner() {
        return owner;
    }
    
    public void setOwner(String owner) {
        this.owner = owner;
    }
    
    // Build photo page url from XML base on Flickr's doc.
    public Uri getPhotoPageUrl() {
        String url = "http://www.flickr.com/photos/" + owner + "/" + id;
        return Uri.parse(url);
    }

    public void print() {
        Log.i("GalleryItem", "stableId " + stableId + ", url_s " + url_s);
    }
}
