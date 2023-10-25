package net.optifine.entity.model;

import net.minecraft.client.我的手艺;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelSnowMan;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderSnowMan;
import net.minecraft.entity.monster.实体Snowman;

public class ModelAdapterSnowman extends ModelAdapter
{
    public ModelAdapterSnowman()
    {
        super(实体Snowman.class, "snow_golem", 0.5F);
    }

    public ModelBase makeModel()
    {
        return new ModelSnowMan();
    }

    public ModelRenderer getModelRenderer(ModelBase model, String modelPart)
    {
        if (!(model instanceof ModelSnowMan))
        {
            return null;
        }
        else
        {
            ModelSnowMan modelsnowman = (ModelSnowMan)model;
            return modelPart.equals("body") ? modelsnowman.body : (modelPart.equals("body_bottom") ? modelsnowman.bottomBody : (modelPart.equals("head") ? modelsnowman.head : (modelPart.equals("left_hand") ? modelsnowman.leftHand : (modelPart.equals("right_hand") ? modelsnowman.rightHand : null))));
        }
    }

    public String[] getModelRendererNames()
    {
        return new String[] {"body", "body_bottom", "head", "right_hand", "left_hand"};
    }

    public IEntityRenderer makeEntityRender(ModelBase modelBase, float shadowSize)
    {
        RenderManager rendermanager = 我的手艺.得到我的手艺().getRenderManager();
        RenderSnowMan rendersnowman = new RenderSnowMan(rendermanager);
        rendersnowman.mainModel = modelBase;
        rendersnowman.shadowSize = shadowSize;
        return rendersnowman;
    }
}
