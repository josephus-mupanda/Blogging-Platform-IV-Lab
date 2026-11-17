package com.example.Blogging_Platform_III.model;

import com.example.Blogging_Platform_III.entity.Review;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Discussion {
    private List<Review> reviews = List.of();
    private double averageRating;
}
