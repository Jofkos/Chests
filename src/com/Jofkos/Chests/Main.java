package com.Jofkos.Chests;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.TrapDoor;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;

public class Main extends JavaPlugin implements CommandExecutor, Listener {

	HashMap<String, Boolean> create = new HashMap<String, Boolean>();
	
	HashMap<Location, Integer> chestloc = new HashMap<Location, Integer>();
	HashMap<Integer, Location> locchest = new HashMap<Integer, Location>();
	
	HashMap<String, Boolean> isOpen = new HashMap<String, Boolean>();
	HashMap<String, Integer> Open = new HashMap<String, Integer>();
	
	HashMap<Integer, Inventory> invs = new HashMap<Integer, Inventory>();
	
	public int Chests;

	File chestfile = new File("plugins/Chests", "chests.yml");
	FileConfiguration chests = YamlConfiguration.loadConfiguration(this.chestfile);
	
	@Override
	public void onEnable() {
		Chests = chests.getInt("chests");
		compress("§aEs wurden §6%s§a Truhen geladen.");
		loadInvs();
		this.getServer().getPluginManager().registerEvents(this, this);
		this.getServer().getPluginCommand("Chests").setExecutor(this);
	}
	
	@Override
	public void onDisable() {
		int savedChests = 0;
		for (int i = 1; i < Chests + 1; i++) {
			String s = Integer.toString(i);
			if (locchest.get(i) != null) {
				Location block = locchest.get(i);
				chests.set("chest" + s + ".world", block.getWorld().getName());
				chests.set("chest" + s + ".x", block.getBlockX());
				chests.set("chest" + s + ".y", block.getBlockY());
				chests.set("chest" + s + ".z", block.getBlockZ());
				chests.set("chest" + s + ".inv", invs.get(i).getContents());
				savedChests++;
			} else {
				chests.set("chest" + s, null);
			}
		}
		chests.set("chests", Chests);
		try {
			chests.save(chestfile);
			log("§aEs wurden §6" + savedChests + "§a Truhen gespeichert.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@EventHandler
	public void onClick(PlayerInteractEvent e) {
		if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
			Player p = e.getPlayer();
			Location block = e.getClickedBlock().getLocation();
			if (create.containsKey(p.getName())) {
				if (create.get(p.getName()).equals(true)) {
					if (!chestloc.containsKey(block)) {
						if (hasChest(p)) {
							createChest(e.getClickedBlock(), p);
						} else {
							p.sendMessage("§cDu musst eine Truhe zum bezahlen haben.");
							create.put(e.getPlayer().getName(), false);
						}
					} else {
						p.sendMessage("§cDieser Block ist schon eine Truhe");
					}
				}
			}
			
			if (!p.isSneaking()) {
				if (chestloc.containsKey(e.getClickedBlock().getLocation())) {
					Inventory chest = invs.get(chestloc.get(e.getClickedBlock().getLocation()));
					p.openInventory(chest);
					isOpen.put(p.getName(), true);
					Open.put(p.getName(), chestloc.get(e.getClickedBlock().getLocation()));
					openChest(e.getClickedBlock(), true);
					e.getClickedBlock().getWorld().playSound(block, Sound.CHEST_OPEN, 1.0F, 1.0F);
				}
			}
		} else if (e.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
			if (WorldEdit() != null) {
				if (WorldEdit().getSession(e.getPlayer()).hasSuperPickAxe()) {
					if (hasPickAxe(e.getPlayer())) {
						if (chestloc.containsKey(e.getClickedBlock().getLocation())) {
							destroyChest(e.getClickedBlock());
						}
					}
				}
			}
		}
	}

	@EventHandler
	public void onClose(InventoryCloseEvent e) {
		Player p = (Player) e.getPlayer();
		if (isOpen.containsKey(p.getName())) {
			if (isOpen.get(p.getName()).equals(true)) {
				isOpen.remove(p.getName());
				Location chestt = locchest.get(Open.get(p.getName()));
				chestt.getWorld().playSound(chestt, Sound.CHEST_CLOSE, 1.0F, 1.0F);
				openChest(chestt.getBlock(), false);
				Open.remove(p.getName());
			}
		}
	}

	@EventHandler
	public void onBreak(BlockBreakEvent e) { 
		if (chestloc.containsKey(e.getBlock().getLocation())) {
			destroyChest(e.getBlock());
		}
	}

	@EventHandler
	public void onBlockPlace(BlockPlaceEvent e) {
		if (chestloc.containsKey(e.getBlockAgainst().getLocation())) {
			if (!e.getPlayer().isSneaking()) {
			}
		}
	}


	@Override
	public boolean onCommand(CommandSender cs, Command cmd, String label, String[] args) {
		if (args.length > 0) {
			if (args[0].equalsIgnoreCase("create")) {
				if (cs instanceof Player) {
					Player p = (Player) cs;
					if (hasChest(p)) {
						p.sendMessage("§2Bitte klicke auf den Block, der zu einer Truhe werden soll.");
						create.put(p.getName(), true);
						return true;
					} else {
						p.sendMessage("§cDu musst eine Truhe zum bezahlen haben.");
						create.put(p.getName(), false);
						return true;
					}
				} else {
					cs.sendMessage("§cDieser Befehl kann nur von einem Spieler benutzt werden.");
					cs.sendMessage("\n");
					this.getServer().dispatchCommand(cs, "chest");
				}
			} 
				else if (args[0].equalsIgnoreCase("show")) {
				if (cs instanceof Player) {
					Player p = (Player) cs;
					if (args.length > 1) {
						if (WorldEdit() != null) {
							if (locchest.containsKey((Integer.valueOf(args[1])))) {
								Location chest = locchest.get((Integer.valueOf(args[1])));
								CuboidSelection selection = new CuboidSelection(p.getWorld(), chest, chest);
								WorldEdit().setSelection(p, selection);
								if (WorldEdit().getSession(p).hasCUISupport()) {
									p.sendMessage("§aDie Truhe §6" + args[1] + "§a wurde mit WorldEditCUI markiert.");
								} else {
									p.sendMessage("§aDie Truhe §6" + args[1] + "§a wurde mit WorldEdit markiert.");
								}
								return true;
							} else {
								p.sendMessage("§cDiese Truhe gibt es nicht!");
								return true;
							}
						} else {
							p.sendMessage("Diese Funktion benötigt WorldEdit.");
							return true;
						}
					}
				}
			} else if (args[0].equalsIgnoreCase("clear")) {
				if (cs instanceof Player) {
					Player p = (Player) cs;
					if (WorldEdit() != null) {
						this.getServer().dispatchCommand(cs, "/sel");
						return true;
					} else {
						p.sendMessage("Diese Funktion benötigt WorldEdit.");
						return true;
					}
				}
			}
		}
		cs.sendMessage("§aChests version §6" + this.getDescription().getVersion() + " §avon §6Jofkos");
		cs.sendMessage("§aEs wurden §6" + Chests + "§a geladen.");
		if (cs instanceof Player) {
			cs.sendMessage("§6/chest create §r-§a um eine neue Truhe zu erstellen.");
			if (WorldEdit() != null) {
				if (WorldEdit().getSession((Player)cs).hasCUISupport()) {
					cs.sendMessage("§6/chest show §r-§a um eine Truhe mit WorldEditCUI zu markieren.");
				} else {
					cs.sendMessage("§6/chest show §r-§a um eine Truhe mit WorldEdit zu markieren.");
				}
			} else {
				cs.sendMessage("§6/chest show §r-§a diese Funktion benötigt WorldEdit, es wurde kein WorldEdit gefunden.");
			}
		}
		return true;
	}
	
	public void loadInvs() {
		for (int i = 1; i < chests.getInt("chests") + 1; i++) {
			String s = Integer.toString(i);
			if (chests.getString("chest" + s) != null) {
				Inventory tempInv = Bukkit.createInventory(null, 3 * 9);
				loadInv(i, tempInv);
				invs.put(i, tempInv);
			}
		}
	}

	public void loadInv(int n, Inventory inv) {
		reloadConf();
		List<?> invContent = chests.getList("chest" + Integer.toString(n) + ".inv");
		if (invContent != null)
			for (int i = 0; i < invContent.size(); i++) {
				inv.setItem(i, (ItemStack) invContent.get(i));
			}
	}
	
	public boolean hasPickAxe(Player p) {
		ItemStack i = p.getItemInHand();
		if (i.getType().equals(Material.WOOD_PICKAXE)) {
			return true;
		} else if (i.getType().equals(Material.STONE_PICKAXE)) {
			return true;
		} else if (i.getType().equals(Material.GOLD_PICKAXE)) {
			return true;
		} else if (i.getType().equals(Material.DIAMOND_PICKAXE)) {
			return true;
		} else {
			return false;
		}
		
	}
	
	public void openChest(final Block b, final boolean open) {
		this.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
					@Override
					public void run() {
						BlockState state = b.getState();
						if (b.getType().equals(Material.TRAP_DOOR)) {
							TrapDoor trapdoor = (TrapDoor) state.getData();
							if (open) {
								trapdoor.setOpen(false);
							} else {
								trapdoor.setOpen(true);
							}
							state.update();
						}
					}
				});
	}
	
	public void createChest(Block b, Player p) {
		Location block = b.getLocation();
		Inventory chestinv = Bukkit.createInventory(null, 3 * 9);

		chestloc.put(block, Chests);
		locchest.put(Chests, block);
		invs.put(Chests, chestinv);

		payChest(p);
		p.sendMessage("§aDie Truhe wurde erfolgreich erstellt!");
		Chests++;
		create.put(p.getName(), false);
		create.put(p.getName(), false);
		openChest(b, true);
	}
	
	public void destroyChest(Block b) {
		Inventory chest = invs.get(chestloc.get(b.getLocation()));
		ItemStack[] content = chest.getContents();
		for (int i = 0; i < content.length; i++) {
			if (content[i] != null) {
				b.getWorld().dropItemNaturally(locchest.get(chestloc.get(b.getLocation())), content[i]);
			}
		}
		b.getWorld().dropItemNaturally(locchest.get(chestloc.get(b.getLocation())), new ItemStack(Material.CHEST, 1));
		
		int chestNum = chestloc.get(b.getLocation());
		locchest.remove(chestloc.get(b.getLocation()));
		chestloc.remove(b.getLocation());
		invs.put(chestNum, null);
		
		log("§aEine Truhe wurde zerstört");
	}
	
	public void compress(String msg) {
		compress();
		log(String.format(msg, Chests));
	}
	
	public void compress() {	
		if (Chests != 0) {
			int a = 0;
			for (int i = 1; i < Chests + 1; i++) {
				String s = Integer.toString(i);
				if (chests.getString("chest" + s) != null) {
					a++;
					if (i != a) {
						rename(i, a);
					}
				}
			}
			chests.set("chests", a);
			Chests = a;
			try {
				chests.save(chestfile);
			} catch (IOException e) {
				e.printStackTrace();
			}
			reloadConf();
			for (int i = 1; i < chests.getInt("chests") + 1; i++) {
				String s = Integer.toString(i);
				if (chests.getString("chest" + s) != null) {
					World world = Bukkit.getWorld(chests.getString("chest" + s + ".world"));
					double x = chests.getDouble("chest" + s + ".x");
					double y = chests.getDouble("chest" + s + ".y");
					double z = chests.getDouble("chest" + s + ".z");
					Location loc = new Location(world, x, y, z);
					chestloc.put(loc, i);
					locchest.put(i, loc);
				}
			}
		}
	}
	
	
	
	public void rename(int i, int iz) {
		
		String s = Integer.toString(i);
		String z = Integer.toString(iz);
		
		chests.set("chest" + z + ".world", chests.get("chest" + s + ".world"));
		chests.set("chest" + z + ".x", chests.get("chest" + s + ".x"));
		chests.set("chest" + z + ".y", chests.get("chest" + s + ".y"));
		chests.set("chest" + z + ".z", chests.get("chest" + s + ".z"));
		chests.set("chest" + z + ".inv", chests.get("chest" + s + ".inv"));

		chests.set("chest" + s, null);
	}
	
	public boolean hasChest(Player p) {
		Inventory pInv = p.getInventory();
		ItemStack[] content = pInv.getContents();
		for (int i = 0; i < content.length; i++) {
			if (content[i] != null) {
				if (content[i].getType().equals(Material.CHEST)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean payChest(Player p) {
		Inventory pInv = p.getInventory();
		ItemStack[] content = pInv.getContents();
		for (int i = 0; i < content.length; i++) {
			if (content[i] != null) {
				if (content[i].getType().equals(Material.CHEST)) {
					if(content[i].getAmount() > 1) {
						p.getInventory().getContents()[i].setAmount(content[i].getAmount() - 1);
					} else {
						p.getInventory().setItem(i, new ItemStack(Material.AIR));
					}
					return true;
				}
			}
		}
		return false;
	}
	
	public WorldEditPlugin WorldEdit() {
		if (this.getServer().getPluginManager().getPlugin("WorldEdit") instanceof WorldEditPlugin) {
			return (WorldEditPlugin) this.getServer().getPluginManager().getPlugin("WorldEdit");
		}
		return null;
	}
	
	public void log(String msg) {
		this.getServer().getConsoleSender().sendMessage("[" + this.getDescription().getName() + "] " + msg);
	}

	public void reloadConf() {
		this.chests = YamlConfiguration.loadConfiguration(this.chestfile);
		reloadConfig();
	}
}