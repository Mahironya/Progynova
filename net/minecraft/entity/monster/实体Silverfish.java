package net.minecraft.entity.monster;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSilverfish;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackOnCollide;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.player.实体Player;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.阻止位置;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

public class 实体Silverfish extends 实体Mob
{
    private 实体Silverfish.AISummonSilverfish summonSilverfish;

    public 实体Silverfish(World worldIn)
    {
        super(worldIn);
        this.setSize(0.4F, 0.3F);
        this.tasks.addTask(1, new EntityAISwimming(this));
        this.tasks.addTask(3, this.summonSilverfish = new 实体Silverfish.AISummonSilverfish(this));
        this.tasks.addTask(4, new EntityAIAttackOnCollide(this, 实体Player.class, 1.0D, false));
        this.tasks.addTask(5, new 实体Silverfish.AIHideInStone(this));
        this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, true, new Class[0]));
        this.targetTasks.addTask(2, new EntityAINearestAttackableTarget(this, 实体Player.class, true));
    }

    public double getYOffset()
    {
        return 0.2D;
    }

    public float getEyeHeight()
    {
        return 0.1F;
    }

    protected void applyEntityAttributes()
    {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(8.0D);
        this.getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.25D);
        this.getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(1.0D);
    }

    protected boolean canTriggerWalking()
    {
        return false;
    }

    protected String getLivingSound()
    {
        return "mob.silverfish.say";
    }

    protected String getHurtSound()
    {
        return "mob.silverfish.hit";
    }

    protected String getDeathSound()
    {
        return "mob.silverfish.kill";
    }

    public boolean attackEntityFrom(DamageSource source, float amount)
    {
        if (this.isEntityInvulnerable(source))
        {
            return false;
        }
        else
        {
            if (source instanceof EntityDamageSource || source == DamageSource.magic)
            {
                this.summonSilverfish.func_179462_f();
            }

            return super.attackEntityFrom(source, amount);
        }
    }

    protected void playStepSound(阻止位置 pos, Block blockIn)
    {
        this.playSound("mob.silverfish.step", 0.15F, 1.0F);
    }

    protected Item getDropItem()
    {
        return null;
    }

    public void onUpdate()
    {
        this.renderYawOffset = this.旋转侧滑;
        super.onUpdate();
    }

    public float getBlockPathWeight(阻止位置 pos)
    {
        return this.worldObj.getBlockState(pos.down()).getBlock() == Blocks.stone ? 10.0F : super.getBlockPathWeight(pos);
    }

    protected boolean isValidLightLevel()
    {
        return true;
    }

    public boolean getCanSpawnHere()
    {
        if (super.getCanSpawnHere())
        {
            实体Player entityplayer = this.worldObj.getClosestPlayerToEntity(this, 5.0D);
            return entityplayer == null;
        }
        else
        {
            return false;
        }
    }

    public EnumCreatureAttribute getCreatureAttribute()
    {
        return EnumCreatureAttribute.ARTHROPOD;
    }

    static class AIHideInStone extends EntityAIWander
    {
        private final 实体Silverfish silverfish;
        private EnumFacing facing;
        private boolean field_179484_c;

        public AIHideInStone(实体Silverfish silverfishIn)
        {
            super(silverfishIn, 1.0D, 10);
            this.silverfish = silverfishIn;
            this.setMutexBits(1);
        }

        public boolean shouldExecute()
        {
            if (this.silverfish.getAttackTarget() != null)
            {
                return false;
            }
            else if (!this.silverfish.getNavigator().noPath())
            {
                return false;
            }
            else
            {
                Random random = this.silverfish.getRNG();

                if (random.nextInt(10) == 0)
                {
                    this.facing = EnumFacing.random(random);
                    阻止位置 blockpos = (new 阻止位置(this.silverfish.X坐标, this.silverfish.Y坐标 + 0.5D, this.silverfish.Z坐标)).offset(this.facing);
                    IBlockState iblockstate = this.silverfish.worldObj.getBlockState(blockpos);

                    if (BlockSilverfish.canContainSilverfish(iblockstate))
                    {
                        this.field_179484_c = true;
                        return true;
                    }
                }

                this.field_179484_c = false;
                return super.shouldExecute();
            }
        }

        public boolean continueExecuting()
        {
            return this.field_179484_c ? false : super.continueExecuting();
        }

        public void startExecuting()
        {
            if (!this.field_179484_c)
            {
                super.startExecuting();
            }
            else
            {
                World world = this.silverfish.worldObj;
                阻止位置 blockpos = (new 阻止位置(this.silverfish.X坐标, this.silverfish.Y坐标 + 0.5D, this.silverfish.Z坐标)).offset(this.facing);
                IBlockState iblockstate = world.getBlockState(blockpos);

                if (BlockSilverfish.canContainSilverfish(iblockstate))
                {
                    world.setBlockState(blockpos, Blocks.monster_egg.getDefaultState().withProperty(BlockSilverfish.VARIANT, BlockSilverfish.EnumType.forModelBlock(iblockstate)), 3);
                    this.silverfish.spawnExplosionParticle();
                    this.silverfish.setDead();
                }
            }
        }
    }

    static class AISummonSilverfish extends EntityAIBase
    {
        private 实体Silverfish silverfish;
        private int field_179463_b;

        public AISummonSilverfish(实体Silverfish silverfishIn)
        {
            this.silverfish = silverfishIn;
        }

        public void func_179462_f()
        {
            if (this.field_179463_b == 0)
            {
                this.field_179463_b = 20;
            }
        }

        public boolean shouldExecute()
        {
            return this.field_179463_b > 0;
        }

        public void updateTask()
        {
            --this.field_179463_b;

            if (this.field_179463_b <= 0)
            {
                World world = this.silverfish.worldObj;
                Random random = this.silverfish.getRNG();
                阻止位置 blockpos = new 阻止位置(this.silverfish);

                for (int i = 0; i <= 5 && i >= -5; i = i <= 0 ? 1 - i : 0 - i)
                {
                    for (int j = 0; j <= 10 && j >= -10; j = j <= 0 ? 1 - j : 0 - j)
                    {
                        for (int k = 0; k <= 10 && k >= -10; k = k <= 0 ? 1 - k : 0 - k)
                        {
                            阻止位置 blockpos1 = blockpos.add(j, i, k);
                            IBlockState iblockstate = world.getBlockState(blockpos1);

                            if (iblockstate.getBlock() == Blocks.monster_egg)
                            {
                                if (world.getGameRules().getBoolean("mobGriefing"))
                                {
                                    world.destroyBlock(blockpos1, true);
                                }
                                else
                                {
                                    world.setBlockState(blockpos1, ((BlockSilverfish.EnumType)iblockstate.getValue(BlockSilverfish.VARIANT)).getModelBlock(), 3);
                                }

                                if (random.nextBoolean())
                                {
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
