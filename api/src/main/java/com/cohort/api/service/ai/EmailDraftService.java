package com.cohort.api.service.ai;

import com.cohort.api.dto.EmailDraftRequest;
import com.cohort.api.dto.EmailDraftResponse;
import com.cohort.api.entity.Application;
import com.cohort.api.entity.Company;
import com.cohort.api.entity.Interview;
import com.cohort.api.entity.Offer;
import com.cohort.api.exception.AiUnavailableException;
import com.cohort.api.exception.ApplicationNotFoundException;
import com.cohort.api.exception.InterviewNotFoundException;
import com.cohort.api.repository.ApplicationRepository;
import com.cohort.api.repository.CompanyRepository;
import com.cohort.api.repository.InterviewRepository;
import com.cohort.api.repository.OfferRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class EmailDraftService {
    static final String FEATURE = "email_draft";

    private final ApplicationRepository applicationRepository;
    private final CompanyRepository companyRepository;
    private final InterviewRepository interviewRepository;
    private final OfferRepository offerRepository;
    private final AiService aiService;

    public EmailDraftService(
            ApplicationRepository applicationRepository,
            CompanyRepository companyRepository,
            InterviewRepository interviewRepository,
            OfferRepository offerRepository,
            AiService aiService) {
        this.applicationRepository = applicationRepository;
        this.companyRepository = companyRepository;
        this.interviewRepository = interviewRepository;
        this.offerRepository = offerRepository;
        this.aiService = aiService;
    }

    public EmailDraftResponse draft(UUID userId, UUID applicationId, EmailDraftRequest request) {
        Application application = applicationRepository.findByIdAndUserId(applicationId, userId)
                .orElseThrow(ApplicationNotFoundException::new);
        String company = companyRepository.findByIdAndUserId(application.getCompanyId(), userId)
                .map(Company::getName).orElse("the company");

        String subject;
        String body;
        switch (request.kind()) {
            case thank_you -> {
                Optional<Interview> interview = interviewFor(userId, application, request.interviewId());
                subject = "Thank you for the " + application.getJobTitle() + " conversation";
                body = thankYou(application, company, interview);
            }
            case follow_up -> {
                subject = "Following up on the " + application.getJobTitle() + " role";
                body = followUp(application, company);
            }
            default -> {
                subject = "Your offer for the " + application.getJobTitle() + " role";
                body = negotiation(application, company, offerRepository.findByUserIdAndApplicationId(userId, applicationId));
            }
        }

        try {
            String reply = aiService.complete(userId, FEATURE, prompt(body));
            String accepted = DraftOutput.accept(reply, body, 40);
            if (accepted != null) {
                return new EmailDraftResponse(request.kind().name(), subject, accepted, "model", true);
            }
            return new EmailDraftResponse(request.kind().name(), subject, body, "template", true);
        } catch (AiUnavailableException ex) {
            return new EmailDraftResponse(request.kind().name(), subject, body, "template", false);
        }
    }

    private Optional<Interview> interviewFor(UUID userId, Application application, UUID interviewId) {
        if (interviewId != null) {
            return Optional.of(interviewRepository.findByIdAndUserId(interviewId, userId)
                    .filter(interview -> interview.getApplicationId().equals(application.getId()))
                    .orElseThrow(InterviewNotFoundException::new));
        }
        List<Interview> rounds = interviewRepository
                .findAllByUserIdAndApplicationIdOrderByRoundNumberAsc(userId, application.getId());
        return rounds.stream().max(Comparator.comparingInt(Interview::getRoundNumber));
    }

    private String thankYou(Application application, String company, Optional<Interview> interview) {
        String name = interview.map(Interview::getInterviewerName).filter(value -> value != null && !value.isBlank())
                .map(EmailDraftService::firstName).orElse("there");
        return "Hi " + name + ",\n\n"
                + "Thank you for taking the time to talk with me about the " + application.getJobTitle() + " role at "
                + company + ". I enjoyed the conversation and I am more interested in the team than before.\n\n"
                + "If there is anything else I can send you, let me know.\n\n"
                + "Best regards,\n[Your name]";
    }

    private String followUp(Application application, String company) {
        String name = application.getRecruiterName() == null || application.getRecruiterName().isBlank()
                ? "there" : firstName(application.getRecruiterName());
        long days = application.getStageEnteredAt() == null
                ? 0 : Duration.between(application.getStageEnteredAt(), Instant.now()).toDays();
        String wait = days > 0 ? "It has been " + days + " days since my last update, and " : "";
        return "Hi " + name + ",\n\n"
                + "I am writing to follow up on my application for the " + application.getJobTitle() + " role at "
                + company + ". " + wait + "I wanted to check whether there is anything further you need from me, "
                + "and if there is an update on the next step.\n\n"
                + "Thank you for your time.\n\n"
                + "Best regards,\n[Your name]";
    }

    private String negotiation(Application application, String company, Optional<Offer> offer) {
        StringBuilder email = new StringBuilder("Hi,\n\n");
        email.append("Thank you for the offer for the ").append(application.getJobTitle()).append(" role at ")
                .append(company).append(". I am excited about the team and the work.\n\n");
        if (offer.isPresent()) {
            Offer details = offer.get();
            email.append("The offer lists a base salary of ").append(plain(details.getBaseSalary())).append(" ")
                    .append(details.getCurrency());
            if (details.getSigningBonus() != null && details.getSigningBonus().signum() > 0) {
                email.append(" and a signing bonus of ").append(plain(details.getSigningBonus()));
            }
            email.append(". ");
        }
        email.append("Based on my experience and what I have seen for similar roles, I was hoping for a base salary of "
                + "[your target]. Is there room to move on the compensation?\n\n");
        if (offer.isPresent() && offer.get().getResponseDeadline() != null) {
            email.append("I understand a response is due by ").append(offer.get().getResponseDeadline())
                    .append(" and I will get back to you before then.\n\n");
        }
        email.append("Thank you for your time and consideration.\n\nBest regards,\n[Your name]");
        return email.toString();
    }

    private static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private static String firstName(String full) {
        return full.strip().split("\\s+")[0];
    }

    private AiPrompt prompt(String body) {
        String system = "You polish short professional emails for a job seeker. Keep every name, date and figure from the "
                + "draft and do not add new ones. Do not add promises or claims. Keep it under 150 words in a plain, "
                + "polite tone. Keep placeholders in square brackets as they are. Reply with the email body only.";
        return AiPrompt.text(system, List.of(AiMessage.user(body)));
    }
}
