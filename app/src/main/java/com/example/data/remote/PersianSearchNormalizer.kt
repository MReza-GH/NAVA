package com.example.data.remote

object PersianSearchNormalizer {

    // Common Iranian artist transliteration map for bi-directional search
    private val artistTransliterations = mapOf(
        "ebi" to "ابی",
        "ابی" to "ebi",
        "googoosh" to "گوگوش",
        "gogoosh" to "گوگوش",
        "گوگوش" to "googoosh",
        "shajarian" to "شجریان",
        "شجریان" to "shajarian",
        "homayoun shajarian" to "همایون شجریان",
        "همایون شجریان" to "homayoun shajarian",
        "mohammad reza shajarian" to "محمدرضا شجریان",
        "محمدرضا شجریان" to "shajarian",
        "mohsen chavoshi" to "محسن چاوشی",
        "chavoshi" to "چاوشی",
        "محسن چاوشی" to "mohsen chavoshi",
        "چاوشی" to "chavoshi",
        "mohsen yeganeh" to "محسن یگانه",
        "محسن یگانه" to "mohsen yeganeh",
        "shadmehr" to "شادمهر",
        "shadmehr aghili" to "شادمهر عقیلی",
        "شادمهر" to "shadmehr",
        "شادمهر عقیلی" to "shadmehr aghili",
        "hayedeh" to "هایده",
        "هایده" to "hayedeh",
        "mahasti" to "مهستی",
        "مهستی" to "mahasti",
        "moein" to "معین",
        "معین" to "moein",
        "dariush" to "داریوش",
        "داریوش" to "dariush",
        "siavash ghomayshi" to "سیاوش قمیشی",
        "ghomayshi" to "قمیشی",
        "سیاوش قمیشی" to "siavash ghomayshi",
        "reza sadeghi" to "رضا صادقی",
        "رضا صادقی" to "reza sadeghi",
        "alireza ghorbani" to "علیرضا قربانی",
        "ghorbani" to "قربانی",
        "علیرضا قربانی" to "alireza ghorbani",
        "sirvan khosravi" to "سیروان خسروی",
        "سیروان خسروی" to "sirvan khosravi",
        "xaniar khosravi" to "زانیار خسروی",
        "زانیار خسروی" to "xaniar khosravi",
        "benam bani" to "بهنام بانی",
        "behnam bani" to "بهنام بانی",
        "بهنام بانی" to "behnam bani",
        "arash" to "آرش",
        "آرش" to "arash",
        "sami yusuf" to "سامی یوسف",
        "سامی یوسف" to "sami yusuf"
    )

    fun normalize(input: String): String {
        if (input.isBlank()) return ""

        return input
            .trim()
            // Arabic to Persian Yeh
            .replace('ي', 'ی')
            .replace('ى', 'ی')
            // Arabic to Persian Kaf
            .replace('ك', 'ک')
            // Arabic Heh variants
            .replace('ة', 'ه')
            .replace('ۀ', 'ه')
            // Alef variants to plain Alef
            .replace('آ', 'ا')
            .replace('إ', 'ا')
            .replace('أ', 'ا')
            // Remove Arabic Diacritics (harakat: fathah, kasrah, dammah, tanwin, sukun, tashdid)
            .replace(Regex("[\u064B-\u065F]"), "")
            // Remove Tatweel / Kashida
            .replace("\u0640", "")
            // Replace Zero-Width Non-Joiner and Zero-Width Space with standard space
            .replace('\u200C', ' ')
            .replace('\u200B', ' ')
            // Normalize spaces
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun generateSearchQueries(query: String): List<String> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return emptyList()

        val normalized = normalize(trimmed)
        val queries = mutableListOf<String>()

        // 1. Primary normalized query
        queries.add(normalized)

        // 2. Original query if different
        if (trimmed.lowercase() != normalized.lowercase()) {
            queries.add(trimmed)
        }

        // 3. Check for Persian/Finglish artist mapping
        val lowerNormalized = normalized.lowercase()
        artistTransliterations[lowerNormalized]?.let { mapped ->
            if (!queries.contains(mapped)) {
                queries.add(mapped)
            }
        }

        // 4. Also check word-by-word artist mapping for multi-word queries
        for ((key, value) in artistTransliterations) {
            if (lowerNormalized.contains(key) && !lowerNormalized.contains(value)) {
                val replaced = lowerNormalized.replace(key, value)
                if (!queries.contains(replaced)) {
                    queries.add(replaced)
                }
            }
        }

        return queries.distinct()
    }
}
