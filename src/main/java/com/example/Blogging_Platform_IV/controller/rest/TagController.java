package com.example.Blogging_Platform_IV.controller.rest;

import static com.example.Blogging_Platform_IV.controller.rest.ApiResponse.ok;

import com.example.Blogging_Platform_IV.dto.TagInput;
import com.example.Blogging_Platform_IV.entity.Tag;
import com.example.Blogging_Platform_IV.service.TagService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tags")
public class TagController {
    private final TagService tags;

    public TagController(TagService tags) {
        this.tags = tags;
    }

    @GetMapping
    ApiResponse<List<Tag>> list() {
        return ok("Tags found.", tags.list());
    }

    @GetMapping("/{id}")
    ApiResponse<Tag> get(@PathVariable Long id) {
        return ok("Tag found.", tags.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<Tag> create(@Valid @RequestBody TagInput input) {
        return ok("Tag created.", tags.create(input.getName()));
    }

    @PutMapping("/{id}")
    ApiResponse<Tag> update(@PathVariable Long id, @Valid @RequestBody TagInput input) {
        return ok("Tag updated.", tags.update(id, input.getName()));
    }

    @DeleteMapping("/{id}")
    ApiResponse<Void> delete(@PathVariable Long id) {
        tags.delete(id);
        return ok("Tag deleted.", null);
    }
}
