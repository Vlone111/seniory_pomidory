package com.example.fileservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileInfoDto {

    private String fileName;
    private Long size;
    private String contentType;
    private String url;
}
