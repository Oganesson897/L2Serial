package dev.xkmc.l2serial.serialization.custom_handler;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.xkmc.l2serial.serialization.generic_types.*;
import dev.xkmc.l2serial.serialization.nulldefer.NullDefer;
import dev.xkmc.l2serial.serialization.nulldefer.PrimitiveNullDefer;
import dev.xkmc.l2serial.serialization.nulldefer.SimpleNullDefer;
import dev.xkmc.l2serial.util.Wrappers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.*;
import java.util.function.Supplier;

public class Handlers {

	public static final Map<Class<?>, JsonClassHandler<?>> JSON_MAP = new HashMap<>();
	public static final Map<Class<?>, NBTClassHandler<?, ?>> NBT_MAP = new HashMap<>();
	public static final Map<Class<?>, PacketClassHandler<?>> PACKET_MAP = new HashMap<>();

	public static final List<GenericCodec> LIST = new ArrayList<>();
	public static final Map<Class<?>, NullDefer<?>> MAP = new HashMap<>();

	// register handlers
	static {
		// primitives

		new ClassHandler<>(long.class, JsonPrimitive::new, JsonElement::getAsLong, FriendlyByteBuf::readLong, FriendlyByteBuf::writeLong, LongTag::getAsLong, LongTag::valueOf, Long.class);
		new ClassHandler<>(int.class, JsonPrimitive::new, JsonElement::getAsInt, FriendlyByteBuf::readInt, FriendlyByteBuf::writeInt, IntTag::getAsInt, IntTag::valueOf, Integer.class);
		new ClassHandler<ShortTag, Short>(short.class, JsonPrimitive::new, JsonElement::getAsShort, FriendlyByteBuf::readShort, FriendlyByteBuf::writeShort, ShortTag::getAsShort, ShortTag::valueOf, Short.class);
		new ClassHandler<ByteTag, Byte>(byte.class, JsonPrimitive::new, JsonElement::getAsByte, FriendlyByteBuf::readByte, FriendlyByteBuf::writeByte, ByteTag::getAsByte, ByteTag::valueOf, Byte.class);
		new ClassHandler<ByteTag, Boolean>(boolean.class, JsonPrimitive::new, JsonElement::getAsBoolean, FriendlyByteBuf::readBoolean, FriendlyByteBuf::writeBoolean, tag -> tag.getAsByte() != 0, ByteTag::valueOf, Boolean.class);
		new ClassHandler<ByteTag, Character>(char.class, JsonPrimitive::new, JsonElement::getAsCharacter, FriendlyByteBuf::readChar, FriendlyByteBuf::writeChar, t -> (char) t.getAsByte(), c -> ByteTag.valueOf((byte) (char) c), Character.class);
		new ClassHandler<>(double.class, JsonPrimitive::new, JsonElement::getAsDouble, FriendlyByteBuf::readDouble, FriendlyByteBuf::writeDouble, DoubleTag::getAsDouble, DoubleTag::valueOf, Double.class);
		new ClassHandler<>(float.class, JsonPrimitive::new, JsonElement::getAsFloat, FriendlyByteBuf::readFloat, FriendlyByteBuf::writeFloat, FloatTag::getAsFloat, FloatTag::valueOf, Float.class);

		new ClassHandler<>(String.class, JsonPrimitive::new, JsonElement::getAsString, FriendlyByteBuf::readUtf, FriendlyByteBuf::writeUtf, Tag::getAsString, StringTag::valueOf);

		// minecraft
		new ClassHandler<>(ItemStack.class, StackHelper::serializeItemStack, StackHelper::deserializeItemStack, FriendlyByteBuf::readItem, FriendlyByteBuf::writeItem, ItemStack::of, is -> is.save(new CompoundTag()));
		new ClassHandler<>(FluidStack.class, StackHelper::serializeFluidStack, StackHelper::deserializeFluidStack, FluidStack::readFromPacket, FriendlyByteBuf::writeFluidStack, FluidStack::loadFluidStackFromNBT, f -> f.writeToNBT(new CompoundTag()));

		new StringClassHandler<>(ResourceLocation.class, ResourceLocation::new, ResourceLocation::toString);
		new StringClassHandler<>(UUID.class, UUID::fromString, UUID::toString);

		// partials

		// no NBT
		new ClassHandler<>(Ingredient.class, StackHelper::serializeIngredient,
				e -> e.isJsonArray() && e.getAsJsonArray().isEmpty() ? Ingredient.EMPTY : Ingredient.fromJson(e, false),
				Ingredient::fromNetwork, (p, o) -> o.toNetwork(p), null, null);

		// no JSON
		new ClassHandler<CompoundTag, CompoundTag>(CompoundTag.class, null, null, FriendlyByteBuf::readNbt, FriendlyByteBuf::writeNbt, e -> e, e -> e);
		new ClassHandler<ListTag, ListTag>(ListTag.class, null, null, buf -> (ListTag) buf.readNbt().get("warp"), (buf, tag) -> {
			CompoundTag comp = new CompoundTag();
			comp.put("warp", tag);
			buf.writeNbt(comp);
		}, e -> e, e -> e);

		new ClassHandler<>(long[].class, null, null, null, null, LongArrayTag::getAsLongArray, LongArrayTag::new);
		new ClassHandler<>(int[].class, null, null, null, null, IntArrayTag::getAsIntArray, IntArrayTag::new);
		new ClassHandler<>(byte[].class, null, null, null, null, ByteArrayTag::getAsByteArray, ByteArrayTag::new);
		new AutoPacketNBTHandler<>(BlockPos.class,
				tag -> new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z")),
				obj -> {
					CompoundTag tag = new CompoundTag();
					tag.putInt("x", obj.getX());
					tag.putInt("y", obj.getY());
					tag.putInt("z", obj.getZ());
					return tag;
				});
		new AutoPacketNBTHandler<>(Vec3.class,
				tag -> new Vec3(tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z")),
				obj -> {
					CompoundTag tag = new CompoundTag();
					tag.putDouble("x", obj.x());
					tag.putDouble("y", obj.y());
					tag.putDouble("z", obj.z());
					return tag;
				});
		new AutoPacketNBTHandler<>(MobEffectInstance.class,
				MobEffectInstance::load, e -> e.save(new CompoundTag()));
	}

	// register generic codec
	static {
		new RecordCodec();
		new EnumCodec();
		new ArrayCodec();
		new AliasCodec();
		new ListCodec();
		new SetCodec();
		new MapCodec();
	}

	// register null defer
	static {
		new SimpleNullDefer<>(ItemStack.class, ItemStack.EMPTY);
		new SimpleNullDefer<>(Ingredient.class, Ingredient.EMPTY);
		new PrimitiveNullDefer<>(Integer.class, 0);
		new PrimitiveNullDefer<>(int.class, 0);
		new PrimitiveNullDefer<>(Long.class, 0L);
		new PrimitiveNullDefer<>(long.class, 0L);
		new PrimitiveNullDefer<>(Short.class, (short) 0);
		new PrimitiveNullDefer<>(short.class, (short) 0);
		new PrimitiveNullDefer<>(Byte.class, (byte) 0);
		new PrimitiveNullDefer<>(byte.class, (byte) 0);
		new PrimitiveNullDefer<>(Character.class, (char) 0);
		new PrimitiveNullDefer<>(char.class, (char) 0);
		new PrimitiveNullDefer<>(Double.class, 0d);
		new PrimitiveNullDefer<>(double.class, 0d);
		new PrimitiveNullDefer<>(Float.class, 0f);
		new PrimitiveNullDefer<>(float.class, 0f);
		new PrimitiveNullDefer<>(Boolean.class, false);
		new PrimitiveNullDefer<>(boolean.class, false);
	}

	private static final Set<Registry<?>> VANILLA_SYNC_REGISTRIES;

	static {
		VANILLA_SYNC_REGISTRIES = Set.of(
				BuiltInRegistries.SOUND_EVENT, // Required for SoundEvent packets
				BuiltInRegistries.MOB_EFFECT, // Required for MobEffect packets
				BuiltInRegistries.BLOCK, // Required for chunk BlockState paletted containers syncing
				BuiltInRegistries.ENCHANTMENT, // Required for EnchantmentMenu syncing
				BuiltInRegistries.ENTITY_TYPE, // Required for Entity spawn packets
				BuiltInRegistries.ITEM, // Required for Item/ItemStack packets
				BuiltInRegistries.PARTICLE_TYPE, // Required for ParticleType packets
				BuiltInRegistries.BLOCK_ENTITY_TYPE, // Required for BlockEntity packets
				BuiltInRegistries.PAINTING_VARIANT, // Required for EntityDataSerializers
				BuiltInRegistries.MENU, // Required for ClientboundOpenScreenPacket
				BuiltInRegistries.COMMAND_ARGUMENT_TYPE, // Required for ClientboundCommandsPacket
				BuiltInRegistries.STAT_TYPE, // Required for ClientboundAwardStatsPacket
				BuiltInRegistries.VILLAGER_TYPE, // Required for EntityDataSerializers
				BuiltInRegistries.VILLAGER_PROFESSION, // Required for EntityDataSerializers
				BuiltInRegistries.CAT_VARIANT, // Required for EntityDataSerializers
				BuiltInRegistries.FROG_VARIANT // Required for EntityDataSerializers
		);
	}

	public static <T> void enableVanilla(Class<T> cls, Supplier<Registry<T>> reg) {
		if (VANILLA_SYNC_REGISTRIES.contains(reg.get()) &&
				reg.get() instanceof MappedRegistry<T> mapped) {
			new RLClassHandler<>(cls, mapped);
		} else {
			new StringRLClassHandler<>(cls, reg);
		}
	}

	static {
		enableVanilla(Item.class, () -> BuiltInRegistries.ITEM);
		enableVanilla(Block.class, () -> BuiltInRegistries.BLOCK);
		enableVanilla(Potion.class, () -> BuiltInRegistries.POTION);
		enableVanilla(Enchantment.class, () -> BuiltInRegistries.ENCHANTMENT);
		enableVanilla(MobEffect.class, () -> BuiltInRegistries.MOB_EFFECT);
		enableVanilla(Attribute.class, () -> BuiltInRegistries.ATTRIBUTE);
		enableVanilla(Wrappers.cast(EntityType.class), () -> BuiltInRegistries.ENTITY_TYPE);
	}

	public static void register() {

	}

}
