package me.nikl.battleship;

import me.nikl.battleship.game.GameManager;
import me.nikl.battleship.game.GameRules;
import me.nikl.battleship.update.*;
import me.nikl.gamebox.ClickAction;
import me.nikl.gamebox.GameBox;
import me.nikl.gamebox.guis.GUIManager;
import me.nikl.gamebox.guis.button.AButton;
import me.nikl.gamebox.guis.gui.game.GameGui;
import me.nikl.gamebox.guis.gui.game.StartMultiplayerGamePage;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class Main extends JavaPlugin{

	private GameManager manager;
	private FileConfiguration config;
	private File con;
	public static Economy econ = null;
	public static final String gameID = "battleship";
	public static String prefix = "[&3Battleship&r]";
	public Boolean econEnabled;
	private int invitationValidFor;
	public Language lang;
	public boolean disabled;
	private InvTitle updater;

	private GameBox gameBox;
	
	public static boolean playSounds = true;
	
	@Override
	public void onEnable(){
        if (!setupUpdater()) {
            getLogger().severe("Your server version is not compatible with this plugin!");

            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        
		this.disabled = false;
		this.con = new File(this.getDataFolder().toString() + File.separatorChar + "config.yml");

		reload();
		if(disabled) return;

		hook();
		if(disabled) return;
	}


	private void hook() {
		if(Bukkit.getPluginManager().getPlugin("GameBox") == null || !Bukkit.getPluginManager().getPlugin("GameBox").isEnabled()){
			Bukkit.getLogger().log(Level.WARNING, " GameBox not found");
			Bukkit.getLogger().log(Level.WARNING, " Continuing as standalone");
			Bukkit.getPluginManager().disablePlugin(this);
			disabled = true;
			return;
		}





		gameBox = (me.nikl.gamebox.GameBox)Bukkit.getPluginManager().getPlugin("GameBox");


		// disable economy if it is disabled for either one of the plugins
		this.econEnabled = this.econEnabled && gameBox.getEconEnabled();
		playSounds = playSounds && GameBox.playSounds;

		GUIManager guiManager = gameBox.getPluginManager().getGuiManager();

		this.manager = new GameManager(this);

		//TodO: move game and prefix to Language
		gameBox.getPluginManager().registerGame(manager, gameID, "Battleship", 2);

		GameGui gameGui = new GameGui(gameBox, guiManager, 54, gameID, "main");



		Map<String, GameRules> gameTypes = new HashMap<>();

		if(config.isConfigurationSection("gameBox.gameButtons")){
			ConfigurationSection gameButtons = config.getConfigurationSection("gameBox.gameButtons");
			ConfigurationSection buttonSec;
			int moves, numberOfGems;
			double cost;
			boolean bombs;

			String displayName;
			ArrayList<String> lore;

			GameRules rules;

			for(String buttonID : gameButtons.getKeys(false)){
				buttonSec = gameButtons.getConfigurationSection(buttonID);


				if(!buttonSec.isString("materialData")){
					Bukkit.getLogger().log(Level.WARNING, " missing material data under: gameBox.gameButtons." + buttonID + "        can not load the button");
					continue;
				}

				ItemStack mat = getItemStack(buttonSec.getString("materialData"));
				if(mat == null){
					Bukkit.getLogger().log(Level.WARNING, " error loading: gameBox.gameButtons." + buttonID);
					Bukkit.getLogger().log(Level.WARNING, "     invalid material data");
					continue;
				}


				AButton button =  new AButton(mat.getData(), 1);
				ItemMeta meta = button.getItemMeta();

				if(buttonSec.isString("displayName")){
					displayName = chatColor(buttonSec.getString("displayName"));
					meta.setDisplayName(displayName);
				}

				if(buttonSec.isList("lore")){
					lore = new ArrayList<>(buttonSec.getStringList("lore"));
					for(int i = 0; i < lore.size();i++){
						lore.set(i, chatColor(lore.get(i)));
					}
					meta.setLore(lore);
				}

				guiManager.registerGameGUI(gameID, buttonID, new StartMultiplayerGamePage(gameBox, guiManager, 54, gameID, buttonID, "Testing start gui"));


				button.setItemMeta(meta);
				button.setAction(ClickAction.CHANGE_GAME_GUI);
				button.setArgs(gameID, buttonID);

				bombs = buttonSec.getBoolean("bombs", true);
				moves = buttonSec.getInt("moves", 20);
				numberOfGems = buttonSec.getInt("differentGems", 8);
				cost = buttonSec.getDouble("cost", 0.);


				rules = new GameRules(moves, numberOfGems, bombs, cost);

				if(buttonSec.isInt("slot")){
					gameGui.setButton(button, buttonSec.getInt("slot"));
				} else {
					gameGui.setButton(button);
				}

				gameTypes.put(buttonID, rules);
			}
		}


		this.manager.setGameTypes(gameTypes);



		getMainButton:
		if(config.isConfigurationSection("gameBox.mainButton")){
			ConfigurationSection mainButtonSec = config.getConfigurationSection("gameBox.mainButton");
			if(!mainButtonSec.isString("materialData")) break getMainButton;

			ItemStack gameButton = getItemStack(mainButtonSec.getString("materialData"));
			if(gameButton == null){
				gameButton = (new ItemStack(Material.EMERALD));
			}
			ItemMeta meta = gameButton.getItemMeta();
			meta.setDisplayName(chatColor(mainButtonSec.getString("displayName","&3GemCrush")));
			if(mainButtonSec.isList("lore")){
				ArrayList<String> lore = new ArrayList<>(mainButtonSec.getStringList("lore"));
				for(int i = 0; i < lore.size();i++){
					lore.set(i, chatColor(lore.get(i)));
				}
				meta.setLore(lore);
			}
			gameButton.setItemMeta(meta);
			guiManager.registerGameGUI(gameID, "main", gameGui, gameButton, "gemcrush", "gc");
		} else {
			Bukkit.getLogger().log(Level.WARNING, " Missing or wrong configured main button in the configuration file!");
		}
	}




	private ItemStack getItemStack(String itemPath){
		Material mat; short data;
		String[] obj = itemPath.split(":");

		if (obj.length == 2) {
			try {
				mat = Material.matchMaterial(obj[0]);
			} catch (Exception e) {
				return null; // material name doesn't exist
			}

			try {
				data = Short.valueOf(obj[1]);
			} catch (NumberFormatException e) {
				e.printStackTrace();
				return null; // data not a number
			}

			//noinspection deprecation
			if(mat == null) return null;
			ItemStack stack = new ItemStack(mat);
			stack.setDurability(data);
			return stack;
		} else {
			try {
				mat = Material.matchMaterial(obj[0]);
			} catch (Exception e) {
				return null; // material name doesn't exist
			}
			//noinspection deprecation
			return (mat == null ? null : new ItemStack(mat));
		}
	}


	private boolean setupUpdater() {
		String version;

	    try {
	        version = Bukkit.getServer().getClass().getPackage().getName().replace(".",  ",").split(",")[3];
	    } catch (ArrayIndexOutOfBoundsException whatVersionAreYouUsingException) {
	        return false;
	    }
	
	    //getLogger().info("Your server is running version " + version);
	
	    if (version.equals("v1_10_R1")) {
	        updater = new Update_1_10_R1();
	        
	    } else if (version.equals("v1_9_R2")) {
	        updater = new Update_1_9_R2();
	        
	    } else if (version.equals("v1_9_R1")) {
	        updater = new Update_1_9_R1();
	        
	    } else if (version.equals("v1_8_R3")) {
	        updater = new Update_1_8_R3();
	        
	    } else if (version.equals("v1_8_R2")) {
	        updater = new Update_1_8_R2();
	        
	    } else if (version.equals("v1_8_R1")) {
	        updater = new Update_1_8_R1();
			
	    } else if (version.equals("v1_11_R1")) {
			updater = new Update_1_11_R1();
		}
	    return updater != null;
	}

	public InvTitle getUpdater(){
		return this.updater;
	}
	
	private void getValuesFromConfig() {
		FileConfiguration config = getConfig();
		if(!config.isConfigurationSection("timers") || !config.isInt("timers.invitationTimer.validFor")){
			Bukkit.getConsoleSender().sendMessage(chatColor(Main.prefix + " &4No 'timers' section or invalid values in 'timers' section"));
			Bukkit.getConsoleSender().sendMessage(chatColor(Main.prefix + " &4Using default values!"));
			this.invitationValidFor = 15;
		} else {
			ConfigurationSection timer = config.getConfigurationSection("timers");
			this.invitationValidFor = timer.getInt("invitationTimer.validFor");
		}		
	}

	@Override
	public void onDisable(){

	}
	
    private boolean setupEconomy(){
    	if (getServer().getPluginManager().getPlugin("Vault") == null) {
    		return false;
    	}
    	RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
    	if (rsp == null) {
    		return false;
    	}
    	econ = (Economy)rsp.getProvider();
    	return econ != null;
    }
	
	public void reloadConfig(){
		try { 
			this.config = YamlConfiguration.loadConfiguration(new InputStreamReader(new FileInputStream(this.con), "UTF-8")); 
		} catch (UnsupportedEncodingException e) { 
			e.printStackTrace(); 
		} catch (FileNotFoundException e) { 
			e.printStackTrace(); 
		}
	} 
	
	public GameManager getManager() {
		return manager;
	}
	
	public void reload(){
		if(!con.exists()){
			this.saveResource("config.yml", false);
		}
		reloadConfig();
		
		this.lang = new Language(this);
		
		Main.playSounds = getConfig().getBoolean("gameRules.playSounds", true);
		
		this.econEnabled = false;
		if(getConfig().getBoolean("economy.enabled")){
			this.econEnabled = true;
			if (!setupEconomy()){
				Bukkit.getConsoleSender().sendMessage(chatColor(prefix + " &4No economy found!"));
				getServer().getPluginManager().disablePlugin(this);
				return;
			}
		}
		
		getValuesFromConfig();
		
		this.setManager(new GameManager(this));
	}

	public void setManager(GameManager manager) {
		this.manager = manager;
	}

	public FileConfiguration getConfig() {
		return config;
	}
	
    public String chatColor(String message){
    	return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    public Boolean getEconEnabled(){
    	return this.econEnabled;
    }

	public int getInvitationValidFor() {
		return invitationValidFor;
	}
}
