package net.blay09.mods.cookingforblockheads;

import net.blay09.mods.cookingforblockheads.api.CookingForBlockheadsAPI;
import net.blay09.mods.cookingforblockheads.block.ModBlocks;
import net.blay09.mods.cookingforblockheads.client.gui.SortButtonHunger;
import net.blay09.mods.cookingforblockheads.client.gui.SortButtonName;
import net.blay09.mods.cookingforblockheads.client.gui.SortButtonSaturation;
import net.blay09.mods.cookingforblockheads.compat.Compat;
import net.blay09.mods.cookingforblockheads.compat.JsonCompatLoader;
import net.blay09.mods.cookingforblockheads.compat.VanillaAddon;
import net.blay09.mods.cookingforblockheads.item.ModItems;
import net.blay09.mods.cookingforblockheads.network.NetworkHandler;
import net.blay09.mods.cookingforblockheads.network.handler.GuiHandler;
import net.blay09.mods.cookingforblockheads.registry.CookingRegistry;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.datafix.FixTypes;
import net.minecraft.util.datafix.IFixableData;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.common.util.CompoundDataFixer;
import net.minecraftforge.common.util.ModFixs;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInterModComms;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

@Mod.EventBusSubscriber(modid = CookingForBlockheads.MOD_ID)
@Mod(modid = CookingForBlockheads.MOD_ID, acceptedMinecraftVersions = "[1.12]", dependencies = "after:mousetweaks[2.8,);after:crafttweaker")
public class CookingForBlockheads {

    public static final String MOD_ID = "cookingforblockheads";
    public static final int MOD_DATA_VERSION = 0;
    public static final Logger logger = LogManager.getLogger(MOD_ID);

    public static final NonNullList<ItemStack> extraCreativeTabItems = NonNullList.create();
    public static final CreativeTabs creativeTab = new CreativeTabs(MOD_ID) {
        @Override
        public ItemStack getTabIconItem() {
            return new ItemStack(ModItems.recipeBook);
        }

        @Override
        public void displayAllRelevantItems(NonNullList<ItemStack> list) {
            super.displayAllRelevantItems(list);

            list.addAll(extraCreativeTabItems);
        }
    };

    @Mod.Instance(MOD_ID)
    public static CookingForBlockheads instance;

    @SidedProxy(clientSide = "net.blay09.mods.cookingforblockheads.client.ClientProxy", serverSide = "net.blay09.mods.cookingforblockheads.CommonProxy")
    public static CommonProxy proxy;

    public static File configDir;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        configDir = event.getModConfigurationDirectory();

        CookingForBlockheadsAPI.setupAPI(new InternalMethods());

        CookingRegistry.addSortButton(new SortButtonName());
        CookingRegistry.addSortButton(new SortButtonHunger());
        CookingRegistry.addSortButton(new SortButtonSaturation());

        MinecraftForge.EVENT_BUS.register(new IMCHandler());
        MinecraftForge.EVENT_BUS.register(new CowJarHandler());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        NetworkHandler.init();
        NetworkRegistry.INSTANCE.registerGuiHandler(CookingForBlockheads.instance, new GuiHandler());

        ModRecipes.load();

        FMLInterModComms.sendFunctionMessage(Compat.THEONEPROBE, "getTheOneProbe", "net.blay09.mods.cookingforblockheads.compat.TheOneProbeAddon");

        proxy.init(event);

        CompoundDataFixer dataFixer = FMLCommonHandler.instance().getDataFixer();
        ModFixs modDataFixer = dataFixer.init(MOD_ID, MOD_DATA_VERSION);
        modDataFixer.registerFix(FixTypes.ITEM_INSTANCE, new IFixableData() {
            @Override
            public int getFixVersion() {
                return MOD_DATA_VERSION;
            }

            @Override
            public NBTTagCompound fixTagCompound(NBTTagCompound compound) {
                String id = compound.getString("id");
                if (id.equals("cookingforblockheads:recipe_book")) {
                    short damage = compound.getShort("Damage");
                    switch (damage) {
                        case 0:
                            compound.setString("id", "cookingforblockheads:no_filter_book");
                            compound.setShort("Damage", (short) 0);
                            break;
                        case 1:
                            compound.setString("id", "cookingforblockheads:recipe_book");
                            compound.setShort("Damage", (short) 0);
                            break;
                        case 2:
                            compound.setString("id", "cookingforblockheads:crafting_book");
                            compound.setShort("Damage", (short) 0);
                            break;
                    }
                }
                return compound;
            }
        });
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        new VanillaAddon();
        event.buildSoftDependProxy(Compat.PAMS_HARVESTCRAFT, "net.blay09.mods.cookingforblockheads.compat.HarvestCraftAddon");
        event.buildSoftDependProxy(Compat.APPLECORE, "net.blay09.mods.cookingforblockheads.compat.AppleCoreAddon");

        if (!JsonCompatLoader.loadCompat()) {
            logger.error("Failed to load Cooking for Blockheads compatibility! Things may not work as expected.");
        }

        CookingRegistry.initFoodRegistry();
    }

    @Mod.EventHandler
    public void imc(FMLInterModComms.IMCEvent event) {
        IMCHandler.handleIMCMessage(event);
    }

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(MOD_ID)) {
            ConfigManager.sync(MOD_ID, Config.Type.INSTANCE);
        }
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        ModBlocks.register(event.getRegistry());
        ModBlocks.registerTileEntities();
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        ModBlocks.registerItemBlocks(event.getRegistry());
        ModItems.register(event.getRegistry());
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        ModSounds.register(event.getRegistry());
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        proxy.registerModels();
        ModBlocks.registerModels();
        ModItems.registerModels();
    }

}
