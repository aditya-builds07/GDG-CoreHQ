package com.gdgcorehq.app;

import com.gdgcorehq.app.model.AttendanceRecord;
import com.gdgcorehq.app.model.Meeting;
import com.gdgcorehq.app.model.PostMeetingReport;
import com.gdgcorehq.app.model.Reimbursement;
import com.gdgcorehq.app.model.StrikeRecord;
import com.gdgcorehq.app.model.TaskItem;
import com.gdgcorehq.app.model.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Isolated unit test fixtures for GDG CoreHQ JVM unit tests.
 * Rule R24: All mock datasets, fake users, sample tasks, and dummy rosters
 * are strictly isolated inside the unit test source tree (app/src/test/).
 */
public class TestFixtures {

    public static class TestUserAccount {
        public final User user;
        public final String password;

        public TestUserAccount(User user, String password) {
            this.user = user;
            this.password = password;
        }
    }

    public static Map<String, TestUserAccount> getTestAccounts() {
        Map<String, TestUserAccount> accounts = new HashMap<>();

        // Super Admin
        User admin = new User(
                "usr_admin_01",
                "Aditya Desai",
                "PRN2022001",
                "aditya.d@gdg.org",
                User.Role.SUPER_ADMIN,
                "Organizer / Lead",
                0
        );
        TestUserAccount adminAcc = new TestUserAccount(admin, "admin123");
        accounts.put("admin", adminAcc);
        accounts.put("organizer", adminAcc);
        accounts.put("aditya.d@gdg.org", adminAcc);
        accounts.put("prn2022001", adminAcc);

        // Core Member (Design Lead)
        User siddhi = new User(
                "usr_member_01",
                "Siddhi Kulkarni",
                "PRN2024001",
                "siddhi.k@gdg.org",
                User.Role.CORE_MEMBER,
                "Design & Web Lead",
                1
        );
        TestUserAccount siddhiAcc = new TestUserAccount(siddhi, "lead123");
        accounts.put("lead", siddhiAcc);
        accounts.put("siddhi", siddhiAcc);
        accounts.put("siddhi.k@gdg.org", siddhiAcc);
        accounts.put("prn2024001", siddhiAcc);

        // Core Member (PR & Outreach)
        User tanmay = new User(
                "usr_member_02",
                "Tanmay Patil",
                "PRN2024002",
                "tanmay.p@gdg.org",
                User.Role.CORE_MEMBER,
                "PR & Outreach",
                0
        );
        TestUserAccount tanmayAcc = new TestUserAccount(tanmay, "member123");
        accounts.put("tanmay", tanmayAcc);
        accounts.put("tanmay.p@gdg.org", tanmayAcc);
        accounts.put("prn2024002", tanmayAcc);

        // Core Member (3 Strikes Critical Flag per PRD §3.6)
        User rahul = new User(
                "usr_member_03",
                "Rahul Patil",
                "PRN2024003",
                "rahul.p@gdg.org",
                User.Role.CORE_MEMBER,
                "Technical Team",
                3
        );
        TestUserAccount rahulAcc = new TestUserAccount(rahul, "strike123");
        accounts.put("strike", rahulAcc);
        accounts.put("member3", rahulAcc);
        accounts.put("rahul", rahulAcc);
        accounts.put("rahul.p@gdg.org", rahulAcc);
        accounts.put("prn2024003", rahulAcc);

        // Core Member (Logistics Lead)
        User ananya = new User(
                "usr_member_04",
                "Ananya Shinde",
                "PRN2024004",
                "ananya.s@gdg.org",
                User.Role.CORE_MEMBER,
                "Logistics Lead",
                2
        );
        TestUserAccount ananyaAcc = new TestUserAccount(ananya, "member123");
        accounts.put("ananya", ananyaAcc);
        accounts.put("ananya.s@gdg.org", ananyaAcc);
        accounts.put("prn2024004", ananyaAcc);

        return accounts;
    }

    public static List<User> getTestMembers() {
        List<User> list = new ArrayList<>();
        list.add(new User("usr_admin_01", "Aditya Desai", "PRN2022001", "aditya.d@gdg.org", User.Role.SUPER_ADMIN, "Organizer", 0));
        list.add(new User("usr_member_01", "Siddhi Kulkarni", "PRN2024001", "siddhi.k@gdg.org", User.Role.CORE_MEMBER, "Design & Web Lead", 1));
        list.add(new User("usr_member_02", "Tanmay Patil", "PRN2024002", "tanmay.p@gdg.org", User.Role.CORE_MEMBER, "PR & Outreach", 0));
        list.add(new User("usr_member_03", "Rahul Patil", "PRN2024003", "rahul.p@gdg.org", User.Role.CORE_MEMBER, "Technical Team", 3));
        list.add(new User("usr_member_04", "Ananya Shinde", "PRN2024004", "ananya.s@gdg.org", User.Role.CORE_MEMBER, "Logistics Lead", 2));
        return list;
    }

    public static List<TaskItem> getTestTasks() {
        List<TaskItem> list = new ArrayList<>();
        list.add(new TaskItem("task_01", "Finalize Cloud Community Day Poster", "Export Instagram & LinkedIn flyers in approved brand dimensions.", "usr_member_01", "Siddhi Kulkarni", "Oct 24, 2026", TaskItem.Status.IN_PROGRESS, "Design", false));
        list.add(new TaskItem("task_02", "Book Auditorium & Sound System", "Coordinate with college administration for Friday seminar hall.", "usr_member_04", "Ananya Shinde", "Oct 22, 2026", TaskItem.Status.TODO, "Logistics", false));
        list.add(new TaskItem("task_03", "Publish Speaker Announcement Post", "Draft caption and tag speaker profiles on Twitter and LinkedIn.", "usr_member_02", "Tanmay Patil", "Oct 21, 2026", TaskItem.Status.DONE, "PR", false));
        list.add(new TaskItem("task_04", "Deploy Hackathon Registration Form", "Complete Google Cloud backend validation and link to CoreHQ.", "usr_member_03", "Rahul Patil", "Oct 18, 2026", TaskItem.Status.TODO, "Technical", true));
        return list;
    }

    public static List<Meeting> getTestMeetings() {
        List<Meeting> list = new ArrayList<>();
        list.add(new Meeting(
                "meet_101",
                "Weekly Chapter Sync & Hackathon Prep",
                "Friday, Oct 24, 2026",
                "05:00 PM - 06:30 PM",
                "Seminar Hall B, TKIET",
                Arrays.asList(
                        "Review registration milestone (150+ signups)",
                        "Sponsorship deck updates & budget allocation",
                        "Departmental roadblock check-in",
                        "Post-meeting report submissions"
                ),
                "Aditya Desai (Organizer)",
                true
        ));
        list.add(new Meeting(
                "meet_100",
                "Core Team Onboarding & Roles Alignment",
                "Friday, Oct 17, 2026",
                "04:30 PM - 06:00 PM",
                "Computer Lab 4, TKIET",
                Arrays.asList(
                        "Welcome new tech and design leads",
                        "Introduce GDG CoreHQ operations system",
                        "PRN ID card distribution for attendance"
                ),
                "Aditya Desai (Organizer)",
                false
        ));
        return list;
    }

    public static List<Reimbursement> getTestReimbursements() {
        List<Reimbursement> list = new ArrayList<>();
        list.add(new Reimbursement(
                "exp_01",
                "usr_member_04",
                "Ananya Shinde",
                1250.00,
                "Oct 19, 2026",
                "Printing & Swag",
                "receipt_banners_tkiet.jpg",
                Reimbursement.Status.PENDING,
                null
        ));
        list.add(new Reimbursement(
                "exp_02",
                "usr_member_01",
                "Siddhi Kulkarni",
                480.00,
                "Oct 15, 2026",
                "Refreshments",
                "receipt_snacks_oct15.jpg",
                Reimbursement.Status.APPROVED,
                null
        ));
        list.add(new Reimbursement(
                "exp_03",
                "usr_member_03",
                "Rahul Patil",
                2100.00,
                "Oct 12, 2026",
                "Logistics",
                "receipt_cables.jpg",
                Reimbursement.Status.REJECTED,
                "Receipt image blurry and missing vendor GST tax invoice. Please re-upload legible bill."
        ));
        return list;
    }

    public static List<StrikeRecord> getTestStrikes() {
        List<StrikeRecord> list = new ArrayList<>();
        list.add(new StrikeRecord(
                "strk_01",
                "usr_member_03",
                "Rahul Patil",
                StrikeRecord.TriggerType.OVERDUE_TASK,
                "Task 'Deploy Hackathon Registration Form' exceeded deadline by > 48 hours without prior notice.",
                "Oct 20, 2026",
                false,
                null,
                null
        ));
        list.add(new StrikeRecord(
                "strk_02",
                "usr_member_03",
                "Rahul Patil",
                StrikeRecord.TriggerType.ABSENCE,
                "Unexcused absence from Chapter Sync (Oct 17, 2026). No attendance scan recorded.",
                "Oct 17, 2026",
                false,
                null,
                null
        ));
        list.add(new StrikeRecord(
                "strk_03",
                "usr_member_03",
                "Rahul Patil",
                StrikeRecord.TriggerType.ABSENCE,
                "Unexcused absence from Chapter Sync (Oct 10, 2026).",
                "Oct 10, 2026",
                false,
                null,
                null
        ));
        list.add(new StrikeRecord(
                "strk_04",
                "usr_member_04",
                "Ananya Shinde",
                StrikeRecord.TriggerType.ABSENCE,
                "Missed meeting on Oct 10 due to exam overlap.",
                "Oct 10, 2026",
                true,
                "Pardoned by Organizer: Mid-term university exam clash verified.",
                "Aditya Desai"
        ));
        return list;
    }
}
