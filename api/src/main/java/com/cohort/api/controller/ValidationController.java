package com.cohort.api.controller;

import com.cohort.api.dto.TextCheckRequest;
import com.cohort.api.dto.TextCheckResponse;
import com.cohort.api.dto.TextCheckResponse.Finding;
import com.cohort.api.validation.TextQuality;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/validate")
public class ValidationController {

    @PostMapping("/text")
    public TextCheckResponse checkText(@Valid @RequestBody TextCheckRequest request) {
        if (request.text().isEmpty()) {
            return new TextCheckResponse(true, List.of(), List.of());
        }
        List<Finding> errors = TextQuality.problem(request.text()).stream()
                .map(problem -> new Finding(problem.name(), problem.message()))
                .toList();
        List<Finding> warnings = errors.isEmpty()
                ? TextQuality.hints(request.text(), request.experience()).stream()
                        .map(hint -> new Finding(hint.name(), hint.message()))
                        .toList()
                : List.of();
        return new TextCheckResponse(errors.isEmpty(), errors, warnings);
    }
}
