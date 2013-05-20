package com.gmail.nossr50.runnables.database;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitRunnable;

import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.config.Config;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.player.PlayerProfile;
import com.gmail.nossr50.datatypes.skills.SkillType;
import com.gmail.nossr50.util.player.UserManager;

public class FormulaConversionTask extends BukkitRunnable {

    private final Map<Integer, Integer> experienceNeeded = new HashMap<Integer, Integer>(); // Experience needed to reach a level

    @Override
    public void run() {
        for (OfflinePlayer player : mcMMO.p.getServer().getOfflinePlayers()) {
            McMMOPlayer mcMMOPlayer = UserManager.getPlayer(player.getName());
            PlayerProfile profile;

            if (mcMMOPlayer == null) {
                profile = new PlayerProfile(player.getName(), false);

                editValues(profile);
                profile.save(); // Since this is a temporary profile, we save it here.
            }
            else {
                profile = mcMMOPlayer.getProfile();
                editValues(profile);
            }

        }

    }

    private void editValues(PlayerProfile profile) {
        System.out.println("==================================");
        System.out.println("Convertion report for " + profile.getPlayerName() + ":");
        for (SkillType skillType : SkillType.values()) {
            if (skillType.isChildSkill()) {
                continue;
            }

            int totalOldXP = calculateTotalExperience(profile, skillType);
            int[] newExperienceValues = calculateNewLevel(totalOldXP / 50);
            int newLevel = newExperienceValues[0];
            int newXPlevel = newExperienceValues[1];

            System.out.println("Skill: " + skillType);

            System.out.println("Old level: " + profile.getSkillLevel(skillType));
            System.out.println("Old XP " + profile.getSkillXpLevel(skillType));
            System.out.println("Total old XP " + totalOldXP);
            System.out.println("---");

            System.out.println("New level " + newLevel);
            System.out.println("New XP " + newXPlevel);
            System.out.println("---------------------------------");

            //            profile.modifySkill(skillType, newLevel);
            //            profile.setSkillXpLevel(skillType, newXPlevel);
        }

    }

    private int calculateTotalExperience(PlayerProfile profile, SkillType skillType) {
        int totalXP = 0;

        int skillLevel = profile.getSkillLevel(skillType);
        int skillXPLevel = profile.getSkillXpLevel(skillType);

        for (int i = 1; i <= skillLevel; i++) {
            totalXP += (1020 + i * Config.getInstance().getFormulaMultiplierCurve());
        }
        totalXP += skillXPLevel;

        return totalXP;
    }

    private int[] calculateNewLevel(int experience) {
        int[] newExperienceValues = new int[2];
        int newLevel = 0;
        int remainder = 0;

        for (int i = 1; i < 100; i++) {
            int experienceToNextLevel = getCachedValue(i);
            if (experience - experienceToNextLevel >= 0) {
                newLevel++;
                experience -= experienceToNextLevel;
            }
            else {
                remainder = experience;
                break;
            }
        }
        newExperienceValues[0] = newLevel;
        newExperienceValues[1] = remainder;
        return newExperienceValues;
    }

    private int getCachedValue(int i) {
        int experience;

        if (experienceNeeded.containsKey(i)) {
            experience = experienceNeeded.get(i);
        }
        else {
            experience = (int) Math.floor(3 * Math.pow(i, 1.85) + 20);
            experienceNeeded.put(i, experience);
        }
        return experience;
    }
}
