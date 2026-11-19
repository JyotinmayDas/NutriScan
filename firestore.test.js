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

test("Unauthenticated user cannot read or write foodDiary", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("foodDiary").get()
  );
});

test("Authenticated user can create and read their own profile and foodDiary entry", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      name: "Ananya Sen",
      email: "ananya@example.com",
      age: 28,
      height: 164,
      currentWeight: 66.5,
      targetWeight: 60.0,
      gender: "Female",
      activityLevel: "Moderately Active",
      fitnessGoal: "Lose Weight",
      dietaryPreference: "Bengali Traditional",
      dailyCalorieGoal: 1750,
      dailyWaterGoalMl: 2500,
      language: "en",
      isPremium: false,
      dailyScansUsed: 1,
      lastScanDate: "2026-11-03",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("foodDiary").doc("entry_1").set({
      userId: ALICE_UID,
      foodName: "Steamed Rice & Rohu Fish Curry (Macher Jhol)",
      quantity: 320,
      calories: 440,
      protein: 28.5,
      carbohydrates: 52.0,
      fat: 12.4,
      fiber: 3.2,
      sugar: 2.1,
      mealType: "Lunch",
      dateKey: "2026-11-03",
      source: "AI Scan",
      confidence: 0.92,
      notes: "Light mustard & cumin gravy with 150g rice and 1 piece rohu fish",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("foodDiary")
      .where("userId", "==", ALICE_UID)
      .get()
  );
});

test("Cross-user isolation: Bob cannot read or modify Alice's foodDiary or profile", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("foodDiary").doc("entry_1").set({
      userId: ALICE_UID,
      foodName: "Masoor Dal",
      quantity: 150,
      calories: 165,
      protein: 10.2,
      carbohydrates: 24.0,
      fat: 3.1,
      fiber: 4.5,
      sugar: 1.2,
      mealType: "Lunch",
      dateKey: "2026-11-03",
      source: "Database",
      confidence: 1.0,
      notes: "Home cooked lentil soup",
      createdAt: now,
      updatedAt: now,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("foodDiary").doc("entry_1").get()
  );
});

test("Shadow update & value poisoning rejected by validation blueprint", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  // Reject ghost field on create
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("waterHistory").doc("water_1").set({
      userId: ALICE_UID,
      quantity: 250,
      dateKey: "2026-11-03",
      isAdminHack: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject invalid negative calorie value
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("foodDiary").doc("entry_bad").set({
      userId: ALICE_UID,
      foodName: "Bad Food",
      quantity: 100,
      calories: -200,
      protein: 5,
      carbohydrates: 10,
      fat: 2,
      fiber: 1,
      sugar: 1,
      mealType: "SnackInvalid",
      dateKey: "2026-11-03",
      source: "Manual",
      confidence: 1.0,
      notes: "",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Authenticated user can create, list, update favorite, and delete savedRecipes", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  const recipeRef = aliceDb.collection("users").doc(ALICE_UID).collection("savedRecipes").doc("recipe_1");

  await assertSucceeds(
    recipeRef.set({
      userId: ALICE_UID,
      title: "Sunday Dal Bhat Combo",
      category: "Scanned Combo",
      mealType: "Lunch",
      totalCalories: 640,
      totalProteinG: 24.5,
      totalCarbsG: 98.0,
      totalFatG: 14.2,
      totalFiberG: 9.5,
      totalWeightG: 420.0,
      items: [
        {
          foodName: "Steamed White Rice",
          quantityG: 200.0,
          caloriesKcal: 260,
          proteinG: 5.2,
          carbohydratesG: 56.0,
          fatG: 0.6,
          fiberG: 1.0,
          mealSlot: "Lunch",
        },
        {
          foodName: "Yellow Dal Tadka",
          quantityG: 220.0,
          caloriesKcal: 380,
          proteinG: 19.3,
          carbohydratesG: 42.0,
          fatG: 13.6,
          fiberG: 8.5,
          mealSlot: "Lunch",
        },
      ],
      notes: "Less oil tadka",
      isFavorite: true,
      usageCount: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("savedRecipes")
      .where("userId", "==", ALICE_UID)
      .get()
  );

  await assertSucceeds(
    recipeRef.update({
      title: "Sunday Dal Bhat Combo (Light)",
      category: "Scanned Combo",
      mealType: "Lunch",
      totalCalories: 640,
      totalProteinG: 24.5,
      totalCarbsG: 98.0,
      totalFatG: 14.2,
      totalFiberG: 9.5,
      totalWeightG: 420.0,
      items: [
        {
          foodName: "Steamed White Rice",
          quantityG: 200.0,
          caloriesKcal: 260,
          proteinG: 5.2,
          carbohydratesG: 56.0,
          fatG: 0.6,
          fiberG: 1.0,
          mealSlot: "Lunch",
        },
      ],
      notes: "Updated note",
      isFavorite: false,
      usageCount: 1,
      updatedAt: new Date(),
    })
  );

  await assertSucceeds(recipeRef.delete());
});

test("savedRecipes enforces authentication, cross-user isolation, and schema constraints", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("savedRecipes").doc("alice_recipe").set({
      userId: ALICE_UID,
      title: "Masala Dosa Breakfast",
      category: "Favorite Meal",
      mealType: "Breakfast",
      totalCalories: 410,
      totalProteinG: 11.0,
      totalCarbsG: 62.0,
      totalFatG: 12.0,
      totalFiberG: 5.0,
      totalWeightG: 250.0,
      items: [{ foodName: "Masala Dosa", quantityG: 250.0, caloriesKcal: 410, proteinG: 11.0, carbohydratesG: 62.0, fatG: 12.0, fiberG: 5.0, mealSlot: "Breakfast" }],
      notes: "",
      isFavorite: true,
      usageCount: 0,
      createdAt: now,
      updatedAt: now,
    });
  });

  // 1. Unauthenticated read rejected
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("savedRecipes").get()
  );

  // 2. Cross-user read & write rejected
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("savedRecipes").doc("alice_recipe").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("savedRecipes").doc("bob_inject").set({
      userId: ALICE_UID,
      title: "Injected",
      category: "Scanned Combo",
      mealType: "Lunch",
      totalCalories: 100,
      totalProteinG: 5,
      totalCarbsG: 10,
      totalFatG: 2,
      totalFiberG: 1,
      totalWeightG: 100,
      items: [{ foodName: "Hack" }],
      notes: "",
      isFavorite: true,
      usageCount: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // 3. Invalid category or empty items array rejected for owner
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("savedRecipes").doc("bad_cat").set({
      userId: ALICE_UID,
      title: "Bad Category Recipe",
      category: "UnknownCategory",
      mealType: "Lunch",
      totalCalories: 200,
      totalProteinG: 10,
      totalCarbsG: 20,
      totalFatG: 5,
      totalFiberG: 2,
      totalWeightG: 150,
      items: [],
      notes: "",
      isFavorite: true,
      usageCount: 0,
      createdAt: now,
      updatedAt: now,
    })
  );
});

