package me.kimmukwan.nonmon.fileset.controller;

import lombok.RequiredArgsConstructor;
import me.kimmukwan.nonmon.fileset.dto.response.OllamaResponse;
import me.kimmukwan.nonmon.fileset.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class FileController {

    private final DocumentService documentService;

    @PostMapping("/analyze")
    public ResponseEntity<OllamaResponse> analyze(@RequestParam("file") MultipartFile file) throws Exception{
        String text;
        OllamaResponse ollamaResponse =  documentService.analyze(file);

        return ResponseEntity.ok().body(ollamaResponse);
    }
}
