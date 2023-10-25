package net.minecraft.block;

import java.util.Random;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.实体;
import net.minecraft.entity.实体LivingBase;
import net.minecraft.entity.player.实体Player;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.阻止位置;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockFarmland extends Block
{
    public static final PropertyInteger MOISTURE = PropertyInteger.create("moisture", 0, 7);

    protected BlockFarmland()
    {
        super(Material.ground);
        this.setDefaultState(this.blockState.getBaseState().withProperty(MOISTURE, Integer.valueOf(0)));
        this.setTickRandomly(true);
        this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.9375F, 1.0F);
        this.setLightOpacity(255);
    }

    public AxisAlignedBB getCollisionBoundingBox(World worldIn, 阻止位置 pos, IBlockState state)
    {
        return new AxisAlignedBB((double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), (double)(pos.getX() + 1), (double)(pos.getY() + 1), (double)(pos.getZ() + 1));
    }

    public boolean isOpaqueCube()
    {
        return false;
    }

    public boolean isFullCube()
    {
        return false;
    }

    public void updateTick(World worldIn, 阻止位置 pos, IBlockState state, Random rand)
    {
        int i = ((Integer)state.getValue(MOISTURE)).intValue();

        if (!this.hasWater(worldIn, pos) && !worldIn.isRainingAt(pos.up()))
        {
            if (i > 0)
            {
                worldIn.setBlockState(pos, state.withProperty(MOISTURE, Integer.valueOf(i - 1)), 2);
            }
            else if (!this.hasCrops(worldIn, pos))
            {
                worldIn.setBlockState(pos, Blocks.dirt.getDefaultState());
            }
        }
        else if (i < 7)
        {
            worldIn.setBlockState(pos, state.withProperty(MOISTURE, Integer.valueOf(7)), 2);
        }
    }

    public void onFallenUpon(World worldIn, 阻止位置 pos, 实体 实体In, float fallDistance)
    {
        if (实体In instanceof 实体LivingBase)
        {
            if (!worldIn.isRemote && worldIn.rand.nextFloat() < fallDistance - 0.5F)
            {
                if (!(实体In instanceof 实体Player) && !worldIn.getGameRules().getBoolean("mobGriefing"))
                {
                    return;
                }

                worldIn.setBlockState(pos, Blocks.dirt.getDefaultState());
            }

            super.onFallenUpon(worldIn, pos, 实体In, fallDistance);
        }
    }

    private boolean hasCrops(World worldIn, 阻止位置 pos)
    {
        Block block = worldIn.getBlockState(pos.up()).getBlock();
        return block instanceof BlockCrops || block instanceof BlockStem;
    }

    private boolean hasWater(World worldIn, 阻止位置 pos)
    {
        for (阻止位置.Mutable阻止位置 blockpos$mutableblockpos : 阻止位置.getAllInBoxMutable(pos.add(-4, 0, -4), pos.add(4, 1, 4)))
        {
            if (worldIn.getBlockState(blockpos$mutableblockpos).getBlock().getMaterial() == Material.water)
            {
                return true;
            }
        }

        return false;
    }

    public void onNeighborBlockChange(World worldIn, 阻止位置 pos, IBlockState state, Block neighborBlock)
    {
        super.onNeighborBlockChange(worldIn, pos, state, neighborBlock);

        if (worldIn.getBlockState(pos.up()).getBlock().getMaterial().isSolid())
        {
            worldIn.setBlockState(pos, Blocks.dirt.getDefaultState());
        }
    }

    public boolean shouldSideBeRendered(IBlockAccess worldIn, 阻止位置 pos, EnumFacing side)
    {
        switch (side)
        {
            case UP:
                return true;

            case NORTH:
            case SOUTH:
            case WEST:
            case EAST:
                Block block = worldIn.getBlockState(pos).getBlock();
                return !block.isOpaqueCube() && block != Blocks.farmland;

            default:
                return super.shouldSideBeRendered(worldIn, pos, side);
        }
    }

    public Item getItemDropped(IBlockState state, Random rand, int fortune)
    {
        return Blocks.dirt.getItemDropped(Blocks.dirt.getDefaultState().withProperty(BlockDirt.VARIANT, BlockDirt.DirtType.DIRT), rand, fortune);
    }

    public Item getItem(World worldIn, 阻止位置 pos)
    {
        return Item.getItemFromBlock(Blocks.dirt);
    }

    public IBlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState().withProperty(MOISTURE, Integer.valueOf(meta & 7));
    }

    public int getMetaFromState(IBlockState state)
    {
        return ((Integer)state.getValue(MOISTURE)).intValue();
    }

    protected BlockState createBlockState()
    {
        return new BlockState(this, new IProperty[] {MOISTURE});
    }
}
