package net.minecraft.block;

import java.util.Random;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.实体Player;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.阻止位置;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockSign extends BlockContainer
{
    protected BlockSign()
    {
        super(Material.wood);
        float f = 0.25F;
        float f1 = 1.0F;
        this.setBlockBounds(0.5F - f, 0.0F, 0.5F - f, 0.5F + f, f1, 0.5F + f);
    }

    public AxisAlignedBB getCollisionBoundingBox(World worldIn, 阻止位置 pos, IBlockState state)
    {
        return null;
    }

    public AxisAlignedBB getSelectedBoundingBox(World worldIn, 阻止位置 pos)
    {
        this.setBlockBoundsBasedOnState(worldIn, pos);
        return super.getSelectedBoundingBox(worldIn, pos);
    }

    public boolean isFullCube()
    {
        return false;
    }

    public boolean isPassable(IBlockAccess worldIn, 阻止位置 pos)
    {
        return true;
    }

    public boolean isOpaqueCube()
    {
        return false;
    }

    public boolean canSpawnInBlock()
    {
        return true;
    }

    public TileEntity createNewTileEntity(World worldIn, int meta)
    {
        return new TileEntitySign();
    }

    public Item getItemDropped(IBlockState state, Random rand, int fortune)
    {
        return Items.sign;
    }

    public Item getItem(World worldIn, 阻止位置 pos)
    {
        return Items.sign;
    }

    public boolean onBlockActivated(World worldIn, 阻止位置 pos, IBlockState state, 实体Player playerIn, EnumFacing side, float hitX, float hitY, float hitZ)
    {
        if (worldIn.isRemote)
        {
            return true;
        }
        else
        {
            TileEntity tileentity = worldIn.getTileEntity(pos);
            return tileentity instanceof TileEntitySign ? ((TileEntitySign)tileentity).executeCommand(playerIn) : false;
        }
    }

    public boolean canPlaceBlockAt(World worldIn, 阻止位置 pos)
    {
        return !this.hasInvalidNeighbor(worldIn, pos) && super.canPlaceBlockAt(worldIn, pos);
    }
}
