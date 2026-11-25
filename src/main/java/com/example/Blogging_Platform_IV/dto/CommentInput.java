package com.example.Blogging_Platform_IV.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class CommentInput {
    @NotBlank
    private String body;

    public void setBody(String body) {
        this.body = body == null ? null : body.trim();
    }
}
