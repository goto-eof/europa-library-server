package com.andreidodu.europealibrary.resource;

import com.andreidodu.europealibrary.annotation.security.AllowCalculatedAuthorities;
import com.andreidodu.europealibrary.annotation.security.AllowOnlyAuthenticatedUsers;
import com.andreidodu.europealibrary.dto.*;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@RequestMapping("/api/v2/file")
public interface CursoredFileResource {
    @GetMapping("/{fileSystemItemId}")
    ResponseEntity<FileSystemItemDTO> get(@PathVariable Long fileSystemItemId);

    @PostMapping("/cursored")
    ResponseEntity<CursoredFileSystemItemDTO> retrieveCursored(@RequestBody CursorRequestDTO cursorRequestDTO);

    @GetMapping("/cursored")
    ResponseEntity<CursoredFileSystemItemDTO> retrieveCursoredRoot();

    @PostMapping("/cursored/category")
    ResponseEntity<CursoredCategoryDTO> retrieveByCategoryId(@RequestBody CursorRequestDTO cursorRequestDTO);

    @PostMapping("/cursored/tag")
    ResponseEntity<CursoredTagDTO> retrieveByTagId(@RequestBody CursorRequestDTO cursorRequestDTO);

    @PostMapping("/cursored/language")
    ResponseEntity<GenericCursoredResponseDTO<String, FileSystemItemDTO>> retrieveByLanguage(@RequestBody GenericCursorRequestDTO<String> cursorRequestDTO);

    @PostMapping("/cursored/publishedDate")
    ResponseEntity<GenericCursoredResponseDTO<String, FileSystemItemDTO>> retrieveByPublishedDate(@RequestBody GenericCursorRequestDTO<String> cursorRequestDTO);

    @PostMapping("/cursored/publisher")
    ResponseEntity<GenericCursoredResponseDTO<String, FileSystemItemDTO>> retrieveByPublisher(@RequestBody GenericCursorRequestDTO<String> cursorRequestDTO);

    @PostMapping("/cursored/extension")
    ResponseEntity<CursoredFileExtensionDTO> retrieveCursoredByFileExtension(@RequestBody CursorTypeRequestDTO cursorTypeRequestDTO);

    @GetMapping("/extension")
    ResponseEntity<List<FileExtensionDTO>> retrieveFileExtensions();

    @AllowCalculatedAuthorities
    @GetMapping(path = "/download/{fileSystemItemId}")
    ResponseEntity<InputStreamResource> download(@PathVariable Long fileSystemItemId);

    @PostMapping("/cursored/search")
    ResponseEntity<SearchResultDTO<SearchFileSystemItemRequestDTO, FileSystemItemDTO>> search(@RequestBody SearchFileSystemItemRequestDTO searchFileSystemItemRequestDTO);

    @PostMapping("/cursored/rating")
    ResponseEntity<GenericCursoredResponseDTO<String, FileSystemItemDTO>> retrieveCursoredByRating(@RequestBody CursorRequestDTO cursorRequestDTO);

    @PostMapping(path = "/cursored/downloadCount")
    ResponseEntity<GenericCursoredResponseDTO<String, FileSystemItemDTO>> retrieveCursoredByDownloadCount(@Valid @RequestBody CursoredRequestByFileTypeDTO cursoredRequestByFileTypeDTO);

    @PostMapping(path = "/cursored/downloadCount/highlight")
    ResponseEntity<GenericCursoredResponseDTO<String, FileSystemItemHighlightDTO>> retrieveCursoredByDownloadCountHighlight(@Valid @RequestBody CursoredRequestByFileTypeDTO cursoredRequestByFileTypeDTO);

    @PostMapping(path = "/cursored/new")
    ResponseEntity<GenericCursoredResponseDTO<String, FileSystemItemDTO>> retrieveCursoredNew(@Valid @RequestBody CursorCommonRequestDTO commonRequestDTO);

    @PostMapping(path = "/cursored/new/highlight")
    ResponseEntity<GenericCursoredResponseDTO<String, FileSystemItemHighlightDTO>> retrieveCursoredNewHighlight(@Valid @RequestBody CursorCommonRequestDTO commonRequestDTO);

}
