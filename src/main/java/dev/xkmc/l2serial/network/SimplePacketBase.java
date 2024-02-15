package dev.xkmc.l2serial.network;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.ConfigurationPayloadContext;

public interface SimplePacketBase {

	void write(FriendlyByteBuf buffer);

	void handle(ConfigurationPayloadContext context);

}
