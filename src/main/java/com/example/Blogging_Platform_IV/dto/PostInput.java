package com.example.Blogging_Platform_IV.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
public class PostInput {
    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    private String body;

    @Setter
    @Size(max = 100)
    private List<@NotNull @Positive Long> tagIds;

    public void setTitle(String title) {
        this.title = title == null ? null : title.trim();
    }

    public void setBody(String body) {
        this.body = body == null ? null : body.trim();
    }

}
