package net.blay09.mods.cookingforblockheads.api;

import net.blay09.mods.cookingforblockheads.api.capability.CapabilityKitchenConnector;
import net.blay09.mods.cookingforblockheads.api.capability.CapabilityKitchenItemProvider;
import net.blay09.mods.cookingforblockheads.api.capability.CapabilityKitchenSmeltingProvider;
import net.blay09.mods.cookingforblockheads.api.client.FridgeAttachmentRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CookingForBlockheadsAPI {

    private static IInternalMethods internalMethods;
    private static FoodStatsProvider foodStatsProvider;

    public static void setupAPI(IInternalMethods internalMethods) {
        CookingForBlockheadsAPI.internalMethods = internalMethods;
        CapabilityKitchenConnector.register();
        CapabilityKitchenItemProvider.register();
        CapabilityKitchenSmeltingProvider.register();
    }

    public static void setFoodStatsProvider(FoodStatsProvider foodStatsProvider) {
        CookingForBlockheadsAPI.foodStatsProvider = foodStatsProvider;
    }

    public static FoodStatsProvider getFoodStatsProvider() {
        return foodStatsProvider;
    }

    public static void addSinkHandler(ItemStack itemStack, SinkHandler sinkHandler) {
        internalMethods.addSinkHandler(itemStack, sinkHandler);
    }

    /**
     * @deprecated use addToasterHandler
     */
    @Deprecated
    public static void addToastHandler(ItemStack itemStack, ToastHandler toastHandler) {
        internalMethods.addToastHandler(itemStack, toastHandler);
    }

    public static void addToasterHandler(ItemStack itemStack, ToasterHandler toastHandler) {
        internalMethods.addToasterHandler(itemStack, toastHandler);
    }

    public static void addOvenFuel(ItemStack fuelItem, int fuelTime) {
        internalMethods.addOvenFuel(fuelItem, fuelTime);
    }

    public static void addOvenRecipe(ItemStack sourceItem, ItemStack resultItem) {
        internalMethods.addOvenRecipe(sourceItem, resultItem);
    }

    public static void addToolItem(ItemStack toolItem) {
        internalMethods.addToolItem(toolItem);
    }

    public static void addWaterItem(ItemStack waterItem) {
        internalMethods.addWaterItem(waterItem);
    }

    public static void addMilkItem(ItemStack milkItem) {
        internalMethods.addMilkItem(milkItem);
    }

    public static void addCowClass(Class<? extends EntityLivingBase> clazz) {
        internalMethods.addCowClass(clazz);
    }

    public static IKitchenMultiBlock getKitchenMultiBlock(World world, BlockPos pos) {
        return internalMethods.getKitchenMultiBlock(world, pos);
    }

    public static void addSortButton(ISortButton button) {
        internalMethods.addSortButton(button);
    }

    public static void registerFridgeAttachment(ResourceLocation registryName, Class<? extends FridgeAttachment> clazz) {
        internalMethods.registerFridgeAttachment(registryName, clazz);
    }

    public static void registerFridgeAttachmentRenderer(Class<? extends FridgeAttachment> clazz, Class<? extends FridgeAttachmentRenderer> rendererClass) {
        internalMethods.registerFridgeAttachmentRenderer(clazz, rendererClass);
    }
}
