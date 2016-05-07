package net.blay09.mods.cookingforblockheads.block;

import net.blay09.mods.cookingforblockheads.CookingForBlockheads;
import net.blay09.mods.cookingforblockheads.balyware.ItemUtils;
import net.blay09.mods.cookingforblockheads.network.handler.GuiHandler;
import net.blay09.mods.cookingforblockheads.tile.TileFridge;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.List;

public class BlockFridge extends BlockKitchen {

	public enum FridgeType implements IStringSerializable {
		SMALL,
		LARGE,
		INVISIBLE;

		@Override
		public String getName() {
			return name().toLowerCase();
		}
	}

	public static final PropertyEnum<FridgeType> TYPE = PropertyEnum.create("type", FridgeType.class);
	public static final PropertyBool FLIPPED = PropertyBool.create("flipped");

	public BlockFridge() {
		super(Material.iron);

		setRegistryName(CookingForBlockheads.MOD_ID, "fridge");
		setUnlocalizedName(getRegistryName());
		setStepSound(soundTypeMetal);
		setHardness(5f);
		setResistance(10f);
	}

	@Override
	public MapColor getMapColor(IBlockState state) {
		return super.getMapColor(state);
	}

	@Override
	protected BlockState createBlockState() {
		return new BlockState(this, FACING, TYPE, FLIPPED);
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		EnumFacing facing;
		switch (meta & 7) {
			case 0:
				facing = EnumFacing.EAST;
				break;
			case 1:
				facing = EnumFacing.WEST;
				break;
			case 2:
				facing = EnumFacing.SOUTH;
				break;
			case 3:
			default:
				facing = EnumFacing.NORTH;
				break;
		}
		return getDefaultState().withProperty(FACING, facing).withProperty(FLIPPED, (meta & 8) != 0);
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		int meta;
		switch (state.getValue(FACING)) {
			case EAST:
				meta = 0;
				break;
			case WEST:
				meta = 1;
				break;
			case SOUTH:
				meta = 2;
				break;
			case NORTH:
			default:
				meta = 3;
				break;
		}
		if (state.getValue(FLIPPED)) {
			meta |= 8;
		}
		return meta;
	}

	@Override
	public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
		if (world.getBlockState(pos.up()).getBlock() == this) {
			state = state.withProperty(TYPE, FridgeType.LARGE);
		} else if (world.getBlockState(pos.down()).getBlock() == this) {
			state = state.withProperty(TYPE, FridgeType.INVISIBLE);
		} else {
			state = state.withProperty(TYPE, FridgeType.SMALL);
		}
		return state;
	}

	@Override
	public boolean canRenderInLayer(EnumWorldBlockLayer layer) {
		return (layer == EnumWorldBlockLayer.CUTOUT || layer == EnumWorldBlockLayer.TRANSLUCENT);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int metadata) {
		return new TileFridge();
	}

	@Override
	public boolean recolorBlock(World world, BlockPos pos, EnumFacing side, EnumDyeColor color) {
		TileFridge fridge = (TileFridge) world.getTileEntity(pos);
		fridge.setFridgeColor(color);
		if (fridge.findNeighbourFridge() != null) {
			fridge.findNeighbourFridge().setFridgeColor(color);
		}
		return true;
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumFacing side, float hitX, float hitY, float hitZ) {
		ItemStack heldItem = player.getHeldItem();
		if (heldItem != null && heldItem.getItem() == Items.dye) {
			if (recolorBlock(world, pos, side, EnumDyeColor.byDyeDamage(heldItem.getItemDamage()))) {
				heldItem.stackSize--;
			}
			return true;
		}
		if(side == state.getValue(FACING)) {
			TileFridge tileFridge = (TileFridge) world.getTileEntity(pos);
			if(player.isSneaking()) {
				tileFridge.getBaseFridge().getDoorAnimator().toggleForcedOpen();
				return true;
			} else if(heldItem != null && tileFridge.getBaseFridge().getDoorAnimator().isForcedOpen()) {
				heldItem = ItemHandlerHelper.insertItemStacked(tileFridge.getCombinedItemHandler(), heldItem, false);
				player.inventory.setInventorySlotContents(player.inventory.currentItem, heldItem);
				return true;
			}
		}
		if (!world.isRemote) {
			if (heldItem != null && Block.getBlockFromItem(heldItem.getItem()) == ModBlocks.fridge) {
				return false;
			}
			player.openGui(CookingForBlockheads.instance, GuiHandler.FRIDGE, world, pos.getX(), pos.getY(), pos.getZ());
		}
		return true;
	}

	@Override
	public boolean canPlaceBlockAt(World world, BlockPos pos) {
		boolean below = world.getBlockState(pos.down()).getBlock() == ModBlocks.fridge;
		boolean above = world.getBlockState(pos.up()).getBlock() == ModBlocks.fridge;
		return !(below && above)
				&& !(below && world.getBlockState(pos.down(2)).getBlock() == ModBlocks.fridge)
				&& !(above && world.getBlockState(pos.up(2)).getBlock() == ModBlocks.fridge)
				&& super.canPlaceBlockAt(world, pos);
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
		double blockRotation = (double) (placer.rotationYaw * 4f / 360f) + 0.5;
		boolean flipped = Math.abs(blockRotation - (int) blockRotation) < 0.5;
		super.onBlockPlacedBy(world, pos, state.withProperty(FLIPPED, !flipped), placer, stack);
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state) {
		TileFridge tileEntity = (TileFridge) world.getTileEntity(pos);
		if (tileEntity != null) {
			ItemUtils.dropContent(world, pos, tileEntity.getItemHandler());
		}
		super.breakBlock(world, pos, state);
	}

	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
		super.addInformation(stack, player, tooltip, advanced);
		for (String s : I18n.format("tooltip." + getRegistryName() + ".description").split("\\\\n")) {
			tooltip.add(EnumChatFormatting.GRAY + s);
		}
		tooltip.add(EnumChatFormatting.AQUA + I18n.format("tooltip." + CookingForBlockheads.MOD_ID + ":dyeable"));
	}

	@Override
	public int colorMultiplier(IBlockAccess world, BlockPos pos, int renderPass) {
		TileEntity tileEntity = world.getTileEntity(pos);
		if (tileEntity instanceof TileFridge) {
			return ((TileFridge) tileEntity).getBaseFridge().getFridgeColor().getMapColor().colorValue;
		}
		return 0xFFFFFFFF;
	}

}
