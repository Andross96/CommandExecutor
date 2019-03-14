package fr.commandexecutor;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class CommandExecutor extends JavaPlugin {
	public static CommandExecutor pl;

    @Override
    public void onEnable() {
    	pl = this;
    	
    	try {
    		saveDefaultConfig();
    	}catch(Exception e) {
    		e.printStackTrace();
    		getServer().getPluginManager().disablePlugin(this);
    	}
    }
    
    @Override
    public void onDisable() {  }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    	if(!sender.hasPermission("commandexecutor.access")) return true;
    	getLogger().info("Player " + sender.getName() + " used command.");
    	
    	final List<String> list = getConfig().getStringList("command");
    	if(list == null || list.isEmpty()) return true;
    	final String playerName = sender.getName();

    	getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
    		@Override
			public void run() {
    			for(String cmd : list) {
    	    		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", playerName));
    	    	}
    		}
    	});
    	
    	
    	return true;
    }
}
    