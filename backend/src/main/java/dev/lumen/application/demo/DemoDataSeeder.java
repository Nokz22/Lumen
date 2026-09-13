package dev.lumen.application.demo;

import dev.lumen.application.assessment.AssessmentService;
import dev.lumen.application.consent.ConsentService;
import dev.lumen.application.exercise.ExerciseCompletionService;
import dev.lumen.application.recommendation.RecommendationService;
import dev.lumen.domain.assessment.AssessmentType;
import dev.lumen.domain.exercise.Exercise;
import dev.lumen.domain.exercise.ExerciseRepository;
import dev.lumen.domain.moodcheckin.MoodCheckIn;
import dev.lumen.domain.moodcheckin.MoodCheckInRepository;
import dev.lumen.domain.moodcheckin.MoodEmotion;
import dev.lumen.domain.recommendation.MoodCheckInSubmittedEvent;
import dev.lumen.domain.user.ConsentType;
import dev.lumen.domain.user.Role;
import dev.lumen.domain.user.User;
import dev.lumen.domain.user.UserRepository;
import dev.lumen.domain.wearable.WearableReading;
import dev.lumen.domain.wearable.WearableReadingRepository;
import dev.lumen.domain.wearable.WearableReadingType;
import dev.lumen.domain.wearable.WearableSourceType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fills a demo instance with eight weeks of coherent, entirely synthetic history, so the
 * app can be opened and understood without first spending a fortnight using it.
 *
 * <p>Written in Java rather than as a Flyway seed for a reason that is not stylistic: the
 * free-text fields are encrypted at rest through a JPA converter, and SQL cannot produce
 * ciphertext. Going through the repositories also means the demo data is built by the same
 * domain objects, invariants and encryption as real data — a seed that bypassed them would
 * be demonstrating something the application does not actually do.
 *
 * <p><b>No crisis scenario is ever seeded.</b> PHQ-9 item 9 is fixed at zero, so no
 * RiskEvent exists in a demo database (project-brief section 11). The crisis flow is
 * demonstrated deliberately, by a person answering item 9 in a controlled setting — never
 * left lying in a public instance as though someone had been in danger.
 *
 * <p>Runs only under the {@code demo} profile. Fabricated health history in a production
 * database would be worse than useless: it would be indistinguishable from a real person's.
 */
@Component
@Profile("demo")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DemoDataSeeder.class);

    static final String DEMO_EMAIL = "demo@lumen.dev";
    static final String DEMO_PASSWORD = "Demo1234!";
    private static final int HISTORY_DAYS = 56;

    /**
     * Item 9 — the self-harm item — is the last value and is always zero. Everything else
     * is a moderate, unremarkable pattern. See the class comment.
     */
    static final List<Integer> PHQ9_DEMO_RESPONSES = List.of(2, 2, 1, 2, 1, 1, 1, 0, 0);

    static final List<Integer> GAD7_DEMO_RESPONSES = List.of(2, 1, 2, 1, 1, 1, 0);

    /** Fixed seed: the demo looks the same every time it is shown. */
    private static final long RANDOM_SEED = 20260907L;

    private final UserRepository userRepository;
    private final MoodCheckInRepository moodCheckInRepository;
    private final WearableReadingRepository wearableReadingRepository;
    private final ExerciseRepository exerciseRepository;
    private final ConsentService consentService;
    private final RecommendationService recommendationService;
    private final ExerciseCompletionService exerciseCompletionService;
    private final AssessmentService assessmentService;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(
            UserRepository userRepository,
            MoodCheckInRepository moodCheckInRepository,
            WearableReadingRepository wearableReadingRepository,
            ExerciseRepository exerciseRepository,
            ConsentService consentService,
            RecommendationService recommendationService,
            ExerciseCompletionService exerciseCompletionService,
            AssessmentService assessmentService,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.moodCheckInRepository = moodCheckInRepository;
        this.wearableReadingRepository = wearableReadingRepository;
        this.exerciseRepository = exerciseRepository;
        this.consentService = consentService;
        this.recommendationService = recommendationService;
        this.exerciseCompletionService = exerciseCompletionService;
        this.assessmentService = assessmentService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.findByEmail(DEMO_EMAIL).isPresent()) {
            LOG.info("Demo data already present, leaving it alone");
            return;
        }

        User demoUser = createDemoUser();
        DemoStory story = new DemoStory(new Random(RANDOM_SEED));
        seedDailyHistory(demoUser, story);
        seedInstruments(demoUser);
        LOG.info("Seeded {} days of synthetic demo history", HISTORY_DAYS);
    }

    private User createDemoUser() {
        User user = userRepository.save(new User(
                DEMO_EMAIL,
                passwordEncoder.encode(DEMO_PASSWORD),
                "Demo User",
                "en",
                "PT",
                LocalDate.of(1990, 1, 1),
                Role.USER));
        for (ConsentType consentType : ConsentType.values()) {
            consentService.grant(user.getId(), consentType);
        }
        return user;
    }

    /**
     * Check-ins and the wearable series are generated from one underlying trend, so the
     * correlation the dashboard surfaces is a real relationship in the data rather than a
     * caption over noise: nights of shorter sleep really are followed by lower-energy days.
     */
    private void seedDailyHistory(User user, DemoStory story) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<Exercise> exercises = exerciseRepository.findAll();
        List<MoodCheckIn> checkIns = new ArrayList<>();

        for (int daysAgo = HISTORY_DAYS; daysAgo >= 1; daysAgo--) {
            LocalDate date = today.minusDays(daysAgo);
            DemoStory.Day day = story.dayAt(HISTORY_DAYS - daysAgo, HISTORY_DAYS);
            if (day.skipped()) {
                continue;
            }

            checkIns.add(moodCheckInRepository.save(new MoodCheckIn(
                    user, day.emotion(), day.energyLevel(), day.sleepHours(), day.sleepQuality(), day.note(), date)));
            wearableReadingRepository.saveAll(readingsFor(user, date, day));
        }

        seedRecommendationsAndCompletions(user, checkIns, exercises);
    }

    private List<WearableReading> readingsFor(User user, LocalDate date, DemoStory.Day day) {
        // The night's sleep is dated to the evening it began, not the morning it ended.
        // That is what makes the insight land: the service pairs a sleep reading with the
        // *next* day's energy, so a night dated to the day it started lines up with the
        // day it actually affected. Dating it to the morning shifts every pair by one and
        // the correlation disappears — which is exactly what happened the first time.
        Instant night = date.minusDays(1).atTime(LocalTime.of(23, 30)).toInstant(ZoneOffset.UTC);
        Instant morning = date.atTime(LocalTime.of(7, 30)).toInstant(ZoneOffset.UTC);
        Instant evening = date.atTime(LocalTime.of(21, 0)).toInstant(ZoneOffset.UTC);
        return List.of(
                reading(user, WearableReadingType.SLEEP_DURATION, day.sleepHours(), night),
                reading(user, WearableReadingType.HRV, day.heartRateVariability(), morning),
                reading(user, WearableReadingType.HEART_RATE, day.restingHeartRate(), morning),
                reading(user, WearableReadingType.STEPS, day.steps(), evening));
    }

    private WearableReading reading(User user, WearableReadingType type, BigDecimal value, Instant recordedAt) {
        return new WearableReading(user.getId(), type, value, recordedAt, WearableSourceType.SIMULATOR);
    }

    /**
     * Recommendations come from the real engine rather than being written by hand, so what
     * the demo shows is what the rules actually produce for that day's state — including
     * the explanation attached to each one.
     */
    private void seedRecommendationsAndCompletions(User user, List<MoodCheckIn> checkIns, List<Exercise> exercises) {
        // Every check-in, not just the recent ones: in real use each submission publishes
        // an event and the engine answers it, so seeding a subset would produce a history
        // the application could never have produced itself.
        for (MoodCheckIn checkIn : checkIns) {
            recommendationService.handleMoodCheckInSubmitted(new MoodCheckInSubmittedEvent(
                    checkIn.getId(),
                    user.getId(),
                    checkIn.getEmotion(),
                    checkIn.getEnergyLevel(),
                    checkIn.getSleepHours(),
                    checkIn.getSleepQuality()));
        }

        // A few exercises actually done, so the loop the product is built around — notice,
        // act, record — is visible rather than implied.
        for (int i = 0; i < Math.min(4, exercises.size()); i++) {
            exerciseCompletionService.complete(user.getId(), exercises.get(i).getId(), null);
        }
    }

    private void seedInstruments(User user) {
        assessmentService.submit(user.getId(), AssessmentType.PHQ9, PHQ9_DEMO_RESPONSES);
        assessmentService.submit(user.getId(), AssessmentType.GAD7, GAD7_DEMO_RESPONSES);
    }

    /**
     * One trend drives everything, so the whole history tells a single believable story: a
     * rough few weeks that gradually improves. The improvement sits inside the last month
     * on purpose — that is the window the correlation insight looks at, and a flat month
     * would leave the dashboard with nothing true to say.
     */
    static final class DemoStory {

        private static final List<String> ROUGH_NOTES = List.of(
                "Woke up a few times again.",
                "Hard to get going today.",
                "Feeling wound up.",
                "Long day, short night.");
        private static final List<String> BETTER_NOTES = List.of(
                "Slept through for once.", "Walked at lunch, helped.", "Calmer than last week.", "Good, steady day.");

        private final Random random;

        DemoStory(Random random) {
            this.random = random;
        }

        Day dayAt(int dayIndex, int totalDays) {
            // Starts rough, ends better — not cured. A story that ends at a perfect score
            // is both untrue to how this works and useless as a demo: no rule fires on a
            // run of flawless days, so the recommendation feed would be empty exactly
            // where a visitor looks first.
            double trend = (double) dayIndex / totalDays;
            double wobble = random.nextGaussian() * 0.14;
            double wellbeing = clamp(0.12 + 0.62 * trend + wobble, 0, 1);

            // A couple of missed days a fortnight: nobody checks in perfectly, and a
            // gapless streak is the tell of generated data.
            boolean skipped = random.nextInt(14) == 0;

            // Each measure gets its own noise on top of the shared trend. Deriving them
            // all from one number produced correlations above 0.9, which is not what a
            // person's own data looks like — a demo that shows an impossibly clean
            // relationship is teaching the visitor the wrong thing about the feature.
            return new Day(
                    skipped,
                    emotionFor(wellbeing),
                    (int) Math.round(clamp(1 + 4 * wellbeing + random.nextGaussian() * 0.45, 1, 5)),
                    round(clamp(4.9 + 2.9 * wellbeing + random.nextGaussian() * 0.35, 4, 10), 1),
                    (int) Math.round(clamp(1 + 4 * wellbeing + random.nextGaussian() * 0.4, 1, 5)),
                    noteFor(wellbeing),
                    round(clamp(34 + 32 * wellbeing + random.nextGaussian() * 5, 20, 90), 0),
                    round(clamp(74 - 12 * wellbeing + random.nextGaussian() * 3, 45, 95), 0),
                    round(Math.max(400, 2600 + 6200 * wellbeing + random.nextGaussian() * 1100), 0));
        }

        private MoodEmotion emotionFor(double wellbeing) {
            if (wellbeing < 0.3) {
                return random.nextBoolean() ? MoodEmotion.ANXIOUS : MoodEmotion.SAD;
            }
            if (wellbeing < 0.55) {
                return random.nextBoolean() ? MoodEmotion.FRUSTRATED : MoodEmotion.NEUTRAL;
            }
            if (wellbeing < 0.8) {
                return MoodEmotion.NEUTRAL;
            }
            return MoodEmotion.HAPPY;
        }

        private String noteFor(double wellbeing) {
            // Most days nobody writes anything.
            if (random.nextInt(3) != 0) {
                return null;
            }
            List<String> pool = wellbeing < 0.5 ? ROUGH_NOTES : BETTER_NOTES;
            return pool.get(random.nextInt(pool.size()));
        }

        private double clamp(double value, double min, double max) {
            return Math.max(min, Math.min(max, value));
        }

        private BigDecimal round(double value, int scale) {
            return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP);
        }

        record Day(
                boolean skipped,
                MoodEmotion emotion,
                int energyLevel,
                BigDecimal sleepHours,
                int sleepQuality,
                String note,
                BigDecimal heartRateVariability,
                BigDecimal restingHeartRate,
                BigDecimal steps) {
        }
    }
}
