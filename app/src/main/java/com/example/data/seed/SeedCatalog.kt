package com.example.data.seed

import com.example.data.model.Innovation
import com.example.data.model.TalentProfile

object SeedCatalog {

    data class SeedTalent(
        val slug: String,
        val key: String,
        val name: String,
        val role: String,
        val headline: String,
        val bio: String,
        val skills: List<String>,
        val location: String,
        val avatarUrl: String,
        val projectsCount: Long
    )

    val talents: List<SeedTalent> = listOf(
        SeedTalent(
            slug = "maya_chen",
            key = "maya",
            name = "Maya Chen",
            role = "Creator",
            headline = "Hardware & IoT Innovator · CleanTech Founder",
            bio = "Passionate about environmental sensors, sustainability, and human-centered embedded hardware design.",
            skills = listOf("IoT", "Hardware Design", "Embedded C", "Rapid Prototyping"),
            location = "San Francisco, CA",
            avatarUrl = "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&w=400&q=80",
            projectsCount = 6L
        ),
        SeedTalent(
            slug = "marcus_vance",
            key = "marcus",
            name = "Marcus Vance",
            role = "Investor",
            headline = "Venture Partner @ Apex Horizons · Angel Investor",
            bio = "Backing early-stage hardware, robotics, and climate resilience technologies across US and Europe. Deployed $4.2M in pre-seed & seed rounds.",
            skills = listOf("Seed Funding", "Due Diligence", "Series A", "Cap Table"),
            location = "London, UK",
            avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=400&q=80",
            projectsCount = 2L
        ),
        SeedTalent(
            slug = "devon_brooks",
            key = "devon",
            name = "Devon Brooks",
            role = "Builder",
            headline = "Principal Robotics Engineer & AI Systems Architect",
            bio = "Over 8 years designing autonomous drone control stacks, bionic mechanics, and embedded edge vision. Open to co-founding deep-tech startups.",
            skills = listOf("ROS2", "PyTorch", "Embedded Linux", "Computer Vision", "CUDA"),
            location = "Austin, TX",
            avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=400&q=80",
            projectsCount = 5L
        ),
        SeedTalent(
            slug = "aiko_tanaka",
            key = "aiko",
            name = "Aiko Tanaka",
            role = "Creator",
            headline = "Lead Industrial Designer & Ergonomics Specialist",
            bio = "Specializing in sustainable consumer hardware, minimalist modular furniture, and human-first spatial computing accessories.",
            skills = listOf("SolidWorks", "3D Rendering", "CMF Design", "Sustainable Materials"),
            location = "Tokyo, Japan",
            avatarUrl = "https://images.unsplash.com/photo-1580489944761-15a19d654956?auto=format&fit=crop&w=400&q=80",
            projectsCount = 7L
        ),
        SeedTalent(
            slug = "elena_rostova",
            key = "elena",
            name = "Elena Rostova",
            role = "Creator",
            headline = "Aerospace Engineer & Autonomous Solar Flight Pioneer",
            bio = "Developing high-altitude continuous solar UAV platforms for wildfire detection and climate atmospheric modeling.",
            skills = listOf("Aerodynamics", "Photovoltaics", "Carbon Fiber", "Autopilot PX4"),
            location = "Munich, Germany",
            avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80",
            projectsCount = 4L
        ),
        SeedTalent(
            slug = "david_miller",
            key = "david",
            name = "David S. Miller, Esq.",
            role = "Patent Professional",
            headline = "Registered Patent Attorney · Tech IP & Licensing Strategist",
            bio = "Advising inventors, university spin-outs and early-stage founders on provisional patents, freedom-to-operate opinions, and IP monetization.",
            skills = listOf("Patent Drafting", "USPTO Filing", "IP Licensing", "Trade Secrets"),
            location = "Washington, D.C.",
            avatarUrl = "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?auto=format&fit=crop&w=400&q=80",
            projectsCount = 3L
        ),
        SeedTalent(
            slug = "aris_thorne",
            key = "aris",
            name = "Dr. Aris Thorne",
            role = "Creator",
            headline = "Lead Bioinformatician & Synthetic Genomics Pioneer",
            bio = "Translating CRISPR gene circuits and rapid enzymatic biosensors into decentralized clinical diagnostics.",
            skills = listOf("CRISPR", "Synthetic Biology", "Microfluidics", "Bioinformatics"),
            location = "Boston, MA",
            avatarUrl = "https://images.unsplash.com/photo-1537368910025-700350fe46c7?auto=format&fit=crop&w=400&q=80",
            projectsCount = 5L
        ),
        SeedTalent(
            slug = "priya_sharma",
            key = "priya",
            name = "Priya Sharma",
            role = "Creator",
            headline = "AgriTech Systems Engineer & Soil Climate Architect",
            bio = "Building autonomous subsurface soil spectrometers and closed-loop solar desalination vertical farming pods.",
            skills = listOf("Soil Chemistry", "IoT Sensors", "Hydroponics", "Embedded Hardware"),
            location = "Bengaluru, India",
            avatarUrl = "https://images.unsplash.com/photo-1573497019940-1c28c88b4f3e?auto=format&fit=crop&w=400&q=80",
            projectsCount = 4L
        ),
        SeedTalent(
            slug = "lucas_moreau",
            key = "lucas",
            name = "Lucas Moreau",
            role = "Builder",
            headline = "Acoustic Ultrasound Engineer & Spatial Haptics Specialist",
            bio = "Pioneering mid-air tactile wave-focusing transducers for immersive AR spatial computing and surgical simulations.",
            skills = listOf("Phased Arrays", "DSP", "C++", "Spatial Audio", "Haptics"),
            location = "Paris, France",
            avatarUrl = "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?auto=format&fit=crop&w=400&q=80",
            projectsCount = 5L
        ),
        SeedTalent(
            slug = "zoe_kalu",
            key = "zoe",
            name = "Zoe Kalu",
            role = "Builder",
            headline = "Zero-Knowledge Cryptographer & Distributed Systems Architect",
            bio = "Focused on trustless verification protocols, cryptographic proof-of-competence, and decentralized trade finance networks.",
            skills = listOf("zk-SNARKs", "Rust", "Distributed Systems", "Cryptography"),
            location = "Nairobi, Kenya",
            avatarUrl = "https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?auto=format&fit=crop&w=400&q=80",
            projectsCount = 5L
        )
    )

    private val talentByKey = talents.associateBy { it.key }

    private data class SeedIdeaSpec(
        val slug: String,
        val title: String,
        val description: String,
        val category: String,
        val authorKey: String,
        val price: Double,
        val licensing: Boolean,
        val funding: Boolean,
        val imageUrl: String,
        val likeCount: Long,
        val stageIndex: Long
    )

    private val ideaSpecs = listOf(
        // Ideas
        SeedIdeaSpec(
            "aquamesh",
            "AquaMesh — Atmospheric Fog & Humidity Water Harvester",
            "Passive biomimetic mesh capturing 15L of potable water daily in arid coastal zones with zero electricity. Inspired by the Namib Desert beetle's hydrophilic-hydrophobic microstructures.",
            "Ideas", "priya", 0.0, true, true,
            "https://images.unsplash.com/photo-1509228468518-180dd4864904?auto=format&fit=crop&w=800&q=80",
            142L, 1L
        ),
        SeedIdeaSpec(
            "directair",
            "DirectAir — Solar Solid-State CO2 Mineralization Matrix",
            "A modular architectural building-cladding filter that binds ambient carbon dioxide into harmless calcium carbonate minerals using ambient humidity and passive solar thermal convection.",
            "Ideas", "maya", 0.0, true, true,
            "https://images.unsplash.com/photo-1497435334941-8c899ee9e8e9?auto=format&fit=crop&w=800&q=80",
            88L, 0L
        ),
        SeedIdeaSpec(
            "decentralgrid",
            "DecentralGrid — P2P Renewable Microgrid Exchange Protocol",
            "Open protocol design allowing residential solar battery owners to trade kilowatt-hours locally with neighbours via ultra-low latency cryptographic state channels.",
            "Ideas", "zoe", 0.0, true, true,
            "https://images.unsplash.com/photo-1473341304170-971dccb5ac1e?auto=format&fit=crop&w=800&q=80",
            76L, 1L
        ),
        SeedIdeaSpec(
            "mycofeed",
            "MycoFeed — Fungal Protein Bioconversion System",
            "Converts organic agricultural byproduct streams into high-nutrient mycoprotein feedstock using oyster mushroom mycelium strains, cutting animal feed emissions by 80%.",
            "Ideas", "aris", 150.0, true, true,
            "https://images.unsplash.com/photo-1546842931-886c185b4c8c?auto=format&fit=crop&w=800&q=80",
            64L, 1L
        ),
        SeedIdeaSpec(
            "oceanplast",
            "OceanPlast — Enzymatic Biodegradation River Trap",
            "Self-anchored floating river booms utilizing engineered bacterial PETases to depolymerize plastic waste into non-toxic terephthalic acid before reaching open oceans.",
            "Ideas", "maya", 0.0, true, true,
            "https://images.unsplash.com/photo-1544551763-46a013bb70d5?auto=format&fit=crop&w=800&q=80",
            91L, 0L
        ),

        // Inventions
        SeedIdeaSpec(
            "aquasense",
            "AquaSense — Smart Water Monitor",
            "A low-cost sensor concept that helps households spot micro-leaks and understand daily water use with high precision. Integrates with mobile notifications and municipal conservation goals.",
            "Inventions", "maya", 120.0, true, true,
            "https://images.unsplash.com/photo-1581092160607-ee22621dd758?auto=format&fit=crop&w=800&q=80",
            118L, 2L
        ),
        SeedIdeaSpec(
            "aeroglide",
            "AeroGlide Solar Drone — Multi-Day Aerial Sentinel",
            "Autonomous carbon-composite drone powered by 24% efficient perovskite-silicon tandem solar cells, delivering 72+ hours of non-stop flight for wildfire detection and ocean monitoring.",
            "Inventions", "elena", 1250.0, true, true,
            "https://images.unsplash.com/photo-1508614589041-895b88991e3e?auto=format&fit=crop&w=800&q=80",
            165L, 2L
        ),
        SeedIdeaSpec(
            "thermaharvest",
            "ThermaHarvest — Waste-Heat Thermoelectric Generator",
            "Solid-state bismuth-telluride thermoelectric generator capturing factory exhaust heat (150-450°C) and converting it to electricity with a 3-year commercial payback period.",
            "Inventions", "maya", 450.0, true, true,
            "https://images.unsplash.com/photo-1513836279014-a89f7a76ae86?auto=format&fit=crop&w=800&q=80",
            104L, 2L
        ),
        SeedIdeaSpec(
            "voltatile",
            "VoltaTile — Kinetic Footstep Energy Harvesting Pavers",
            "Piezoelectric-electromagnetic hybrid floor tiles for high-traffic train stations and airports generating up to 7 Joules per footstep to power local emergency lighting and beacons.",
            "Inventions", "devon", 195.0, true, true,
            "https://images.unsplash.com/photo-1517646287270-a5a9ca602e5c?auto=format&fit=crop&w=800&q=80",
            83L, 1L
        ),
        SeedIdeaSpec(
            "canopyeye",
            "CanopyEye — Autonomous Tree-Planting Drone Swarm",
            "Autonomous lidar-guided drone swarm planting pre-germinated seed pods in deforested terrains with 85% germination success, planting up to 40,000 trees per day.",
            "Inventions", "elena", 890.0, true, true,
            "https://images.unsplash.com/photo-1527977966376-1c8408f9f108?auto=format&fit=crop&w=800&q=80",
            129L, 3L
        ),

        // Physical Products
        SeedIdeaSpec(
            "modunest",
            "ModuNest Furniture System",
            "Flat-pack modular furniture engineered for compact urban living spaces. Easily disassembled, expanded, and customized without tools using precision magnetic snap-joints.",
            "Physical Products", "aiko", 280.0, true, true,
            "https://images.unsplash.com/photo-1555041469-a586c61ea9bc?auto=format&fit=crop&w=800&q=80",
            94L, 2L
        ),
        SeedIdeaSpec(
            "neurogrip",
            "NeuroGrip Haptic Bionic Hand",
            "Sub-$800 3D-printed multi-articulating bionic prosthesis with embedded neural myoelectric sensors and tactile vibration feedback for amputees.",
            "Physical Products", "devon", 790.0, true, true,
            "https://images.unsplash.com/photo-1589254065878-42c9da997008?auto=format&fit=crop&w=800&q=80",
            184L, 3L
        ),
        SeedIdeaSpec(
            "bioloom",
            "BioLoom — Algae Carbon Capture Façade Panels",
            "Architectural double-glazed exterior glass panels circulating chlorella microalgae to absorb indoor building CO2 while providing biophilic shade and biomass insulation.",
            "Physical Products", "aiko", 340.0, true, true,
            "https://images.unsplash.com/photo-1497366216548-37526070297c?auto=format&fit=crop&w=800&q=80",
            112L, 2L
        ),
        SeedIdeaSpec(
            "cobotgrip",
            "CobotGrip — Soft-Pneumatic Tactile End-Effector",
            "Food-safe silicone soft-robotics gripper with embedded micro-pressure sensors for handling delicate produce and fragile biological samples without bruising.",
            "Physical Products", "devon", 520.0, true, true,
            "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=800&q=80",
            67L, 1L
        ),
        SeedIdeaSpec(
            "hydropod",
            "HydroPod — Solar Desalination Greenhouse",
            "Compact modular greenhouse utilizing passive solar thermal evaporation to desalinate saline groundwater for organic hydroponic produce in desert communities.",
            "Physical Products", "priya", 1100.0, true, true,
            "https://images.unsplash.com/photo-1585320806297-9794b3e4eeae?auto=format&fit=crop&w=800&q=80",
            98L, 2L
        ),

        // Websites
        SeedIdeaSpec(
            "openpatent",
            "OpenPatent — Prior-Art Invalidation Exchange",
            "Global collaborative platform incentivizing patent researchers and engineers with bounty pools to unearth prior art against predatory patent claims.",
            "Websites", "david", 0.0, true, true,
            "https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=800&q=80",
            107L, 2L
        ),
        SeedIdeaSpec(
            "reclaim",
            "ReClaim — Circular Industrial Surplus Marketplace",
            "B2B web exchange connecting manufacturing plants with circular upcyclers to divert scrap metals, chemicals, and industrial polymers from landfills.",
            "Websites", "maya", 75.0, true, true,
            "https://images.unsplash.com/photo-1532996122724-e3c354a0b15b?auto=format&fit=crop&w=800&q=80",
            58L, 1L
        ),
        SeedIdeaSpec(
            "soundforge",
            "SoundForge — Procedural Ambient Audio Synthesizer",
            "Browser-based WebAudio DSP synthesizer generating infinite dynamic, royalty-free soundscapes tailored for focus workspaces, podcasts, and game designers.",
            "Websites", "lucas", 35.0, true, true,
            "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&w=800&q=80",
            72L, 2L
        ),
        SeedIdeaSpec(
            "venturetam",
            "VentureTAM — AI Market Size & Patent Modeler",
            "Interactive analytics platform empowering hardware and deep-tech founders to compute bottom-up Total Addressable Market (TAM) with verified patent graphs.",
            "Websites", "marcus", 85.0, true, true,
            "https://images.unsplash.com/photo-1460925895917-afdab827c52f?auto=format&fit=crop&w=800&q=80",
            89L, 3L
        ),
        SeedIdeaSpec(
            "skilllink",
            "SkillLink — Scientific Equipment Sharing Portal",
            "Academic equipment booking network connecting biomedical startups with idle university electron microscopes, sequencers, and mass spectrometers.",
            "Websites", "aris", 40.0, true, true,
            "https://images.unsplash.com/photo-1507668077129-56e32842fceb?auto=format&fit=crop&w=800&q=80",
            61L, 1L
        ),

        // Mobile Apps
        SeedIdeaSpec(
            "localloop",
            "LocalLoop Community Marketplace",
            "A neighbourhood-first mobile application connecting independent makers with nearby customers for on-demand local pickups and hyper-local delivery.",
            "Mobile Apps", "maya", 0.0, false, true,
            "https://images.unsplash.com/photo-1522202176988-66273c2fd55f?auto=format&fit=crop&w=800&q=80",
            53L, 1L
        ),
        SeedIdeaSpec(
            "vitalpatch",
            "VitalPatch — Biodegradable Cardiac Telemetry App",
            "Ultra-thin chest patch made from biodegradable silk fibroin paired with a mobile telemetry app for continuous ECG, arrhythmia detection, and doctor alerts.",
            "Mobile Apps", "aris", 45.0, true, true,
            "https://images.unsplash.com/photo-1576091160399-112ba8d25d1d?auto=format&fit=crop&w=800&q=80",
            136L, 2L
        ),
        SeedIdeaSpec(
            "kindredkit",
            "KindredKit — Offline Mesh Emergency Network",
            "Mesh-networking offline mobile application that organizes neighborhood disaster supplies, CPR responders, and rescue coordination without cellular towers.",
            "Mobile Apps", "zoe", 0.0, false, true,
            "https://images.unsplash.com/photo-1512941937669-90a1b58e7e9c?auto=format&fit=crop&w=800&q=80",
            96L, 2L
        ),
        SeedIdeaSpec(
            "skillbridge",
            "SkillBridge — Trade Micro-Mentorship App",
            "Micro-coaching mobile platform pairing early-career tradespeople and technicians with retired master craftspeople for asynchronous video review.",
            "Mobile Apps", "devon", 29.0, true, true,
            "https://images.unsplash.com/photo-1531482615713-2afd69097998?auto=format&fit=crop&w=800&q=80",
            49L, 1L
        ),
        SeedIdeaSpec(
            "pantryzero",
            "PantryZero — Vision Food Expiration Planner",
            "Mobile kitchen assistant that scans grocery receipts and fridge shelves to generate zero-waste chef recipes before ingredients spoil.",
            "Mobile Apps", "priya", 19.0, true, true,
            "https://images.unsplash.com/photo-1540420773420-3366772f4999?auto=format&fit=crop&w=800&q=80",
            63L, 2L
        ),

        // Games
        SeedIdeaSpec(
            "chronos",
            "Chronos: Echoes of Carthage — Tactical RPG",
            "Isometric turn-based tactical strategy game set during the Punic Wars with authentic Hellenistic engineering, siege mechanics, and tactical dilemmas.",
            "Games", "lucas", 39.0, true, true,
            "https://images.unsplash.com/photo-1538481199705-c710c4e965fc?auto=format&fit=crop&w=800&q=80",
            74L, 2L
        ),
        SeedIdeaSpec(
            "aetheria",
            "Aetheria — Gravity-Shifting Puzzle Platformer",
            "Two-player physics platformer where players manipulate localized gravitational fields and quantum polarity to solve intricate environmental puzzles.",
            "Games", "lucas", 25.0, true, true,
            "https://images.unsplash.com/photo-1550745165-9bc0b252726f?auto=format&fit=crop&w=800&q=80",
            82L, 2L
        ),
        SeedIdeaSpec(
            "subgrid",
            "SubGrid: Cyber Defense Sandbox",
            "Real-time network security simulation game where players defend simulated smart cities, satellites, and electric power grids against cyber-physical attacks.",
            "Games", "zoe", 30.0, true, true,
            "https://images.unsplash.com/photo-1542751371-adc38448a05e?auto=format&fit=crop&w=800&q=80",
            68L, 1L
        ),
        SeedIdeaSpec(
            "biomecraft",
            "BiomeCraft: Terraforming Mars Simulator",
            "Hard sci-fi biological planetary simulation balancing atmospheric pressure, extremophile genetics, cyanobacteria cultures, and orbital solar mirrors.",
            "Games", "aris", 34.0, true, true,
            "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=800&q=80",
            59L, 1L
        ),
        SeedIdeaSpec(
            "echoesdeep",
            "Echoes of the Deep: Sonar Adventure",
            "Atmospheric abyss submarine exploration title where navigation, hydrothermal discovery, and survival rely entirely on realistic acoustic raytracing.",
            "Games", "lucas", 28.0, true, true,
            "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=800&q=80",
            77L, 2L
        ),

        // Software
        SeedIdeaSpec(
            "formflow",
            "FormFlow — No-code Conversational Intake",
            "A lightweight workflow tool that turns complex intake forms and multi-step applications into simple, friendly conversations.",
            "Software", "maya", 49.0, true, false,
            "https://images.unsplash.com/photo-1551288049-bebda4e38f71?auto=format&fit=crop&w=800&q=80",
            45L, 2L
        ),
        SeedIdeaSpec(
            "klarityai",
            "Klarity AI — Real-time Contract & IP Risk Scanner",
            "Privacy-first AI assistant that scans commercial NDAs, licensing agreements, and assignment clauses in 5 seconds to highlight unfavorable terms.",
            "Software", "david", 89.0, true, false,
            "https://images.unsplash.com/photo-1507238691740-187a5b1d37b8?auto=format&fit=crop&w=800&q=80",
            121L, 3L
        ),
        SeedIdeaSpec(
            "ciphervault",
            "CipherVault — Zero-Knowledge Credential Registry",
            "Cryptographic identity service enabling verified talent and university graduates to prove certifications without revealing personal identity.",
            "Software", "zoe", 120.0, true, true,
            "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?auto=format&fit=crop&w=800&q=80",
            103L, 2L
        ),
        SeedIdeaSpec(
            "latencyzero",
            "LatencyZero — Edge ML Quantization Compiler",
            "LLVM-backed compiler optimization suite that shrinks transformer neural networks by 85% for zero-latency inference on low-cost microcontrollers.",
            "Software", "devon", 150.0, true, true,
            "https://images.unsplash.com/photo-1555949963-aa79dcee981c?auto=format&fit=crop&w=800&q=80",
            148L, 3L
        ),
        SeedIdeaSpec(
            "auditforge",
            "AuditForge — Smart Contract Vulnerability Fuzzer",
            "AI-guided symbolic execution engine that tests decentralized smart contracts against reentrancy, flash-loan, and oracle manipulation exploits.",
            "Software", "zoe", 210.0, true, true,
            "https://images.unsplash.com/photo-1563986768609-322da13575f3?auto=format&fit=crop&w=800&q=80",
            92L, 2L
        ),

        // Creative Designs
        SeedIdeaSpec(
            "lumafold",
            "LumaFold — Origami Kinematic Solar Lantern",
            "Waterproof recycled PET lantern that folds completely flat for backpack transport and expands into a 360-degree ambient solar campsite lantern.",
            "Creative Designs", "aiko", 65.0, true, true,
            "https://images.unsplash.com/photo-1513519245088-0e12902e5a38?auto=format&fit=crop&w=800&q=80",
            115L, 2L
        ),
        SeedIdeaSpec(
            "aurasound",
            "AuraSound — Parametric Acoustic Timber Panels",
            "Sculptural wooden acoustic tiles engineered with algorithmic sound-diffusing Voronoi patterns for recording studios, home theaters, and lofts.",
            "Creative Designs", "aiko", 180.0, true, true,
            "https://images.unsplash.com/photo-1513694203232-719a280e022f?auto=format&fit=crop&w=800&q=80",
            86L, 2L
        ),
        SeedIdeaSpec(
            "aerozero",
            "AeroZero — Ultra-Light Aerogel Winter Jacket",
            "Minimalist extreme-weather outerwear utilizing encapsulated silica aerogel insulation just 2mm thick to provide -30°C thermal protection.",
            "Creative Designs", "aiko", 320.0, true, true,
            "https://images.unsplash.com/photo-1551028719-00167b16eac5?auto=format&fit=crop&w=800&q=80",
            109L, 2L
        ),
        SeedIdeaSpec(
            "biomorph",
            "BioMorph — 3D-Knitted Ergonomic Kinetic Chair",
            "Single-strand recycled ocean polymer knit chair that dynamically conforms to lumbar micro-movements without mechanical adjustment levers.",
            "Creative Designs", "aiko", 490.0, true, true,
            "https://images.unsplash.com/photo-1586023492125-27b2c045efd7?auto=format&fit=crop&w=800&q=80",
            99L, 3L
        ),
        SeedIdeaSpec(
            "chromadesk",
            "ChromaDesk — Modular Magnetic Workstation",
            "Solid anodized aerospace aluminum desk with integrated flush magnetic power rails and modular accessory docks for creative digital professionals.",
            "Creative Designs", "aiko", 640.0, true, true,
            "https://images.unsplash.com/photo-1524758631624-e2822e304c36?auto=format&fit=crop&w=800&q=80",
            84L, 2L
        )
    )

    fun buildSeedInnovations(ownerUid: String): List<Pair<String, Innovation>> {
        return ideaSpecs.map { spec ->
            val author = talentByKey[spec.authorKey] ?: talents.first()
            val docId = "seed_${spec.slug}"
            docId to Innovation(
                id = docId,
                creatorId = ownerUid,
                authorName = author.name,
                authorHeadline = author.headline,
                authorAvatar = author.avatarUrl,
                title = spec.title,
                description = spec.description,
                category = spec.category,
                price = spec.price,
                visibility = "PUBLIC",
                licensing = spec.licensing,
                funding = spec.funding,
                ownershipConfirmed = true,
                imageUrl = spec.imageUrl,
                likeCount = spec.likeCount,
                commentCount = 0L,
                stageIndex = spec.stageIndex,
                isShowcaseSeed = true
            )
        }
    }

    fun buildSeedTalents(ownerUid: String): List<Pair<String, TalentProfile>> {
        return talents.map { t ->
            val docId = "seed_talent_${t.slug}"
            docId to TalentProfile(
                id = docId,
                userId = ownerUid,
                name = t.name,
                role = t.role,
                headline = t.headline,
                bio = t.bio,
                skills = t.skills,
                location = t.location,
                avatarUrl = t.avatarUrl,
                projectsCount = t.projectsCount,
                isPublic = true
            )
        }
    }
}
