package net.blay09.mods.cookingforblockheads.registry;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.blay09.mods.cookingforblockheads.KitchenMultiBlock;
import net.blay09.mods.cookingforblockheads.api.ISortButton;
import net.blay09.mods.cookingforblockheads.api.RecipeStatus;
import net.blay09.mods.cookingforblockheads.api.SinkHandler;
import net.blay09.mods.cookingforblockheads.api.ToastHandler;
import net.blay09.mods.cookingforblockheads.api.capability.IKitchenItemProvider;
import net.blay09.mods.cookingforblockheads.api.capability.KitchenItemProvider;
import net.blay09.mods.cookingforblockheads.api.event.FoodRegistryInitEvent;
import net.blay09.mods.cookingforblockheads.blaycommon.ItemUtils;
import net.blay09.mods.cookingforblockheads.compat.HarvestCraftAddon;
import net.blay09.mods.cookingforblockheads.container.inventory.InventoryCraftBook;
import net.blay09.mods.cookingforblockheads.registry.recipe.FoodIngredient;
import net.blay09.mods.cookingforblockheads.registry.recipe.FoodRecipe;
import net.blay09.mods.cookingforblockheads.registry.recipe.ShapedCraftingFood;
import net.blay09.mods.cookingforblockheads.registry.recipe.ShapedOreCraftingFood;
import net.blay09.mods.cookingforblockheads.registry.recipe.ShapelessCraftingFood;
import net.blay09.mods.cookingforblockheads.registry.recipe.ShapelessOreCraftingFood;
import net.blay09.mods.cookingforblockheads.registry.recipe.SmeltingFood;
import net.minecraft.block.Block;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import java.util.Objects;
import javax.annotation.Nullable;

public class CookingRegistry {
    public static class ItemID {
        ResourceLocation location;
        int metadata;

        public ItemID(ResourceLocation location, int metadata) {
            this.location = location;
            this.metadata = metadata;
        }

        public ItemID(ItemStack object) {
            this.location = object.getItem().getRegistryName();
            this.metadata = object.getItemDamage();
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof ItemID)) {
                return false;
            }
            ItemID itemid = (ItemID) o;
            return metadata == itemid.metadata &&
                Objects.equals(location, itemid.location);
        }

        @Override
        public int hashCode() {
            return Objects.hash(location, metadata);
        }

        @Override
        public String toString() {
            return location.toString() + "@" + metadata;
        }
    }

    private static final List<IRecipe> recipeList = Lists.newArrayList();
    private static final ArrayListMultimap<ItemID, FoodRecipe> foodItems = ArrayListMultimap.create();
    private static final NonNullList<ItemStack> tools = NonNullList.create();
    private static final Map<ItemStack, Integer> ovenFuelItems = Maps.newHashMap();
    private static final Map<ItemStack, ItemStack> ovenRecipes = Maps.newHashMap();
    private static final Map<ItemStack, SinkHandler> sinkHandlers = Maps.newHashMap();
    private static final Map<ItemStack, ToastHandler> toastHandlers = Maps.newHashMap();
    private static final NonNullList<ItemStack> waterItems = NonNullList.create();
    private static final NonNullList<ItemStack> milkItems = NonNullList.create();
    private static final List<ISortButton> customSortButtons = Lists.newArrayList();

    private static Collection<ItemStack> nonFoodRecipes;

    public static void initFoodRegistry() {
        recipeList.clear();
        foodItems.clear();

        FoodRegistryInitEvent init = new FoodRegistryInitEvent();
        MinecraftForge.EVENT_BUS.post(init);

        nonFoodRecipes = init.getNonFoodRecipes();

        // Crafting Recipes of Food Items
        for(Object obj : CraftingManager.getInstance().getRecipeList()) {
            IRecipe recipe = (IRecipe) obj;
            ItemStack output = recipe.getRecipeOutput();
            if(!output.isEmpty()) {
                if (output.getItem() instanceof ItemFood) {
                    if (HarvestCraftAddon.isWeirdBrokenRecipe(recipe)) {
                        continue;
                    }
                    addFoodRecipe(recipe);
                } else {
                    for (ItemStack itemStack : nonFoodRecipes) {
                        if (ItemUtils.areItemStacksEqualWithWildcard(recipe.getRecipeOutput(), itemStack)) {
                            addFoodRecipe(recipe);
                            break;
                        }
                    }
                }
            }
        }

        // Smelting Recipes of Food Items
        for(Object obj : FurnaceRecipes.instance().getSmeltingList().entrySet()) {
            Map.Entry entry = (Map.Entry) obj;
            ItemStack sourceStack = ItemStack.EMPTY;
            if(entry.getKey() instanceof Item) {
                sourceStack = new ItemStack((Item) entry.getKey());
            } else if(entry.getKey() instanceof Block) {
                sourceStack = new ItemStack((Block) entry.getKey());
            } else if(entry.getKey() instanceof ItemStack) {
                sourceStack = (ItemStack) entry.getKey();
            }
            ItemStack resultStack = (ItemStack) entry.getValue();
            if(resultStack.getItem() instanceof ItemFood) {
                foodItems.put(new ItemID(resultStack), new SmeltingFood(resultStack, sourceStack));
            } else {
                if(isNonFoodRecipe(resultStack)) {
                    foodItems.put(new ItemID(resultStack), new SmeltingFood(resultStack, sourceStack));
                }
            }
        }
    }

    public static boolean isNonFoodRecipe(ItemStack itemStack) {
        for(ItemStack nonFoodStack : nonFoodRecipes) {
            if (ItemUtils.areItemStacksEqualWithWildcard(itemStack, nonFoodStack)) {
                return true;
            }
        }
        return false;
    }

    public static void addFoodRecipe(IRecipe recipe) {
        ItemStack output = recipe.getRecipeOutput();
        if(!output.isEmpty()) {
            recipeList.add(recipe);
            if (recipe instanceof ShapedRecipes) {
                foodItems.put(new ItemID(output), new ShapedCraftingFood((ShapedRecipes) recipe));
            } else if (recipe instanceof ShapelessRecipes) {
                foodItems.put(new ItemID(output), new ShapelessCraftingFood((ShapelessRecipes) recipe));
            } else if (recipe instanceof ShapelessOreRecipe) {
                foodItems.put(new ItemID(output), new ShapelessOreCraftingFood((ShapelessOreRecipe) recipe));
            } else if (recipe instanceof ShapedOreRecipe) {
                foodItems.put(new ItemID(output), new ShapedOreCraftingFood((ShapedOreRecipe) recipe));
            }
        }
    }

    public static Multimap<ItemID, FoodRecipe> getFoodRecipes() {
        return foodItems;
    }

    public static Collection<FoodRecipe> getFoodRecipes(ItemStack outputItem) {
        return foodItems.get(new ItemID(outputItem));
    }

    public static Collection<FoodRecipe> getFoodRecipes(ItemID outputItem) {
        return foodItems.get(outputItem);
    }

    public static void addToolItem(ItemStack toolItem) {
        tools.add(toolItem);
    }

    public static boolean isToolItem(ItemStack itemStack) {
        if(itemStack.isEmpty()) {
            return false;
        }
        for(ItemStack toolItem : tools) {
            if(ItemUtils.areItemStacksEqualWithWildcard(toolItem, itemStack)) {
                return true;
            }
        }
        return false;
    }

    public static void addOvenFuel(ItemStack itemStack, int fuelTime) {
        ovenFuelItems.put(itemStack, fuelTime);
    }

    public static int getOvenFuelTime(ItemStack itemStack) {
        for(Map.Entry<ItemStack, Integer> entry : ovenFuelItems.entrySet()) {
            if(ItemUtils.areItemStacksEqualWithWildcard(entry.getKey(), itemStack)) {
                return entry.getValue();
            }
        }
        return 0;
    }

    public static void addSmeltingItem(ItemStack source, ItemStack result) {
        ovenRecipes.put(source, result);
    }

    public static ItemStack getSmeltingResult(ItemStack itemStack) {
        for(Map.Entry<ItemStack, ItemStack> entry : ovenRecipes.entrySet()) {
            if(ItemUtils.areItemStacksEqualWithWildcard(entry.getKey(), itemStack)) {
                return entry.getValue();
            }
        }
        return ItemStack.EMPTY;
    }

    public static void addToastHandler(ItemStack itemStack, ToastHandler toastHandler) {
        toastHandlers.put(itemStack, toastHandler);
    }

    @Nullable
    public static ToastHandler getToastHandler(ItemStack itemStack) {
        for(Map.Entry<ItemStack, ToastHandler> entry : toastHandlers.entrySet()) {
            if(ItemUtils.areItemStacksEqualWithWildcard(entry.getKey(), itemStack)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public static void addSinkHandler(ItemStack itemStack, SinkHandler sinkHandler) {
        sinkHandlers.put(itemStack, sinkHandler);
    }

    public static ItemStack getSinkOutput(ItemStack itemStack) {
        if(itemStack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        for(Map.Entry<ItemStack, SinkHandler> entry : sinkHandlers.entrySet()) {
            if(ItemUtils.areItemStacksEqualWithWildcard(entry.getKey(), itemStack)) {
                return entry.getValue().getSinkOutput(itemStack);
            }
        }
        return ItemStack.EMPTY;
    }

    public static ItemStack findAnyItemStack(ItemStack checkStack, List<IKitchenItemProvider> inventories, boolean requireBucket) {
        if(checkStack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        for (int i = 0; i < inventories.size(); i++) {
            IKitchenItemProvider itemProvider = inventories.get(i);
            for (int j = 0; j < itemProvider.getSlots(); j++) {
                ItemStack itemStack = itemProvider.getStackInSlot(j);
                if (ItemUtils.areItemStacksEqualWithWildcard(itemStack, checkStack) && !itemProvider.useItemStack(j, 1, true, inventories, requireBucket).isEmpty()) {
                    return itemStack;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    public static ItemStack findAnyItemStack(@Nullable FoodIngredient ingredient, List<IKitchenItemProvider> inventories, boolean requireBucket) {
        if(ingredient == null) {
            return ItemStack.EMPTY;
        }
        for (int i = 0; i < inventories.size(); i++) {
            IKitchenItemProvider itemProvider = inventories.get(i);
            for (int j = 0; j < itemProvider.getSlots(); j++) {
                ItemStack itemStack = itemProvider.getStackInSlot(j);
                if (!itemStack.isEmpty() && ingredient.isValidItem(itemStack) && !itemProvider.useItemStack(j, 1, true, inventories, requireBucket).isEmpty()) {
                    return itemStack;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    public static boolean consumeBucket(List<IKitchenItemProvider> inventories, boolean simulate) {
        ItemStack itemStack = new ItemStack(Items.BUCKET);
        for (int i = 0; i < inventories.size(); i++) {
            IKitchenItemProvider itemProvider = inventories.get(i);
            for (int j = 0; j < itemProvider.getSlots(); j++) {
                ItemStack providedStack = itemProvider.getStackInSlot(j);
                if (!providedStack.isEmpty() && ItemUtils.areItemStacksEqualWithWildcard(itemStack, providedStack) && !itemProvider.useItemStack(j, 1, simulate, inventories, false).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    public static RecipeStatus getRecipeStatus(FoodRecipe recipe, List<IKitchenItemProvider> inventories) {
        boolean requireBucket = doesItemRequireBucketForCrafting(recipe.getOutputItem());
        for (IKitchenItemProvider itemProvider : inventories) {
            itemProvider.resetSimulation();
        }
        List<FoodIngredient> craftMatrix = recipe.getCraftMatrix();
        NonNullList<ItemStack> itemFound = NonNullList.withSize(craftMatrix.size(), ItemStack.EMPTY);
        boolean missingTools = false;
        for(int i = 0; i < craftMatrix.size(); i++) {
            FoodIngredient ingredient = craftMatrix.get(i);
            itemFound.set(i, findAnyItemStack(ingredient, inventories, requireBucket));
            if(itemFound.get(i).isEmpty() && ingredient != null) {
                if(ingredient.isToolItem()) {
                    missingTools = true;
                    continue;
                }
                return RecipeStatus.MISSING_INGREDIENTS;
            }
        }
        return missingTools ? RecipeStatus.MISSING_TOOLS : RecipeStatus.AVAILABLE;
    }

    public static List<IKitchenItemProvider> getItemProviders(@Nullable KitchenMultiBlock multiBlock, InventoryPlayer inventory) {
        return multiBlock != null ? multiBlock.getItemProviders(inventory) : Lists.<IKitchenItemProvider>newArrayList(new KitchenItemProvider(new InvWrapper(inventory)));
    }

    @Nullable
    public static IRecipe findFoodRecipe(InventoryCraftBook craftMatrix, World world) {
        for(IRecipe recipe : recipeList) {
            if(recipe.matches(craftMatrix, world)) {
                return recipe;
            }
        }
        return null;
    }

    public static void addWaterItem(ItemStack waterItem) {
        waterItems.add(waterItem);
    }

    public static void addMilkItem(ItemStack milkItem) {
        milkItems.add(milkItem);
    }

    public static void addSortButton(ISortButton button) {
        customSortButtons.add(button);
    }

    public static NonNullList<ItemStack> getWaterItems() {
        return waterItems;
    }

    public static NonNullList<ItemStack> getMilkItems() {
        return milkItems;
    }

    public static List<ISortButton> getSortButtons() {
        return customSortButtons;
    }

    public static boolean doesItemRequireBucketForCrafting(ItemStack outputItem) {
        ItemStack containerItem = ForgeHooks.getContainerItem(outputItem);
        if(!containerItem.isEmpty() && containerItem.getItem() == Items.BUCKET) {
            return true;
        }
        ResourceLocation registryName = outputItem.getItem().getRegistryName();
        return registryName != null && registryName.getResourcePath().contains("bucket");
    }
}
