package com.cohort.api.service.ai;

import com.cohort.api.dto.AiDraftResponse;
import com.cohort.api.dto.ResumeMatchResponse;
import com.cohort.api.dto.ResumeMatchResponse.TailoringBullet;
import com.cohort.api.entity.Application;
import com.cohort.api.entity.Company;
import com.cohort.api.exception.ApplicationNotFoundException;
import com.cohort.api.exception.AiUnavailableException;
import com.cohort.api.repository.ApplicationRepository;
import com.cohort.api.repository.CompanyRepository;
import com.cohort.api.service.ai.ResumeContent.Content;
import com.cohort.api.service.ai.ResumeContent.Line;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ResumeMatchService {
    static final String FEATURE = "cover_letter";
    private static final int MAX_BULLETS = 5;
    private static final int MAX_DESCRIPTION = 4000;

    private final ApplicationRepository applicationRepository;
    private final CompanyRepository companyRepository;
    private final ResumeContent resumeContent;
    private final SkillExtractor skillExtractor;
    private final AiService aiService;

    public ResumeMatchService(
            ApplicationRepository applicationRepository,
            CompanyRepository companyRepository,
            ResumeContent resumeContent,
            SkillExtractor skillExtractor,
            AiService aiService) {
        this.applicationRepository = applicationRepository;
        this.companyRepository = companyRepository;
        this.resumeContent = resumeContent;
        this.skillExtractor = skillExtractor;
        this.aiService = aiService;
    }

    public ResumeMatchResponse match(UUID userId, UUID applicationId) {
        return analyse(userId, requireApplication(userId, applicationId));
    }

    public AiDraftResponse coverLetter(UUID userId, UUID applicationId) {
        Application application = requireApplication(userId, applicationId);
        ResumeMatchResponse match = analyse(userId, application);
        String company = companyRepository.findByIdAndUserId(application.getCompanyId(), userId)
                .map(Company::getName).orElse("your company");
        String template = template(application, company, match);

        try {
            String reply = aiService.complete(userId, FEATURE, prompt(application, company, match, template));
            String accepted = DraftOutput.accept(reply, template + "\n" + application.getJobDescription(), 80);
            if (accepted != null) {
                return new AiDraftResponse(accepted, "model", true);
            }
            return new AiDraftResponse(template, "template", true);
        } catch (AiUnavailableException ex) {
            return new AiDraftResponse(template, "template", false);
        }
    }

    private ResumeMatchResponse analyse(UUID userId, Application application) {
        String description = application.getJobDescription();
        boolean hasDescription = description != null && !description.isBlank();
        List<String> required = skillExtractor.skillsIn(application.getJobTitle() + "\n" + (hasDescription ? description : ""));
        Optional<Content> resume = resumeContent.forApplication(userId, application);

        if (!hasDescription || resume.isEmpty()) {
            String why = !hasDescription
                    ? "Add a job description to this application to see how your resume matches."
                    : "Attach a resume to this application to see how it matches.";
            return new ResumeMatchResponse(hasDescription, resume.isPresent(), required, List.of(), List.of(), null, why, List.of());
        }

        List<String> have = skillExtractor.skillsIn(resume.get().fullText());
        List<String> matched = required.stream().filter(have::contains).toList();
        List<String> missing = required.stream().filter(skill -> !have.contains(skill)).toList();
        if (required.isEmpty()) {
            return new ResumeMatchResponse(true, true, required, matched, missing, null,
                    "No known skills were found in the job description, so there is nothing to compare.", bullets(resume.get(), matched));
        }
        int score = Math.round(100f * matched.size() / required.size());
        String explanation = "Your resume covers %d of %d skills named in the posting (%d%%).".formatted(matched.size(), required.size(), score)
                + (missing.isEmpty() ? "" : " Not found in your resume: " + String.join(", ", missing) + ".");
        return new ResumeMatchResponse(true, true, required, matched, missing, score, explanation, bullets(resume.get(), matched));
    }

    private List<TailoringBullet> bullets(Content content, List<String> matched) {
        if (matched.isEmpty()) {
            return List.of();
        }
        return content.lines().stream()
                .map(line -> new TailoringBullet(line.text(), keywordsIn(line, matched), line.source()))
                .filter(bullet -> !bullet.keywords().isEmpty())
                .sorted(Comparator.comparingInt((TailoringBullet bullet) -> bullet.keywords().size()).reversed())
                .limit(MAX_BULLETS)
                .toList();
    }

    private List<String> keywordsIn(Line line, List<String> matched) {
        List<String> inLine = skillExtractor.skillsIn(line.text());
        return matched.stream().filter(inLine::contains).toList();
    }

    private String template(Application application, String company, ResumeMatchResponse match) {
        StringBuilder letter = new StringBuilder();
        String recruiter = application.getRecruiterName();
        letter.append(recruiter == null || recruiter.isBlank() ? "Dear hiring team," : "Dear " + recruiter + ",").append("\n\n");
        letter.append("I am applying for the ").append(application.getJobTitle()).append(" role at ").append(company).append(".");
        if (!match.matched().isEmpty()) {
            letter.append(" My background lines up with what you are asking for in ")
                    .append(String.join(", ", match.matched())).append(".");
        }
        letter.append("\n\n");
        if (!match.bullets().isEmpty()) {
            letter.append("A few things I have done that relate to this role:\n");
            match.bullets().stream().limit(3).forEach(bullet -> letter.append("- ").append(bullet.text()).append("\n"));
            letter.append("\n");
        }
        letter.append("I would be glad to talk through how this experience fits the team. Thank you for your time.\n\n");
        letter.append("Kind regards,\n[Your name]");
        return letter.toString();
    }

    private AiPrompt prompt(Application application, String company, ResumeMatchResponse match, String template) {
        String description = application.getJobDescription() == null ? "" : application.getJobDescription();
        if (description.length() > MAX_DESCRIPTION) {
            description = description.substring(0, MAX_DESCRIPTION);
        }
        String system = "You edit cover letters for a job seeker. Rewrite the draft so it reads naturally and stays under 250 words. "
                + "Use only the facts in the draft. Do not add employers, degrees, numbers or skills that are not in it. "
                + "Keep the greeting and the sign-off. Reply with the letter only.";
        String user = "Role: " + application.getJobTitle() + " at " + company + "\n"
                + "Skills the posting asks for that the candidate has: " + String.join(", ", match.matched()) + "\n"
                + "Job description:\n" + description + "\n\nDraft:\n" + template;
        return AiPrompt.text(system, List.of(AiMessage.user(user)));
    }

    private Application requireApplication(UUID userId, UUID applicationId) {
        return applicationRepository.findByIdAndUserId(applicationId, userId).orElseThrow(ApplicationNotFoundException::new);
    }
}
