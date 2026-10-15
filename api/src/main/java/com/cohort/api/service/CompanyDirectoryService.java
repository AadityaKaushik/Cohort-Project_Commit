package com.cohort.api.service;

import com.cohort.api.dto.DirectoryCompanyDetail;
import com.cohort.api.dto.DirectoryCompanyListItem;
import com.cohort.api.dto.DirectoryRolePattern;
import com.cohort.api.entity.CompanyProfile;
import com.cohort.api.entity.CompanyRolePattern;
import com.cohort.api.exception.CompanyNotFoundException;
import com.cohort.api.repository.CompanyProfileRepository;
import com.cohort.api.repository.CompanyRolePatternRepository;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** US-96: read-only directory of sourced general interview patterns. */
@Service
public class CompanyDirectoryService {

    static final int COMMUNITY_MIN_CONTRIBUTORS = 5;

    private final CompanyProfileRepository profiles;
    private final CompanyRolePatternRepository patterns;

    public CompanyDirectoryService(CompanyProfileRepository profiles, CompanyRolePatternRepository patterns) {
        this.profiles = profiles;
        this.patterns = patterns;
    }

    @Transactional(readOnly = true)
    public List<DirectoryCompanyListItem> list(String query) {
        String term = query == null ? "" : query.trim();
        List<CompanyProfile> found = profiles.findByNameContainingIgnoreCaseOrderByNameAsc(term);
        Map<UUID, List<String>> familiesByCompany = patterns
                .findByCompanyIdIn(found.stream().map(CompanyProfile::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(
                        CompanyRolePattern::getCompanyId,
                        Collectors.mapping(CompanyRolePattern::getRoleFamily, Collectors.collectingAndThen(
                                Collectors.toCollection(TreeSet::new), List::copyOf))));
        return found.stream()
                .map(profile -> new DirectoryCompanyListItem(
                        profile.getSlug(),
                        profile.getName(),
                        profile.getIndustry(),
                        profile.getWebsite(),
                        familiesByCompany.getOrDefault(profile.getId(), List.of()),
                        profile.getLastChecked()))
                .toList();
    }

    @Transactional(readOnly = true)
    public DirectoryCompanyDetail get(String slug) {
        CompanyProfile profile = profiles.findBySlug(slug).orElseThrow(CompanyNotFoundException::new);
        List<DirectoryRolePattern> rolePatterns = patterns.findByCompanyIdOrderByRoleFamilyAscLevelAsc(profile.getId())
                .stream()
                .map(pattern -> new DirectoryRolePattern(
                        pattern.getRoleFamily(),
                        pattern.getLevel(),
                        pattern.getRounds(),
                        pattern.getTopicSlugs(),
                        pattern.getNotes(),
                        pattern.getExperienceSummary(),
                        pattern.getPay(),
                        pattern.getSources(),
                        pattern.getLastChecked()))
                .toList();
        return new DirectoryCompanyDetail(
                profile.getSlug(),
                profile.getName(),
                profile.getIndustry(),
                profile.getLocations(),
                profile.getSizeBand(),
                profile.getWebsite(),
                profile.getSummary(),
                profile.getSources(),
                profile.getLastChecked(),
                null,
                COMMUNITY_MIN_CONTRIBUTORS,
                rolePatterns);
    }
}
