package com.cohort.api.controller;

import com.cohort.api.dto.DirectoryCompanyDetail;
import com.cohort.api.dto.DirectoryCompanyListItem;
import com.cohort.api.service.CompanyDirectoryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** US-96: shared reference data, so there is no per-user scoping. Authentication is still required. */
@RestController
@RequestMapping("/api/v1/directory/companies")
public class CompanyDirectoryController {

    private final CompanyDirectoryService directoryService;

    public CompanyDirectoryController(CompanyDirectoryService directoryService) {
        this.directoryService = directoryService;
    }

    @GetMapping
    public List<DirectoryCompanyListItem> list(@RequestParam(required = false) String query) {
        return directoryService.list(query);
    }

    @GetMapping("/{slug}")
    public DirectoryCompanyDetail get(@PathVariable String slug) {
        return directoryService.get(slug);
    }
}
