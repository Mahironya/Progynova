package net.minecraft.client.gui.stream;

import java.io.IOException;

import net.minecraft.client.gui.鬼Button;
import net.minecraft.client.我的手艺;
import net.minecraft.client.gui.鬼Screen;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.stream.IngestServerTester;
import net.minecraft.util.枚举聊天格式;
import tv.twitch.broadcast.IngestServer;

public class 鬼IngestServers extends 鬼Screen
{
    private final 鬼Screen field_152309_a;
    private String field_152310_f;
    private 鬼IngestServers.ServerList field_152311_g;

    public 鬼IngestServers(鬼Screen p_i46312_1_)
    {
        this.field_152309_a = p_i46312_1_;
    }

    public void initGui()
    {
        this.field_152310_f = I18n.format("options.stream.ingest.title", new Object[0]);
        this.field_152311_g = new 鬼IngestServers.ServerList(this.mc);

        if (!this.mc.getTwitchStream().func_152908_z())
        {
            this.mc.getTwitchStream().func_152909_x();
        }

        this.buttonList.add(new 鬼Button(1, this.width / 2 - 155, this.height - 24 - 6, 150, 20, I18n.format("gui.done", new Object[0])));
        this.buttonList.add(new 鬼Button(2, this.width / 2 + 5, this.height - 24 - 6, 150, 20, I18n.format("options.stream.ingest.reset", new Object[0])));
    }

    public void handleMouseInput() throws IOException
    {
        super.handleMouseInput();
        this.field_152311_g.handleMouseInput();
    }

    public void onGuiClosed()
    {
        if (this.mc.getTwitchStream().func_152908_z())
        {
            this.mc.getTwitchStream().func_152932_y().func_153039_l();
        }
    }

    protected void actionPerformed(鬼Button button) throws IOException
    {
        if (button.enabled)
        {
            if (button.id == 1)
            {
                this.mc.displayGuiScreen(this.field_152309_a);
            }
            else
            {
                this.mc.游戏一窝.streamPreferredServer = "";
                this.mc.游戏一窝.saveOptions();
            }
        }
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        this.drawDefaultBackground();
        this.field_152311_g.drawScreen(mouseX, mouseY, partialTicks);
        this.drawCenteredString(this.fontRendererObj, this.field_152310_f, this.width / 2, 20, 16777215);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    class ServerList extends GuiSlot
    {
        public ServerList(我的手艺 mcIn)
        {
            super(mcIn, 鬼IngestServers.this.width, 鬼IngestServers.this.height, 32, 鬼IngestServers.this.height - 35, (int)((double)mcIn.字体渲染员.FONT_HEIGHT * 3.5D));
            this.setShowSelectionBox(false);
        }

        protected int getSize()
        {
            return this.mc.getTwitchStream().func_152925_v().length;
        }

        protected void elementClicked(int slotIndex, boolean isDoubleClick, int mouseX, int mouseY)
        {
            this.mc.游戏一窝.streamPreferredServer = this.mc.getTwitchStream().func_152925_v()[slotIndex].serverUrl;
            this.mc.游戏一窝.saveOptions();
        }

        protected boolean isSelected(int slotIndex)
        {
            return this.mc.getTwitchStream().func_152925_v()[slotIndex].serverUrl.equals(this.mc.游戏一窝.streamPreferredServer);
        }

        protected void drawBackground()
        {
        }

        protected void drawSlot(int entryID, int p_180791_2_, int p_180791_3_, int p_180791_4_, int mouseXIn, int mouseYIn)
        {
            IngestServer ingestserver = this.mc.getTwitchStream().func_152925_v()[entryID];
            String s = ingestserver.serverUrl.replaceAll("\\{stream_key\\}", "");
            String s1 = (int)ingestserver.bitrateKbps + " kbps";
            String s2 = null;
            IngestServerTester ingestservertester = this.mc.getTwitchStream().func_152932_y();

            if (ingestservertester != null)
            {
                if (ingestserver == ingestservertester.func_153040_c())
                {
                    s = 枚举聊天格式.GREEN + s;
                    s1 = (int)(ingestservertester.func_153030_h() * 100.0F) + "%";
                }
                else if (entryID < ingestservertester.func_153028_p())
                {
                    if (ingestserver.bitrateKbps == 0.0F)
                    {
                        s1 = 枚举聊天格式.RED + "Down!";
                    }
                }
                else
                {
                    s1 = 枚举聊天格式.OBFUSCATED + "1234" + 枚举聊天格式.RESET + " kbps";
                }
            }
            else if (ingestserver.bitrateKbps == 0.0F)
            {
                s1 = 枚举聊天格式.RED + "Down!";
            }

            p_180791_2_ = p_180791_2_ - 15;

            if (this.isSelected(entryID))
            {
                s2 = 枚举聊天格式.BLUE + "(Preferred)";
            }
            else if (ingestserver.defaultServer)
            {
                s2 = 枚举聊天格式.GREEN + "(Default)";
            }

            鬼IngestServers.this.drawString(鬼IngestServers.this.fontRendererObj, ingestserver.serverName, p_180791_2_ + 2, p_180791_3_ + 5, 16777215);
            鬼IngestServers.this.drawString(鬼IngestServers.this.fontRendererObj, s, p_180791_2_ + 2, p_180791_3_ + 鬼IngestServers.this.fontRendererObj.FONT_HEIGHT + 5 + 3, 3158064);
            鬼IngestServers.this.drawString(鬼IngestServers.this.fontRendererObj, s1, this.getScrollBarX() - 5 - 鬼IngestServers.this.fontRendererObj.getStringWidth(s1), p_180791_3_ + 5, 8421504);

            if (s2 != null)
            {
                鬼IngestServers.this.drawString(鬼IngestServers.this.fontRendererObj, s2, this.getScrollBarX() - 5 - 鬼IngestServers.this.fontRendererObj.getStringWidth(s2), p_180791_3_ + 5 + 3 + 鬼IngestServers.this.fontRendererObj.FONT_HEIGHT, 8421504);
            }
        }

        protected int getScrollBarX()
        {
            return super.getScrollBarX() + 15;
        }
    }
}
