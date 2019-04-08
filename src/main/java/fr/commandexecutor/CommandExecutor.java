package fr.commandexecutor;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.NoPermissionException;
import javax.naming.OperationNotSupportedException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class CommandExecutor extends JavaPlugin {
	public static CommandExecutor pl;

    @Override
    public void onEnable() { pl = this; saveDefaultConfig(); }
    
    @Override
    public void onDisable() {  }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    	String commandUsage = null;
    	try {
    		// Checking perm
        	if(!sender.hasPermission("commandexecutor.access")) throw new NoPermissionException();
        	
	    	// Checking if player used valid arg
	    	if(args.length == 0 || args[0].isEmpty()) throw new OperationNotSupportedException();
	    	
	    	// If /ce reload is used
	    	if(args[0].equals("reload")) {
	    		if(!sender.hasPermission("commandexecutor.reload")) throw new NoPermissionException();
	    		saveDefaultConfig();
	    		reloadConfig();
	    		sender.sendMessage(colorize("&3&l[&e&lCommandExecutor&3&l] &2Reloaded."));
	    		return true;
	    	}
	
	    	// Send in console info about command used
	    	if(getConfig().getBoolean("sendConsoleMessage")) getLogger().info("Player " + sender.getName() + " used: " + args[0]);
	    	
	    	// Checking for command list
	    	ConfigurationSection cs = getConfig().getConfigurationSection("commands");
	    	if(cs == null) throw new OperationNotSupportedException();
	    	
	    	// Initialise list
	    	final List<String> list = cs.getStringList(args[0].toLowerCase());
	    	if(list == null || list.isEmpty()) throw new OperationNotSupportedException();
	    	
			// Executing the command list
	    	final String playerName = sender.getName();
	    	CommandSender executor = getServer().getConsoleSender();
	    	
			for(String cmd : list) {
				// Replacing variables
				cmd = cmd.replace("%player%", playerName);
				
				// Checking the executor if executor must be the player
				if(cmd.startsWith("%executor:")) {
					if(cmd.contains("player")) {
						if(!(sender instanceof Player)) {
							sender.sendMessage(colorize("This command can only be executed by players in game."));
							return true;
						}
						executor = ((Player)sender);
					}
					continue;
				}
				
				// Checking if we need permission
				if(cmd.startsWith("%permission%")) {
					if(sender == null) break;
					if(!sender.hasPermission(cmd.replace("%permission%", ""))) {
						sender.sendMessage(colorize(getConfig().getString("no-perm")));
						break;
					}
					continue;
				}
				
				// Checking if a command usage is entered
				if(cmd.startsWith("%usage%")) {
					commandUsage = cmd.replace("%usage%", "");
					continue;
				}
				
				// Adding arguments needed in command
				if(cmd.matches("(?i).*%arg\\d+%.*")) {
					LinkedList<Integer> argsid = new LinkedList<Integer>();
					final Pattern p = Pattern.compile("%arg\\d+%");
					final Matcher m = p.matcher(cmd);
					while (m.find()) argsid.add(Integer.parseInt(m.group(0).replaceAll("\\D+","")));
					for(Integer arg : argsid) cmd = cmd.replace("%arg" + arg + "%", args[arg]);
				}
				
				// Adding all arguments if needed
				if(cmd.contains("%argAll")) {
					String formatting = cmd.replaceAll("(?!%argAll\\d+%)", "");
					final int starts = Integer.parseInt(formatting.replaceAll("\\D+", ""));
					cmd = cmd.replace("%argAll" + starts + "%", unifyArguments(starts, args));
				}
				
				// Checking if we send a message
				if(cmd.startsWith("%sendMessage%")) {
					sender.sendMessage(colorize(cmd.replace("%sendMessage%", "")));
					continue;
				}
				
				// Checking if we broadcast a message
				if(cmd.startsWith("%broadcast%")) {
					for(Player p : getServer().getOnlinePlayers()) p.sendMessage(colorize(cmd.replace("%broadcast%", "")));
					continue;
				}
				
				// Checking if the player say something
				if(cmd.startsWith("%say%")) {
					((Player)sender).chat(colorize(cmd.replace("%say%", "")));
					continue;
				}
				
				// Executing
				if(executor instanceof Player) ((Player)sender).performCommand(cmd);
				else Bukkit.dispatchCommand((ConsoleCommandSender)executor, cmd);
			}
    	} catch (NoPermissionException e) {
    		sender.sendMessage(colorize(getConfig().getString("no-perm")));
    	} catch (OperationNotSupportedException e) {
    		sender.sendMessage(colorize(getConfig().getString("unknown-command")));
    	} catch (Exception e) {
    		if(commandUsage != null) sender.sendMessage(colorize(commandUsage));
    		else sender.sendMessage(colorize(getConfig().getString("unknown-command")));
    	}
    	
    	return true;
    }
    
    private String colorize(String message) {
    	return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    private String unifyArguments(int start, String[] args) {
    	final StringBuilder message = new StringBuilder();
		for (int i = start + 1; i < args.length; i++){
			if (i > start + 1) message.append(" ");
			message.append(args[i]);
		}
		return message.toString();
    }
}
    