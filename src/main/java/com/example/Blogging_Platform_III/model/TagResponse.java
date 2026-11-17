package com.example.Blogging_Platform_III.model;

import com.example.Blogging_Platform_III.entity.Tag;

public class TagResponse {
    private long id;
    private String name;

    public TagResponse() {
    }

    public TagResponse(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public static TagResponse from(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName());
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
