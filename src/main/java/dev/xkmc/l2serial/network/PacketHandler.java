package dev.xkmc.l2serial.network;

import dev.xkmc.l2serial.util.Wrappers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.handling.ConfigurationPayloadContext;
import net.neoforged.neoforge.network.handling.IConfigurationPayloadHandler;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

@SuppressWarnings("unused")
public class PacketHandler {

	public final String name;
	public final int ver;
	public final String verStr;

	private final Function<PacketHandler, PacketConfiguration<?>>[] values;
	private final Map<Class<?>, PacketConfiguration<?>> map = new LinkedHashMap<>();

	/**
	 * Required to be registered manually
	 */
	@SafeVarargs
	public PacketHandler(String id, int version, Function<PacketHandler, PacketConfiguration<?>>... values) {
		name = id;
		ver = version;
		verStr = String.valueOf(ver);
		this.values = values;
	}

	private ResourceLocation of(Class<?> cls) {
		String name = cls.getSimpleName();
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < name.length(); i++) {
			char ch = name.charAt(i);
			if (ch >= 'a' && ch <= 'z' || ch >= '0' && ch <= '9') {
				builder.append(ch);
			} else if (ch >= 'A' && ch <= 'Z') {
				builder.append((char)(ch - 'A' + 'a'));
			}
		}
		return new ResourceLocation(this.name, builder.toString());
	}

	public <T extends SimplePacketBase> PacketConfiguration<T> create(Class<T> type, Function<FriendlyByteBuf, T> factory) {
		return new PacketConfiguration<>(of(type), type, factory);
	}

	public <T extends Record & SerialPacketBase<T>> PacketConfiguration<T> create(Class<T> type) {
		return new PacketConfiguration<>(of(type), type, buf -> SerialPacketBase.serial(type, buf));
	}

	private <T extends SimplePacketBase> BasePayload<T> get(T val) {
		return new BasePayload<>(Wrappers.cast(map.get(val.getClass())), val);
	}

	public void toServer(SimplePacketBase packet) {
		PacketDistributor.SERVER.noArg().send(get(packet));
	}

	public void toTrackingPlayers(SimplePacketBase packet, Entity e) {
		PacketDistributor.TRACKING_ENTITY_AND_SELF.with(e).send(get(packet));
	}

	public void toTrackingOnly(SimplePacketBase packet, Entity e) {
		PacketDistributor.TRACKING_ENTITY.with(e).send(get(packet));
	}

	public void toClientPlayer(SimplePacketBase packet, ServerPlayer e) {
		PacketDistributor.PLAYER.with(e).send(get(packet));
	}

	public void toAllClient(SimplePacketBase packet) {
		PacketDistributor.ALL.noArg().send(get(packet));
	}

	public void toTrackingChunk(LevelChunk chunk, SimplePacketBase packet) {
		PacketDistributor.TRACKING_CHUNK.with(chunk).send(get(packet));
	}

	public void sendToNear(Level world, BlockPos pos, int range, SimplePacketBase packet) {
		PacketDistributor.NEAR.with(new PacketDistributor.TargetPoint(pos.getX(), pos.getY(), pos.getZ(), range, world.dimension())).send(get(packet));
	}

	public void register(RegisterPayloadHandlerEvent event) {
		var reg = event.registrar(name).versioned(verStr).optional();
		for (var packet : values) {
			var config = packet.apply(this);
			config.register(reg);
			map.put(config.type, config);
		}
	}

	public record PacketConfiguration<T extends SimplePacketBase>(
			ResourceLocation id,
			Class<T> type,
			Function<FriendlyByteBuf, T> decoder
	) implements IConfigurationPayloadHandler<BasePayload<T>> {

		private void register(IPayloadRegistrar reg) {
			reg.configuration(id, this::read, this);
		}

		private BasePayload<T> read(FriendlyByteBuf buf) {
			return new BasePayload<>(this, decoder.apply(buf));
		}

		@Override
		public void handle(BasePayload<T> payload, ConfigurationPayloadContext context) {
			payload.packet().handle(context);
		}

	}

	public record BasePayload<T extends SimplePacketBase>(PacketConfiguration<T> config, T packet)
			implements CustomPacketPayload {

		@Override
		public void write(FriendlyByteBuf buffer) {
			packet.write(buffer);
		}

		@Override
		public ResourceLocation id() {
			return config.id();
		}

	}

}
