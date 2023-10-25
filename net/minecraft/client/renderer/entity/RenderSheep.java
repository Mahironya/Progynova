package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.layers.LayerSheepWool;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.util.图像位置;

public class RenderSheep extends RenderLiving<EntitySheep>
{
    private static final 图像位置 shearedSheepTextures = new 图像位置("textures/entity/sheep/sheep.png");

    public RenderSheep(RenderManager renderManagerIn, ModelBase modelBaseIn, float shadowSizeIn)
    {
        super(renderManagerIn, modelBaseIn, shadowSizeIn);
        this.addLayer(new LayerSheepWool(this));
    }

    protected 图像位置 getEntityTexture(EntitySheep entity)
    {
        return shearedSheepTextures;
    }
}
