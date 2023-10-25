package net.optifine;

import net.minecraft.block.BlockAir;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.实体;
import net.minecraft.init.Blocks;
import net.minecraft.src.Config;
import net.minecraft.util.阻止位置;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.IChunkProvider;

public class ClearWater
{
    public static void updateWaterOpacity(GameSettings settings, World world)
    {
        if (settings != null)
        {
            int i = 3;

            if (settings.ofClearWater)
            {
                i = 1;
            }

            BlockAir.setLightOpacity(Blocks.water, i);
            BlockAir.setLightOpacity(Blocks.flowing_water, i);
        }

        if (world != null)
        {
            IChunkProvider ichunkprovider = world.getChunkProvider();

            if (ichunkprovider != null)
            {
                实体 实体 = Config.getMinecraft().getRenderViewEntity();

                if (实体 != null)
                {
                    int j = (int) 实体.X坐标 / 16;
                    int k = (int) 实体.Z坐标 / 16;
                    int l = j - 512;
                    int i1 = j + 512;
                    int j1 = k - 512;
                    int k1 = k + 512;
                    int l1 = 0;

                    for (int i2 = l; i2 < i1; ++i2)
                    {
                        for (int j2 = j1; j2 < k1; ++j2)
                        {
                            if (ichunkprovider.chunkExists(i2, j2))
                            {
                                Chunk chunk = ichunkprovider.provideChunk(i2, j2);

                                if (chunk != null && !(chunk instanceof EmptyChunk))
                                {
                                    int k2 = i2 << 4;
                                    int l2 = j2 << 4;
                                    int i3 = k2 + 16;
                                    int j3 = l2 + 16;
                                    阻止位置M blockposm = new 阻止位置M(0, 0, 0);
                                    阻止位置M blockposm1 = new 阻止位置M(0, 0, 0);

                                    for (int k3 = k2; k3 < i3; ++k3)
                                    {
                                        for (int l3 = l2; l3 < j3; ++l3)
                                        {
                                            blockposm.setXyz(k3, 0, l3);
                                            阻止位置 blockpos = world.getPrecipitationHeight(blockposm);

                                            for (int i4 = 0; i4 < blockpos.getY(); ++i4)
                                            {
                                                blockposm1.setXyz(k3, i4, l3);
                                                IBlockState iblockstate = world.getBlockState(blockposm1);

                                                if (iblockstate.getBlock().getMaterial() == Material.water)
                                                {
                                                    world.markBlocksDirtyVertical(k3, l3, blockposm1.getY(), blockpos.getY());
                                                    ++l1;
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (l1 > 0)
                    {
                        String s = "server";

                        if (Config.isMinecraftThread())
                        {
                            s = "client";
                        }

                        Config.dbg("ClearWater (" + s + ") relighted " + l1 + " chunks");
                    }
                }
            }
        }
    }
}
