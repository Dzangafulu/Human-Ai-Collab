package com.example.human_ai_collaboration_platform;

import java.util.List;
import java.util.Locale;

/** Local recommendations: deterministic and available without a generative AI connection. */
public final class LocalStudyAssistant {
    private LocalStudyAssistant() { }
    public static String recommendation(String userQuestion, List<Task> all) {
        String q = userQuestion == null ? "" : userQuestion.toLowerCase(Locale.ROOT);
        boolean planningQuestion = q.matches("(?s).*(task|deadline|schedule|plan|tonight|time|stud(y|ies)|priorit|focus|assignment|productiv|workload|today|tomorrow|overdue|exam|test|manage).*?");
        if (!planningQuestion) {
            return "Live Gemini AI is temporarily unavailable. The built-in assistant can still " +
                   "rank your tasks or suggest a study plan. Try: 'What should I focus on?' " +
                   "For subject explanations, try again when Gemini is available.";
        }
        List<Task> ranked = StudyPlanEngine.ranked(all);
        if (ranked.isEmpty()) {
            return "Live Gemini AI is temporarily unavailable. I couldn't find any unfinished tasks " +
                   "in your Task Board. Add tasks and deadlines first, then ask for a study recommendation.";
        }
        StringBuilder reply = new StringBuilder("Local task recommendations (Gemini unavailable):\n\n");
        for (int i = 0; i < Math.min(3, ranked.size()); i++) {
            Task t = ranked.get(i);
            reply.append(i + 1).append(". ").append(t.getTitle())
                .append(" — due ").append(t.getDueDate())
                .append(" (priority: ").append(t.getPriority()).append(")\n");
        }
        if (q.contains("plan") || q.contains("schedule") || q.contains("today") || q.contains("tonight") || q.contains("time")) {
            List<StudyPlanEngine.Session> sessions = StudyPlanEngine.generate(ranked, 2, 18);
            if (!sessions.isEmpty()) {
                reply.append("\nSuggested sessions (based on the app's default 18:00 start):\n");
                for (int i = 0; i < Math.min(3, sessions.size()); i++) {
                    StudyPlanEngine.Session s = sessions.get(i);
                    reply.append("• ").append(s.day).append(" ").append(s.time).append(" — ").append(s.title).append("\n");
                }
            }
        }
        reply.append("\nThese are calculated from saved task dates and priorities. ")
             .append("You can review or edit the plan in Home → calendar. ")
             .append("Subject explanations require the live AI service.");
        return reply.toString();
    }
}
