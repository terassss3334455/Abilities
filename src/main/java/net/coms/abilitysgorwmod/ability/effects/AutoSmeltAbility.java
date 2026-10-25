package net.coms.abilitysgorwmod.ability.effects;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;

import java.util.Optional;

/** 一度の取得につき精錬工程を一段階だけ進める。 */
public final class AutoSmeltAbility {
    private AutoSmeltAbility() {}

    public static ItemStack smeltOneStep(ItemStack input, Level level) {
        if (input.isEmpty()) return input;
        SimpleContainer container = new SimpleContainer(1);
        container.setItem(0, input.copyWithCount(1));
        Optional<SmeltingRecipe> recipe = level.getRecipeManager().getRecipeFor(
                RecipeType.SMELTING, container, level);
        if (recipe.isEmpty()) return input;
        ItemStack result = recipe.get().assemble(container, level.registryAccess());
        if (result.isEmpty()) return input;
        result.setCount(Math.max(1, result.getCount()) * input.getCount());
        return result;
    }
}
