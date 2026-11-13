# IDEA2IMPACT (Android)

**IDEA2IMPACT** is a native Android application built with **Kotlin** and **Jetpack Compose (Material 3)** where bold ideas, inventions, physical products, and software prototypes connect with builders, patent professionals, and seed investors.

## Core Features

- **Google Sign-In & Real-Time Cloud Sync**: Authenticated with Jetpack `CredentialManager` (`GetSignInWithGoogleOption`) and backed by **Cloud Firestore** across innovations, comments, proposals, talent profiles, saved bookmarks, signed NDAs, and micro-backing pledges.
- **Innovation Marketplace & Discovery**: Browse 40+ curated showcase concepts and community-published listings across 8 sectors with search, category filters, sorting, likes, saves, and real-time updates.
- **Interactive 3D CAD & Prototype Explorer**: Custom Jetpack Compose `Canvas` 3D viewer supporting touch drag-to-orbit, zoom, wireframe toggle, auto-rotation, exploded CAD disassembly slider (0–100%), and 4 metallic finishes.
- **Cryptographic IP Protection & Deal Room**:
  - **SHA-256 IP Timestamp Certificate** with deterministic visual verification matrix seal.
  - **Digital Mutual NDA (v2.4)** signature workflow unlocking confidential deal room packs and financial models.
  - **Micro-Backing Pledges** and **Partnership / Investment Proposals** (`Investment`, `Licensing`, `Co-founder`, `Acquisition`, `Collaboration`).
- **Impact AI Copilot & Investor One-Pager**: Powered by **Firebase AI Logic (`gemini-3.8-flash`)** and **Firebase App Check** for streaming novelty scans, TAM/SAM/SOM sizing, BOM blueprints, and structured JSON investor memos.
- **Dual Brand Themes**: Persisted via Jetpack DataStore with default **Dark Mode** (`#080B14` Cosmic Navy / `#54E6F5` Neon Cyan / `#6485FF` Electric Blue) and **Lavender Light Mode** (`#F5F0FF` Lavender / `#7041C7` Royal Purple / `#2563FF` Accent Blue).

## Tech Stack

- **UI**: Kotlin, Jetpack Compose, Material Design 3, Navigation Compose (`@Serializable` routes), Coil
- **Backend & AI**: Firebase Authentication, Cloud Firestore, Firebase AI Logic (`gemini-3.8-flash`), Firebase App Check (reCAPTCHA Enterprise)
- **Local Preferences**: Jetpack DataStore Preferences
- **Testing**: Node `@firebase/rules-unit-testing` & Robolectric + Firebase Emulator Suite
