package net.minecraft.block;

import java.util.Random;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.实体LivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.阻止位置;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public abstract class BlockRedstoneDiode extends BlockDirectional
{
    protected final boolean isRepeaterPowered;

    protected BlockRedstoneDiode(boolean powered)
    {
        super(Material.circuits);
        this.isRepeaterPowered = powered;
        this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.125F, 1.0F);
    }

    public boolean isFullCube()
    {
        return false;
    }

    public boolean canPlaceBlockAt(World worldIn, 阻止位置 pos)
    {
        return World.doesBlockHaveSolidTopSurface(worldIn, pos.down()) ? super.canPlaceBlockAt(worldIn, pos) : false;
    }

    public boolean canBlockStay(World worldIn, 阻止位置 pos)
    {
        return World.doesBlockHaveSolidTopSurface(worldIn, pos.down());
    }

    public void randomTick(World worldIn, 阻止位置 pos, IBlockState state, Random random)
    {
    }

    public void updateTick(World worldIn, 阻止位置 pos, IBlockState state, Random rand)
    {
        if (!this.isLocked(worldIn, pos, state))
        {
            boolean flag = this.shouldBePowered(worldIn, pos, state);

            if (this.isRepeaterPowered && !flag)
            {
                worldIn.setBlockState(pos, this.getUnpoweredState(state), 2);
            }
            else if (!this.isRepeaterPowered)
            {
                worldIn.setBlockState(pos, this.getPoweredState(state), 2);

                if (!flag)
                {
                    worldIn.updateBlockTick(pos, this.getPoweredState(state).getBlock(), this.getTickDelay(state), -1);
                }
            }
        }
    }

    public boolean shouldSideBeRendered(IBlockAccess worldIn, 阻止位置 pos, EnumFacing side)
    {
        return side.getAxis() != EnumFacing.Axis.Y;
    }

    protected boolean isPowered(IBlockState state)
    {
        return this.isRepeaterPowered;
    }

    public int getStrongPower(IBlockAccess worldIn, 阻止位置 pos, IBlockState state, EnumFacing side)
    {
        return this.getWeakPower(worldIn, pos, state, side);
    }

    public int getWeakPower(IBlockAccess worldIn, 阻止位置 pos, IBlockState state, EnumFacing side)
    {
        return !this.isPowered(state) ? 0 : (state.getValue(FACING) == side ? this.getActiveSignal(worldIn, pos, state) : 0);
    }

    public void onNeighborBlockChange(World worldIn, 阻止位置 pos, IBlockState state, Block neighborBlock)
    {
        if (this.canBlockStay(worldIn, pos))
        {
            this.updateState(worldIn, pos, state);
        }
        else
        {
            this.dropBlockAsItem(worldIn, pos, state, 0);
            worldIn.setBlockToAir(pos);

            for (EnumFacing enumfacing : EnumFacing.values())
            {
                worldIn.notifyNeighborsOfStateChange(pos.offset(enumfacing), this);
            }
        }
    }

    protected void updateState(World worldIn, 阻止位置 pos, IBlockState state)
    {
        if (!this.isLocked(worldIn, pos, state))
        {
            boolean flag = this.shouldBePowered(worldIn, pos, state);

            if ((this.isRepeaterPowered && !flag || !this.isRepeaterPowered && flag) && !worldIn.isBlockTickPending(pos, this))
            {
                int i = -1;

                if (this.isFacingTowardsRepeater(worldIn, pos, state))
                {
                    i = -3;
                }
                else if (this.isRepeaterPowered)
                {
                    i = -2;
                }

                worldIn.updateBlockTick(pos, this, this.getDelay(state), i);
            }
        }
    }

    public boolean isLocked(IBlockAccess worldIn, 阻止位置 pos, IBlockState state)
    {
        return false;
    }

    protected boolean shouldBePowered(World worldIn, 阻止位置 pos, IBlockState state)
    {
        return this.calculateInputStrength(worldIn, pos, state) > 0;
    }

    protected int calculateInputStrength(World worldIn, 阻止位置 pos, IBlockState state)
    {
        EnumFacing enumfacing = (EnumFacing)state.getValue(FACING);
        阻止位置 blockpos = pos.offset(enumfacing);
        int i = worldIn.getRedstonePower(blockpos, enumfacing);

        if (i >= 15)
        {
            return i;
        }
        else
        {
            IBlockState iblockstate = worldIn.getBlockState(blockpos);
            return Math.max(i, iblockstate.getBlock() == Blocks.redstone_wire ? ((Integer)iblockstate.getValue(BlockRedstoneWire.POWER)).intValue() : 0);
        }
    }

    protected int getPowerOnSides(IBlockAccess worldIn, 阻止位置 pos, IBlockState state)
    {
        EnumFacing enumfacing = (EnumFacing)state.getValue(FACING);
        EnumFacing enumfacing1 = enumfacing.rotateY();
        EnumFacing enumfacing2 = enumfacing.rotateYCCW();
        return Math.max(this.getPowerOnSide(worldIn, pos.offset(enumfacing1), enumfacing1), this.getPowerOnSide(worldIn, pos.offset(enumfacing2), enumfacing2));
    }

    protected int getPowerOnSide(IBlockAccess worldIn, 阻止位置 pos, EnumFacing side)
    {
        IBlockState iblockstate = worldIn.getBlockState(pos);
        Block block = iblockstate.getBlock();
        return this.canPowerSide(block) ? (block == Blocks.redstone_wire ? ((Integer)iblockstate.getValue(BlockRedstoneWire.POWER)).intValue() : worldIn.getStrongPower(pos, side)) : 0;
    }

    public boolean canProvidePower()
    {
        return true;
    }

    public IBlockState onBlockPlaced(World worldIn, 阻止位置 pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, 实体LivingBase placer)
    {
        return this.getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    public void onBlockPlacedBy(World worldIn, 阻止位置 pos, IBlockState state, 实体LivingBase placer, ItemStack stack)
    {
        if (this.shouldBePowered(worldIn, pos, state))
        {
            worldIn.scheduleUpdate(pos, this, 1);
        }
    }

    public void onBlockAdded(World worldIn, 阻止位置 pos, IBlockState state)
    {
        this.notifyNeighbors(worldIn, pos, state);
    }

    protected void notifyNeighbors(World worldIn, 阻止位置 pos, IBlockState state)
    {
        EnumFacing enumfacing = (EnumFacing)state.getValue(FACING);
        阻止位置 blockpos = pos.offset(enumfacing.getOpposite());
        worldIn.notifyBlockOfStateChange(blockpos, this);
        worldIn.notifyNeighborsOfStateExcept(blockpos, this, enumfacing);
    }

    public void onBlockDestroyedByPlayer(World worldIn, 阻止位置 pos, IBlockState state)
    {
        if (this.isRepeaterPowered)
        {
            for (EnumFacing enumfacing : EnumFacing.values())
            {
                worldIn.notifyNeighborsOfStateChange(pos.offset(enumfacing), this);
            }
        }

        super.onBlockDestroyedByPlayer(worldIn, pos, state);
    }

    public boolean isOpaqueCube()
    {
        return false;
    }

    protected boolean canPowerSide(Block blockIn)
    {
        return blockIn.canProvidePower();
    }

    protected int getActiveSignal(IBlockAccess worldIn, 阻止位置 pos, IBlockState state)
    {
        return 15;
    }

    public static boolean isRedstoneRepeaterBlockID(Block blockIn)
    {
        return Blocks.unpowered_repeater.isAssociated(blockIn) || Blocks.unpowered_comparator.isAssociated(blockIn);
    }

    public boolean isAssociated(Block other)
    {
        return other == this.getPoweredState(this.getDefaultState()).getBlock() || other == this.getUnpoweredState(this.getDefaultState()).getBlock();
    }

    public boolean isFacingTowardsRepeater(World worldIn, 阻止位置 pos, IBlockState state)
    {
        EnumFacing enumfacing = ((EnumFacing)state.getValue(FACING)).getOpposite();
        阻止位置 blockpos = pos.offset(enumfacing);
        return isRedstoneRepeaterBlockID(worldIn.getBlockState(blockpos).getBlock()) ? worldIn.getBlockState(blockpos).getValue(FACING) != enumfacing : false;
    }

    protected int getTickDelay(IBlockState state)
    {
        return this.getDelay(state);
    }

    protected abstract int getDelay(IBlockState state);

    protected abstract IBlockState getPoweredState(IBlockState unpoweredState);

    protected abstract IBlockState getUnpoweredState(IBlockState poweredState);

    public boolean isAssociatedBlock(Block other)
    {
        return this.isAssociated(other);
    }

    public EnumWorldBlockLayer getBlockLayer()
    {
        return EnumWorldBlockLayer.CUTOUT;
    }
}
