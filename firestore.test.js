const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const serverTimestamp = () => new Date(Date.now() - 5000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const CHARLIE_UID = "charlie_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validUserProfile(uid, name = "Alice Innovator") {
  return {
    userId: uid,
    name,
    email: `${uid}@example.com`,
    role: "Creator",
    headline: "CleanTech Founder",
    bio: "Building hardware for impact.",
    skills: ["IoT", "Embedded C"],
    location: "San Francisco, CA",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

function validInnovation(creatorId, visibility = "PUBLIC") {
  return {
    creatorId,
    authorName: "Alice Innovator",
    authorHeadline: "CleanTech Founder",
    title: "AquaMesh Fog Harvester",
    description: "Passive biomimetic mesh capturing potable water daily in coastal zones.",
    category: "Inventions",
    price: 120,
    visibility,
    licensing: true,
    funding: true,
    ownershipConfirmed: true,
    imageUrl: "https://images.unsplash.com/photo-1509228468518-180dd4864904?w=800",
    likeCount: 0,
    commentCount: 0,
    stageIndex: 1,
    isShowcaseSeed: false,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
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

test("Unauthenticated user cannot read or write any protected collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("innovations").where("visibility", "==", "PUBLIC").get());
  await assertFails(unauthDb.collection("talents").where("isPublic", "==", true).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("proposals").where("recipientId", "==", ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validUserProfile(ALICE_UID)));
});

test("UserProfile: owner can create and update; cross-user denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validUserProfile(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      headline: "Senior IoT Architect",
      updatedAt: serverTimestamp(),
    })
  );

  // Cross-user isolation
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ name: "Hacked" }));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).delete());

  // Ghost field & invalid role rejected
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      ...validUserProfile(BOB_UID, "Bob"),
      admin: true,
    })
  );
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      ...validUserProfile(BOB_UID, "Bob"),
      role: "SuperAdmin",
    })
  );
});

test("User subcollections (saved, liked, signedNdas, pledges): owner access and append-only pledges", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const savedRef = aliceDb.collection("users").doc(ALICE_UID).collection("saved").doc("inno_1");
  await assertSucceeds(
    savedRef.set({
      userId: ALICE_UID,
      innovationId: "inno_1",
      title: "AquaMesh",
      category: "Inventions",
      price: 120,
      authorName: "Alice",
      createdAt: serverTimestamp(),
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("saved").get());
  await assertSucceeds(savedRef.delete());

  const ndaRef = aliceDb.collection("users").doc(ALICE_UID).collection("signedNdas").doc("inno_1");
  await assertSucceeds(
    ndaRef.set({
      userId: ALICE_UID,
      innovationId: "inno_1",
      innovationTitle: "AquaMesh",
      signerName: "Alice Innovator",
      signerOrg: "CleanTech Lab",
      signatureId: "NDA-INNO1-XYZ",
      signedAt: serverTimestamp(),
    })
  );

  const pledgeRef = aliceDb.collection("users").doc(ALICE_UID).collection("pledges").doc("pledge_1");
  await assertSucceeds(
    pledgeRef.set({
      userId: ALICE_UID,
      innovationId: "inno_1",
      innovationTitle: "AquaMesh",
      supporterName: "Alice Innovator",
      amount: 25,
      message: "Keep going!",
      receiptId: "I2I-TX-1001",
      createdAt: serverTimestamp(),
    })
  );
  // Append-only pledge cannot be updated or deleted
  await assertFails(pledgeRef.update({ amount: 500 }));
  await assertFails(pledgeRef.delete());
});

test("Innovations: public visibility query alignment, private isolation, and likeCount/commentCount rules", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(aliceDb.collection("innovations").doc("inno_pub").set(validInnovation(ALICE_UID, "PUBLIC")));
  await assertSucceeds(aliceDb.collection("innovations").doc("inno_priv").set(validInnovation(ALICE_UID, "PRIVATE")));

  // Aligned queries succeed
  await assertSucceeds(bobDb.collection("innovations").where("visibility", "==", "PUBLIC").get());
  await assertSucceeds(aliceDb.collection("innovations").where("creatorId", "==", ALICE_UID).get());

  // Unfiltered query fails because PRIVATE doc of Alice exists; direct private doc read fails for Bob
  await assertFails(bobDb.collection("innovations").get());
  await assertFails(bobDb.collection("innovations").doc("inno_priv").get());

  // Bob can increment likeCount by +1 on PUBLIC innovation, but cannot modify title or jump +5
  await assertSucceeds(
    bobDb.collection("innovations").doc("inno_pub").update({
      likeCount: 1,
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(
    bobDb.collection("innovations").doc("inno_pub").update({
      likeCount: 10,
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(
    bobDb.collection("innovations").doc("inno_pub").update({
      title: "Hijacked Title",
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(bobDb.collection("innovations").doc("inno_pub").delete());

  // Owner Alice can update title
  await assertSucceeds(
    aliceDb.collection("innovations").doc("inno_pub").update({
      title: "AquaMesh v2",
      updatedAt: serverTimestamp(),
    })
  );
});

test("Comments: create on existing innovation and owner/innovation-creator delete permissions", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  await assertSucceeds(aliceDb.collection("innovations").doc("inno_pub").set(validInnovation(ALICE_UID, "PUBLIC")));

  const bobCommentRef = bobDb.collection("innovations").doc("inno_pub").collection("comments").doc("c1");
  await assertSucceeds(
    bobCommentRef.set({
      innovationId: "inno_pub",
      authorId: BOB_UID,
      authorName: "Bob Builder",
      body: "Great membrane geometry!",
      visibility: "PUBLIC",
      createdAt: serverTimestamp(),
    })
  );

  // Aligned list succeeds
  await assertSucceeds(
    charlieDb.collection("innovations").doc("inno_pub").collection("comments").where("visibility", "==", "PUBLIC").get()
  );

  // Charlie cannot delete Bob's comment
  await assertFails(charlieDb.collection("innovations").doc("inno_pub").collection("comments").doc("c1").delete());

  // Bob can delete own comment
  await assertSucceeds(bobCommentRef.delete());
});

test("Talents: public listing and owner-only mutations", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("talents").doc(ALICE_UID).set({
      userId: ALICE_UID,
      name: "Alice Innovator",
      role: "Creator",
      headline: "Hardware Founder",
      bio: "Bio text",
      skills: ["IoT", "CAD"],
      location: "Austin, TX",
      projectsCount: 3,
      isPublic: true,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(bobDb.collection("talents").where("isPublic", "==", true).get());
  await assertFails(bobDb.collection("talents").doc(ALICE_UID).update({ headline: "Hacked" }));
});

test("Proposals: sender creates PENDING, recipient reads and accepts/declines, third-party denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  await assertSucceeds(aliceDb.collection("innovations").doc("inno_pub").set(validInnovation(ALICE_UID, "PUBLIC")));

  const proposalRef = bobDb.collection("proposals").doc("prop_1");
  await assertSucceeds(
    proposalRef.set({
      senderId: BOB_UID,
      senderName: "Bob Investor",
      senderHeadline: "Angel Partner",
      senderLocation: "London, UK",
      recipientId: ALICE_UID,
      innovationId: "inno_pub",
      innovationTitle: "AquaMesh Fog Harvester",
      proposalType: "Investment",
      amount: "$50,000 Seed",
      message: "Would love to discuss pre-seed funding and pilot manufacturing.",
      contactInfo: "bob@apex.example",
      status: "PENDING",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Aligned list queries succeed
  await assertSucceeds(aliceDb.collection("proposals").where("recipientId", "==", ALICE_UID).get());
  await assertSucceeds(bobDb.collection("proposals").where("senderId", "==", BOB_UID).get());

  // Third-party denied
  await assertFails(charlieDb.collection("proposals").doc("prop_1").get());
  await assertFails(charlieDb.collection("proposals").get());

  // Sender cannot accept own proposal
  await assertFails(
    bobDb.collection("proposals").doc("prop_1").update({
      status: "ACCEPTED",
      updatedAt: serverTimestamp(),
    })
  );

  // Recipient Alice accepts proposal
  await assertSucceeds(
    aliceDb.collection("proposals").doc("prop_1").update({
      status: "ACCEPTED",
      updatedAt: serverTimestamp(),
    })
  );
});

test("VerifiedOrganizations: admin-only writes, signed-in reads of VERIFIED/NEEDS_REVIEW, UNVERIFIED hidden", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  const orgPayload = (status = "VERIFIED") => ({
    name: "Example BioIncubator",
    organizationType: "Incubator",
    sector: "Biotech",
    description: "Publicly listed biotech incubator cohort.",
    location: "Bengaluru, Karnataka, India",
    stageOrFocus: "Pre-seed & Lab Prototyping",
    website: "https://example.org",
    publicContactChannel: "https://example.org/apply",
    publicOpportunities: ["Ignition Grant Window"],
    eligibility: "Early-stage biotech startups registered in India",
    geography: "India",
    deadline: "2026-12-31",
    sourceName: "Official Program Portal",
    sourceUrl: "https://example.org/program",
    verificationStatus: status,
    lastVerifiedAt: "2026-11-16",
    updatedAt: serverTimestamp(),
  });

  // Non-admin Bob cannot create or delete verified_organizations
  await assertFails(bobDb.collection("verified_organizations").doc("org_1").set(orgPayload("VERIFIED")));

  // Seed admin marker + org docs via rules-bypassed admin context
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminBypass = ctx.firestore();
    await adminBypass.collection("admins").doc(ALICE_UID).set({ role: "admin" });
    await adminBypass.collection("verified_organizations").doc("org_hidden").set(orgPayload("UNVERIFIED"));
  });

  // Admin Alice can write verified organization
  await assertSucceeds(aliceDb.collection("verified_organizations").doc("org_1").set(orgPayload("VERIFIED")));

  // Signed-in Bob can read VERIFIED doc & query aligned status, cannot read UNVERIFIED doc, unauth denied
  await assertSucceeds(bobDb.collection("verified_organizations").doc("org_1").get());
  await assertSucceeds(
    bobDb.collection("verified_organizations").where("verificationStatus", "in", ["VERIFIED", "NEEDS_REVIEW"]).get()
  );
  await assertFails(bobDb.collection("verified_organizations").doc("org_hidden").get());
  await assertFails(unauthDb.collection("verified_organizations").doc("org_1").get());
});

test("SavedOpportunities: owner create/read/delete, update & cross-user denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const savedRef = aliceDb.collection("users").doc(ALICE_UID).collection("saved_opportunities").doc("org_1");
  await assertSucceeds(
    savedRef.set({
      userId: ALICE_UID,
      organizationId: "org_1",
      organizationName: "Example BioIncubator",
      organizationType: "Incubator",
      sector: "Biotech",
      sourceUrl: "https://example.org/program",
      lastVerifiedAt: "2026-11-16",
      createdAt: serverTimestamp(),
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("saved_opportunities").get());
  await assertFails(savedRef.update({ sector: "FinTech" }));
  await assertSucceeds(savedRef.delete());
});

test("Talents visibility & Direct Connection Proposals: private toggle & innovationId='none' gating", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("talents").doc(ALICE_UID).set({
      userId: ALICE_UID,
      name: "Alice Innovator",
      role: "Creator",
      headline: "Hardware Founder",
      bio: "Bio text",
      skills: ["IoT"],
      location: "Bengaluru, India",
      projectsCount: 1,
      isPublic: false,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Private talent doc is hidden from Bob, readable & toggleable by owner Alice
  await assertFails(bobDb.collection("talents").doc(ALICE_UID).get());
  await assertSucceeds(aliceDb.collection("talents").doc(ALICE_UID).get());
  await assertSucceeds(
    aliceDb.collection("talents").doc(ALICE_UID).update({
      isPublic: true,
      updatedAt: serverTimestamp(),
    })
  );
  await assertSucceeds(bobDb.collection("talents").doc(ALICE_UID).get());

  // Direct collaboration proposal (innovationId = "none") succeeds to Alice's talent profile
  const directProp = {
    senderId: BOB_UID,
    senderName: "Bob Builder",
    recipientId: ALICE_UID,
    recipientName: "Alice Innovator",
    innovationId: "none",
    innovationTitle: "Direct Mentorship & Co-founder Intro",
    proposalType: "Mentorship",
    message: "Happy to collaborate on hardware validation without sharing IP yet.",
    sharedIdeaScope: "NONE",
    status: "PENDING",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
  await assertSucceeds(bobDb.collection("proposals").doc("prop_direct").set(directProp));

  // Self-proposal or proposal to nonexistent talent is rejected
  await assertFails(
    bobDb.collection("proposals").doc("prop_self").set({
      ...directProp,
      recipientId: BOB_UID,
    })
  );
  await assertFails(
    bobDb.collection("proposals").doc("prop_ghost").set({
      ...directProp,
      recipientId: "unknown_user_999",
    })
  );
});

