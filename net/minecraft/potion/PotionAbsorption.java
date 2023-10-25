package net.minecraft.potion;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.attributes.BaseAttributeMap;
import net.minecraft.util.图像位置;

public class PotionAbsorption extends Potion
{
    protected PotionAbsorption(int potionID, 图像位置 location, boolean badEffect, int potionColor)
    {
        super(potionID, location, badEffect, potionColor);
    }

    public void removeAttributesModifiersFromEntity(EntityLivingBase entityLivingBaseIn, BaseAttributeMap p_111187_2_, int amplifier)
    {
        entityLivingBaseIn.setAbsorptionAmount(entityLivingBaseIn.getAbsorptionAmount() - (float)(4 * (amplifier + 1)));
        super.removeAttributesModifiersFromEntity(entityLivingBaseIn, p_111187_2_, amplifier);
    }

    public void applyAttributesModifiersToEntity(EntityLivingBase entityLivingBaseIn, BaseAttributeMap p_111185_2_, int amplifier)
    {
        entityLivingBaseIn.setAbsorptionAmount(entityLivingBaseIn.getAbsorptionAmount() + (float)(4 * (amplifier + 1)));
        super.applyAttributesModifiersToEntity(entityLivingBaseIn, p_111185_2_, amplifier);
    }
}
