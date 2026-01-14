package com.example.fileservice.controller;

import com.example.fileservice.dto.response.FileInfoDto;
import com.example.fileservice.dto.response.FileUploadResponseDto;
import com.example.fileservice.model.FileCategory;
import com.example.fileservice.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "File Management", description = "APIs for uploading, downloading, and managing files in MinIO storage")
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    @Operation(summary = "Upload a file", description = "Upload a single file to the default category")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "File uploaded successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid file or file too large")
    })
    public ResponseEntity<FileUploadResponseDto> uploadFile(
        @Parameter(description = "File to upload", required = true)
        @RequestParam("file") MultipartFile file
    ) {
        log.info("Upload request received for file: {}", file.getOriginalFilename());
        FileUploadResponseDto response = fileService.uploadFile(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/upload/{category}")
    @Operation(summary = "Upload a file to category", description = "Upload a single file to a specific category (SHOP_AVATAR, SHOP_BANNER, PRODUCT_IMAGE, USER_AVATAR)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "File uploaded successfully with thumbnail if applicable"),
        @ApiResponse(responseCode = "400", description = "Invalid file, category, or file too large")
    })
    public ResponseEntity<FileUploadResponseDto> uploadFileToCategory(
        @Parameter(description = "File to upload", required = true)
        @RequestParam("file") MultipartFile file,
        @Parameter(description = "File category", required = true)
        @PathVariable String category
    ) {
        log.info("Upload request received for file: {} to category: {}", file.getOriginalFilename(), category);
        FileCategory fileCategory = FileCategory.fromString(category);
        FileUploadResponseDto response = fileService.uploadFile(file, fileCategory);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/upload/{category}/batch")
    @Operation(summary = "Batch upload files", description = "Upload multiple files to a specific category")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Files uploaded successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid files or category")
    })
    public ResponseEntity<List<FileUploadResponseDto>> uploadFilesToCategory(
        @Parameter(description = "Files to upload", required = true)
        @RequestParam("files") List<MultipartFile> files,
        @Parameter(description = "File category", required = true)
        @PathVariable String category
    ) {
        log.info("Batch upload request received for {} files to category: {}", files.size(), category);
        FileCategory fileCategory = FileCategory.fromString(category);
        List<FileUploadResponseDto> responses = fileService.uploadFiles(files, fileCategory);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    @PostMapping("/upload/{category}/async")
    @Operation(summary = "Async upload a file", description = "Upload a file asynchronously for better performance")
    public CompletableFuture<ResponseEntity<FileUploadResponseDto>> uploadFileAsync(
        @RequestParam("file") MultipartFile file,
        @PathVariable String category
    ) {
        log.info("Async upload request received for file: {} to category: {}", file.getOriginalFilename(), category);
        FileCategory fileCategory = FileCategory.fromString(category);
        return fileService.uploadFileAsync(file, fileCategory)
                .thenApply(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @GetMapping("/download/{*filePath}")
    @Operation(summary = "Download a file", description = "Download a file by its full path")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "File downloaded successfully"),
        @ApiResponse(responseCode = "404", description = "File not found")
    })
    public ResponseEntity<InputStreamResource> downloadFile(
        @Parameter(description = "Full file path including category prefix")
        @PathVariable String filePath
    ) {
        log.info("Download request received for file: {}", filePath);
        InputStream fileStream = fileService.getFile(filePath);
        
        String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(fileStream));
    }

    @DeleteMapping("/{*filePath}")
    @Operation(summary = "Delete a file", description = "Delete a file by its full path")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "File deleted successfully"),
        @ApiResponse(responseCode = "404", description = "File not found")
    })
    public ResponseEntity<Void> deleteFile(
        @Parameter(description = "Full file path including category prefix")
        @PathVariable String filePath
    ) {
        log.info("Delete request received for file: {}", filePath);
        fileService.deleteFile(filePath);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "List all files", description = "List all files in storage")
    public ResponseEntity<List<FileInfoDto>> listFiles() {
        log.info("List files request received");
        List<FileInfoDto> files = fileService.listFiles();
        return ResponseEntity.ok(files);
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "List files by category", description = "List all files in a specific category")
    public ResponseEntity<List<FileInfoDto>> listFilesByCategory(
        @Parameter(description = "File category", required = true)
        @PathVariable String category
    ) {
        log.info("List files request received for category: {}", category);
        FileCategory fileCategory = FileCategory.fromString(category);
        List<FileInfoDto> files = fileService.listFiles(fileCategory);
        return ResponseEntity.ok(files);
    }

    @GetMapping("/exists/{*filePath}")
    @Operation(summary = "Check if file exists", description = "Check if a file exists in storage")
    public ResponseEntity<Boolean> fileExists(
        @Parameter(description = "Full file path")
        @PathVariable String filePath
    ) {
        boolean exists = fileService.fileExists(filePath);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/url")
    @Operation(summary = "Get file URL", description = "Get the public URL for a file")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "URL returned successfully"),
        @ApiResponse(responseCode = "404", description = "File not found")
    })
    public ResponseEntity<String> getFileUrl(
        @Parameter(description = "File name/path", required = true)
        @RequestParam String fileName
    ) {
        String url = fileService.getFileUrl(fileName);
        return ResponseEntity.ok(url);
    }

    @GetMapping("/categories")
    @Operation(summary = "Get available categories", description = "Get list of available file categories")
    public ResponseEntity<FileCategory[]> getCategories() {
        return ResponseEntity.ok(FileCategory.values());
    }
}