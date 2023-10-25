package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.ModelArmorStand;
import net.minecraft.client.model.ModelArmorStandArmor;
import net.minecraft.client.renderer.光照状态经理;
import net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import net.minecraft.client.renderer.entity.layers.LayerCustomHead;
import net.minecraft.client.renderer.entity.layers.LayerHeldItem;
import net.minecraft.entity.item.实体ArmorStand;
import net.minecraft.util.图像位置;

public class ArmorStandRenderer extends RendererLivingEntity<实体ArmorStand>
{
    public static final 图像位置 TEXTURE_ARMOR_STAND = new 图像位置("textures/entity/armorstand/wood.png");

    public ArmorStandRenderer(RenderManager p_i46195_1_)
    {
        super(p_i46195_1_, new ModelArmorStand(), 0.0F);
        LayerBipedArmor layerbipedarmor = new LayerBipedArmor(this)
        {
            protected void initArmor()
            {
                this.modelLeggings = new ModelArmorStandArmor(0.5F);
                this.modelArmor = new ModelArmorStandArmor(1.0F);
            }
        };
        this.addLayer(layerbipedarmor);
        this.addLayer(new LayerHeldItem(this));
        this.addLayer(new LayerCustomHead(this.getMainModel().bipedHead));
    }

    protected 图像位置 getEntityTexture(实体ArmorStand entity)
    {
        return TEXTURE_ARMOR_STAND;
    }

    public ModelArmorStand getMainModel()
    {
        return (ModelArmorStand)super.getMainModel();
    }

    protected void rotateCorpse(实体ArmorStand bat, float p_77043_2_, float p_77043_3_, float partialTicks)
    {
        光照状态经理.辐射(180.0F - p_77043_3_, 0.0F, 1.0F, 0.0F);
    }

    protected boolean canRenderName(实体ArmorStand entity)
    {
        return entity.getAlwaysRenderNameTag();
    }
}
