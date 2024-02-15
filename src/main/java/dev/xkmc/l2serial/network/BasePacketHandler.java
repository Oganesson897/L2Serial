package dev.xkmc.l2serial.network;

import dev.xkmc.l2serial.util.Wrappers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
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
public class BasePacketHandler {

	public final String serverName, clientName;
	public final int ver;
	public final String verStr;

	private final Function<BasePacketHandler, PacketConfiguration<?>>[] values;
	private final Map<Class<?>, PacketConfiguration<?>> map = new LinkedHashMap<>();

	/**
	 * Required to be registered manually
	 */
	@SafeVarargs
	public BasePacketHandler(String id, int version, Function<BasePacketHandler, PacketConfiguration<?>>... values) {
		serverName = id + "_to_server";
		clientName = id + "_to_client";
		ver = version;
		verStr = String.valueOf(ver);
		this.values = values;
	}

	public <T extends SimplePacketBase> PacketConfiguration<T> create(ResourceLocation id, Class<T> type, Function<FriendlyByteBuf, T> factory) {
		return new PacketConfiguration<>(id, type, factory);
	}

	public <T extends Record & SerialPacketBase<T>> PacketConfiguration<T> create(ResourceLocation id, Class<T> type) {
		return new PacketConfiguration<>(id, type, buf -> SerialPacketBase.serial(type, buf));
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

	public void onServerReg(RegisterPayloadHandlerEvent event) {
		var reg = event.registrar(serverName).versioned(verStr).optional();
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
