package com.cohort.api.service.ai;

import com.cohort.api.dto.ContactDraftRequest;
import com.cohort.api.dto.EmailDraftRequest;
import com.cohort.api.dto.EmailDraftResponse;
import com.cohort.api.entity.Application;
import com.cohort.api.entity.Company;
import com.cohort.api.entity.Contact;
import com.cohort.api.entity.Interview;
import com.cohort.api.entity.Offer;
import com.cohort.api.exception.AiUnavailableException;
import com.cohort.api.exception.ApplicationNotFoundException;
import com.cohort.api.exception.ContactNotFoundException;
import com.cohort.api.exception.InterviewNotFoundException;
import com.cohort.api.repository.ApplicationRepository;
import com.cohort.api.repository.CompanyRepository;
import com.cohort.api.repository.ContactRepository;
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
    private final ContactRepository contactRepository;
    private final OfferRepository offerRepository;
    private final AiService aiService;

    public EmailDraftService(
            ApplicationRepository applicationRepository,
            CompanyRepository companyRepository,
            InterviewRepository interviewRepository,
            ContactRepository contactRepository,
            OfferRepository offerRepository,
            AiService aiService) {
        this.applicationRepository = applicationRepository;
        this.companyRepository = companyRepository;
        this.interviewRepository = interviewRepository;
        this.contactRepository = contactRepository;
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
            case referral_request, reconnect, referral_thanks -> {
                Optional<Contact> contact = contactFor(userId, request.contactId(), applicationId);
                Message message = contactMessage(request.kind(), contact, Optional.of(application), company);
                subject = message.subject();
                body = message.body();
            }
            default -> {
                subject = "Your offer for the " + application.getJobTitle() + " role";
                body = negotiation(application, company, offerRepository.findByUserIdAndApplicationId(userId, applicationId));
            }
        }

        return polish(userId, request.kind().name(), subject, body);
    }

    public EmailDraftResponse draftForContact(UUID userId, UUID contactId, ContactDraftRequest request) {
        Contact contact = contactRepository.findByIdAndUserId(contactId, userId).orElseThrow(ContactNotFoundException::new);
        Optional<Application> application = Optional.ofNullable(contact.getApplicationId())
                .flatMap(id -> applicationRepository.findByIdAndUserId(id, userId));
        UUID companyId = contact.getCompanyId() != null
                ? contact.getCompanyId()
                : application.map(Application::getCompanyId).orElse(null);
        String company = companyId == null
                ? "the company"
                : companyRepository.findByIdAndUserId(companyId, userId).map(Company::getName).orElse("the company");

        EmailDraftRequest.Kind kind = EmailDraftRequest.Kind.valueOf(request.kind().name());
        Message message = contactMessage(kind, Optional.of(contact), application, company);
        return polish(userId, kind.name(), message.subject(), message.body());
    }

    private EmailDraftResponse polish(UUID userId, String kind, String subject, String body) {
        try {
            String reply = aiService.complete(userId, FEATURE, prompt(body));
            String accepted = DraftOutput.accept(reply, body, 40);
            if (accepted != null) {
                return new EmailDraftResponse(kind, subject, accepted, "model", true);
            }
            return new EmailDraftResponse(kind, subject, body, "template", true);
        } catch (AiUnavailableException ex) {
            return new EmailDraftResponse(kind, subject, body, "template", false);
        }
    }

    private Optional<Contact> contactFor(UUID userId, UUID contactId, UUID applicationId) {
        if (contactId != null) {
            return Optional.of(contactRepository.findByIdAndUserId(contactId, userId)
                    .orElseThrow(ContactNotFoundException::new));
        }
        return contactRepository.findAllByUserIdAndApplicationIdOrderByCreatedAtDesc(userId, applicationId)
                .stream().findFirst();
    }

    private record Message(String subject, String body) {
    }

    private Message contactMessage(
            EmailDraftRequest.Kind kind, Optional<Contact> contact, Optional<Application> application, String company) {
        String name = contact.map(Contact::getName).filter(value -> !value.isBlank())
                .map(EmailDraftService::firstName).orElse("there");
        String role = application.map(Application::getJobTitle).orElse(null);
        String roleAtCompany = role == null ? "a role at " + company : "the " + role + " role at " + company;
        String title = contact.map(Contact::getRoleTitle).filter(value -> value != null && !value.isBlank()).orElse(null);

        return switch (kind) {
            case referral_request -> new Message(
                    role == null ? "Could you refer me at " + company + "?" : "Referral for the " + role + " role at " + company,
                    "Hi " + name + ",\n\n"
                            + "I am applying for " + roleAtCompany + ". "
                            + (title == null ? "I thought of you because you are there." : "I thought of you because you work there as " + title + ".")
                            + " Would you be open to referring me?\n\n"
                            + "If it is not a good fit or you would rather not, that is completely fine. "
                            + "If you are happy to, I can send my resume and a two-line note on why I fit.\n\n"
                            + "Thanks either way,\n[Your name]");
            case reconnect -> new Message(
                    "Catching up",
                    "Hi " + name + ",\n\n"
                            + "It has been a while" + sinceLastSpoke(contact) + ". I hope things are going well"
                            + (company.equals("the company") ? "" : " at " + company) + ".\n\n"
                            + "I wanted to say hello and hear what you have been working on. "
                            + (role == null ? "" : "I am also looking at " + roleAtCompany + ", so any thoughts on the team would help. ")
                            + "No rush at all.\n\n"
                            + "Best,\n[Your name]");
            default -> new Message(
                    "Thank you for the referral",
                    "Hi " + name + ",\n\n"
                            + "Thank you for referring me for " + roleAtCompany + ". I appreciate you putting your name "
                            + "behind my application.\n\n"
                            + "I will let you know how it goes.\n\n"
                            + "Best,\n[Your name]");
        };
    }

    private static String sinceLastSpoke(Optional<Contact> contact) {
        Instant last = contact.map(Contact::getLastContactedAt).orElse(null);
        if (last == null) {
            return " since we last spoke";
        }
        long days = Duration.between(last, Instant.now()).toDays();
        if (days < 30) {
            return " since we last spoke";
        }
        long months = days / 30;
        return " since we last spoke, about " + months + (months == 1 ? " month" : " months") + " ago";
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
