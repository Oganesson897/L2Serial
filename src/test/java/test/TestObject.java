package test;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.EquipmentSlot;

import java.util.*;

public class TestObject {

	public static TestA get(Random r) {
		var ans = new TestA(new LinkedHashMap<>());
		for (var e : Direction.values()) {
			ans.map.put(e, new ArrayList<>(List.of(getB(r), getB(r), getB(r))));
		}
		return ans;
	}

	public static TestB getB(Random r) {
		var ans = new TestB(EquipmentSlot.values()[r.nextInt(6)], new LinkedHashSet<>());
		int n = r.nextInt(4);
		for (int i=0;i<n;i++){
			ans.set.add("Test");
		}
		return ans;
	}

	public record TestA(LinkedHashMap<Direction, ArrayList<TestB>> map) {

	}

	public record TestB(EquipmentSlot slot, LinkedHashSet<String> set) {

	}

}
