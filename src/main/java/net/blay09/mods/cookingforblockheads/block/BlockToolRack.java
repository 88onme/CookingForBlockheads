package net.blay09.mods.cookingforblockheads.block;

import net.blay09.mods.cookingforblockheads.CookingForBlockheads;
import net.blay09.mods.cookingforblockheads.balyware.ItemUtils;
import net.blay09.mods.cookingforblockheads.tile.TileToolRack;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.List;

public class BlockToolRack extends BlockKitchen {

	private static final AxisAlignedBB[] BOUNDING_BOXES = new AxisAlignedBB[] {
			new AxisAlignedBB(0, 0.25, 1 - 0.125, 1, 1, 1),
			new AxisAlignedBB(0, 0.25, 0, 1, 1, 0.125),
			new AxisAlignedBB(1 - 0.125, 0.25, 0, 1, 1, 1),
			new AxisAlignedBB(0, 0.25, 0, 0.125, 1, 1),
	};

    public BlockToolRack() {
        super(Material.wood);

		setRegistryName(CookingForBlockheads.MOD_ID, "toolRack");
		setUnlocalizedName(getRegistryName());
        setStepSound(soundTypeWood);
        setHardness(2.5f);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new TileToolRack();
    }

	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess world, BlockPos pos) {
		super.setBlockBoundsBasedOnState(world, pos);
		EnumFacing facing = world.getBlockState(pos).getValue(FACING);
		AxisAlignedBB aabb = BOUNDING_BOXES[facing.getIndex() - 2];
		setBlockBounds((float) aabb.minX, (float) aabb.minY, (float) aabb.minZ, (float) aabb.maxX, (float) aabb.maxY, (float) aabb.maxZ);
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBox(World worldIn, BlockPos pos, IBlockState state) {
		return null;
	}

	@Override
	public boolean canPlaceBlockAt(World world, BlockPos pos) {
		return world.isSideSolid(pos.offset(EnumFacing.WEST), EnumFacing.EAST) ||
				world.isSideSolid(pos.offset(EnumFacing.EAST), EnumFacing.WEST) ||
				world.isSideSolid(pos.offset(EnumFacing.NORTH), EnumFacing.SOUTH) ||
				world.isSideSolid(pos.offset(EnumFacing.SOUTH), EnumFacing.NORTH);
	}

	@Override
	public IBlockState onBlockPlaced(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
		if(facing == EnumFacing.UP || facing == EnumFacing.DOWN) {
			facing = EnumFacing.NORTH;
		}
		return getDefaultState().withProperty(FACING, facing);
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumFacing side, float hitX, float hitY, float hitZ) {
		ItemStack heldItem = player.getHeldItem();
        if(heldItem != null && heldItem.getItem() instanceof ItemBlock) {
            return true;
        }
        if(hitY > 0.25f) {
			EnumFacing facing = state.getValue(FACING);
            float hit = hitX;
            switch(facing) {
                case NORTH: hit = hitX; break;
                case SOUTH: hit = 1f - hitX; break;
                case WEST: hit = 1f - hitZ; break;
                case EAST: hit = hitZ; break;
            }
            int hitSlot = hit > 0.5f ? 0 : 1;
            TileToolRack tileToolRack = (TileToolRack) world.getTileEntity(pos);
            if (tileToolRack != null) {
                if (heldItem != null) {

                    ItemStack oldToolItem = tileToolRack.getItemHandler().getStackInSlot(hitSlot);
                    ItemStack toolItem = heldItem.splitStack(1);
                    if (oldToolItem != null) {
                        if (!player.inventory.addItemStackToInventory(oldToolItem)) {
                            player.dropPlayerItemWithRandomChoice(oldToolItem, false);
                        }
                        tileToolRack.getItemHandler().setStackInSlot(hitSlot, toolItem);
                    } else {
                        tileToolRack.getItemHandler().setStackInSlot(hitSlot, toolItem);
                    }
                } else {
                    ItemStack itemStack = tileToolRack.getItemHandler().getStackInSlot(hitSlot);
                    if (itemStack != null) {
                        tileToolRack.getItemHandler().setStackInSlot(hitSlot, null);
                        player.inventory.setInventorySlotContents(player.inventory.currentItem, itemStack);
                    }
                }
                return true;
            }
        }
        return true;
    }

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state) {
		TileToolRack tileEntity = (TileToolRack) world.getTileEntity(pos);
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
    }

}