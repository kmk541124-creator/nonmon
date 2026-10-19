package me.kimmukwan.nonmon.fileset.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OllamaResponse(
    String feature,
    String check,
    List<OllamaItemResponse> mat
) {
}
