package net.minecraft.network.play.client;

import java.io.IOException;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.INetHandlerPlayServer;
import net.minecraft.util.阻止位置;
import net.minecraft.util.IChatComponent;

public class C12PacketUpdateSign implements Packet<INetHandlerPlayServer>
{
    private 阻止位置 pos;
    private IChatComponent[] lines;

    public C12PacketUpdateSign()
    {
    }

    public C12PacketUpdateSign(阻止位置 pos, IChatComponent[] lines)
    {
        this.pos = pos;
        this.lines = new IChatComponent[] {lines[0], lines[1], lines[2], lines[3]};
    }

    public void readPacketData(PacketBuffer buf) throws IOException
    {
        this.pos = buf.readBlockPos();
        this.lines = new IChatComponent[4];

        for (int i = 0; i < 4; ++i)
        {
            String s = buf.readStringFromBuffer(384);
            IChatComponent ichatcomponent = IChatComponent.Serializer.jsonToComponent(s);
            this.lines[i] = ichatcomponent;
        }
    }

    public void writePacketData(PacketBuffer buf) throws IOException
    {
        buf.writeBlockPos(this.pos);

        for (int i = 0; i < 4; ++i)
        {
            IChatComponent ichatcomponent = this.lines[i];
            String s = IChatComponent.Serializer.componentToJson(ichatcomponent);
            buf.writeString(s);
        }
    }

    public void processPacket(INetHandlerPlayServer handler)
    {
        handler.processUpdateSign(this);
    }

    public 阻止位置 getPosition()
    {
        return this.pos;
    }

    public IChatComponent[] getLines()
    {
        return this.lines;
    }
}
