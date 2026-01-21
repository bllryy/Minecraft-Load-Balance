package io.spigotrce.mclb.server.protcol;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;

public final class BufUtils {
  public static <T> T readNullable(Function<ByteBuf, T> reader, ByteBuf buf) {
    return buf.readBoolean() ? reader.apply(buf) : null;
  }

  public static <T> void writeNullable(T value, BiConsumer<T, ByteBuf> writer, ByteBuf buf) {
    if (value != null) {
      buf.writeBoolean(true);
      writer.accept(value, buf);
    } else {
      buf.writeBoolean(false);
    }
  }

  public static <T> T readLengthPrefixed(Function<ByteBuf, T> reader, ByteBuf buf, int maxSize) throws IOException {

    int size = readVarInt(buf);
    if (size > maxSize) {
      throw new IOException("Cannot read length prefixed with limit " + maxSize + " (got " + size + ")");
    }

    return reader.apply(buf.readSlice(size));
  }

  public static <T> void writeLengthPrefixed(T value, BiConsumer<T, ByteBuf> writer, ByteBuf buf, int maxSize)
    throws IOException {

    ByteBuf tmp = buf.alloc().buffer();
    try {
      writer.accept(value, tmp);

      int size = tmp.readableBytes();
      if (size > maxSize) {
        throw new IOException("Cannot write length prefixed with limit " + maxSize + " (got " + size + ")");
      }

      writeVarInt(size, buf);
      buf.writeBytes(tmp);
    } finally {
      tmp.release();
    }
  }

  public static void writeString(String s, ByteBuf buf) throws IOException {
    writeString(s, buf, Short.MAX_VALUE);
  }

  public static void writeString(String s, ByteBuf buf, int maxLength) throws IOException {
    if (s.length() > maxLength) {
      throw new IOException("Cannot send string longer than " + maxLength + " (got " + s.length() + " characters)");
    }

    byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
    if (bytes.length > maxLength * 3) {
      throw new IOException("Cannot send string longer than " + (maxLength * 3) + " (got " + bytes.length + " bytes)");
    }

    writeVarInt(bytes.length, buf);
    buf.writeBytes(bytes);
  }

  public static String readString(ByteBuf buf) throws IOException {
    return readString(buf, Short.MAX_VALUE);
  }

  public static String readString(ByteBuf buf, int maxLength) throws IOException {
    int len = readVarInt(buf);
    if (len > maxLength * 3) {
      throw new IOException("Cannot receive string longer than " + (maxLength * 3) + " (got " + len + " bytes)");
    }

    String s = buf.readString(len, StandardCharsets.UTF_8);
    if (s.length() > maxLength) {
      throw new IOException("Cannot receive string longer than " + maxLength + " (got " + s.length() + " characters)");
    }

    return s;
  }

  public static <T> T readStringMapKey(ByteBuf buf, Map<String, T> map) throws IOException {
    String key = readString(buf);
    T value = map.get(key);
    Preconditions.checkArgument(value != null, "Unknown string key %s", key);
    return value;
  }

  public static void writeArray(byte[] bytes, ByteBuf buf) throws IOException {
    if (bytes.length > Short.MAX_VALUE) {
      throw new IOException("Cannot send byte array longer than " + Short.MAX_VALUE + " (got " + bytes.length + ")");
    }

    writeVarInt(bytes.length, buf);
    buf.writeBytes(bytes);
  }

  public static byte[] readArray(ByteBuf buf) throws IOException {
    return readArray(buf, buf.readableBytes());
  }

  public static byte[] readArray(ByteBuf buf, int limit) throws IOException {
    int len = readVarInt(buf);
    if (len > limit) {
      throw new IOException("Cannot receive byte array longer than " + limit + " (got " + len + ")");
    }

    byte[] out = new byte[len];
    buf.readBytes(out);
    return out;
  }

  public static byte[] toArray(ByteBuf buf) {
    byte[] out = new byte[buf.readableBytes()];
    buf.readBytes(out);
    return out;
  }

  public static int readVarInt(ByteBuf buf) throws IOException {
    return readVarInt(buf, 5);
  }

  public static int readVarInt(ByteBuf buf, int maxBytes) throws IOException {
    int value = 0;
    int shift = 0;
    byte b;

    do {
      b = buf.readByte();
      value |= (b & 0x7F) << shift;

      shift += 7;
      if (shift > maxBytes * 7) {
        throw new IOException("VarInt too big (max " + maxBytes + ")");
      }
    } while ((b & 0x80) != 0);

    return value;
  }

  public static void writeVarInt(int value, ByteBuf buf) {
    do {
      int part = value & 0x7F;
      value >>>= 7;
      if (value != 0) {
        part |= 0x80;
      }
      buf.writeByte(part);
    } while (value != 0);
  }

  public static int readVarShort(ByteBuf buf) {
    int low = buf.readUnsignedShort();
    int high = 0;

    if ((low & 0x8000) != 0) {
      low &= 0x7FFF;
      high = buf.readUnsignedByte();
    }

    return (high << 15) | low;
  }

  public static void writeVarShort(ByteBuf buf, int value) {
    int low = value & 0x7FFF;
    int high = (value >>> 15) & 0xFF;

    if (high != 0) {
      low |= 0x8000;
    }

    buf.writeShort(low);
    if (high != 0) {
      buf.writeByte(high);
    }
  }

  public static void writeUUID(UUID uuid, ByteBuf buf) {
    buf.writeLong(uuid.getMostSignificantBits());
    buf.writeLong(uuid.getLeastSignificantBits());
  }

  public static UUID readUUID(ByteBuf buf) {
    return new UUID(buf.readLong(), buf.readLong());
  }

  public static <E extends Enum<E>> void writeEnum(E value, ByteBuf buf) throws IOException {
    writeVarInt(value.ordinal(), buf);
  }

  public static <E extends Enum<E>> E readEnum(Class<E> enumClass, ByteBuf buf) throws IOException {
    return enumClass.getEnumConstants()[readVarInt(buf)];
  }

  public static BitSet readFixedBitSet(int size, ByteBuf buf) {
    byte[] data = new byte[(size + 7) >> 3];
    buf.readBytes(data);
    return BitSet.valueOf(data);
  }

  public static void writeFixedBitSet(BitSet bits, int size, ByteBuf buf) throws IOException {
    if (bits.length() > size) {
      throw new IOException("BitSet too large (expected " + size + ", got " + bits.length() + ")");
    }

    buf.writeBytes(Arrays.copyOf(bits.toByteArray(), (size + 7) >> 3));
  }
}
