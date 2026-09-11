package com.elmotamyez.gallery.util

/**
 * Normalizes Arabic text for fuzzy search:
 *  - Unifies all alef variants (أ إ آ ٱ) → ا
 *  - Unifies teh marbuta (ة) → ه
 *  - Strips diacritics / harakat (َ ُ ِ ً ٌ ٍ ّ ْ ـ)
 */
fun normalizeArabic(text: String): String = buildString(text.length) {
    for (c in text) {
        when (c) {
            'أ', 'إ', 'آ', 'ٱ' -> append('ا')
            'ة'               -> append('ه')
            // diacritics (harakat) — drop
            'ً', 'ٌ', 'ٍ',
            'َ', 'ُ', 'ِ',
            'ّ', 'ْ', 'ـ' -> Unit
            else -> append(c)
        }
    }
}

/** Returns true if [name] contains every word in [query] after Arabic normalization. */
fun arabicContains(name: String, query: String): Boolean {
    val normalizedName = normalizeArabic(name).lowercase()
    return query.trim().split(Regex("\\s+")).all { word ->
        normalizedName.contains(normalizeArabic(word).lowercase())
    }
}
