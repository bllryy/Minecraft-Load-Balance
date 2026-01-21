package io.spigotrce.mclb.server.protcol.packet;

import io.netty.buffer.ByteBuf;
import io.spigotrce.mclb.server.protcol.ProtocolDirection;

import java.io.IOException;

public class Packet {
  public void write(int version, ProtocolDirection direction, ByteBuf buf) throws IOException {
  }

  public void read(int version, ProtocolDirection direction, ByteBuf buf) throws IOException {
  }
}
