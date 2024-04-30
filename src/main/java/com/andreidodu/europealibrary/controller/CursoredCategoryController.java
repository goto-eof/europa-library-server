package com.andreidodu.europealibrary.controller;

import com.andreidodu.europealibrary.annotation.auth.AllowOnlyAdministrator;
import com.andreidodu.europealibrary.dto.*;
import com.andreidodu.europealibrary.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/category")
@RequiredArgsConstructor
public class CursoredCategoryController {
    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<CursorDTO<CategoryDTO>> retrieveCategories(@RequestBody CommonCursoredRequestDTO commonCursoredRequestDTO) {
        return ResponseEntity.ok(this.categoryService.retrieveAllCategories(commonCursoredRequestDTO));
    }

    @AllowOnlyAdministrator
    @PostMapping("/rename")
    public ResponseEntity<OperationStatusDTO> bulkCategoryRename(@Valid @RequestBody RenameDTO renameDTO) {
        return ResponseEntity.ok(this.categoryService.bulkRename(renameDTO));
    }
}
