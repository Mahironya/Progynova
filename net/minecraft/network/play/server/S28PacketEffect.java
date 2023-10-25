package net.minecraft.network.play.server;

import java.io.IOException;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.util.阻止位置;

public class S28PacketEffect implements Packet<INetHandlerPlayClient>
{
    private int soundType;
    private 阻止位置 soundPos;
    private int soundData;
    private boolean serverWide;

    public S28PacketEffect()
    {
    }

    public S28PacketEffect(int soundTypeIn, 阻止位置 soundPosIn, int soundDataIn, boolean serverWideIn)
    {
        this.soundType = soundTypeIn;
        this.soundPos = soundPosIn;
        this.soundData = soundDataIn;
        this.serverWide = serverWideIn;
    }

    public void readPacketData(PacketBuffer buf) throws IOException
    {
        this.soundType = buf.readInt();
        this.soundPos = buf.readBlockPos();
        this.soundData = buf.readInt();
        this.serverWide = buf.readBoolean();
    }

    public void writePacketData(PacketBuffer buf) throws IOException
    {
        buf.writeInt(this.soundType);
        buf.writeBlockPos(this.soundPos);
        buf.writeInt(this.soundData);
        buf.writeBoolean(this.serverWide);
    }

    public void processPacket(INetHandlerPlayClient handler)
    {
        handler.handleEffect(this);
    }

    public boolean isSoundServerwide()
    {
        return this.serverWide;
    }

    public int getSoundType()
    {
        return this.soundType;
    }

    public int getSoundData()
    {
        return this.soundData;
    }

    public 阻止位置 getSoundPos()
    {
        return this.soundPos;
    }
}
