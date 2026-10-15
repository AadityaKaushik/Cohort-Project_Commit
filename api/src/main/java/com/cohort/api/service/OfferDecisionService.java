package com.cohort.api.service;

import com.cohort.api.dto.OfferDecisionResponse;
import com.cohort.api.dto.OfferFactorValues;
import com.cohort.api.dto.OfferResponse;
import com.cohort.api.entity.OfferFactor;
import com.cohort.api.entity.OfferFactorWeight;
import com.cohort.api.entity.OfferRating;
import com.cohort.api.exception.OfferNotFoundException;
import com.cohort.api.repository.OfferFactorWeightRepository;
import com.cohort.api.repository.OfferRatingRepository;
import com.cohort.api.repository.OfferRepository;
import com.cohort.api.service.offer.OfferScorer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfferDecisionService {
    private final OfferService offerService;
    private final OfferRepository offerRepository;
    private final OfferFactorWeightRepository weightRepository;
    private final OfferRatingRepository ratingRepository;

    public OfferDecisionService(
            OfferService offerService,
            OfferRepository offerRepository,
            OfferFactorWeightRepository weightRepository,
            OfferRatingRepository ratingRepository) {
        this.offerService = offerService;
        this.offerRepository = offerRepository;
        this.weightRepository = weightRepository;
        this.ratingRepository = ratingRepository;
    }

    @Transactional(readOnly = true)
    public OfferDecisionResponse get(UUID userId, List<UUID> onlyOffers) {
        return build(userId, onlyOffers);
    }

    @Transactional
    public OfferDecisionResponse setWeights(UUID userId, OfferFactorValues request) {
        request.toMap().forEach((factor, value) -> {
            OfferFactorWeight weight = weightRepository
                    .findByUserIdAndFactor(userId, factor)
                    .orElseGet(() -> new OfferFactorWeight(userId, factor, value.shortValue()));
            weight.setWeight(value.shortValue());
            weightRepository.save(weight);
        });
        return build(userId, null);
    }

    @Transactional
    public OfferDecisionResponse setRatings(UUID userId, UUID offerId, OfferFactorValues request) {
        offerRepository.findByIdAndUserId(offerId, userId).orElseThrow(OfferNotFoundException::new);
        request.toMap().forEach((factor, value) -> {
            OfferRating rating = ratingRepository
                    .findByUserIdAndOfferIdAndFactor(userId, offerId, factor)
                    .orElseGet(() -> new OfferRating(userId, offerId, factor, value.shortValue()));
            rating.setRating(value.shortValue());
            ratingRepository.save(rating);
        });
        return build(userId, null);
    }

    private OfferDecisionResponse build(UUID userId, List<UUID> onlyOffers) {
        Map<OfferFactor, Integer> weights = new EnumMap<>(OfferFactor.class);
        for (OfferFactor factor : OfferFactor.values()) {
            weights.put(factor, OfferScorer.DEFAULT_WEIGHT);
        }
        weightRepository.findAllByUserId(userId).forEach(row -> weights.put(row.getFactor(), (int) row.getWeight()));

        Map<UUID, Map<OfferFactor, Integer>> ratingsByOffer = new HashMap<>();
        ratingRepository.findAllByUserId(userId).forEach(row -> ratingsByOffer
                .computeIfAbsent(row.getOfferId(), id -> new EnumMap<>(OfferFactor.class))
                .put(row.getFactor(), (int) row.getRating()));

        record Scored(OfferResponse offer, Map<OfferFactor, Integer> ratings, Integer score) {
        }
        List<Scored> scored = new ArrayList<>();
        for (OfferResponse offer : offerService.list(userId)) {
            if (onlyOffers != null && !onlyOffers.contains(offer.id())) {
                continue;
            }
            Map<OfferFactor, Integer> ratings = ratingsByOffer.getOrDefault(offer.id(), new EnumMap<>(OfferFactor.class));
            scored.add(new Scored(offer, ratings, OfferScorer.score(weights, ratings)));
        }

        List<Scored> complete = scored.stream()
                .filter(item -> item.score() != null)
                .sorted(Comparator.comparing(Scored::score).reversed())
                .toList();

        List<Integer> sortedScores = complete.stream().map(Scored::score).toList();

        List<OfferDecisionResponse.Entry> entries = scored.stream()
                .map(item -> new OfferDecisionResponse.Entry(
                        item.offer().id(),
                        item.offer().companyName(),
                        item.offer().jobTitle(),
                        item.ratings(),
                        item.score(),
                        item.score() == null ? null : sortedScores.indexOf(item.score()) + 1))
                .toList();

        String summary = OfferScorer.explainLeader(
                weights,
                complete.stream()
                        .map(item -> new OfferScorer.Ranked(item.offer().companyName(), item.ratings(), item.score()))
                        .toList());
        return new OfferDecisionResponse(weights, entries, summary);
    }
}
