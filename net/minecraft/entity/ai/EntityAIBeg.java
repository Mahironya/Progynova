package net.minecraft.entity.ai;

import net.minecraft.entity.passive.实体Wolf;
import net.minecraft.entity.player.实体Player;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class EntityAIBeg extends EntityAIBase
{
    private 实体Wolf theWolf;
    private 实体Player thePlayer;
    private World worldObject;
    private float minPlayerDistance;
    private int timeoutCounter;

    public EntityAIBeg(实体Wolf wolf, float minDistance)
    {
        this.theWolf = wolf;
        this.worldObject = wolf.worldObj;
        this.minPlayerDistance = minDistance;
        this.setMutexBits(2);
    }

    public boolean shouldExecute()
    {
        this.thePlayer = this.worldObject.getClosestPlayerToEntity(this.theWolf, (double)this.minPlayerDistance);
        return this.thePlayer == null ? false : this.hasPlayerGotBoneInHand(this.thePlayer);
    }

    public boolean continueExecuting()
    {
        return !this.thePlayer.isEntityAlive() ? false : (this.theWolf.getDistanceSqToEntity(this.thePlayer) > (double)(this.minPlayerDistance * this.minPlayerDistance) ? false : this.timeoutCounter > 0 && this.hasPlayerGotBoneInHand(this.thePlayer));
    }

    public void startExecuting()
    {
        this.theWolf.setBegging(true);
        this.timeoutCounter = 40 + this.theWolf.getRNG().nextInt(40);
    }

    public void resetTask()
    {
        this.theWolf.setBegging(false);
        this.thePlayer = null;
    }

    public void updateTask()
    {
        this.theWolf.getLookHelper().setLookPosition(this.thePlayer.X坐标, this.thePlayer.Y坐标 + (double)this.thePlayer.getEyeHeight(), this.thePlayer.Z坐标, 10.0F, (float)this.theWolf.getVerticalFaceSpeed());
        --this.timeoutCounter;
    }

    private boolean hasPlayerGotBoneInHand(实体Player player)
    {
        ItemStack itemstack = player.inventory.getCurrentItem();
        return itemstack == null ? false : (!this.theWolf.isTamed() && itemstack.getItem() == Items.bone ? true : this.theWolf.isBreedingItem(itemstack));
    }
}
