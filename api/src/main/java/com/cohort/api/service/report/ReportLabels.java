package com.cohort.api.service.report;

import com.cohort.api.dto.ConversionRateView;
import com.cohort.api.dto.ConversionRatesResponse;
import com.cohort.api.entity.ApplicationStatus;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

// Human-readable labels shared by every renderer, so a PDF and a CSV of the same report never disagree on wording.
final class ReportLabels {
    private ReportLabels() {
    }

    static String status(ApplicationStatus status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case SAVED -> "Saved";
            case APPLIED -> "Applied";
            case OA -> "Online assessment";
            case INTERVIEW -> "Interview";
            case FINAL_ROUND -> "Final round";
            case OFFER -> "Offer";
            case REJECTED -> "Rejected";
            case WITHDRAWN -> "Withdrawn";
            case ACCEPTED -> "Accepted";
            case DECLINED -> "Declined";
        };
    }

    static String capitalised(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.substring(0, 1).toUpperCase(Locale.ENGLISH) + value.substring(1);
    }

    static Map<String, ConversionRateView> conversions(ConversionRatesResponse rates) {
        Map<String, ConversionRateView> ordered = new LinkedHashMap<>();
        ordered.put("Applied to online assessment", rates.applicationToOa());
        ordered.put("Online assessment to interview", rates.oaToInterview());
        ordered.put("Interview to final round", rates.interviewToFinal());
        ordered.put("Final round to offer", rates.finalToOffer());
        ordered.put("Overall: applied to offer", rates.overallSuccess());
        return ordered;
    }

    static String days(double value) {
        return String.format(Locale.ENGLISH, "%.1f", value);
    }
}
