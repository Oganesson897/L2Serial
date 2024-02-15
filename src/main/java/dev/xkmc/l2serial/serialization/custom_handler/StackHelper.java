package dev.xkmc.l2serial.serialization.custom_handler;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.JsonOps;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

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
		ans.addProperty("item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
		if (stack.getCount() > 1) {
			ans.addProperty("count", stack.getCount());
		}
		if (stack.hasTag()) {
			ans.addProperty("nbt", stack.getTag().toString());
		}
		return ans;
	}

	public static ItemStack deserializeItemStack(JsonElement elem) {
		return Util.getOrThrow(ItemStack.CODEC.decode(JsonOps.INSTANCE, elem), IllegalStateException::new).getFirst();
	}

	public static FluidStack deserializeFluidStack(JsonElement e) {
		JsonObject json = e.getAsJsonObject();
		ResourceLocation id = new ResourceLocation(GsonHelper.getAsString(json, "fluid"));
		if (!BuiltInRegistries.FLUID.containsKey(id))
			throw new JsonSyntaxException("Unknown fluid '" + id + "'");
		Fluid fluid = BuiltInRegistries.FLUID.get(id);
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
		json.addProperty("fluid", BuiltInRegistries.FLUID.getKey(stack.getFluid()).toString());
		json.addProperty("amount", stack.getAmount());
		if (stack.hasTag())
			json.addProperty("nbt", stack.getTag().toString());
		return json;
	}

	public static JsonElement serializeIngredient(Ingredient ing) {
		return Util.getOrThrow(Ingredient.CODEC.encodeStart(JsonOps.INSTANCE, ing), IllegalStateException::new);
	}
}
