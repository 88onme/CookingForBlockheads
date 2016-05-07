package net.blay09.mods.cookingforblockheads.item;

import net.blay09.mods.cookingforblockheads.CookingForBlockheads;
import net.blay09.mods.cookingforblockheads.network.handler.GuiHandler;
import net.minecraft.client.renderer.ItemModelMesher;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

public class ItemRecipeBook extends Item {

	public ItemRecipeBook() {
		setMaxStackSize(1);
		setRegistryName(CookingForBlockheads.MOD_ID, "recipeBook");
		setCreativeTab(CookingForBlockheads.creativeTab);
		setHasSubtypes(true);
	}

	@Override
	public String getUnlocalizedName(ItemStack itemstack) {
		return "item." + CookingForBlockheads.MOD_ID + ":recipeBookTier" + itemstack.getItemDamage();
	}

	@Override
	public void getSubItems(Item item, CreativeTabs tab, List<ItemStack> subItems) {
		subItems.add(new ItemStack(item, 1, 0));
		subItems.add(new ItemStack(item, 1, 1));
		subItems.add(new ItemStack(item, 1, 2));
	}

	@Override
	public ItemStack onItemRightClick(ItemStack itemStack, World world, EntityPlayer player) {
		player.openGui(CookingForBlockheads.instance, GuiHandler.ITEM_RECIPE_BOOK, world, 0, 0, 0);
		return itemStack;
	}

	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer player, List<String> list, boolean isShiftDown) {
		super.addInformation(itemStack, player, list, isShiftDown);

		list.add(EnumChatFormatting.YELLOW + I18n.format("tooltip." + CookingForBlockheads.MOD_ID + ":recipeBookTier" + itemStack.getItemDamage()));
		for (String s : I18n.format("tooltip." + CookingForBlockheads.MOD_ID + ":recipeBookTier" + itemStack.getItemDamage() + ".description").split("\\\\n")) {
			list.add(EnumChatFormatting.GRAY + s);
		}
	}

	@SideOnly(Side.CLIENT)
	public void registerModels(ItemModelMesher mesher) {
		ModelBakery.registerItemVariants(this, new ResourceLocation(CookingForBlockheads.MOD_ID, "recipeBook"), new ResourceLocation(CookingForBlockheads.MOD_ID, "recipeBookTier1"), new ResourceLocation(CookingForBlockheads.MOD_ID, "recipeBookTier2"));

		mesher.register(this, 0, new ModelResourceLocation(CookingForBlockheads.MOD_ID + ":recipeBook", "inventory"));
		mesher.register(this, 1, new ModelResourceLocation(CookingForBlockheads.MOD_ID + ":recipeBookTier1", "inventory"));
		mesher.register(this, 2, new ModelResourceLocation(CookingForBlockheads.MOD_ID + ":recipeBookTier2", "inventory"));
	}
}
