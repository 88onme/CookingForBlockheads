package net.blay09.mods.cookingforblockheads.block;

import net.blay09.mods.cookingforblockheads.tile.TileToolRack;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class BlockToolRack extends BlockKitchen implements IRegisterableBlock {

    private static final AxisAlignedBB[] BOUNDING_BOXES = new AxisAlignedBB[]{
            new AxisAlignedBB(0, 0.25, 1 - 0.125, 1, 1, 1),
            new AxisAlignedBB(0, 0.25, 0, 1, 1, 0.125),
            new AxisAlignedBB(1 - 0.125, 0.25, 0, 1, 1, 1),
            new AxisAlignedBB(0, 0.25, 0, 0.125, 1, 1),
    };

    public BlockToolRack() {
        super(Material.WOOD);

        setSoundType(SoundType.WOOD);
        setHardness(2.5f);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new TileToolRack();
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        EnumFacing facing = state.getValue(FACING);
        return BOUNDING_BOXES[facing.getIndex() - 2];
    }

    @Nullable
    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState blockState, IBlockAccess world, BlockPos pos) {
        return NULL_AABB;
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
        if (facing == EnumFacing.UP || facing == EnumFacing.DOWN) {
            facing = EnumFacing.NORTH;
        }

        return getDefaultState().withProperty(FACING, facing);
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (hand != EnumHand.MAIN_HAND) {
            return true;
        }

        ItemStack heldItem = player.getHeldItem(hand);
        if (!heldItem.isEmpty() && heldItem.getItem() instanceof ItemBlock) {
            return true;
        }

        if (hitY > 0.25f) {
            EnumFacing stateFacing = state.getValue(FACING);
            float hit = hitX;
            switch (stateFacing) {
                case NORTH:
                    hit = hitX;
                    break;
                case SOUTH:
                    hit = 1f - hitX;
                    break;
                case WEST:
                    hit = 1f - hitZ;
                    break;
                case EAST:
                    hit = hitZ;
                    break;
            }

            int hitSlot = hit > 0.5f ? 0 : 1;
            TileToolRack tileToolRack = (TileToolRack) world.getTileEntity(pos);
            if (tileToolRack != null) {
                if (!heldItem.isEmpty()) {
                    ItemStack oldToolItem = tileToolRack.getItemHandler().getStackInSlot(hitSlot);
                    ItemStack toolItem = heldItem.splitStack(1);
                    if (!oldToolItem.isEmpty()) {
                        if (!player.inventory.addItemStackToInventory(oldToolItem)) {
                            player.dropItem(oldToolItem, false);
                        }
                        tileToolRack.getItemHandler().setStackInSlot(hitSlot, toolItem);
                    } else {
                        tileToolRack.getItemHandler().setStackInSlot(hitSlot, toolItem);
                    }
                } else {
                    ItemStack itemStack = tileToolRack.getItemHandler().getStackInSlot(hitSlot);
                    if (!itemStack.isEmpty()) {
                        tileToolRack.getItemHandler().setStackInSlot(hitSlot, ItemStack.EMPTY);
                        player.setHeldItem(hand, itemStack);
                    }
                }
                return true;
            }
        }

        return true;
    }

    @Override
    public String getIdentifier() {
        return "tool_rack";
    }

    @Nullable
    @Override
    public Class<? extends TileEntity> getTileEntityClass() {
        return TileToolRack.class;
    }

}