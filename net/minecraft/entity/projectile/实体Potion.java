package net.minecraft.entity.projectile;

import java.util.List;
import net.minecraft.entity.实体LivingBase;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.阻止位置;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class 实体Potion extends 实体Throwable
{
    private ItemStack potionDamage;

    public 实体Potion(World worldIn)
    {
        super(worldIn);
    }

    public 实体Potion(World worldIn, 实体LivingBase throwerIn, int meta)
    {
        this(worldIn, throwerIn, new ItemStack(Items.potionitem, 1, meta));
    }

    public 实体Potion(World worldIn, 实体LivingBase throwerIn, ItemStack potionDamageIn)
    {
        super(worldIn, throwerIn);
        this.potionDamage = potionDamageIn;
    }

    public 实体Potion(World worldIn, double x, double y, double z, int p_i1791_8_)
    {
        this(worldIn, x, y, z, new ItemStack(Items.potionitem, 1, p_i1791_8_));
    }

    public 实体Potion(World worldIn, double x, double y, double z, ItemStack potionDamageIn)
    {
        super(worldIn, x, y, z);
        this.potionDamage = potionDamageIn;
    }

    protected float getGravityVelocity()
    {
        return 0.05F;
    }

    protected float getVelocity()
    {
        return 0.5F;
    }

    protected float getInaccuracy()
    {
        return -20.0F;
    }

    public void setPotionDamage(int potionId)
    {
        if (this.potionDamage == null)
        {
            this.potionDamage = new ItemStack(Items.potionitem, 1, 0);
        }

        this.potionDamage.setItemDamage(potionId);
    }

    public int getPotionDamage()
    {
        if (this.potionDamage == null)
        {
            this.potionDamage = new ItemStack(Items.potionitem, 1, 0);
        }

        return this.potionDamage.getMetadata();
    }

    protected void onImpact(MovingObjectPosition p_70184_1_)
    {
        if (!this.worldObj.isRemote)
        {
            List<PotionEffect> list = Items.potionitem.getEffects(this.potionDamage);

            if (list != null && !list.isEmpty())
            {
                AxisAlignedBB axisalignedbb = this.getEntityBoundingBox().expand(4.0D, 2.0D, 4.0D);
                List<实体LivingBase> list1 = this.worldObj.<实体LivingBase>getEntitiesWithinAABB(实体LivingBase.class, axisalignedbb);

                if (!list1.isEmpty())
                {
                    for (实体LivingBase entitylivingbase : list1)
                    {
                        double d0 = this.getDistanceSqToEntity(entitylivingbase);

                        if (d0 < 16.0D)
                        {
                            double d1 = 1.0D - Math.sqrt(d0) / 4.0D;

                            if (entitylivingbase == p_70184_1_.实体Hit)
                            {
                                d1 = 1.0D;
                            }

                            for (PotionEffect potioneffect : list)
                            {
                                int i = potioneffect.getPotionID();

                                if (Potion.potionTypes[i].isInstant())
                                {
                                    Potion.potionTypes[i].affectEntity(this, this.getThrower(), entitylivingbase, potioneffect.getAmplifier(), d1);
                                }
                                else
                                {
                                    int j = (int)(d1 * (double)potioneffect.getDuration() + 0.5D);

                                    if (j > 20)
                                    {
                                        entitylivingbase.addPotionEffect(new PotionEffect(i, j, potioneffect.getAmplifier()));
                                    }
                                }
                            }
                        }
                    }
                }
            }

            this.worldObj.playAuxSFX(2002, new 阻止位置(this), this.getPotionDamage());
            this.setDead();
        }
    }

    public void readEntityFromNBT(NBTTagCompound tagCompund)
    {
        super.readEntityFromNBT(tagCompund);

        if (tagCompund.hasKey("Potion", 10))
        {
            this.potionDamage = ItemStack.loadItemStackFromNBT(tagCompund.getCompoundTag("Potion"));
        }
        else
        {
            this.setPotionDamage(tagCompund.getInteger("potionValue"));
        }

        if (this.potionDamage == null)
        {
            this.setDead();
        }
    }

    public void writeEntityToNBT(NBTTagCompound tagCompound)
    {
        super.writeEntityToNBT(tagCompound);

        if (this.potionDamage != null)
        {
            tagCompound.setTag("Potion", this.potionDamage.writeToNBT(new NBTTagCompound()));
        }
    }
}
