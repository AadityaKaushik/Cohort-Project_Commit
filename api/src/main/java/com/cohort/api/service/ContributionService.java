package com.cohort.api.service;

import com.cohort.api.dto.CommunitySummary;
import com.cohort.api.dto.CommunitySummary.RoundShare;
import com.cohort.api.dto.ContributionResponse;
import com.cohort.api.dto.ContributionRoundInput;
import com.cohort.api.dto.CreateContributionRequest;
import com.cohort.api.entity.CompanyProfile;
import com.cohort.api.entity.ExperienceContribution;
import com.cohort.api.entity.ExperienceContribution.RoundType;
import com.cohort.api.exception.CompanyNotFoundException;
import com.cohort.api.exception.ContributionNotFoundException;
import com.cohort.api.exception.DuplicateContributionException;
import com.cohort.api.exception.InvalidContributionException;
import com.cohort.api.repository.CompanyProfileRepository;
import com.cohort.api.repository.ExperienceContributionRepository;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** US-96: anonymous contributions. The user id only feeds a per-company hash; the table has no user column. */
@Service
public class ContributionService {

    static final int MIN_CONTRIBUTORS = 5;
    private static final int MAX_TIPS = 5;
    private static final int YEARS_BACK = 10;

    private final CompanyProfileRepository profiles;
    private final ExperienceContributionRepository contributions;
    private final String salt;

    public ContributionService(
            CompanyProfileRepository profiles,
            ExperienceContributionRepository contributions,
            @Value("${cohort.contributions.salt}") String salt) {
        this.profiles = profiles;
        this.contributions = contributions;
        this.salt = salt;
    }

    @Transactional
    public ContributionResponse create(UUID userId, CreateContributionRequest request) {
        int thisYear = LocalDate.now(ZoneOffset.UTC).getYear();
        if (request.year() > thisYear || request.year() < thisYear - YEARS_BACK) {
            throw new InvalidContributionException("year", "Year must be within the last " + YEARS_BACK + " years.");
        }
        Set<RoundType> seen = request.rounds().stream()
                .map(ContributionRoundInput::roundType)
                .collect(Collectors.toSet());
        if (seen.size() != request.rounds().size()) {
            throw new InvalidContributionException("rounds", "Each round type can be listed once.");
        }
        CompanyProfile company = resolveCompany(request);
        String hash = contributorHash(userId, company.getId());
        if (contributions.existsByCompanyIdAndRoleFamilyAndContributorHashAndYear(
                company.getId(), request.roleFamily(), hash, request.year())) {
            throw new DuplicateContributionException();
        }
        List<Map<String, Object>> rounds = request.rounds().stream()
                .map(round -> Map.<String, Object>of("round_type", round.roundType().name(), "difficulty", round.difficulty()))
                .toList();
        String tip = request.tip() == null || request.tip().isBlank() ? null : request.tip().trim();
        try {
            ExperienceContribution saved = contributions.saveAndFlush(new ExperienceContribution(
                    company.getId(), hash, request.roleFamily(), request.level(), request.year(), rounds,
                    request.overallDifficulty(), request.overallExperience(), request.outcome(), tip));
            return toResponse(saved, company);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateContributionException();
        }
    }

    @Transactional(readOnly = true)
    public List<ContributionResponse> mine(UUID userId) {
        List<CompanyProfile> companies = profiles.findAll();
        Map<String, CompanyProfile> byHash = companies.stream()
                .collect(Collectors.toMap(company -> contributorHash(userId, company.getId()), Function.identity()));
        return contributions.findByContributorHashIn(byHash.keySet()).stream()
                .sorted(Comparator.comparing(ExperienceContribution::getCreatedAt).reversed())
                .map(contribution -> toResponse(contribution, byHash.get(contribution.getContributorHash())))
                .toList();
    }

    @Transactional
    public void withdraw(UUID userId, UUID id) {
        ExperienceContribution contribution = contributions.findById(id).orElseThrow(ContributionNotFoundException::new);
        if (!contribution.getContributorHash().equals(contributorHash(userId, contribution.getCompanyId()))) {
            throw new ContributionNotFoundException();
        }
        contributions.delete(contribution);
    }

    @Transactional(readOnly = true)
    public List<CommunitySummary> summaries(UUID companyId) {
        Map<String, List<ExperienceContribution>> byFamily = contributions.findByCompanyId(companyId).stream()
                .collect(Collectors.groupingBy(ExperienceContribution::getRoleFamily));
        return byFamily.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> summarise(entry.getKey(), entry.getValue()))
                .toList();
    }

    private CommunitySummary summarise(String roleFamily, List<ExperienceContribution> rows) {
        int contributors = (int) rows.stream().map(ExperienceContribution::getContributorHash).distinct().count();
        if (contributors < MIN_CONTRIBUTORS) {
            return new CommunitySummary(roleFamily, contributors, null, null, List.of(), Map.of(), List.of());
        }
        int total = rows.size();
        Map<String, List<Integer>> difficultyByRound = new LinkedHashMap<>();
        for (ExperienceContribution row : rows) {
            for (Map<String, Object> round : row.getRounds()) {
                difficultyByRound
                        .computeIfAbsent((String) round.get("round_type"), key -> new ArrayList<>())
                        .add(((Number) round.get("difficulty")).intValue());
            }
        }
        List<RoundShare> commonRounds = difficultyByRound.entrySet().stream()
                .map(entry -> new RoundShare(
                        entry.getKey(),
                        Math.round(100f * entry.getValue().size() / total),
                        average(entry.getValue())))
                .sorted(Comparator.comparingInt(RoundShare::percent).reversed().thenComparing(RoundShare::roundType))
                .toList();
        Map<String, Integer> outcomes = new LinkedHashMap<>();
        for (ExperienceContribution.Outcome outcome : ExperienceContribution.Outcome.values()) {
            long count = rows.stream().filter(row -> row.getOutcome().equals(outcome.name())).count();
            outcomes.put(outcome.name(), Math.round(100f * count / total));
        }
        List<String> tips = rows.stream()
                .map(ExperienceContribution::getTip)
                .filter(Objects::nonNull)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .limit(MAX_TIPS)
                .toList();
        return new CommunitySummary(
                roleFamily,
                contributors,
                average(rows.stream().map(ExperienceContribution::getOverallExperience).toList()),
                average(rows.stream().map(ExperienceContribution::getOverallDifficulty).toList()),
                commonRounds,
                outcomes,
                tips);
    }

    private static Double average(List<Integer> values) {
        double mean = values.stream().mapToInt(Integer::intValue).average().orElse(0);
        return Math.round(mean * 10) / 10.0;
    }

    private CompanyProfile resolveCompany(CreateContributionRequest request) {
        if (request.companySlug() != null && !request.companySlug().isBlank()) {
            return profiles.findBySlug(request.companySlug().trim()).orElseThrow(CompanyNotFoundException::new);
        }
        if (request.companyName() == null || request.companyName().isBlank()) {
            throw new InvalidContributionException("company_name", "Choose a company or type its name.");
        }
        String name = request.companyName().trim().replaceAll("\\s+", " ");
        return profiles.findFirstByNameIgnoreCase(name)
                .or(() -> profiles.findBySlug(slugOf(name)))
                .orElseGet(() -> profiles.save(CompanyProfile.stub(name, slugOf(name), LocalDate.now(ZoneOffset.UTC))));
    }

    private static String slugOf(String name) {
        String ascii = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = ascii.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        return slug.isEmpty() ? "company-" + UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)) : slug;
    }

    String contributorHash(UUID userId, UUID companyId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(salt.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal((companyId + ":" + userId).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static ContributionResponse toResponse(ExperienceContribution row, CompanyProfile company) {
        List<ContributionRoundInput> rounds = row.getRounds().stream()
                .map(round -> new ContributionRoundInput(
                        RoundType.valueOf((String) round.get("round_type")),
                        ((Number) round.get("difficulty")).intValue()))
                .toList();
        return new ContributionResponse(
                row.getId(), company.getSlug(), company.getName(), row.getRoleFamily(), row.getLevel(), row.getYear(),
                rounds, row.getOverallDifficulty(), row.getOverallExperience(), row.getOutcome(), row.getTip(),
                row.getCreatedAt());
    }
}
