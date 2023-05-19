package main;

import dev.xkmc.l2serial.serialization.type_cache.RecordCache;
import dev.xkmc.l2serial.serialization.type_cache.TypeInfo;

import java.lang.reflect.Field;

public class Test {

	public interface I<T extends Record & I<T>> {

	}

	public record IR(int a) implements I<IR> {

	}

	public record R1<T extends Record & I<T>>(int a, T v) {

	}

	public static void main(String[] args) throws Exception {
		RecordCache cache = RecordCache.get(R1.class);
		Field[] fields = cache.getFields();
		for (Field f : fields) {
			System.out.println(TypeInfo.of(f).getAsClass());
		}
	}

}
