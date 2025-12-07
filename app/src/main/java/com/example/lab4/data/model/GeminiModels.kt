package com.example.lab4.data.model

// --- Gemini API Request Models ---

data class GeminiRequest(
    val contents: List<Content>
)

data class Content(
    val parts: List<Part>
)

data class Part(
    val text: String
)

// --- Gemini API Response Models ---

data class GeminiResponse(
    val candidates: List<Candidate>?
)

data class Candidate(
    val content: Content?,
    val finishReason: String?,
    val index: Int?,
    val safetyRatings: List<SafetyRating>?
)

data class SafetyRating(
    val category: String?,
    val probability: String?
)
