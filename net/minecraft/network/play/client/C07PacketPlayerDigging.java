package net.minecraft.network.play.client;

import java.io.IOException;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.INetHandlerPlayServer;
import net.minecraft.util.阻止位置;
import net.minecraft.util.EnumFacing;

public class C07PacketPlayerDigging implements Packet<INetHandlerPlayServer>
{
    private 阻止位置 position;
    private EnumFacing facing;
    private C07PacketPlayerDigging.Action status;

    public C07PacketPlayerDigging()
    {
    }

    public C07PacketPlayerDigging(C07PacketPlayerDigging.Action statusIn, 阻止位置 posIn, EnumFacing facingIn)
    {
        this.status = statusIn;
        this.position = posIn;
        this.facing = facingIn;
    }

    public void readPacketData(PacketBuffer buf) throws IOException
    {
        this.status = (C07PacketPlayerDigging.Action)buf.readEnumValue(C07PacketPlayerDigging.Action.class);
        this.position = buf.readBlockPos();
        this.facing = EnumFacing.getFront(buf.readUnsignedByte());
    }

    public void writePacketData(PacketBuffer buf) throws IOException
    {
        buf.writeEnumValue(this.status);
        buf.writeBlockPos(this.position);
        buf.writeByte(this.facing.getIndex());
    }

    public void processPacket(INetHandlerPlayServer handler)
    {
        handler.processPlayerDigging(this);
    }

    public 阻止位置 getPosition()
    {
        return this.position;
    }

    public EnumFacing getFacing()
    {
        return this.facing;
    }

    public C07PacketPlayerDigging.Action getStatus()
    {
        return this.status;
    }

    public static enum Action
    {
        START_DESTROY_BLOCK,
        ABORT_DESTROY_BLOCK,
        STOP_DESTROY_BLOCK,
        DROP_ALL_ITEMS,
        DROP_ITEM,
        RELEASE_USE_ITEM;
    }
}
