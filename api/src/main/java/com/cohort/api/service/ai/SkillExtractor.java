package com.cohort.api.service.ai;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class SkillExtractor {
    private static final Map<String, Pattern> VOCABULARY = new LinkedHashMap<>();

    static {
        add("Java", "\\bjava\\b");
        add("Spring Boot", "\\bspring(\\s*boot)?\\b");
        add("Python", "\\bpython\\b");
        add("TypeScript", "\\btypescript\\b");
        add("JavaScript", "\\bjavascript\\b|\\bnode\\.?js\\b");
        add("React", "\\breact(\\.js)?\\b");
        add("Angular", "\\bangular\\b");
        add("Vue", "\\bvue(\\.js)?\\b");
        add("HTML and CSS", "\\bhtml\\b|\\bcss\\b");
        add("SQL", "\\bsql\\b");
        add("PostgreSQL", "\\bpostgres(ql)?\\b");
        add("MySQL", "\\bmysql\\b");
        add("MongoDB", "\\bmongo(db)?\\b");
        add("Redis", "\\bredis\\b");
        add("Docker", "\\bdocker\\b|\\bcontainers?\\b");
        add("Kubernetes", "\\bkubernetes\\b|\\bk8s\\b");
        add("AWS", "\\baws\\b|\\bamazon web services\\b");
        add("Azure", "\\bazure\\b");
        add("GCP", "\\bgcp\\b|\\bgoogle cloud\\b");
        add("REST APIs", "\\brest(ful)?\\b|\\bapis?\\b");
        add("GraphQL", "\\bgraphql\\b");
        add("Microservices", "\\bmicroservices?\\b");
        add("Git", "\\bgit(hub|lab)?\\b");
        add("CI/CD", "\\bci\\s*/\\s*cd\\b|\\bcontinuous (integration|delivery|deployment)\\b|\\bjenkins\\b");
        add("Testing", "\\b(unit|integration|automated)?\\s*test(ing|s)?\\b|\\bjunit\\b|\\bjest\\b|\\bpytest\\b");
        add("System design", "\\bsystem design\\b|\\bdistributed systems?\\b|\\bscalab(le|ility)\\b");
        add("Kafka", "\\bkafka\\b");
        add("Data structures and algorithms", "\\bdata structures?\\b|\\balgorithms?\\b");
        add("Machine learning", "\\bmachine learning\\b|\\bml\\b|\\bscikit-learn\\b|\\btensorflow\\b|\\bpytorch\\b");
        add("Linux", "\\blinux\\b");
        add("Agile", "\\bagile\\b|\\bscrum\\b");
        add("C++", "\\bc\\+\\+");
        add("C#", "\\bc#|\\.net\\b");
        add("Go", "\\bgolang\\b");
        add("Communication", "\\bcommunication\\b|\\bcollaborat(e|ion)\\b|\\bteamwork\\b");
        add("Leadership", "\\bleadership\\b|\\bmentor(ing)?\\b");
    }

    private static void add(String skill, String regex) {
        VOCABULARY.put(skill, Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
    }

    public List<String> skillsIn(String text) {
        List<String> found = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return found;
        }
        VOCABULARY.forEach((skill, pattern) -> {
            if (pattern.matcher(text).find()) {
                found.add(skill);
            }
        });
        return found;
    }
}
