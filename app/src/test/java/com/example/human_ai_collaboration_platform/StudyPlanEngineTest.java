package com.example.human_ai_collaboration_platform;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.*;

/** Unit tests for the Java-only planner. Android/Firebase integration requires device tests. */
public class StudyPlanEngineTest {
    @Test public void prioritisesEarlierDeadlines() {
        Task later = new Task(1, "Later", "", "", "To Do", "High", "25/12/2099");
        Task soon = new Task(2, "Soon", "", "", "In Progress", "Medium", "20/10/2026");
        assertEquals("Soon", StudyPlanEngine.ranked(Arrays.asList(later, soon)).get(0).getTitle());
    }
    @Test public void skipsCompletedTasks() {
        Task done = new Task(3, "Finished", "", "", "Completed", "High", "19/10/2026");
        assertTrue(StudyPlanEngine.generate(Collections.singletonList(done), 2, 18).isEmpty());
    }
    @Test public void generatesSlotsWithoutOverlap() {
        Task a = new Task(1,"A","","","To Do","Medium","25/12/2099");
        Task b = new Task(2,"B","","","To Do","Medium","25/12/2099");
        List<StudyPlanEngine.Session> sessions=StudyPlanEngine.generate(Arrays.asList(a,b),2,18);
        assertEquals(2,sessions.size());
        assertEquals("18:00",sessions.get(0).time);
        assertEquals("19:00",sessions.get(1).time);
    }
}
