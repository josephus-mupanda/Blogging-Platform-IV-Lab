package com.example.Blogging_Platform_IV.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.Locale;

@Getter
public class TagInput {
    @NotBlank
    @Size(max = 50)
    private String name;

    public void setName(String name) {
        this.name = name == null ? null : name.trim().toLowerCase(Locale.ROOT);
    }
}
