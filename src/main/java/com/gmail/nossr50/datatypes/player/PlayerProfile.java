package com.gmail.nossr50.datatypes.player;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.scoreboard.Scoreboard;

import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.config.Config;
import com.gmail.nossr50.config.spout.SpoutConfig;
import com.gmail.nossr50.database.SQLDatabaseManager;
import com.gmail.nossr50.datatypes.MobHealthbarType;
import com.gmail.nossr50.datatypes.skills.AbilityType;
import com.gmail.nossr50.datatypes.skills.SkillType;
import com.gmail.nossr50.datatypes.spout.huds.HudType;
import com.gmail.nossr50.datatypes.spout.huds.McMMOHud;
import com.gmail.nossr50.skills.child.FamilyTree;
import com.gmail.nossr50.util.Misc;

public class PlayerProfile {
    private final String playerName;
    private int userId;
    private boolean loaded;

    /* HUDs */
    private HudType hudType;
    private MobHealthbarType mobHealthbarType;
    private McMMOHud spoutHud;
    private Scoreboard playerStatsScoreboard;

    /* Skill Data */
    private final Map<SkillType, Integer>   skills     = new HashMap<SkillType, Integer>();   // Skill & Level
    private final Map<SkillType, Float>     skillsXp   = new HashMap<SkillType, Float>();   // Skill & XP
    private final Map<AbilityType, Integer> skillsDATS = new HashMap<AbilityType, Integer>(); // Ability & Cooldown

    public PlayerProfile(String playerName, boolean addNew) {
        this.playerName = playerName;

        hudType = mcMMO.isSpoutEnabled() ? SpoutConfig.getInstance().getDefaultHudType() : HudType.DISABLED;
        mobHealthbarType = Config.getInstance().getMobHealthbarDefault();

        for (AbilityType abilityType : AbilityType.values()) {
            skillsDATS.put(abilityType, 0);
        }

        for (SkillType skillType : SkillType.values()) {
            if (skillType.isChildSkill()) {
                continue;
            }

            skills.put(skillType, 0);
            skillsXp.put(skillType, 0F);
        }

        if (!loadPlayer() && addNew) {
            addPlayer();
            loaded = true;
        }
    }

    public void save() {
        if (Config.getInstance().getUseMySQL()) {
            saveMySQL();
        }
        else {
            saveFlatfile();
        }
    }

    public String getPlayerName() {
        return playerName;
    }

    public boolean isLoaded() {
        return loaded;
    }

    /*
     * HUD Stuff
     */

    public HudType getHudType() {
        return hudType;
    }

    public McMMOHud getSpoutHud() {
        return spoutHud;
    }

    public void setSpoutHud(McMMOHud spoutHud) {
        this.spoutHud = spoutHud;
    }

    public void setHudType(HudType hudType) {
        this.hudType = hudType;
    }

    /*
     * Mob Healthbars
     */

    public MobHealthbarType getMobHealthbarType() {
        return mobHealthbarType;
    }

    public void setMobHealthbarType(MobHealthbarType mobHealthbarType) {
        this.mobHealthbarType = mobHealthbarType;
    }

    /*
     * Scoreboards
     */

    public Scoreboard getPlayerStatsScoreboard() {
        return playerStatsScoreboard;
    }

    public void setPlayerStatsScoreboard(Scoreboard statsScoreboard) {
        this.playerStatsScoreboard = statsScoreboard;
    }

    /*
     * Cooldowns
     */

    /**
     * Get the current DATS of a skill.
     *
     * @param abilityType Ability to get the DATS for
     * @return the DATS for the ability
     */
    public long getSkillDATS(AbilityType abilityType) {
        return skillsDATS.get(abilityType);
    }

    /**
     * Set the current DATS of a skill.
     *
     * @param abilityType Ability to set the DATS for
     * @param DATS the DATS of the ability
     */
    public void setSkillDATS(AbilityType abilityType, long DATS) {
        int wearsOff = (int) (DATS * .001D);

        skillsDATS.put(abilityType, wearsOff);
    }

    /**
     * Reset all skill cooldowns.
     */
    public void resetCooldowns() {
        for (AbilityType ability : skillsDATS.keySet()) {
            skillsDATS.put(ability, 0);
        }
    }

    /*
     * Xp Functions
     */

    public int getSkillLevel(SkillType skillType) {
        if (skillType.isChildSkill()) {
            return getChildSkillLevel(skillType);
        }

        return skills.get(skillType);
    }

    public float getSkillXpLevelRaw(SkillType skillType) {
        return skillsXp.get(skillType);
    }

    public int getSkillXpLevel(SkillType skillType) {
        return (int) Math.floor(getSkillXpLevelRaw(skillType));
    }

    public void setSkillXpLevel(SkillType skillType, float newValue) {
        if (skillType.isChildSkill()) {
            return;
        }

        skillsXp.put(skillType, newValue);
    }

    public void skillUp(SkillType skillType, int newValue) {
        skills.put(skillType, skills.get(skillType) + newValue);
    }

    /**
     * Remove Xp from a skill.
     *
     * @param skillType Type of skill to modify
     * @param xp Amount of xp to remove
     */
    public void removeXp(SkillType skillType, int xp) {
        if (skillType.isChildSkill()) {
            return;
        }

        skillsXp.put(skillType, skillsXp.get(skillType) - xp);
    }

    /**
     * Modify a skill level.
     *
     * @param skillType Type of skill to modify
     * @param newValue New level value for the skill
     */
    public void modifySkill(SkillType skillType, int newValue) {
        if (skillType.isChildSkill()) {
            return;
        }

        skills.put(skillType, newValue);
        skillsXp.put(skillType, 0F);
    }

    /**
     * Add levels to a skill.
     *
     * @param skillType Type of skill to add levels to
     * @param levels Number of levels to add
     */
    public void addLevels(SkillType skillType, int levels) {
        if (skillType.isChildSkill()) {
            return;
        }

        skills.put(skillType, skills.get(skillType) + levels);
        skillsXp.put(skillType, 0F);
    }

    /**
     * Add Experience to a skill.
     *
     * @param skillType Type of skill to add experience to
     * @param experience Number of experience to add
     */
    public void addExperience(SkillType skillType, int experience) {
        if (skillType.isChildSkill()) {
            return;
        }

        skillsXp.put(skillType, skillsXp.get(skillType) + experience);
    }

    /**
     * Get the amount of Xp remaining before the next level.
     *
     * @param skillType Type of skill to check
     * @return the Xp remaining until next level
     */
    public int getXpToLevel(SkillType skillType) {
        return 1020 + (skills.get(skillType) *  Config.getInstance().getFormulaMultiplierCurve());
    }

    private int getChildSkillLevel(SkillType skillType) {
        Set<SkillType> parents = FamilyTree.getParents(skillType);
        int sum = 0;

        for (SkillType parent : parents) {
            sum += Math.min(getSkillLevel(parent), 1000);
        }

        return sum / parents.size();
    }

    private boolean loadPlayer() {
        return Config.getInstance().getUseMySQL() ? loadMySQL() : loadFlatfile();
    }

    private void addPlayer() {
        if (Config.getInstance().getUseMySQL()) {
            addMySQLPlayer();
        }
        else {
            addFlatfilePlayer();
        }
    }

    private boolean loadMySQL() {
        ArrayList<String> playerData = SQLDatabaseManager.read(
                "SELECT " +
                "u.id, " +
                "s.taming, s.mining, s.repair, s.woodcutting, s.unarmed, s.herbalism, s.excavation, s.archery, s.swords, s.axes, s.acrobatics, s.fishing, " +
                "e.taming, e.mining, e.repair, e.woodcutting, e.unarmed, e.herbalism, e.excavation, e.archery, e.swords, e.axes, e.acrobatics, e.fishing, " +
                "c.taming, c.mining, c.repair, c.woodcutting, c.unarmed, c.herbalism, c.excavation, c.archery, c.swords, c.axes, c.acrobatics, c.blast_mining, " +
                "h.hudtype, h.mobhealthbar " +
                "FROM " + tablePrefix + "users u " +
                "JOIN " + tablePrefix + "skills s ON (u.id = s.user_id) " +
                "JOIN " + tablePrefix + "experience e ON (u.id = e.user_id) " +
                "JOIN " + tablePrefix + "cooldowns c ON (u.id = c.user_id) " +
                "JOIN " + tablePrefix + "huds h ON (u.id = h.user_id) " +
                "WHERE u.user = '" + playerName + "'"
                ).get(1);

        if (playerData == null || playerData.size() == 0) {
            userId = SQLDatabaseManager.getInt("SELECT id FROM " + tablePrefix + "users WHERE user = '" + playerName + "'");

            // Check if user doesn't exist
            if (userId == 0) {
                return false;
            }

            // Write missing table rows
            SQLDatabaseManager.write("INSERT IGNORE INTO " + tablePrefix + "skills (user_id) VALUES (" + userId + ")");
            SQLDatabaseManager.write("INSERT IGNORE INTO " + tablePrefix + "experience (user_id) VALUES (" + userId + ")");
            SQLDatabaseManager.write("INSERT IGNORE INTO " + tablePrefix + "cooldowns (user_id) VALUES (" + userId + ")");
            SQLDatabaseManager.write("INSERT IGNORE INTO " + tablePrefix + "huds (user_id, mobhealthbar) VALUES (" + userId + ",'" + mobHealthbarType.name() + "')");

            // Re-read data
            playerData = SQLDatabaseManager.read(
                    "SELECT " +
                    "u.id, " +
                    "s.taming, s.mining, s.repair, s.woodcutting, s.unarmed, s.herbalism, s.excavation, s.archery, s.swords, s.axes, s.acrobatics, s.fishing, " +
                    "e.taming, e.mining, e.repair, e.woodcutting, e.unarmed, e.herbalism, e.excavation, e.archery, e.swords, e.axes, e.acrobatics, e.fishing, " +
                    "c.taming, c.mining, c.repair, c.woodcutting, c.unarmed, c.herbalism, c.excavation, c.archery, c.swords, c.axes, c.acrobatics, c.blast_mining, " +
                    "h.hudtype, h.mobhealthbar " +
                    "FROM " + tablePrefix + "users u " +
                    "JOIN " + tablePrefix + "skills s ON (u.id = s.user_id) " +
                    "JOIN " + tablePrefix + "experience e ON (u.id = e.user_id) " +
                    "JOIN " + tablePrefix + "cooldowns c ON (u.id = c.user_id) " +
                    "JOIN " + tablePrefix + "huds h ON (u.id = h.user_id) " +
                    "WHERE u.user = '" + playerName + "'"
                    ).get(1);

            // Should never happen but just in case
            if (playerData == null || playerData.size() == 0) {
                return false;
            }
        }

        }

        skills.put(SkillType.TAMING, Integer.valueOf(playerData.get(1)));
        skills.put(SkillType.MINING, Integer.valueOf(playerData.get(2)));
        skills.put(SkillType.REPAIR, Integer.valueOf(playerData.get(3)));
        skills.put(SkillType.WOODCUTTING, Integer.valueOf(playerData.get(4)));
        skills.put(SkillType.UNARMED, Integer.valueOf(playerData.get(5)));
        skills.put(SkillType.HERBALISM, Integer.valueOf(playerData.get(6)));
        skills.put(SkillType.EXCAVATION, Integer.valueOf(playerData.get(7)));
        skills.put(SkillType.ARCHERY, Integer.valueOf(playerData.get(8)));
        skills.put(SkillType.SWORDS, Integer.valueOf(playerData.get(9)));
        skills.put(SkillType.AXES, Integer.valueOf(playerData.get(10)));
        skills.put(SkillType.ACROBATICS, Integer.valueOf(playerData.get(11)));
        skills.put(SkillType.FISHING, Integer.valueOf(playerData.get(12)));

        skillsXp.put(SkillType.TAMING, (float) Integer.valueOf(playerData.get(13)));
        skillsXp.put(SkillType.MINING, (float) Integer.valueOf(playerData.get(14)));
        skillsXp.put(SkillType.REPAIR, (float) Integer.valueOf(playerData.get(15)));
        skillsXp.put(SkillType.WOODCUTTING, (float) Integer.valueOf(playerData.get(16)));
        skillsXp.put(SkillType.UNARMED, (float) Integer.valueOf(playerData.get(17)));
        skillsXp.put(SkillType.HERBALISM, (float) Integer.valueOf(playerData.get(18)));
        skillsXp.put(SkillType.EXCAVATION, (float) Integer.valueOf(playerData.get(19)));
        skillsXp.put(SkillType.ARCHERY, (float) Integer.valueOf(playerData.get(20)));
        skillsXp.put(SkillType.SWORDS, (float) Integer.valueOf(playerData.get(21)));
        skillsXp.put(SkillType.AXES, (float) Integer.valueOf(playerData.get(22)));
        skillsXp.put(SkillType.ACROBATICS, (float) Integer.valueOf(playerData.get(23)));
        skillsXp.put(SkillType.FISHING, (float) Integer.valueOf(playerData.get(24)));

        // Taming 25 - Unused
        skillsDATS.put(AbilityType.SUPER_BREAKER, Integer.valueOf(playerData.get(26)));
        // Repair 27 - Unused
        skillsDATS.put(AbilityType.TREE_FELLER, Integer.valueOf(playerData.get(28)));
        skillsDATS.put(AbilityType.BERSERK, Integer.valueOf(playerData.get(29)));
        skillsDATS.put(AbilityType.GREEN_TERRA, Integer.valueOf(playerData.get(30)));
        skillsDATS.put(AbilityType.GIGA_DRILL_BREAKER, Integer.valueOf(playerData.get(31)));
        // Archery 32 - Unused
        skillsDATS.put(AbilityType.SERRATED_STRIKES, Integer.valueOf(playerData.get(33)));
        skillsDATS.put(AbilityType.SKULL_SPLITTER, Integer.valueOf(playerData.get(34)));
        // Acrobatics 35 - Unused
        skillsDATS.put(AbilityType.BLAST_MINING, Integer.valueOf(playerData.get(36)));

        try {
            hudType = HudType.valueOf(playerData.get(37));
        }
        catch (Exception e) {
            // Shouldn't happen unless database is being tampered with
            hudType = HudType.STANDARD;
        }

        try {
            mobHealthbarType = MobHealthbarType.valueOf(playerData.get(38));
        }
        catch (Exception e) {
            mobHealthbarType = Config.getInstance().getMobHealthbarDefault();
        }

        loaded = true;
        return true;
    }

    private void addMySQLPlayer() {
        String tablePrefix = Config.getInstance().getMySQLTablePrefix();

        SQLDatabaseManager.write("INSERT INTO " + tablePrefix + "users (user, lastlogin) VALUES ('" + playerName + "'," + System.currentTimeMillis() / Misc.TIME_CONVERSION_FACTOR + ")");
        userId = SQLDatabaseManager.getInt("SELECT id FROM " + tablePrefix + "users WHERE user = '" + playerName + "'");

        SQLDatabaseManager.write("INSERT INTO " + tablePrefix + "huds (user_id, mobhealthbar) VALUES (" + userId + ", '" + mobHealthbarType.name() + "')");
        SQLDatabaseManager.write("INSERT INTO " + tablePrefix + "cooldowns (user_id) VALUES (" + userId + ")");
        SQLDatabaseManager.write("INSERT INTO " + tablePrefix + "skills (user_id) VALUES (" + userId + ")");
        SQLDatabaseManager.write("INSERT INTO " + tablePrefix + "experience (user_id) VALUES (" + userId + ")");
    }


    private void addFlatfilePlayer() {
        try {
            // Open the file to write the player
            BufferedWriter out = new BufferedWriter(new FileWriter(mcMMO.getUsersFilePath(), true));

            // Add the player to the end
            out.append(playerName).append(":");
            out.append("0:"); // Mining
            out.append(":");
            out.append(":");
            out.append("0:"); // Xp
            out.append("0:"); // Woodcutting
            out.append("0:"); // WoodCuttingXp
            out.append("0:"); // Repair
            out.append("0:"); // Unarmed
            out.append("0:"); // Herbalism
            out.append("0:"); // Excavation
            out.append("0:"); // Archery
            out.append("0:"); // Swords
            out.append("0:"); // Axes
            out.append("0:"); // Acrobatics
            out.append("0:"); // RepairXp
            out.append("0:"); // UnarmedXp
            out.append("0:"); // HerbalismXp
            out.append("0:"); // ExcavationXp
            out.append("0:"); // ArcheryXp
            out.append("0:"); // SwordsXp
            out.append("0:"); // AxesXp
            out.append("0:"); // AcrobaticsXp
            out.append(":");
            out.append("0:"); // Taming
            out.append("0:"); // TamingXp
            out.append("0:"); // DATS
            out.append("0:"); // DATS
            out.append("0:"); // DATS
            out.append("0:"); // DATS
            out.append("0:"); // DATS
            out.append("0:"); // DATS
            out.append("0:"); // DATS
            out.append(hudType == null ? "STANDARD" : hudType.toString()).append(":"); // HUD
            out.append("0:"); // Fishing
            out.append("0:"); // FishingXp
            out.append("0:"); // Blast Mining
            out.append(String.valueOf(System.currentTimeMillis() / Misc.TIME_CONVERSION_FACTOR)).append(":"); // LastLogin
            out.append(mobHealthbarType == null ? Config.getInstance().getMobHealthbarDefault().toString() : mobHealthbarType.toString()).append(":"); // Mob Healthbar HUD

            // Add more in the same format as the line above

            out.newLine();
            out.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
