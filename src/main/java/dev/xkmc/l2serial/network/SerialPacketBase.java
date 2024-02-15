package dev.xkmc.l2serial.network;

import dev.xkmc.l2serial.serialization.codec.PacketCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.ConfigurationPayloadContext;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public interface SerialPacketBase<T extends Record & SerialPacketBase<T>> extends SimplePacketBase {

	static <T extends Record & SerialPacketBase<T>> T serial(Class<T> cls, FriendlyByteBuf buf) {
		return Objects.requireNonNull(PacketCodec.from(buf, cls, null));
	}

	@Override
	default void write(FriendlyByteBuf buffer) {
		PacketCodec.to(buffer, this);
	}

	@Override
	default void handle(ConfigurationPayloadContext context) {
		context.workHandler().execute(() -> handle(context.player().orElse(null)));
	}

	void handle(@Nullable Player ctx);

}
