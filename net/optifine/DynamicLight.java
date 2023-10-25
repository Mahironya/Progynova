package net.optifine;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.entity.实体;
import net.minecraft.init.Blocks;
import net.minecraft.src.Config;
import net.minecraft.util.阻止位置;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

public class DynamicLight
{
    private 实体 实体 = null;
    private double offsetY = 0.0D;
    private double lastPosX = -2.147483648E9D;
    private double lastPosY = -2.147483648E9D;
    private double lastPosZ = -2.147483648E9D;
    private int lastLightLevel = 0;
    private boolean underwater = false;
    private long timeCheckMs = 0L;
    private Set<阻止位置> setLitChunkPos = new HashSet();
    private 阻止位置.Mutable阻止位置 blockPosMutable = new 阻止位置.Mutable阻止位置();

    public DynamicLight(实体 实体)
    {
        this.实体 = 实体;
        this.offsetY = (double) 实体.getEyeHeight();
    }

    public void update(RenderGlobal renderGlobal)
    {
        if (Config.isDynamicLightsFast())
        {
            long i = System.currentTimeMillis();

            if (i < this.timeCheckMs + 500L)
            {
                return;
            }

            this.timeCheckMs = i;
        }

        double d6 = this.实体.X坐标 - 0.5D;
        double d0 = this.实体.Y坐标 - 0.5D + this.offsetY;
        double d1 = this.实体.Z坐标 - 0.5D;
        int j = DynamicLights.getLightLevel(this.实体);
        double d2 = d6 - this.lastPosX;
        double d3 = d0 - this.lastPosY;
        double d4 = d1 - this.lastPosZ;
        double d5 = 0.1D;

        if (Math.abs(d2) > d5 || Math.abs(d3) > d5 || Math.abs(d4) > d5 || this.lastLightLevel != j)
        {
            this.lastPosX = d6;
            this.lastPosY = d0;
            this.lastPosZ = d1;
            this.lastLightLevel = j;
            this.underwater = false;
            World world = renderGlobal.getWorld();

            if (world != null)
            {
                this.blockPosMutable.set(MathHelper.floor_double(d6), MathHelper.floor_double(d0), MathHelper.floor_double(d1));
                IBlockState iblockstate = world.getBlockState(this.blockPosMutable);
                Block block = iblockstate.getBlock();
                this.underwater = block == Blocks.water;
            }

            Set<阻止位置> set = new HashSet();

            if (j > 0)
            {
                EnumFacing enumfacing2 = (MathHelper.floor_double(d6) & 15) >= 8 ? EnumFacing.EAST : EnumFacing.WEST;
                EnumFacing enumfacing = (MathHelper.floor_double(d0) & 15) >= 8 ? EnumFacing.UP : EnumFacing.DOWN;
                EnumFacing enumfacing1 = (MathHelper.floor_double(d1) & 15) >= 8 ? EnumFacing.SOUTH : EnumFacing.NORTH;
                阻止位置 blockpos = new 阻止位置(d6, d0, d1);
                RenderChunk renderchunk = renderGlobal.getRenderChunk(blockpos);
                阻止位置 blockpos1 = this.getChunkPos(renderchunk, blockpos, enumfacing2);
                RenderChunk renderchunk1 = renderGlobal.getRenderChunk(blockpos1);
                阻止位置 blockpos2 = this.getChunkPos(renderchunk, blockpos, enumfacing1);
                RenderChunk renderchunk2 = renderGlobal.getRenderChunk(blockpos2);
                阻止位置 blockpos3 = this.getChunkPos(renderchunk1, blockpos1, enumfacing1);
                RenderChunk renderchunk3 = renderGlobal.getRenderChunk(blockpos3);
                阻止位置 blockpos4 = this.getChunkPos(renderchunk, blockpos, enumfacing);
                RenderChunk renderchunk4 = renderGlobal.getRenderChunk(blockpos4);
                阻止位置 blockpos5 = this.getChunkPos(renderchunk4, blockpos4, enumfacing2);
                RenderChunk renderchunk5 = renderGlobal.getRenderChunk(blockpos5);
                阻止位置 blockpos6 = this.getChunkPos(renderchunk4, blockpos4, enumfacing1);
                RenderChunk renderchunk6 = renderGlobal.getRenderChunk(blockpos6);
                阻止位置 blockpos7 = this.getChunkPos(renderchunk5, blockpos5, enumfacing1);
                RenderChunk renderchunk7 = renderGlobal.getRenderChunk(blockpos7);
                this.updateChunkLight(renderchunk, this.setLitChunkPos, set);
                this.updateChunkLight(renderchunk1, this.setLitChunkPos, set);
                this.updateChunkLight(renderchunk2, this.setLitChunkPos, set);
                this.updateChunkLight(renderchunk3, this.setLitChunkPos, set);
                this.updateChunkLight(renderchunk4, this.setLitChunkPos, set);
                this.updateChunkLight(renderchunk5, this.setLitChunkPos, set);
                this.updateChunkLight(renderchunk6, this.setLitChunkPos, set);
                this.updateChunkLight(renderchunk7, this.setLitChunkPos, set);
            }

            this.updateLitChunks(renderGlobal);
            this.setLitChunkPos = set;
        }
    }

    private 阻止位置 getChunkPos(RenderChunk renderChunk, 阻止位置 pos, EnumFacing facing)
    {
        return renderChunk != null ? renderChunk.getBlockPosOffset16(facing) : pos.offset(facing, 16);
    }

    private void updateChunkLight(RenderChunk renderChunk, Set<阻止位置> setPrevPos, Set<阻止位置> setNewPos)
    {
        if (renderChunk != null)
        {
            CompiledChunk compiledchunk = renderChunk.getCompiledChunk();

            if (compiledchunk != null && !compiledchunk.isEmpty())
            {
                renderChunk.setNeedsUpdate(true);
            }

            阻止位置 blockpos = renderChunk.getPosition();

            if (setPrevPos != null)
            {
                setPrevPos.remove(blockpos);
            }

            if (setNewPos != null)
            {
                setNewPos.add(blockpos);
            }
        }
    }

    public void updateLitChunks(RenderGlobal renderGlobal)
    {
        for (阻止位置 blockpos : this.setLitChunkPos)
        {
            RenderChunk renderchunk = renderGlobal.getRenderChunk(blockpos);
            this.updateChunkLight(renderchunk, (Set<阻止位置>)null, (Set<阻止位置>)null);
        }
    }

    public 实体 getEntity()
    {
        return this.实体;
    }

    public double getLastPosX()
    {
        return this.lastPosX;
    }

    public double getLastPosY()
    {
        return this.lastPosY;
    }

    public double getLastPosZ()
    {
        return this.lastPosZ;
    }

    public int getLastLightLevel()
    {
        return this.lastLightLevel;
    }

    public boolean isUnderwater()
    {
        return this.underwater;
    }

    public double getOffsetY()
    {
        return this.offsetY;
    }

    public String toString()
    {
        return "Entity: " + this.实体 + ", offsetY: " + this.offsetY;
    }
}
