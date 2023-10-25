package net.minecraft.scoreboard;

import java.util.List;
import net.minecraft.entity.player.实体Player;
import net.minecraft.util.MathHelper;

public class ScoreHealthCriteria extends ScoreDummyCriteria
{
    public ScoreHealthCriteria(String name)
    {
        super(name);
    }

    public int setScore(List<实体Player> p_96635_1_)
    {
        float f = 0.0F;

        for (实体Player entityplayer : p_96635_1_)
        {
            f += entityplayer.getHealth() + entityplayer.getAbsorptionAmount();
        }

        if (p_96635_1_.size() > 0)
        {
            f /= (float)p_96635_1_.size();
        }

        return MathHelper.ceiling_float_int(f);
    }

    public boolean isReadOnly()
    {
        return true;
    }

    public IScoreObjectiveCriteria.EnumRenderType getRenderType()
    {
        return IScoreObjectiveCriteria.EnumRenderType.HEARTS;
    }
}
