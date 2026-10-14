package com.cohort.api.service;

import com.cohort.api.dto.CreateOfferRequest;
import com.cohort.api.dto.OfferResponse;
import com.cohort.api.dto.UpdateOfferRequest;
import com.cohort.api.entity.Application;
import com.cohort.api.entity.Company;
import com.cohort.api.entity.Offer;
import com.cohort.api.exception.ApplicationNotFoundException;
import com.cohort.api.exception.DuplicateOfferException;
import com.cohort.api.exception.OfferNotFoundException;
import com.cohort.api.mapper.OfferMapper;
import com.cohort.api.repository.ApplicationRepository;
import com.cohort.api.repository.CompanyRepository;
import com.cohort.api.repository.OfferRepository;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfferService {

    private final OfferRepository offerRepository;
    private final ApplicationRepository applicationRepository;
    private final CompanyRepository companyRepository;
    private final OfferMapper mapper;

    public OfferService(
            OfferRepository offerRepository,
            ApplicationRepository applicationRepository,
            CompanyRepository companyRepository,
            OfferMapper mapper) {
        this.offerRepository = offerRepository;
        this.applicationRepository = applicationRepository;
        this.companyRepository = companyRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<OfferResponse> list(UUID userId) {
        List<Offer> offers = offerRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        return toResponses(userId, offers).stream()
                .sorted(Comparator.comparing(OfferResponse::responseDeadline, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    @Transactional(readOnly = true)
    public OfferResponse get(UUID userId, UUID id) {
        return toResponses(userId, List.of(requireOwned(userId, id))).get(0);
    }

    @Transactional
    public OfferResponse create(UUID userId, CreateOfferRequest request) {
        applicationRepository.findByIdAndUserId(request.applicationId(), userId)
                .orElseThrow(ApplicationNotFoundException::new);
        if (offerRepository.existsByApplicationIdAndUserId(request.applicationId(), userId)) {
            throw new DuplicateOfferException();
        }
        Offer offer = new Offer(userId, request.applicationId(), request.baseSalary());
        offer.setBonus(request.bonus());
        offer.setEquityValue(request.equityValue());
        offer.setSigningBonus(request.signingBonus());
        if (request.currency() != null) {
            offer.setCurrency(request.currency());
        }
        offer.setLocation(request.location());
        offer.setWorkMode(request.workMode());
        offer.setBenefits(request.benefits());
        offer.setNotes(request.notes());
        offer.setStartDate(request.startDate());
        offer.setResponseDeadline(request.responseDeadline());
        if (request.status() != null) {
            offer.setStatus(request.status());
        }
        return toResponses(userId, List.of(offerRepository.save(offer))).get(0);
    }

    @Transactional
    public OfferResponse update(UUID userId, UUID id, UpdateOfferRequest request) {
        Offer offer = requireOwned(userId, id);
        if (request.baseSalary() != null) {
            offer.setBaseSalary(request.baseSalary());
        }
        if (request.bonus() != null) {
            offer.setBonus(request.bonus());
        }
        if (request.equityValue() != null) {
            offer.setEquityValue(request.equityValue());
        }
        if (request.signingBonus() != null) {
            offer.setSigningBonus(request.signingBonus());
        }
        if (request.currency() != null) {
            offer.setCurrency(request.currency());
        }
        if (request.location() != null) {
            offer.setLocation(request.location());
        }
        if (request.workMode() != null) {
            offer.setWorkMode(request.workMode());
        }
        if (request.benefits() != null) {
            offer.setBenefits(request.benefits());
        }
        if (request.notes() != null) {
            offer.setNotes(request.notes());
        }
        if (request.startDate() != null) {
            offer.setStartDate(request.startDate());
        }
        if (request.responseDeadline() != null) {
            offer.setResponseDeadline(request.responseDeadline());
        }
        if (request.status() != null) {
            offer.setStatus(request.status());
        }
        return toResponses(userId, List.of(offerRepository.save(offer))).get(0);
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        requireOwned(userId, id);
        offerRepository.deleteByIdAndUserId(id, userId);
    }

    private Offer requireOwned(UUID userId, UUID id) {
        return offerRepository.findByIdAndUserId(id, userId).orElseThrow(OfferNotFoundException::new);
    }

    private List<OfferResponse> toResponses(UUID userId, List<Offer> offers) {
        Map<UUID, Application> applications = applicationRepository.findAllByUserId(userId).stream()
                .collect(Collectors.toMap(Application::getId, Function.identity()));
        Map<UUID, String> companyNames = companyRepository.findAllByUserId(userId).stream()
                .collect(Collectors.toMap(Company::getId, Company::getName));
        LocalDate today = LocalDate.now();
        return offers.stream().map(offer -> {
            Application application = applications.get(offer.getApplicationId());
            return mapper.toResponse(offer, companyNames.get(application.getCompanyId()), application.getJobTitle(), today);
        }).toList();
    }
}
