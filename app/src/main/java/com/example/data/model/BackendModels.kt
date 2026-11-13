package com.example.data.model

data class AuthUser(
    val id: String,
    val name: String,
    val email: String,
    val role: String = "Creator",
    val headline: String = "Innovator on IDEA2IMPACT",
    val bio: String = "",
    val avatarUrl: String? = null,
    val location: String = "Global",
    val skills: List<String> = emptyList(),
    val verified: Boolean = true
)

data class CategoryItem(
    val name: String,
    val description: String,
    val count: Int = 0
)

object ExactCategories {
    val defaultItems: List<CategoryItem> = listOf(
        CategoryItem("Ideas", "Concepts ready for their next chapter", 5),
        CategoryItem("Inventions", "New solutions to real problems", 5),
        CategoryItem("Physical Products", "Objects made to make life better", 5),
        CategoryItem("Websites", "Digital experiences and platforms", 5),
        CategoryItem("Mobile Apps", "Apps built around people", 5),
        CategoryItem("AI & Robotics", "Intelligent systems shaping tomorrow", 5),
        CategoryItem("Software", "Tools that empower creators", 5),
        CategoryItem("Green Tech", "Sustainable innovations for the planet", 5)
    )

    val stages: List<String> = listOf("All", "Idea", "Prototype", "MVP", "Launched")
    val talentRoles: List<String> = listOf("All", "Builders", "Creators", "Investors", "Patent Professionals")
    val proposalTypes: List<String> = listOf("Investment", "Co-Founder", "Engineering", "Distribution")
}

data class BackendInnovation(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val stage: String,
    val fundingGoal: Double,
    val valuation: Double,
    val imageUrl: String?,
    val authorId: String,
    val authorName: String,
    val authorTitle: String,
    val authorAvatar: String?,
    val isVerifiedCreator: Boolean,
    val likeCount: Int,
    val commentCount: Int,
    val isLiked: Boolean,
    val isBookmarked: Boolean,
    val ndaRequired: Boolean = true,
    val createdAt: String = ""
)

data class BackendComment(
    val id: String,
    val innovationId: String,
    val authorName: String,
    val authorAvatar: String?,
    val body: String,
    val createdAt: String
)

data class BackendTalent(
    val id: String,
    val name: String,
    val role: String,
    val roleCategory: String,
    val headline: String,
    val skills: List<String>,
    val location: String,
    val avatarUrl: String?,
    val verified: Boolean = true
)

data class PitchSlide(
    val slideNumber: Int,
    val title: String,
    val subtitle: String,
    val bullets: List<String>
)

data class PitchDeckResult(
    val innovationTitle: String,
    val slides: List<PitchSlide>
)

data class BackendProposal(
    val id: String,
    val innovationId: String,
    val innovationTitle: String,
    val senderName: String,
    val proposalType: String,
    val amount: String,
    val message: String,
    val contactInfo: String,
    val status: String,
    val createdAt: String
)

data class DashboardStats(
    val innovationsPosted: Int = 0,
    val savedOpportunities: Int = 0,
    val proposalsReceived: Int = 0,
    val totalUpvotes: Int = 0,
    val myInnovations: List<BackendInnovation> = emptyList(),
    val savedList: List<BackendInnovation> = emptyList(),
    val inboundProposals: List<BackendProposal> = emptyList()
)

data class CopilotMessage(
    val id: String,
    val isAssistant: Boolean,
    val content: String,
    val timestamp: String,
    val isError: Boolean = false
)

data class DigitalNdaRecord(
    val innovationId: String,
    val signerName: String,
    val signerOrg: String,
    val signatureHash: String,
    val signedAt: String
)
