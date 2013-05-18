package com.gmail.nossr50.runnables.skills;

import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.skills.SkillType;

public class AwardCombatXpTask extends BukkitRunnable {
    private McMMOPlayer mcMMOPlayer;
    private double baseXp;
    private SkillType skillType;
    private LivingEntity target;
    private int baseHealth;

    public AwardCombatXpTask(McMMOPlayer mcMMOPlayer, SkillType skillType, double baseXp, LivingEntity target) {
        this.mcMMOPlayer = mcMMOPlayer;
        this.skillType = skillType;
        this.baseXp = baseXp;
        this.target = target;
        baseHealth = target.getHealth();
    }

    @Override
    public void run() {
        int health = target.getHealth();
        int damage = baseHealth - health;
        mcMMO.p.debug("[Award XP] " + mcMMOPlayer.getPlayer().getName() + " " + skillType + " " + target.getType() + " damage = " + damage);

        // May avoid negative xp, we don't know what other plugins do with the entity health
        if (damage <= 0) {
            return;
        }

        // Don't reward the player for overkills
        if (health < 0) {
            damage += health;
        }

        mcMMOPlayer.beginXpGain(skillType, (int) (damage * baseXp));
    }
}
