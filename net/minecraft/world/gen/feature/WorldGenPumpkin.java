package net.minecraft.world.gen.feature;

import java.util.Random;
import net.minecraft.block.BlockPumpkin;
import net.minecraft.init.Blocks;
import net.minecraft.util.阻止位置;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

public class WorldGenPumpkin extends WorldGenerator
{
    public boolean generate(World worldIn, Random rand, 阻止位置 position)
    {
        for (int i = 0; i < 64; ++i)
        {
            阻止位置 blockpos = position.add(rand.nextInt(8) - rand.nextInt(8), rand.nextInt(4) - rand.nextInt(4), rand.nextInt(8) - rand.nextInt(8));

            if (worldIn.isAirBlock(blockpos) && worldIn.getBlockState(blockpos.down()).getBlock() == Blocks.grass && Blocks.pumpkin.canPlaceBlockAt(worldIn, blockpos))
            {
                worldIn.setBlockState(blockpos, Blocks.pumpkin.getDefaultState().withProperty(BlockPumpkin.FACING, EnumFacing.Plane.HORIZONTAL.random(rand)), 2);
            }
        }

        return true;
    }
}
