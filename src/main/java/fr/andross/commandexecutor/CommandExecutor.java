/*
 * CommandExecutor - Create or replace commands with list of commands,
 * optionally executed by player/console
 * Copyright (C) 2020 André Sustac
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.andross.commandexecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.NoPermissionException;
import javax.naming.OperationNotSupportedException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Main plugin class
 * @version 1.4
 * @author Andross
 */
public final class CommandExecutor extends JavaPlugin {
    private final Pattern argPattern = Pattern.compile("(?i).*%arg\\d+%.*");
    private final Pattern singleArgPattern = Pattern.compile("%arg\\d+%");

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
    }

    @Override
    public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String label, @NotNull final String[] args) {
        String commandUsage = null;
        try {
            // Checking perm
            if (!sender.hasPermission("commandexecutor.access")) throw new NoPermissionException();

            // Checking if player used valid arg
            if (args.length == 0 || args[0].isEmpty()) throw new OperationNotSupportedException();

            // If /ce reload is used
            if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("commandexecutor.reload")) throw new NoPermissionException();
                saveDefaultConfig();
                reloadConfig();
                sender.sendMessage(color("&3&l[&e&lCommandExecutor&3&l] &2Reloaded."));
                return true;
            }

            // Send in console info about command used
            if (getConfig().getBoolean("sendConsoleMessage")) getLogger().info("Player " + sender.getName() + " used: " + args[0]);

            // Checking for command list
            final ConfigurationSection cs = getConfig().getConfigurationSection("commands");
            if (cs == null) throw new OperationNotSupportedException();

            // Initialise list
            final List<String> list = cs.getStringList(args[0].toLowerCase());
            if (list.isEmpty()) throw new OperationNotSupportedException();

            // Executing the command list
            final String playerName = sender.getName();
            CommandSender executor = getServer().getConsoleSender();

            for (String cmd : list) {
                // Replacing variables
                cmd = cmd.replace("%player%", playerName);

                // Checking the executor if executor must be the player
                if (cmd.startsWith("%executor:")) {
                    if (cmd.contains("player")) {
                        if (!(sender instanceof Player)) {
                            sender.sendMessage(color("This command can only be executed by players in game."));
                            return true;
                        }
                        executor = sender;
                    }
                    continue;
                }

                // Checking if we need permission
                if (cmd.startsWith("%permission%")) {
                    if (!sender.hasPermission(cmd.replace("%permission%", ""))) {
                        sender.sendMessage(color(Objects.requireNonNull(getConfig().getString("no-perm"))));
                        break;
                    }
                    continue;
                }

                // Checking if a command usage is entered
                if (cmd.startsWith("%usage%")) {
                    commandUsage = cmd.replace("%usage%", "");
                    continue;
                }

                // Adding arguments needed in command
                if (argPattern.matcher(cmd).matches()) {
                    final List<Integer> argsid = new ArrayList<>();
                    final Matcher m = singleArgPattern.matcher(cmd);
                    while (m.find()) argsid.add(Integer.parseInt(m.group(0).replaceAll("\\D+","")));
                    for (final Integer arg : argsid) cmd = cmd.replace("%arg" + arg + "%", args[arg]);
                }

                // Adding all arguments if needed
                if (cmd.contains("%argAll")) {
                    final String formatting = cmd.replaceAll("(?!%argAll\\d+%)", "");
                    final int starts = Integer.parseInt(formatting.replaceAll("\\D+", ""));
                    cmd = cmd.replace("%argAll" + starts + "%", getMessage(starts, args));
                }

                // Checking if we send a message
                if (cmd.startsWith("%sendMessage%")) {
                    sender.sendMessage(color(cmd.replace("%sendMessage%", "")));
                    continue;
                }

                // Checking if we broadcast a message
                if (cmd.startsWith("%broadcast%")) {
                    final String message = color(cmd.replace("%broadcast%", ""));
                    getServer().getOnlinePlayers().forEach(p -> p.sendMessage(message));
                    continue;
                }

                // Checking if the player say something
                if (cmd.startsWith("%say%")) {
                    ((Player)sender).chat(color(cmd.replace("%say%", "")));
                    continue;
                }

                // Executing
                if (executor instanceof Player) ((Player)sender).performCommand(cmd);
                else Bukkit.dispatchCommand(executor, cmd);
            }
        } catch (final NoPermissionException e) {
            sender.sendMessage(color(Objects.requireNonNull(getConfig().getString("no-perm"))));
        } catch (final OperationNotSupportedException e) {
            sender.sendMessage(color(Objects.requireNonNull(getConfig().getString("unknown-command"))));
        } catch (final Exception e) {
            if(commandUsage != null) sender.sendMessage(color(commandUsage));
            else sender.sendMessage(color(Objects.requireNonNull(getConfig().getString("unknown-command"))));
        }

        return true;
    }

    /**
     * Colorize a text
     * @param text text to colorize
     * @return the colored text
     */
    @NotNull
    private String color(@NotNull final String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * Unify all args into a single string
     * @param start starting index
     * @param args arguments
     * @return an unified string of args
     */
    @NotNull
    private String getMessage(final int start, @NotNull final String[] args) {
        final StringBuilder message = new StringBuilder();
        for (int i = start + 1; i < args.length; i++){
            if (i > start + 1) message.append(" ");
            message.append(args[i]);
        }
        return message.toString();
    }
}
