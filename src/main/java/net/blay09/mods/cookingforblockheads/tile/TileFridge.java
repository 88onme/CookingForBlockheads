package net.blay09.mods.cookingforblockheads.tile;

import com.google.common.collect.Lists;
import net.blay09.mods.cookingforblockheads.ModSounds;
import net.blay09.mods.cookingforblockheads.api.CookingForBlockheadsAPI;
import net.blay09.mods.cookingforblockheads.api.FridgeAttachment;
import net.blay09.mods.cookingforblockheads.api.capability.CapabilityKitchenItemProvider;
import net.blay09.mods.cookingforblockheads.api.capability.KitchenItemProvider;
import net.blay09.mods.cookingforblockheads.block.ModBlocks;
import net.blay09.mods.cookingforblockheads.compat.Compat;
import net.blay09.mods.cookingforblockheads.network.VanillaPacketHandler;
import net.blay09.mods.cookingforblockheads.registry.FridgeAttachmentRegistry;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;
import vazkii.quark.api.IDropoffManager;

import javax.annotation.Nullable;
import java.util.List;

@Optional.Interface(modid = Compat.QUARK, iface = "vazkii.quark.api.IDropoffManager", striprefs = true)
public class TileFridge extends TileEntity implements ITickable, IDropoffManager {

    private final ItemStackHandler itemHandler = new ItemStackHandler(27) {
        @Override
        protected void onContentsChanged(int slot) {
            isDirty = true;
            markDirty();
        }
    };

    private final KitchenItemProvider itemProvider = new KitchenItemProvider(itemHandler);
    private final DoorAnimator doorAnimator = new DoorAnimator(this, 1, 2);
    private final List<FridgeAttachment> attachments = Lists.newArrayList();

    private EnumDyeColor fridgeColor = EnumDyeColor.WHITE;
    private boolean isDirty;

    public TileFridge() {
        doorAnimator.setOpenRadius(2);
        doorAnimator.setSoundEventOpen(ModSounds.fridgeOpen);
        doorAnimator.setSoundEventClose(ModSounds.fridgeClose);
    }

    public void setFridgeColor(EnumDyeColor fridgeColor) {
        this.fridgeColor = fridgeColor;
        IBlockState state = world.getBlockState(pos);
        world.markAndNotifyBlock(pos, world.getChunkFromBlockCoords(pos), state, state, 3);
        markDirty();
    }

    @Override
    public void update() {
        doorAnimator.update();

        if(isDirty) {
            VanillaPacketHandler.sendTileEntityUpdate(this);
            isDirty = false;
        }
    }

    @Override
    public boolean receiveClientEvent(int id, int type) {
        return doorAnimator.receiveClientEvent(id, type) || super.receiveClientEvent(id, type);
    }

    @Override
    public void readFromNBT(NBTTagCompound tagCompound) {
        super.readFromNBT(tagCompound);
        itemHandler.deserializeNBT(tagCompound.getCompoundTag("ItemHandler"));
        fridgeColor = EnumDyeColor.byDyeDamage(tagCompound.getByte("FridgeColor"));

        NBTTagList attachments = tagCompound.getTagList("Attachments", Constants.NBT.TAG_COMPOUND);
        for(NBTBase entry : attachments) {
            NBTTagCompound compound = (NBTTagCompound) entry;
            ResourceLocation registryName = new ResourceLocation(compound.getString("id"));
            FridgeAttachment attachment = FridgeAttachmentRegistry.createFridgeAttachment(registryName);
            attachment.readFromNBT(compound);
            this.attachments.add(attachment);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tagCompound) {
        super.writeToNBT(tagCompound);
        tagCompound.setTag("ItemHandler", itemHandler.serializeNBT());
        tagCompound.setByte("FridgeColor", (byte) fridgeColor.getDyeDamage());

        NBTTagList attachments = new NBTTagList();
        for(FridgeAttachment attachment : this.attachments) {
            NBTTagCompound compound = attachment.writeToNBT(new NBTTagCompound());
            compound.setString("id", FridgeAttachmentRegistry.getRegistryNameFromClass(attachment.getClass()).toString());
            attachments.appendTag(compound);
        }
        tagCompound.setTag("Attachments", attachments);

        return tagCompound;
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        super.onDataPacket(net, pkt);
        readFromNBT(pkt.getNbtCompound());
        doorAnimator.setForcedOpen(pkt.getNbtCompound().getBoolean("IsForcedOpen"));
        doorAnimator.setNumPlayersUsing(pkt.getNbtCompound().getByte("NumPlayersUsing"));
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound tagCompound = new NBTTagCompound();
        writeToNBT(tagCompound);
        tagCompound.setBoolean("IsForcedOpen", doorAnimator.isForcedOpen());
        tagCompound.setByte("NumPlayersUsing", (byte) doorAnimator.getNumPlayersUsing());
        return tagCompound;
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Nullable
    public TileFridge findNeighbourFridge() {
        if (world.getBlockState(pos.up()).getBlock() == ModBlocks.fridge) {
            return (TileFridge) world.getTileEntity(pos.up());
        } else if (world.getBlockState(pos.down()).getBlock() == ModBlocks.fridge) {
            return (TileFridge) world.getTileEntity(pos.down());
        }
        return null;
    }

    public TileFridge getBaseFridge() {
        if (world.getBlockState(pos.down()).getBlock() == ModBlocks.fridge) {
            TileFridge baseFridge = (TileFridge) world.getTileEntity(pos.down());
            if(baseFridge != null) {
                return baseFridge;
            }
        }
        return this;
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
                || capability == CapabilityKitchenItemProvider.CAPABILITY
                || super.hasCapability(capability, facing);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if(capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return (T) getCombinedItemHandler();
        }
        if(capability == CapabilityKitchenItemProvider.CAPABILITY) {
            return (T) itemProvider;
        }
        return super.getCapability(capability, facing);
    }

    public EnumDyeColor getFridgeColor() {
        return fridgeColor;
    }

    public DoorAnimator getDoorAnimator() {
        return doorAnimator;
    }

    public IItemHandler getCombinedItemHandler() {
        TileFridge baseFridge = getBaseFridge();
        TileFridge neighbourFridge;
        if(baseFridge == this) {
            neighbourFridge = findNeighbourFridge();
        } else {
            neighbourFridge = this;
        }
        if (neighbourFridge != null) {
            return new CombinedInvWrapper(neighbourFridge.itemHandler, baseFridge.itemHandler);
        }
        return itemHandler;
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newSate) {
        return oldState.getBlock() != newSate.getBlock();
    }

    @Override
    public boolean acceptsDropoff(EntityPlayer entityPlayer) {
        return true;
    }

}