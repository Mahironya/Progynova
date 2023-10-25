package net.minecraft.entity;

import net.minecraft.block.Block;
import net.minecraft.util.阻止位置;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

public abstract class 实体Flying extends 实体Living
{
    public 实体Flying(World worldIn)
    {
        super(worldIn);
    }

    public void fall(float distance, float damageMultiplier)
    {
    }

    protected void updateFallState(double y, boolean onGroundIn, Block blockIn, 阻止位置 pos)
    {
    }

    public void moveEntityWithHeading(float strafe, float forward)
    {
        if (this.isInWater())
        {
            this.moveFlying(strafe, forward, 0.02F);
            this.moveEntity(this.通便X, this.motionY, this.通便Z);
            this.通便X *= 0.800000011920929D;
            this.motionY *= 0.800000011920929D;
            this.通便Z *= 0.800000011920929D;
        }
        else if (this.isInLava())
        {
            this.moveFlying(strafe, forward, 0.02F);
            this.moveEntity(this.通便X, this.motionY, this.通便Z);
            this.通便X *= 0.5D;
            this.motionY *= 0.5D;
            this.通便Z *= 0.5D;
        }
        else
        {
            float f = 0.91F;

            if (this.onGround)
            {
                f = this.worldObj.getBlockState(new 阻止位置(MathHelper.floor_double(this.X坐标), MathHelper.floor_double(this.getEntityBoundingBox().minY) - 1, MathHelper.floor_double(this.Z坐标))).getBlock().slipperiness * 0.91F;
            }

            float f1 = 0.16277136F / (f * f * f);
            this.moveFlying(strafe, forward, this.onGround ? 0.1F * f1 : 0.02F);
            f = 0.91F;

            if (this.onGround)
            {
                f = this.worldObj.getBlockState(new 阻止位置(MathHelper.floor_double(this.X坐标), MathHelper.floor_double(this.getEntityBoundingBox().minY) - 1, MathHelper.floor_double(this.Z坐标))).getBlock().slipperiness * 0.91F;
            }

            this.moveEntity(this.通便X, this.motionY, this.通便Z);
            this.通便X *= (double)f;
            this.motionY *= (double)f;
            this.通便Z *= (double)f;
        }

        this.prevLimbSwingAmount = this.limbSwingAmount;
        double d1 = this.X坐标 - this.prevPosX;
        double d0 = this.Z坐标 - this.prevPosZ;
        float f2 = MathHelper.sqrt_double(d1 * d1 + d0 * d0) * 4.0F;

        if (f2 > 1.0F)
        {
            f2 = 1.0F;
        }

        this.limbSwingAmount += (f2 - this.limbSwingAmount) * 0.4F;
        this.limbSwing += this.limbSwingAmount;
    }

    public boolean isOnLadder()
    {
        return false;
    }
}
