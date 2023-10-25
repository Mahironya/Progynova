package net.optifine;

import net.minecraft.src.Config;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.阻止位置;
import net.minecraft.world.biome.BiomeGenBase;
import net.optifine.util.TileEntityUtils;

public class RandomTileEntity implements IRandomEntity
{
    private TileEntity tileEntity;

    public int getId()
    {
        return Config.getRandom(this.tileEntity.getPos(), this.tileEntity.getBlockMetadata());
    }

    public 阻止位置 getSpawnPosition()
    {
        return this.tileEntity.getPos();
    }

    public String getName()
    {
        String s = TileEntityUtils.getTileEntityName(this.tileEntity);
        return s;
    }

    public BiomeGenBase getSpawnBiome()
    {
        return this.tileEntity.getWorld().getBiomeGenForCoords(this.tileEntity.getPos());
    }

    public int getHealth()
    {
        return -1;
    }

    public int getMaxHealth()
    {
        return -1;
    }

    public TileEntity getTileEntity()
    {
        return this.tileEntity;
    }

    public void setTileEntity(TileEntity tileEntity)
    {
        this.tileEntity = tileEntity;
    }
}
