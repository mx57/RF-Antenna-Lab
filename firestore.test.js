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

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function nowTimestamp() {
  return new Date(Date.now() - 1000);
}

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

test("Unauthenticated user: cannot read or write antenna_profiles", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("antenna_profiles").get());
  const ts = nowTimestamp();
  await assertFails(
    unauthDb.collection("antenna_profiles").doc("prof_1").set({
      userId: ALICE_UID,
      name: "20m Dipole",
      antennaType: "DIPOLE",
      frequencyMhz: 14.15,
      wireDiameterMm: 2.0,
      mastHeightM: 12.0,
      elementCount: 1,
      velocityFactor: 0.95,
      calculatedSwr: 1.12,
      gainDbi: 2.15,
      notes: "Resonant on 20m",
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Authenticated user: can create, query, and update own antenna_profile, but rejects ghost fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = nowTimestamp();
  await assertSucceeds(
    aliceDb.collection("antenna_profiles").doc("prof_1").set({
      userId: ALICE_UID,
      name: "20m Dipole",
      antennaType: "DIPOLE",
      frequencyMhz: 14.15,
      wireDiameterMm: 2.0,
      mastHeightM: 12.0,
      elementCount: 1,
      velocityFactor: 0.95,
      calculatedSwr: 1.12,
      gainDbi: 2.15,
      notes: "Resonant on 20m",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Query aligned with userId filter succeeds
  await assertSucceeds(
    aliceDb.collection("antenna_profiles").where("userId", "==", ALICE_UID).get()
  );

  // Query without userId filter fails
  await assertFails(aliceDb.collection("antenna_profiles").get());

  // Shadow update with ghost field fails
  await assertFails(
    aliceDb.collection("antenna_profiles").doc("prof_1").update({
      name: "Updated Dipole",
      isVerified: true,
      updatedAt: nowTimestamp(),
    })
  );

  // Valid update succeeds
  await assertSucceeds(
    aliceDb.collection("antenna_profiles").doc("prof_1").update({
      name: "20m DX Dipole",
      calculatedSwr: 1.05,
      updatedAt: nowTimestamp(),
    })
  );

  // Cross-user read by Bob fails
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("antenna_profiles").doc("prof_1").get());
});

test("Authenticated user: can create and query own vna_measurements and custom_plugins", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = nowTimestamp();
  await assertSucceeds(
    aliceDb.collection("vna_measurements").doc("vna_1").set({
      userId: ALICE_UID,
      deviceName: "Arinst V2D",
      connectionType: "USB_OTG",
      targetFreqMhz: 145.5,
      measuredResonanceMhz: 143.8,
      measuredSwr: 1.42,
      measuredR: 46.2,
      measuredX: 12.4,
      recommendation: "Trim driven element by 6.0 mm",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("custom_plugins").doc("plug_1").set({
      userId: ALICE_UID,
      name: "Coax Loss Calculator",
      author: "RA3AAA",
      version: "1.0.0",
      category: "CABLE_LOSS",
      description: "Calculates total coax attenuation including SWR loss",
      formulaExpression: "k1 * sqrt(freqMhz) * lengthM / 100",
      inputSchemaJson: "lengthM=30,k1=0.42",
      enabled: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});
