package fr.commandexecutor;

import java.util.List;

import javax.naming.NoPermissionException;
import javax.naming.OperationNotSupportedException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

public class CommandExecutor extends JavaPlugin {
	public static CommandExecutor pl;

    @Override
    public void onEnable() { pl = this; saveDefaultConfig(); }
    
    @Override
    public void onDisable() {  }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    	try {
    		// Checking perm
        	if(!sender.hasPermission("commandexecutor.access")) throw new NoPermissionException();
        	
	    	// Checking if player used valid arg
	    	if(args.length == 0 || args[0].isEmpty()) throw new OperationNotSupportedException();
	
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
			final ConsoleCommandSender console = getServer().getConsoleSender();
	    	getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
	    		@Override
				public void run() {
	    			if(!CommandExecutor.pl.isEnabled()) return;
	    			for(String cmd : list) {
	    	    		Bukkit.dispatchCommand(console, cmd.replace("%player%", playerName));
	    	    	}
	    		}
	    	});
    	} catch (NoPermissionException e) {
    		sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("no-perm")));
    	} catch (OperationNotSupportedException e) {
    		sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("unknown-command")));
    	}
    	
    	return true;
    }
}
    