package com.gmail.nossr50.commands.database;

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.config.Config;
import com.gmail.nossr50.runnables.database.FormulaConversionTask;
import com.gmail.nossr50.util.player.UserManager;
import com.google.common.collect.ImmutableList;

public class McconvertCommand implements TabExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (Config.getInstance().getFormulaExponential()) {
            sender.sendMessage("Already converted."); // TODO: Localize
            return true;
        }

        switch (args.length) {
            case 0:
                sender.sendMessage("Starting conversion from Linear to Exponential curve");
                UserManager.saveAll();
                UserManager.clearAll();
                new FormulaConversionTask().runTaskLater(mcMMO.p, 1);

                for (Player player : mcMMO.p.getServer().getOnlinePlayers()) {
                    UserManager.addUser(player);
                }

                sender.sendMessage("Commands.mcconvert.Finish");
                return true;

            default:
                return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return ImmutableList.of();
    }
}