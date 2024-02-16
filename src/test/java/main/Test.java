package main;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class Test {

	public static ByteBuf unit() {
		return Unpooled.buffer();
	}

	public static ByteBuf decode(byte[] arr) {
		return Unpooled.wrappedBuffer(arr);
	}

	public static byte[] encode(ByteBuf buf) {
		byte[] arr = new byte[buf.writerIndex()];
		buf.getBytes(0, arr);
		return arr;
	}

	public static void main(String[] args) {
		var buf = unit();
		int test = 10;
		for (int i = 0; i < test; i++)
			buf.writeInt(i);
		var arr = encode(buf);
		var ans = unit();
		ans.writeInt(arr.length);
		for (var b : arr) ans.writeByte(b);
		int size = ans.readInt();
		byte[] brr = new byte[size];
		for (int i = 0; i < size; i++) brr[i] = ans.readByte();
		var packet = decode(brr);
		for (int i = 0; i < test; i++)
			System.out.println(packet.readInt());

	}

}
