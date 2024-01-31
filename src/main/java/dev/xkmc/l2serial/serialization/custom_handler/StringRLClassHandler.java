package dev.xkmc.l2serial.serialization.custom_handler;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.function.Supplier;

public class StringRLClassHandler<T> extends StringClassHandler<T> {

	@Deprecated
	public StringRLClassHandler(Class<T> cls, Supplier<IForgeRegistry<T>> reg) {
		super(cls, s -> reg.get().getValue(new ResourceLocation(s)), t -> reg.get().getKey(t).toString());
	}

}
