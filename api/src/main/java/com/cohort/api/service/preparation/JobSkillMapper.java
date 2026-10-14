package com.cohort.api.service.preparation;

import com.cohort.api.entity.Topic;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class JobSkillMapper {
    private static final Pattern PARENTHETICAL = Pattern.compile("\\s*\\(.*?\\)");

    /** Topic name mentions in the posting text, used as weights. Topics never mentioned are left out. */
    public Map<Topic, Integer> mentions(String text, List<Topic> topics) {
        Map<Topic, Integer> result = new LinkedHashMap<>();
        if (text == null || text.isBlank()) {
            return result;
        }
        String haystack = text.toLowerCase(Locale.ROOT);
        for (Topic topic : topics) {
            int count = count(haystack, topic.getName()) + count(haystack, topic.getSlug().replace('_', ' '));
            if (count > 0) {
                result.put(topic, count);
            }
        }
        return result;
    }

    private int count(String haystack, String phrase) {
        String needle = PARENTHETICAL.matcher(phrase).replaceAll("").trim().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) {
            return 0;
        }
        Matcher matcher = Pattern.compile("(?<![a-z0-9])" + Pattern.quote(needle) + "(?![a-z0-9])").matcher(haystack);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }
}
