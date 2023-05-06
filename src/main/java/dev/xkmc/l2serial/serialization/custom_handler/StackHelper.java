package dev.xkmc.l2serial.serialization.custom_handler;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

public class StackHelper {

	private static final Gson GSON = new Gson();

	/**
	 * for mod recipes that use automatic serialization
	 */
	public static JsonElement serializeItemStack(ItemStack stack) {
		return serializeForgeItemStack(stack);
	}

	/**
	 * for vanilla recipes
	 */
	public static JsonElement serializeForgeItemStack(ItemStack stack) {
		JsonObject ans = new JsonObject();
		ans.addProperty("item", ForgeRegistries.ITEMS.getKey(stack.getItem()).toString());
		if (stack.getCount() > 1) {
			ans.addProperty("count", stack.getCount());
		}
		if (stack.hasTag()) {
			ans.addProperty("nbt", stack.getTag().toString());
		}
		return ans;
	}

	public static ItemStack deserializeItemStack(JsonElement elem) {
		JsonObject obj = elem.getAsJsonObject();
		return CraftingHelper.getItemStack(obj, true, false);
	}

	public static FluidStack deserializeFluidStack(JsonElement e) {
		JsonObject json = e.getAsJsonObject();
		ResourceLocation id = new ResourceLocation(GsonHelper.getAsString(json, "fluid"));
		Fluid fluid = ForgeRegistries.FLUIDS.getValue(id);
		if (fluid == null)
			throw new JsonSyntaxException("Unknown fluid '" + id + "'");
		int amount = GsonHelper.getAsInt(json, "amount");
		FluidStack stack = new FluidStack(fluid, amount);

		if (!json.has("nbt"))
			return stack;

		try {
			JsonElement element = json.get("nbt");
			stack.setTag(TagParser.parseTag(
					element.isJsonObject() ? GSON.toJson(element) : GsonHelper.convertToString(element, "nbt")));

		} catch (CommandSyntaxException err) {
			err.printStackTrace();
		}

		return stack;
	}

	public static JsonElement serializeFluidStack(FluidStack stack) {
		JsonObject json = new JsonObject();
		json.addProperty("fluid", ForgeRegistries.FLUIDS.getKey(stack.getFluid()).toString());
		json.addProperty("amount", stack.getAmount());
		if (stack.hasTag())
			json.addProperty("nbt", stack.getTag().toString());
		return json;
	}

}
