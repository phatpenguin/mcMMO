package com.gmail.nossr50.skills.excavation;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.block.BlockState;

import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.skills.SkillType;
import com.gmail.nossr50.datatypes.treasure.ExcavationTreasure;
import com.gmail.nossr50.skills.SkillManager;
import com.gmail.nossr50.util.Misc;
import com.gmail.nossr50.util.Permissions;
import com.gmail.nossr50.util.skills.SkillUtils;

public class ExcavationManager extends SkillManager {
    public ExcavationManager(McMMOPlayer mcMMOPlayer) {
        super(mcMMOPlayer, SkillType.EXCAVATION);
    }

    /**
     * Process treasure drops & XP gain for Excavation.
     *
     * @param blockState The {@link BlockState} to check ability activation for
     */
    public void excavationBlockCheck(BlockState blockState) {
        int xp = Excavation.getBlockXP(blockState);
        mcMMO.p.debug("[Award XP] " + getPlayer().getName() + " " + SkillType.EXCAVATION + " BLOCK_BREAK " + blockState.getBlock().getType());

        if (Permissions.excavationTreasureHunter(getPlayer())) {
            List<ExcavationTreasure> treasures = Excavation.getTreasures(blockState);

            if (!treasures.isEmpty()) {
                int skillLevel = getSkillLevel();
                Location location = blockState.getLocation();

                for (ExcavationTreasure treasure : treasures) {
                    if (skillLevel >= treasure.getDropLevel() && SkillUtils.treasureDropSuccessful(treasure.getDropChance(), activationChance)) {
                        xp += treasure.getXp();
                        mcMMO.p.debug("[Award XP] " + getPlayer().getName() + " " + SkillType.EXCAVATION + " " + blockState.getBlock().getType() + " TREASURE = " + treasure.getDrop().getType());
                        Misc.dropItem(location, treasure.getDrop());
                    }
                }
            }
        }

        applyXpGain(xp);
    }

    /**
     * Process the Giga Drill Breaker ability.
     *
     * @param blockState The {@link BlockState} to check ability activation for
     */
    public void gigaDrillBreaker(BlockState blockState) {
        excavationBlockCheck(blockState);
        excavationBlockCheck(blockState);
    }
}
