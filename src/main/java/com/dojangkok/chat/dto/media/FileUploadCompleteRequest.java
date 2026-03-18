package com.dojangkok.chat.dto.media;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class FileUploadCompleteRequest {

    private List<String> fileAssetIds;
}
