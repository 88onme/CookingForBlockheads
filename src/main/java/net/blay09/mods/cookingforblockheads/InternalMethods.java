package net.blay09.mods.cookingforblockheads;

import net.blay09.mods.cookingforblockheads.api.FridgeAttachment;
import net.blay09.mods.cookingforblockheads.api.IInternalMethods;
import net.blay09.mods.cookingforblockheads.api.IKitchenMultiBlock;
import net.blay09.mods.cookingforblockheads.api.ISortButton;
import net.blay09.mods.cookingforblockheads.api.SinkHandler;
import net.blay09.mods.cookingforblockheads.api.ToastHandler;
import net.blay09.mods.cookingforblockheads.api.ToastOutputHandler;
import net.blay09.mods.cookingforblockheads.api.ToasterHandler;
import net.blay09.mods.cookingforblockheads.api.client.FridgeAttachmentRenderer;
import net.blay09.mods.cookingforblockheads.registry.FridgeAttachmentRegistry;
import net.blay09.mods.cookingforblockheads.registry.CookingRegistry;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class InternalMethods implements IInternalMethods {

    @Override
    public void addSinkHandler(ItemStack itemStack, SinkHandler sinkHandler) {
        CookingRegistry.addSinkHandler(itemStack, sinkHandler);
    }

    @Override
    public void addToastHandler(ItemStack itemStack, ToastHandler toastHandler) {
        CookingRegistry.addToastHandler(itemStack, toastHandler);
    }

    @Override
    public void addToasterHandler(ItemStack itemStack, ToasterHandler toastHandler) {
        CookingRegistry.addToastHandler(itemStack, (ToastOutputHandler) toastHandler::getToasterOutput);
    }

    @Override
    public void addWaterItem(ItemStack waterItem) {
        CookingRegistry.addWaterItem(waterItem);
    }

    @Override
    public void addMilkItem(ItemStack milkItem) {
        CookingRegistry.addMilkItem(milkItem);
    }

    @Override
    public void addOvenFuel(ItemStack fuelItem, int fuelTime) {
        CookingRegistry.addOvenFuel(fuelItem, fuelTime);
    }

    @Override
    public void addOvenRecipe(ItemStack sourceItem, ItemStack resultItem) {
        CookingRegistry.addSmeltingItem(sourceItem, resultItem);
    }

    @Override
    public void addToolItem(ItemStack toolItem) {
        CookingRegistry.addToolItem(toolItem);
    }

    @Override
    public void addCowClass(Class<? extends EntityLivingBase> clazz) {
        CowJarHandler.registerCowClass(clazz);
    }

    @Override
    public void registerFridgeAttachment(ResourceLocation registryName, Class<? extends FridgeAttachment> clazz) {
        FridgeAttachmentRegistry.registerFridgeAttachment(registryName, clazz);
    }

    @Override
    public void registerFridgeAttachmentRenderer(Class<? extends FridgeAttachment> clazz, Class<? extends FridgeAttachmentRenderer> renderer) {
        CookingForBlockheads.proxy.registerFridgeAttachmentRenderer(renderer);
    }
    
    @Override
    public IKitchenMultiBlock getKitchenMultiBlock(World world, BlockPos pos) {
        return new KitchenMultiBlock(world, pos);
    }
    
	@Override
	public void addSortButton(ISortButton button) {
		CookingRegistry.addSortButton(button);
	}
}
