# L2Serial

This is a Minecraft serialization package for automatic 
serialization and de-serialization of Java and Minecraft objects,
to JSON, NBT, and packets.
Note that not all objects support all ways of serialization,
though they will be specially marked as such.

##### Table of Contents
- [Supported Classes](#supported-classes)
- [How to Use](#how-to-use)
- [Networking](#networking)

## Supported Classes
### List of objects supporting all 3 serialization methods:
- All Primitive types and their boxed types (`char`,`byte`,`short`,`int`,`long`,`float`,`double`,`boolean`)
- Commonly used Java objects (`String`, `UUID`)
- Collections or collection-like of leaf class (`Set`, `List`, `Map`, `Record`, `Enum`, `Array`).
  - Note that they must be in leaf class. For example, `List<String>` is illegal, but `ArrayList<String>` is good.
  - Make sure the classes (except record) have a default constructor with no parameters.
  - For enum, only leaf class allowed. Parameterized types such as `<T extends Enum & Ixxx>` is not allowed.
  - For record, parameterized types are allowed. You can use `<T extends Record & Ixxx>` to represent a record that implements interface `Ixxx`.
- Commonly used registered Minecraft objects
  - Default implementation supports `Item`, `Block`, `Enchantment`, `MobEffect`, `Potion`, `EntityType`
  - You can add others yourself using `RLClassHandler`. Example: `new RLClassHandler<>(Item.class, () -> ForgeRegistries.ITEMS);`
  - Datapack registry not allowed.
  - For custom registry, you need to register them through `RLClassHandler`. If you are using `L2Registrate`, it handles automatically for you.
- Common Minecraft Objects
  - `ResourceLocation`, `ItemStack`, `FluidStack`
- Classes you defines that are marked as `@SerialClass`

### List of objects that support everything besides NBT:
- `Ingredient`

### List of objects that support everything besides JSON:
- NBT elements: `CompountTag`, `ListTag`
- Simple class that serves as record: `BlockPos`, `Vec3`, `MobEffectInstance`
  - If you want to use these in json, please create a record.

### Null Defer
Note that `ItemStack` and `Ingredient` cannot be null: null value will be treated as `ItemStack.EMPTY` and `Ingredient.EMPTY` respectively.

## How to use
To mark a class as serializable, you need to also mark its fields. Here is an example:
```java
// Mark this class as serializable
@SerialClass
public class TestClassA { 
	
    // regular field. private is allowed.
    @SerialClass.SerialField
    private String name;

    // final field. During de-serialziation,
    // this object will have values injected
    // instead of constructed again
    @SerialClass.SerialField 
    private final TestClassB base = new TestClassB(this);
	
    // for collections, being final or not doesn't matter
    @SerialClass.SerialField
    public final ArrayList<TestClassB> list = new ArrayList<>();
	
    // supports nested collections and maps
    @SerialClass.SerialField
    public final HashMap<Item, ArrayList<ItemStack>> list = new HashMap<>();

    // this field is ignored during serialization
    private boolean valid;
    
    // to be de-serializable directly,
    // it needs to have a default constructor
    @Deprecated 
    public TestClassA(){
		
    }
	
    // Optional. This method will be called after de-serialization.
    @SerialClass.OnInject
    public void onInject(){
        valid = true;
    }
	
    // regular constructor
    public TestClassA(String name){
        this.name = name;
        this.valid = true;
        // warning: if the object serialized doesn't match 
        // its declared class, the algorithm will serialize
        // the class name as well. Same for records.
        list.add(new TestClassC(this));
    }
	
}

@SerialClass
public class TestClassB {
	
    // do not loop reference
    public final TestClassA parent;
	
    @SerialClass.SerialField
    public int value;
	
    // This class cannot be constructed directly
    // but can still have injections
    public TestClassB(TestClassA parent) {
        this.parent = parent;
    }
	
}

// inheritance allowed.
// However, they must not have fields of the same name.
@SerialClass
public class TestClassC extends TestClassB {

    @SerialClass.SerialField
    public double secondValue;

    public TestClassB(TestClassA parent) {
        super(parent);
    }
	
}

// records don't need annotation
public record TestClassD(int a, TestClassA obj) {
	
}

```
How to serialize:
```java

public class Test {
	
    public static void jsonConvert() {
        TestClassA obj = new TestClassA("abc");
        // convert obj to json
        JsonElement a = JsonCodec.toJson(obj);
        // convert json back to obj, constructing object
        TestClassA rec = JsonCodec.from(TestClassA.class, json, null);
        //convert obj to json, but mark available type information
        JsonElement b = JsonCodec.toJson(a.base, TestClassB.class);
        // inject data into existing object
        JsonCodec.from(TestClassB.class, c, a.base);
        
        JsonObject json = new JsonObject();
        // inject data into existing json object
        JsonCodec.toJsonObject(obj, json);
		
    }
	
}

```
Same for NBT and Packets. Note that NBT has 9 methods of 4 type:
- `TagCodec::toTag`: serialize `@SerialClass` object to tag
- `TagCodec::valueToTag`: serialize any value to tag
- `TagCodec::fromTag`: deserialize `@SerialClass` object from tag
- `TagCodec::valueFromTag`: deserialize any value from tag
Also, it supports 3 different serialization filter:
- All: serialize everything
- toClient: serialize fields that are marked as toClient only
  - This is for BlockEntity and Entity synchronization
- toTracking: serialize fields that are marked as toTracking only
  - This is for Capability syncronization

## Networking

This library also comes with a networking tool. The `BasePacketHandler` can
simplify packet handing by a lot. You only need to extend `SerialPacketBase`,
mark your class as `@SerialClass`, mark your fields to serialize as `@SerialClass.SerialField`,
and implement `handle` method. The method will be executed in main thread.

It also has functions to send a packet to server, to one client, to all clients, 
to tracking clients of an entity, and to tracking clients of a block.
