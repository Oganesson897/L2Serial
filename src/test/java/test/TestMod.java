package test;

import com.mojang.serialization.JsonOps;
import dev.xkmc.l2serial.serialization.codec.CodecAdaptor;
import dev.xkmc.l2serial.serialization.custom_handler.Handlers;
import net.minecraft.nbt.NbtOps;
import net.neoforged.fml.common.Mod;

import java.util.Random;

@Mod(TestMod.MODID)
public class TestMod {

	public static final String MODID = "testmod";

	public TestMod() {
		Handlers.register();
		TestObject.TestA obj = TestObject.get(new Random());
		var cls = TestObject.TestA.class;
		var codec = new CodecAdaptor<>(cls);

		var jsonB = codec.encodeStart(JsonOps.INSTANCE, obj).getOrThrow(false, e -> {
		});
		var resB = codec.decode(JsonOps.INSTANCE, jsonB).getOrThrow(false, e -> {
		}).getFirst();
		System.out.println("Json Codec test: " + obj.equals(resB));

		var ct = codec.decode(NbtOps.INSTANCE, codec.encodeStart(NbtOps.INSTANCE, obj)
				.getOrThrow(false, e -> {
				})).getOrThrow(false, e -> {
		}).getFirst();
		System.out.println("Codec NBT test: " + obj.equals(ct));
		var nc = codec.toNetwork();
		var cb = nc.decode(NbtOps.INSTANCE, nc.encodeStart(NbtOps.INSTANCE, obj)
				.getOrThrow(false, e -> {
				})).getOrThrow(false, e -> {
		}).getFirst();
		;
		System.out.println("Codec Packet test: " + obj.equals(cb));

	}

}
