package me.justahuman.slimefun_essentials.compat.jei;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HexFormat;
import java.util.Locale;

/** Produces stable, collision-free JEI paths for case-sensitive Slimefun category ids. */
final class RecipeTypeIds {
    private RecipeTypeIds() {}

    static String pathFor(String categoryId, Collection<String> categoryIds) {
        String lowerCaseId = categoryId.toLowerCase(Locale.ROOT);
        long caseInsensitiveMatches = categoryIds.stream()
                .filter(candidate -> candidate.equalsIgnoreCase(categoryId))
                .count();
        if (caseInsensitiveMatches <= 1) {
            return lowerCaseId;
        }

        String encodedId = HexFormat.of().formatHex(categoryId.getBytes(StandardCharsets.UTF_8));
        return lowerCaseId + "__" + encodedId;
    }
}
