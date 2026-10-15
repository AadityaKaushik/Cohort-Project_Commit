package com.cohort.api.service.ai;

import com.cohort.api.dto.AiDraftResponse;
import com.cohort.api.dto.PrepPackResponse;
import com.cohort.api.dto.PrepPackResponse.AskedQuestion;
import com.cohort.api.dto.PrepPackResponse.PastRound;
import com.cohort.api.dto.PrepPackResponse.WeakTopic;
import com.cohort.api.dto.ReadinessResponse;
import com.cohort.api.entity.Application;
import com.cohort.api.entity.Company;
import com.cohort.api.entity.CompanyNote;
import com.cohort.api.entity.Interview;
import com.cohort.api.entity.InterviewFeedback;
import com.cohort.api.entity.InterviewQuestion;
import com.cohort.api.exception.AiUnavailableException;
import com.cohort.api.exception.ApplicationNotFoundException;
import com.cohort.api.exception.InterviewNotFoundException;
import com.cohort.api.repository.ApplicationRepository;
import com.cohort.api.repository.CompanyNoteRepository;
import com.cohort.api.repository.CompanyRepository;
import com.cohort.api.repository.InterviewFeedbackRepository;
import com.cohort.api.repository.InterviewQuestionRepository;
import com.cohort.api.repository.InterviewRepository;
import com.cohort.api.service.preparation.ReadinessService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class InterviewPrepService {
    static final String FEATURE = "prep_summary";
    private static final int MAX_NOTES = 5;
    private static final int MAX_WEAK_TOPICS = 5;
    private static final int MAX_HIGHLIGHTS = 4;
    private static final double WEAK_BELOW = 0.6;

    private final InterviewRepository interviewRepository;
    private final ApplicationRepository applicationRepository;
    private final CompanyRepository companyRepository;
    private final CompanyNoteRepository noteRepository;
    private final InterviewQuestionRepository questionRepository;
    private final InterviewFeedbackRepository feedbackRepository;
    private final ReadinessService readinessService;
    private final ResumeContent resumeContent;
    private final SkillExtractor skillExtractor;
    private final AiService aiService;

    public InterviewPrepService(
            InterviewRepository interviewRepository,
            ApplicationRepository applicationRepository,
            CompanyRepository companyRepository,
            CompanyNoteRepository noteRepository,
            InterviewQuestionRepository questionRepository,
            InterviewFeedbackRepository feedbackRepository,
            ReadinessService readinessService,
            ResumeContent resumeContent,
            SkillExtractor skillExtractor,
            AiService aiService) {
        this.interviewRepository = interviewRepository;
        this.applicationRepository = applicationRepository;
        this.companyRepository = companyRepository;
        this.noteRepository = noteRepository;
        this.questionRepository = questionRepository;
        this.feedbackRepository = feedbackRepository;
        this.readinessService = readinessService;
        this.resumeContent = resumeContent;
        this.skillExtractor = skillExtractor;
        this.aiService = aiService;
    }

    public PrepPackResponse pack(UUID userId, UUID interviewId) {
        Interview interview = interviewRepository.findByIdAndUserId(interviewId, userId)
                .orElseThrow(InterviewNotFoundException::new);
        Application application = applicationRepository.findByIdAndUserId(interview.getApplicationId(), userId)
                .orElseThrow(ApplicationNotFoundException::new);
        Company company = companyRepository.findByIdAndUserId(application.getCompanyId(), userId).orElseThrow();

        List<Application> sameCompany = applicationRepository.findAllByUserIdAndCompanyId(userId, company.getId());
        Map<UUID, Application> applicationsById = sameCompany.stream()
                .collect(Collectors.toMap(Application::getId, Function.identity()));
        List<Interview> related = new ArrayList<>();
        for (Application each : sameCompany) {
            related.addAll(interviewRepository.findAllByUserIdAndApplicationIdOrderByRoundNumberAsc(userId, each.getId()));
        }

        List<Interview> past = related.stream()
                .filter(each -> !each.getId().equals(interviewId))
                .filter(each -> each.getCompletedAt() != null || each.getOutcome() != Interview.Outcome.pending)
                .toList();
        Map<UUID, InterviewFeedback> feedback = feedbackRepository
                .findAllByUserIdAndInterviewIdIn(userId, past.stream().map(Interview::getId).toList()).stream()
                .collect(Collectors.toMap(InterviewFeedback::getInterviewId, Function.identity()));

        List<PastRound> pastRounds = past.stream().map(each -> {
            InterviewFeedback note = feedback.get(each.getId());
            return new PastRound(each.getRoundNumber(), each.getRoundType().name(),
                    applicationsById.get(each.getApplicationId()).getJobTitle(), each.getOutcome().name(),
                    note == null ? null : note.getWhatWentWell(),
                    note == null ? null : note.getWhatWentPoorly(),
                    note == null ? null : note.getImprovementAreas());
        }).toList();

        List<Interview> withQuestions = new ArrayList<>(past);
        withQuestions.add(interview);
        List<AskedQuestion> asked = new ArrayList<>();
        for (Interview each : withQuestions) {
            for (InterviewQuestion question : questionRepository
                    .findAllByUserIdAndInterviewIdOrderByCreatedAtAsc(userId, each.getId())) {
                asked.add(new AskedQuestion(question.getQuestionText(),
                        question.getCategory() == null ? null : question.getCategory().name(), each.getRoundNumber()));
            }
        }

        List<String> notes = noteRepository.findAllByUserIdAndCompanyIdOrderByCreatedAtDesc(userId, company.getId()).stream()
                .limit(MAX_NOTES).map(CompanyNote::getBody).toList();

        ReadinessResponse readiness = readinessService.forApplication(userId, application.getId());
        List<WeakTopic> weak = readiness.topics().stream()
                .filter(topic -> !topic.hasEvidence() || topic.masteryScore() < WEAK_BELOW)
                .sorted(Comparator.comparingDouble(ReadinessResponse.TopicContribution::masteryScore))
                .limit(MAX_WEAK_TOPICS)
                .map(topic -> new WeakTopic(topic.topicName(), topic.hasEvidence(), (int) Math.round(topic.masteryScore() * 100)))
                .toList();

        return new PrepPackResponse(interview.getId(), company.getName(), application.getJobTitle(),
                interview.getRoundNumber(), interview.getRoundType().name(), interview.getScheduledAt(),
                interview.getInterviewerName(), interview.getInterviewerRole(), notes, pastRounds, asked, weak,
                highlights(userId, application), readiness.sufficientData() ? readiness.score() : null,
                readiness.formatted());
    }

    public AiDraftResponse summary(UUID userId, UUID interviewId) {
        PrepPackResponse pack = pack(userId, interviewId);
        String facts = facts(pack);
        try {
            String reply = aiService.complete(userId, FEATURE, prompt(facts));
            String accepted = DraftOutput.accept(reply, facts, 40);
            if (accepted != null) {
                return new AiDraftResponse(accepted, "model", true);
            }
            return new AiDraftResponse(fallbackSummary(pack), "template", true);
        } catch (AiUnavailableException ex) {
            return new AiDraftResponse(fallbackSummary(pack), "template", false);
        }
    }

    private List<String> highlights(UUID userId, Application application) {
        String posting = application.getJobTitle() + "\n"
                + (application.getJobDescription() == null ? "" : application.getJobDescription());
        List<String> wanted = skillExtractor.skillsIn(posting);
        return resumeContent.forApplication(userId, application)
                .map(content -> {
                    List<String> matching = content.lines().stream()
                            .map(ResumeContent.Line::text)
                            .filter(text -> skillExtractor.skillsIn(text).stream().anyMatch(wanted::contains))
                            .limit(MAX_HIGHLIGHTS)
                            .toList();
                    return matching.isEmpty()
                            ? content.lines().stream().map(ResumeContent.Line::text).limit(3).toList()
                            : matching;
                })
                .orElse(List.of());
    }

    private String facts(PrepPackResponse pack) {
        StringBuilder facts = new StringBuilder();
        facts.append("Round ").append(pack.roundNumber()).append(" (").append(pack.roundType()).append(") at ")
                .append(pack.companyName()).append(" for ").append(pack.jobTitle()).append("\n");
        if (pack.interviewerName() != null) {
            facts.append("Interviewer: ").append(pack.interviewerName()).append("\n");
        }
        if (pack.readinessScore() != null) {
            facts.append("Readiness: ").append(pack.readinessFormatted()).append("\n");
        }
        if (!pack.weakTopics().isEmpty()) {
            facts.append("Weak topics: ")
                    .append(pack.weakTopics().stream().map(WeakTopic::name).collect(Collectors.joining(", "))).append("\n");
        }
        pack.pastRounds().forEach(round -> {
            facts.append("Earlier round ").append(round.roundNumber()).append(": ").append(round.outcome());
            if (round.improvementAreas() != null) {
                facts.append(", to improve: ").append(round.improvementAreas());
            }
            facts.append("\n");
        });
        pack.companyNotes().forEach(note -> facts.append("Note: ").append(note).append("\n"));
        pack.questionsAsked().forEach(question -> facts.append("Question asked: ").append(question.text()).append("\n"));
        return facts.toString();
    }

    private AiPrompt prompt(String facts) {
        String system = "You write a short preparation summary for a job seeker before an interview. "
                + "Write one paragraph of at most 90 words in plain language. Use only the facts given. "
                + "Do not invent questions, numbers or advice about the company.";
        return AiPrompt.text(system, List.of(AiMessage.user(facts)));
    }

    private String fallbackSummary(PrepPackResponse pack) {
        StringBuilder text = new StringBuilder();
        text.append("Round ").append(pack.roundNumber()).append(" at ").append(pack.companyName())
                .append(" for ").append(pack.jobTitle()).append(".");
        if (pack.readinessScore() != null) {
            text.append(" Readiness for this role is ").append(pack.readinessFormatted()).append(".");
        }
        if (!pack.weakTopics().isEmpty()) {
            text.append(" Spend time on ")
                    .append(pack.weakTopics().stream().map(WeakTopic::name).collect(Collectors.joining(", "))).append(".");
        }
        if (!pack.pastRounds().isEmpty()) {
            text.append(" You have ").append(pack.pastRounds().size()).append(" earlier round(s) with this company to review.");
        }
        return text.toString();
    }
}
