package io.bllry.mclb.server.protcol.packet.impl;

import io.bllry.mclb.server.protcol.BufUtils;
import io.bllry.mclb.server.protcol.ProtocolDirection;
import io.netty.buffer.ByteBuf;
import io.spigotrce.mclb.server.protcol.*;
import io.bllry.mclb.server.protcol.packet.Packet;

import java.io.IOException;

public class PacketToServerHandshake extends Packet {
  private int protocolVersion;
  private String host;
  private int port;
  private HandshakeIntent intent;

  @Override public void read(int version, ProtocolDirection direction, ByteBuf buf) throws IOException {
    protocolVersion = BufUtils.readVarInt(buf);
    host = BufUtils.readString(buf, 255);
    port = buf.readUnsignedShort();
    intent = BufUtils.readEnum(HandshakeIntent.class, buf);
  }

  public int getProtocolVersion() {
    return protocolVersion;
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public HandshakeIntent getIntent() {
    return intent;
  }

  public static enum HandshakeIntent {
    STATUS,
    LOGIN,
    TRANSFER;
  }
}
