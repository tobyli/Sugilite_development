package edu.cmu.hcii.sugilite.sharing.model;

import java.util.Date;

import javax.annotation.Nullable;

/**
 * @author toby
 * @date 9/24/19
 * @time 1:39 PM
 */
public class SugiliteRepoListing {
    private int id;
    private String title;
    private String author;
    private Date uploadedTimeStamp;

    public SugiliteRepoListing (int id, String title, @Nullable String author, @Nullable Date uploadedTimeStamp) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.uploadedTimeStamp = uploadedTimeStamp;
    }

    public SugiliteRepoListing (int id, String title) {
       this(id, title, null, null);
    }


    public void setId(int id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setUploadedTimeStamp(Date uploadedTimeStamp) {
        this.uploadedTimeStamp = uploadedTimeStamp;
    }

    public int getId() {
        return id;
    }

    public String getAuthor() {
        return author;
    }

    public String getTitle() {
        return title;
    }

    public Date getUploadedTimeStamp() {
        return uploadedTimeStamp;
    }
}
