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

/**
 * Strips invisible Unicode direction/formatting marks that Arabic keyboards inject
 * around digits, then trims whitespace. Use before comparing barcode strings.
 */
fun String.normalizeBarcode(): String = this
    .filter { it.code !in INVISIBLE_CHARS }
    .trim()

private val INVISIBLE_CHARS = setOf(
    0x200B, // zero-width space
    0x200C, // zero-width non-joiner
    0x200D, // zero-width joiner
    0x200E, // left-to-right mark
    0x200F, // right-to-left mark  ← Arabic keyboards inject this around numbers
    0x202A, // left-to-right embedding
    0x202B, // right-to-left embedding
    0x202C, // pop directional formatting
    0x202D, // left-to-right override
    0x202E, // right-to-left override
    0x2066, // left-to-right isolate
    0x2067, // right-to-left isolate
    0x2068, // first strong isolate
    0x2069, // pop directional isolate
    0x00A0, // non-breaking space
    0xFEFF  // byte order mark / zero-width no-break space
)

/** Returns true if [name] contains every word in [query] after Arabic normalization. */
fun arabicContains(name: String, query: String): Boolean {
    val normalizedName = normalizeArabic(name).lowercase()
    return query.trim().split(Regex("\\s+")).all { word ->
        normalizedName.contains(normalizeArabic(word).lowercase())
    }
}
