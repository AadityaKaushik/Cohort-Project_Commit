package com.cohort.api.service.offer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cohort.api.entity.OfferFactor;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OfferScorerTest {
    private static Map<OfferFactor, Integer> ratings(int pay, int growth, int learning, int balance, int location, int brand) {
        Map<OfferFactor, Integer> map = new EnumMap<>(OfferFactor.class);
        map.put(OfferFactor.pay, pay);
        map.put(OfferFactor.growth, growth);
        map.put(OfferFactor.learning, learning);
        map.put(OfferFactor.work_life_balance, balance);
        map.put(OfferFactor.location, location);
        map.put(OfferFactor.brand, brand);
        return map;
    }

    @Test
    void perfectRatingsScoreOneHundredWhateverTheWeights() {
        Map<OfferFactor, Integer> weights = Map.of(OfferFactor.pay, 5, OfferFactor.brand, 1);
        assertEquals(100, OfferScorer.score(weights, ratings(5, 5, 5, 5, 5, 5)));
    }

    @Test
    void weightsChangeWhichOfferScoresHigher() {
        Map<OfferFactor, Integer> payFirst = Map.of(OfferFactor.pay, 5, OfferFactor.work_life_balance, 1);
        Map<OfferFactor, Integer> balanceFirst = Map.of(OfferFactor.pay, 1, OfferFactor.work_life_balance, 5);
        Map<OfferFactor, Integer> highPay = ratings(5, 3, 3, 2, 3, 3);
        Map<OfferFactor, Integer> highBalance = ratings(2, 3, 3, 5, 3, 3);

        assertTrue(OfferScorer.score(payFirst, highPay) > OfferScorer.score(payFirst, highBalance));
        assertTrue(OfferScorer.score(balanceFirst, highBalance) > OfferScorer.score(balanceFirst, highPay));
    }

    @Test
    void anUnratedFactorMeansNoScore() {
        Map<OfferFactor, Integer> partial = ratings(5, 5, 5, 5, 5, 5);
        partial.remove(OfferFactor.brand);
        assertNull(OfferScorer.score(Map.of(), partial));
    }

    @Test
    void explanationNamesTheFactorsThatDecideIt() {
        Map<OfferFactor, Integer> weights = Map.of(OfferFactor.pay, 5, OfferFactor.brand, 2);
        var acme = new OfferScorer.Ranked("Acme", ratings(5, 3, 3, 3, 3, 2), 0);
        var globex = new OfferScorer.Ranked("Globex", ratings(2, 3, 3, 3, 3, 5), 0);
        int acmeScore = OfferScorer.score(weights, acme.ratings());
        int globexScore = OfferScorer.score(weights, globex.ratings());

        String text = OfferScorer.explainLeader(
                weights,
                List.of(new OfferScorer.Ranked("Acme", acme.ratings(), acmeScore),
                        new OfferScorer.Ranked("Globex", globex.ratings(), globexScore)));

        assertTrue(text.startsWith("Acme leads Globex by "), text);
        assertTrue(text.contains("pay (rated 5 against 2, weight 5)"), text);
        assertTrue(text.contains("Globex is ahead on brand (rated 5 against 2, weight 2)"), text);
    }

    @Test
    void closeScoresAreReportedAsATie() {
        Map<OfferFactor, Integer> same = ratings(3, 3, 3, 3, 3, 3);
        String text = OfferScorer.explainLeader(Map.of(),
                List.of(new OfferScorer.Ranked("Acme", same, 60), new OfferScorer.Ranked("Globex", same, 60)));
        assertTrue(text.contains("do not separate them"), text);
    }

    @Test
    void oneOfferHasNothingToLead() {
        assertNull(OfferScorer.explainLeader(Map.of(), List.of(new OfferScorer.Ranked("Acme", ratings(3, 3, 3, 3, 3, 3), 60))));
    }
}
