package com.cohort.api.service.recommendation.tiering;

public enum EvidenceTier {
    A(1.0),
    B(0.7),
    C(0.6),
    D(0.4);

    private final double weight;

    EvidenceTier(double weight) {
        this.weight = weight;
    }

    public double weight() {
        return weight;
    }
}
