const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function pastDate(msOffset = 5000) {
  return new Date(Date.now() - msOffset);
}

function validUserProfile(uid) {
  const now = pastDate();
  return {
    userId: uid,
    displayName: "Hamza",
    quranWirdJuz: 1,
    quranMurajaahPages: 5,
    cyclingTargetKm: 22.0,
    youtubeDailyVideos: 2,
    pushupGoal: 100,
    ttsEnabled: true,
    ttsSpeed: 1.0,
    ttsPitch: 1.0,
    ttsVoiceName: "",
    ttsRepetitions: 2,
    ttsDelaySeconds: 3,
    alarmVolume: 0.9,
    vibrationEnabled: true,
    soundEnabled: true,
    soundDurationSeconds: 20,
    snoozeMinutes: 5,
    themeMode: "DARK",
    use24HourFormat: false,
    createdAt: now,
    updatedAt: now,
  };
}

function validActivity(uid) {
  const now = pastDate();
  return {
    userId: uid,
    title: "Quran",
    description: "Quran reading",
    category: "QURAN",
    startHour: 0,
    startMinute: 0,
    endHour: 0,
    endMinute: 50,
    weekdaysMask: 127,
    repeatWeekly: true,
    alarmEnabled: true,
    soundKey: "sound_quran_tone",
    announcementText: "Quran! Quran! Quran! It is time for Quran. Begin your Wird and Muraja'a.",
    sortOrder: 2,
    defaultKey: "quran",
    createdAt: now,
    updatedAt: now,
  };
}

function validCompletion(uid) {
  const now = pastDate();
  return {
    userId: uid,
    dateKey: "2026-11-22",
    activityId: "quran",
    status: "COMPLETED",
    completedAtMillis: Date.now(),
    notes: "Finished 1 Juz",
    quranWirdDone: 1,
    quranMurajaahDone: 5,
    cyclingDistanceKm: 0.0,
    cyclingDurationMin: 0,
    sleepDurationMin: 0,
    v1Edited: false,
    v1Uploaded: false,
    v2Edited: false,
    v2Uploaded: false,
    studyMinutes: 0,
    webMinutes: 0,
    workoutCompletedCount: 0,
    createdAt: now,
    updatedAt: now,
  };
}

function validWorkoutPlan(uid) {
  const now = pastDate();
  return {
    userId: uid,
    groupName: "Group A",
    focusArea: "Biceps and Back",
    weekdaysMask: 42,
    restDay: false,
    exerciseItems: [
      "pushups|Push-ups|FIRST_20|100|4|8|Editable personal goal.",
      "biceps|Biceps|NEXT_20|12|3|10|Controlled curls.",
    ],
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated requests are denied across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validUserProfile(ALICE_UID)));
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("activities").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("completions").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("workoutPlans").get());
});

test("Owner can create, read, list, and partially update all user-scoped entities", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validUserProfile(ALICE_UID)));
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      pushupGoal: 80,
      updatedAt: pastDate(1000),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("activities").doc("quran").set(validActivity(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("activities").get()
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("activities").doc("quran").update({
      alarmEnabled: false,
      updatedAt: pastDate(1000),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("completions").doc("2026-11-22__quran").set(validCompletion(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("completions").get()
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("workoutPlans").doc("group_a").set(validWorkoutPlan(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("workoutPlans").get()
  );
});

test("Cross-user access is denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validUserProfile(ALICE_UID)));
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("activities").doc("quran").set(validActivity(ALICE_UID))
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("activities").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("activities").doc("quran").update({
      title: "Hijacked",
      updatedAt: pastDate(1000),
    })
  );
});

test("Rejects ghost fields, @DocumentId id key, future timestamps, and immutable createdAt edits", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Ghost field on user profile
  const badProfile = { ...validUserProfile(ALICE_UID), isAdmin: true };
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).set(badProfile));

  // Extra id key on activity
  const badActivity = { ...validActivity(ALICE_UID), id: "quran" };
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("activities").doc("quran").set(badActivity)
  );

  // Future timestamp rejected
  const futureActivity = { ...validActivity(ALICE_UID), createdAt: new Date(Date.now() + 120000) };
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("activities").doc("quran2").set(futureActivity)
  );

  // Immutable createdAt mutation rejected
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("activities").doc("quran").set(validActivity(ALICE_UID))
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("activities").doc("quran").update({
      createdAt: pastDate(500),
      updatedAt: pastDate(500),
    })
  );
});
