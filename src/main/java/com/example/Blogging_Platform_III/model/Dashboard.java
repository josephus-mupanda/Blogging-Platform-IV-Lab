package com.example.Blogging_Platform_III.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Dashboard {
    private long users;
    private long posts;
    private long tags;
    private long comments;
    private long reviews;
}
