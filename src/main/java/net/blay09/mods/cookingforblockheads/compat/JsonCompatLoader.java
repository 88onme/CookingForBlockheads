package net.blay09.mods.cookingforblockheads.compat;

import com.google.common.collect.Lists;
import com.google.gson.*;
import net.blay09.mods.cookingforblockheads.CookingForBlockheads;
import net.blay09.mods.cookingforblockheads.api.CookingForBlockheadsAPI;
import net.blay09.mods.cookingforblockheads.api.event.FoodRegistryInitEvent;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.commons.io.FilenameUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber
public class JsonCompatLoader {

    private static final Gson gson = new Gson();
    private static final NonNullList<ItemStack> nonFoodRecipes = NonNullList.create();

    public static boolean loadCompat() {
        nonFoodRecipes.clear();
        ModContainer mod = Loader.instance().getIndexedModList().get(CookingForBlockheads.MOD_ID);
        return findConfigFiles() && CraftingHelper.findFiles(mod, "assets/cookingforblockheads/compat", (root) -> true, (root, file) -> {
            String relative = root.relativize(file).toString();
            if (!"json".equals(FilenameUtils.getExtension(file.toString())) || relative.startsWith("_")) {
                return true;
            }

            String fileName = FilenameUtils.removeExtension(relative).replaceAll("\\\\", "/");
            try (BufferedReader reader = Files.newBufferedReader(file)) {
                parse(reader);
            } catch (JsonParseException e) {
                CookingForBlockheads.logger.error("Parsing error loading compat {}", fileName, e);
                return false;
            } catch (IOException e) {
                CookingForBlockheads.logger.error("Couldn't read compat {}", fileName, e);
                return false;
            }
            return true;
        }, true, true);
    }

    private static boolean findConfigFiles() {
        File compatDir = new File(CookingForBlockheads.configDir, "CookingForBlockheadsCompat");
        if (!compatDir.exists()) {
            if (!compatDir.mkdirs()) {
                CookingForBlockheads.logger.info("If you wish to setup additional CookingForBlockheads compatibility, create a folder called 'CookingForBlockheadsCompat' in your config directory and place JSON files inside.");
            }
            return true;
        }

        Path path = compatDir.toPath();
        try {
            Files.walk(path).filter(it -> it.toString().endsWith(".json")).forEach(it -> {
                try (Reader reader = Files.newBufferedReader(it)) {
                    parse(reader);
                } catch (IOException e) {
                    CookingForBlockheads.logger.error("Couldn't read compat {}", it, e);
                }
            });
        } catch (IOException e) {
            CookingForBlockheads.logger.error("Couldn't walk compat dir", e);
            return false;
        }

        return true;
    }

    @SubscribeEvent
    public static void onCookingRegistry(FoodRegistryInitEvent event) {
        for (ItemStack itemStack : nonFoodRecipes) {
            event.registerNonFoodRecipe(itemStack);
        }
    }

    private static final JsonObject EMPTY_OBJECT = new JsonObject();
    private static final JsonArray EMPTY_ARRAY = new JsonArray();

    private static final ItemStack[] SINGLE_BUFFER = new ItemStack[1];

    public static void parse(String json) {
        parse(gson.fromJson(json, JsonObject.class));
    }

    public static void parse(Reader reader) {
        JsonObject json = JsonUtils.fromJson(gson, reader, JsonObject.class);
        if (json != null) {
            parse(json);
        }
    }

    private static void parse(JsonObject root) {
        String modId = JsonUtils.getString(root, "modid");
        if (!modId.equals("minecraft") && !Loader.isModLoaded(modId)) {
            return;
        }
        JsonObject foods = JsonUtils.getJsonObject(root, "foods", EMPTY_OBJECT);
        for (Map.Entry<String, JsonElement> entry : foods.entrySet()) {
            String category = entry.getKey();
            JsonArray array = entry.getValue().getAsJsonArray();
            for (JsonElement element : array) {
                ItemStack[] results = parseItemStacks(modId, element);
                for (ItemStack result : results) {
                    if (!result.isEmpty()) {
                        nonFoodRecipes.add(result);
                    }
                }
            }
        }
        JsonArray tools = JsonUtils.getJsonArray(root, "tools", EMPTY_ARRAY);
        for (JsonElement element : tools) {
            ItemStack[] results = parseItemStacks(modId, element);
            for (ItemStack result : results) {
                if (!result.isEmpty()) {
                    CookingForBlockheadsAPI.addToolItem(result);
                }
            }
        }
        JsonArray water = JsonUtils.getJsonArray(root, "water", EMPTY_ARRAY);
        for (JsonElement element : water) {
            ItemStack[] results = parseItemStacks(modId, element);
            for (ItemStack result : results) {
                CookingForBlockheadsAPI.addWaterItem(result);
            }
        }
        JsonArray milk = JsonUtils.getJsonArray(root, "milk", EMPTY_ARRAY);
        for (JsonElement element : milk) {
            ItemStack[] results = parseItemStacks(modId, element);
            for (ItemStack result : results) {
                CookingForBlockheadsAPI.addMilkItem(result);
            }
        }
        JsonArray ovenFuel = JsonUtils.getJsonArray(root, "oven_fuel", EMPTY_ARRAY);
        for (JsonElement element : ovenFuel) {
            if (!element.isJsonObject()) {
                throw new JsonSyntaxException("Expected array elements to be an object, got " + element);
            }
            JsonObject object = element.getAsJsonObject();
            if (!object.has("item")) {
                throw new JsonSyntaxException("Missing item, expected to find a string, array or object");
            }
            JsonElement item = object.get("item");
            ItemStack[] results = parseItemStacks(modId, item);
            int fuelTime = JsonUtils.getInt(object, "value");
            for (ItemStack result : results) {
                CookingForBlockheadsAPI.addOvenFuel(result, fuelTime);
            }
        }
        JsonObject ovenRecipes = JsonUtils.getJsonObject(root, "oven_recipes", EMPTY_OBJECT);
        for (Map.Entry<String, JsonElement> entry : ovenRecipes.entrySet()) {
            ItemStack input = parseItemStackSimple(modId, entry.getKey());
            ItemStack output = parseItemStack(modId, entry.getValue());
            if (!input.isEmpty() && !output.isEmpty()) {
                CookingForBlockheadsAPI.addOvenRecipe(input, output);
            }
        }
        JsonObject toaster = JsonUtils.getJsonObject(root, "toaster", EMPTY_OBJECT);
        for (Map.Entry<String, JsonElement> entry : toaster.entrySet()) {
            ItemStack input = parseItemStackSimple(modId, entry.getKey());
            ItemStack output = parseItemStack(modId, entry.getValue());
            if (!input.isEmpty() && !output.isEmpty()) {
                CookingForBlockheadsAPI.addToasterHandler(input, itemStack -> output);
            }
        }
        JsonObject tilesObject = JsonUtils.getJsonObject(root, "tiles", EMPTY_OBJECT);
        JsonArray tiles = JsonUtils.getJsonArray(tilesObject, "kitchenItemProviders", EMPTY_ARRAY);
        for (JsonElement element : tiles) {
            if (!element.isJsonPrimitive()) {
                throw new JsonSyntaxException("Expected array elements to be a primitive, got " + element);
            }
            CompatCapabilityLoader.addKitchenItemProviderClass(element.getAsString());
        }
        tiles = JsonUtils.getJsonArray(tilesObject, "kitchenConnectors", EMPTY_ARRAY);
        for (JsonElement element : tiles) {
            if (!element.isJsonPrimitive()) {
                throw new JsonSyntaxException("Expected array elements to be a primitive, got " + element);
            }
            CompatCapabilityLoader.addKitchenConnectorClass(element.getAsString());
        }
    }

    private static ItemStack[] parseItemStacks(String modId, JsonElement element) {
        if (element.isJsonPrimitive()) {
            SINGLE_BUFFER[0] = parseItemStackSimple(modId, element.getAsString());
            return SINGLE_BUFFER;
        }
        List<ItemStack> itemStackList = Lists.newArrayList();
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement child : array) {
                Collections.addAll(itemStackList, parseItemStacks(modId, child));
            }
        } else if (element.isJsonObject()) {
            itemStackList.add(parseItemStack(modId, element.getAsJsonObject()));
        }
        return itemStackList.toArray(new ItemStack[0]);
    }

    private static ItemStack parseItemStack(String modId, JsonElement element) {
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            String name = JsonUtils.getString(object, "item");
            Item item = Item.REGISTRY.getObject(new ResourceLocation(modId, name));
            if (item == null || item == Items.AIR) {
                return ItemStack.EMPTY;
            }
            int count = JsonUtils.getInt(object, "count", 1);
            int data = JsonUtils.getInt(object, "data", 0);
            return new ItemStack(item, count, data);
        } else if (element.isJsonPrimitive()) {
            return parseItemStackSimple(modId, element.getAsString());
        } else {
            throw new JsonSyntaxException("Excepted object or string, got " + element);
        }
    }

    private static ItemStack parseItemStackSimple(String modId, String name) {
        int colon = name.indexOf(':');
        if (colon != -1) {
            modId = name.substring(0, colon);
            name = name.substring(colon + 1);
        }
        Item item = Item.REGISTRY.getObject(new ResourceLocation(modId, name));
        if (item == null || item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item);
    }

}
