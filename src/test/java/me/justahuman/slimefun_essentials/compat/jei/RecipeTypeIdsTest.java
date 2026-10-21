package me.justahuman.slimefun_essentials.compat.jei;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class RecipeTypeIdsTest {
    @Test
    void preservesReadablePathWhenThereIsNoCaseCollision() {
        assertEquals("mona_industry_expert_crafting", RecipeTypeIds.pathFor(
                "MONA_INDUSTRY_EXPERT_CRAFTING", List.of("MONA_INDUSTRY_EXPERT_CRAFTING")));
    }

    @Test
    void separatesCategoryIdsThatOnlyDifferByCase() {
        List<String> ids = List.of("ENHANCED_CRAFTING_TABLE", "enhanced_crafting_table");
        String upper = RecipeTypeIds.pathFor("ENHANCED_CRAFTING_TABLE", ids);
        String lower = RecipeTypeIds.pathFor("enhanced_crafting_table", ids);

        assertNotEquals(upper, lower);
        assertEquals(upper, RecipeTypeIds.pathFor("ENHANCED_CRAFTING_TABLE", ids));
        assertEquals(lower, RecipeTypeIds.pathFor("enhanced_crafting_table", ids));
    }
}
