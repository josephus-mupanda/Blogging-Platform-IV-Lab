package com.example.Blogging_Platform_III.model;

import com.example.Blogging_Platform_III.entity.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TagResponse {
    private long id;
    private String name;

    public static TagResponse from(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName());
    }
}
