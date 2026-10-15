package com.cohort.api.service;

import com.cohort.api.dto.SampleDataResponse;
import com.cohort.api.repository.SampleDataRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SampleDataService {
    private final SampleDataRepository repository;

    public SampleDataService(SampleDataRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public SampleDataResponse load(UUID userId) {
        if (repository.hasSampleCompanies(userId)) {
            return repository.counts(userId);
        }

        UUID nimbus = repository.insertCompany(userId, "Nimbus Systems", "https://nimbus.example.com", "Software");
        UUID cascade = repository.insertCompany(userId, "Cascade Analytics", "https://cascade.example.com", "Data & Analytics");
        UUID fernbridge = repository.insertCompany(userId, "Fernbridge Robotics", "https://fernbridge.example.com", "Robotics");
        UUID solstice = repository.insertCompany(userId, "Solstice Health", "https://solstice.example.com", "Healthcare Tech");
        UUID larkspur = repository.insertCompany(userId, "Larkspur Labs", "https://larkspur.example.com", "Software");

        UUID nimbusApp = repository.insertApplication(userId, nimbus, "Backend Engineer Intern", "interview", "interview",
                21, "Bengaluru", "hybrid", "Careers page", null);
        UUID cascadeApp = repository.insertApplication(userId, cascade, "Data Engineer Intern", "applied", "applied",
                9, "Remote", "remote", "Referral", null);
        repository.insertApplication(userId, fernbridge, "Software Engineer Intern", "oa", "oa",
                14, "Pune", "onsite", "Campus drive", null);
        UUID solsticeApp = repository.insertApplication(userId, solstice, "Platform Engineer", "offer", "offer",
                40, "Hyderabad", "hybrid", "Recruiter", null);
        repository.insertApplication(userId, larkspur, "Full Stack Intern", "rejected", "interview",
                30, "Remote", "remote", "Job board", null);

        UUID nimbusRound1 = repository.insertInterview(userId, nimbusApp, 1, "technical", -10, true, "passed", 3, "Asha Rao");
        repository.insertInterview(userId, nimbusApp, 2, "system_design", 3, false, "pending", null, "Ben Carter");
        repository.insertQuestion(userId, nimbusRound1, "Reverse a linked list in place and explain the complexity.", "dsa", "easy", 4);
        repository.insertQuestion(userId, nimbusRound1, "How would you design a rate limiter for a public API?", "system_design", "medium", 3);

        UUID solsticeRound1 = repository.insertInterview(userId, solsticeApp, 1, "technical", -25, true, "passed", 4, "Meera Iyer");
        repository.insertInterview(userId, solsticeApp, 2, "behavioural", -18, true, "passed", 2, "Tom Fischer");
        repository.insertQuestion(userId, solsticeRound1, "Explain the difference between a process and a thread.", "technical", "medium", 3);
        repository.insertOffer(userId, solsticeApp, 95000, 10);

        repository.insertCompanyNote(userId, nimbus, "Team works on the payments backend. Ask about on-call during the second round.");
        repository.insertContact(userId, nimbus, nimbusApp, "Priya Nair", "Technical Recruiter", "recruiter", "none", 6);
        repository.insertContact(userId, cascade, cascadeApp, "Daniel Kim", "Senior Data Engineer", "referrer", "requested", 12);

        return repository.counts(userId);
    }

    @Transactional
    public SampleDataResponse remove(UUID userId) {
        repository.deleteSample(userId);
        return repository.counts(userId);
    }

    @Transactional(readOnly = true)
    public SampleDataResponse status(UUID userId) {
        return repository.counts(userId);
    }
}
