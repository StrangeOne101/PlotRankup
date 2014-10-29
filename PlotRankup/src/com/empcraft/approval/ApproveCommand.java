package com.empcraft.approval;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.intellectualcrafters.plot.C;
import com.intellectualcrafters.plot.Flag;
import com.intellectualcrafters.plot.FlagManager;
import com.intellectualcrafters.plot.PlayerFunctions;
import com.intellectualcrafters.plot.Plot;
import com.intellectualcrafters.plot.PlotBlock;
import com.intellectualcrafters.plot.PlotHelper;
import com.intellectualcrafters.plot.PlotId;
import com.intellectualcrafters.plot.PlotMain;
import com.intellectualcrafters.plot.PlotWorld;
import com.intellectualcrafters.plot.UUIDHandler;
import com.intellectualcrafters.plot.commands.SubCommand;
import com.intellectualcrafters.plot.database.DBFunc;
import com.intellectualcrafters.plot.generator.DefaultPlotWorld;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class ApproveCommand extends SubCommand {

    public ApproveCommand() {
        super("approve", "plots.admin", "Used to approve player's plots", "approve", "approval", CommandCategory.ACTIONS, true);
    }
    @Override
    public boolean execute(Player player, String... args) {
        List<String> validArgs = Arrays.asList("approve","list","next","listworld","deny");
        if (args.length==0 || !validArgs.contains(args[0].toLowerCase())) {
            Main.sendMessage(player, "&7Syntax: &c/plots approval <approve|deny|list|listworld|next>");
            return false;
        }
        args[0] = args[0].toLowerCase();
        if (args[0].equals("approve")) {
            
            if(!PlayerFunctions.isInPlot(player)) {
                sendMessage(player, C.NOT_IN_PLOT);
                return false;
            }
            PlotId id = PlayerFunctions.getPlot(player.getLocation());
            
            World world = player.getWorld();
            Plot plot = PlotMain.getPlots(world).get(id); 
            if (plot==null || !plot.hasOwner()) {
                sendMessage(player, C.NOT_IN_PLOT);
                return false;
            }
            
            Flag flag = plot.settings.getFlag("done");
            if (flag==null || flag.getValue().equals("true")) {
                if (flag==null) {
                    Main.sendMessage(player, "&7This plot is not &cpending&7 for approval.");
                }
                else {
                    Main.sendMessage(player, "&7This plot has already been approved.");
                }
                return false;
            }
            Set<Flag> flags = plot.settings.getFlags();
            flags.remove(flag);
            flags.add(new Flag(FlagManager.getFlag("done"), "true"));
            plot.settings.setFlags(flags.toArray(new Flag[0]));
            DBFunc.setFlags(player.getWorld().getName(), plot, plot.settings.getFlags().toArray(new Flag[0]));
            
            Player owner = Bukkit.getPlayer(plot.owner);
            if (owner!=null) {
                if (plot.settings.getAlias() != null && !plot.settings.getAlias().equals("")) {
                    Main.sendMessage(owner, "&7Your plot &a"+plot.id+" &7/ &a"+plot.settings.getAlias()+" has been approved!");
                }
                else {
                    Main.sendMessage(owner, "&7Your plot &a"+plot.id+"&7 has been approved!");
                }
            }
            
            int count = countApproved(plot.owner, world);
            
            for (String commandargs : Main.config.getStringList(world.getName()+".approval.actions")) {
                try {
                    int required = Integer.parseInt(commandargs.split(":")[0]);
                    if (required==count) {
                        String ownername = UUIDHandler.getName(plot.owner);
                        if (ownername==null) {
                            ownername = "";
                        }
                        String cmd = commandargs.substring(commandargs.indexOf(":")+1);
                        if (cmd.contains("%player%")) {
                            cmd = cmd.replaceAll("%player%", ownername);
                        }
                        cmd = cmd.replaceAll("%world%", world.getName());
                        
                        if (Main.vaultFeatures) {
                            if (cmd.contains("%nextrank%")) {
                                cmd.replaceAll("%nextrank%", VaultListener.getNextRank(world, VaultListener.getGroup(world, plot.owner)));
                            }
                        }
                        Main.sendMessage(null, "Console: "+cmd);
                        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd);
                    }
                }
                catch (Exception e) {
                    Main.sendMessage(null, "[PlotApproval] &cInvalid approval command "+commandargs+"!");
                    Main.sendMessage(player, "[PlotApproval] &cInvalid approval command "+commandargs+"!");
                    return true;
                }
            }
            Main.sendMessage(player, "&aSuccessfully approved plot!");
            return true;
        }
        if (args[0].equals("listworld")) { // Plots are sorted in claim order.
            World world = player.getWorld();
            
            ArrayList<Plot> plots = getPlots(world);
            if (plots.size()==0) {
                Main.sendMessage(player, "&7There are currently &c0&7 plots pending for approval.");
                return true;
            }
            Main.sendMessage(player, "&7There are currently &c"+plots.size()+"&7 plots pending for approval.");
            for (Plot current : plots) {
                String ownername = UUIDHandler.getName(current.owner);
                if (ownername==null) {
                    ownername = "unknown";
                }
                Main.sendMessage(player, "&8 - &3"+current.world+"&7;&3"+current.id.x+"&7;&3"+current.id.y+" &7: "+ownername);
            }
            return true;
        }
        if (args[0].equals("list")) {
            ArrayList<Plot> plots = getPlots();
            if (plots.size()==0) {
                Main.sendMessage(player, "&7There are currently &c0&7 plots pending for approval.");
                return true;
            }
            Main.sendMessage(player, "&7There are currently &c"+plots.size()+"&7 plots pending for approval.");
            for (Plot current : plots) {
                String ownername = UUIDHandler.getName(current.owner);
                if (ownername==null) {
                    ownername = "unknown";
                }
                Main.sendMessage(player, "&8 - &3"+current.world+"&7;&3"+current.id.x+"&7;&3"+current.id.y+" &7: "+ownername);
            }
            return true;
        }
        if (args[0].equals("next")) {

            PlotId id = PlayerFunctions.getPlot(player.getLocation());
            World world = player.getWorld();
            String worldname = world.getName();
            
            ArrayList<Plot> plots = getPlots();
            if (plots.size() > 0) {
                if (id!=null) {
                    Plot plot = PlotMain.getPlots(world).get(id); 
                    if (plot!=null && plot.hasOwner()) {
                        for (int i = 0; i < plots.size(); i++) {
                            if (plots.get(i).id.equals(id) && plots.get(i).world.equals(worldname)) {
                                if (i < plots.size()-1) {
                                    PlotMain.teleportPlayer(player, player.getLocation(), plots.get(i+1));
                                }
                                break;
                            }
                        }
                    }
                }
                PlotMain.teleportPlayer(player, player.getLocation(), plots.get(0));
                return true;
            }
            Main.sendMessage(player, "&7There are currently &c0&7 plots pending for approval.");
            return true;
        }
        if (args[0].equals("deny")) {
            
            if(!PlayerFunctions.isInPlot(player)) {
                sendMessage(player, C.NOT_IN_PLOT);
                return false;
            }
            PlotId id = PlayerFunctions.getPlot(player.getLocation());
            
            World world = player.getWorld();
            Plot plot = PlotMain.getPlots(world).get(id); 
            if (plot==null || !plot.hasOwner()) {
                sendMessage(player, C.NOT_IN_PLOT);
                return false;
            }
            
            Flag flag = plot.settings.getFlag("done");
            if (flag==null) {
                Main.sendMessage(player, "&7This plot is not &cpending&7 for approval.");
                return false;
            }
            Set<Flag> flags = plot.settings.getFlags();
            flags.remove(flag);
            plot.settings.setFlags(flags.toArray(new Flag[0]));
            DBFunc.setFlags(player.getWorld().getName(), plot, plot.settings.getFlags().toArray(new Flag[0]));
            
//            int coolTime = Main.config.getInt("reapproval-wait-time-sec");
            
            String owner = UUIDHandler.getName(plot.owner);
            if (owner!=null) {
                Main.cooldown.put(owner, (System.currentTimeMillis()/1000));
            }
            Main.sendMessage(player, "&aSuccessfully unapproved plot!");
            return true;
        }
        return true;
    }
    private ArrayList<Plot> getPlots() {
        
        ArrayList<Plot> plots = new ArrayList<Plot>();
        
        for (Plot plot : PlotMain.getPlots()) {
            if (plot.hasOwner()) {
                Flag flag = plot.settings.getFlag("done");
                if (flag!=null) {
                    if (flag.getValue().equals("false")) {
                        plots.add(plot);
                    }
                }
            }
        }
        return plots;
    }
    private ArrayList<Plot> getPlots(World world) {
        
        ArrayList<Plot> plots = new ArrayList<Plot>();
        
        for (Plot plot : PlotMain.getPlots(world).values()) {
            if (plot.hasOwner()) {
                Flag flag = plot.settings.getFlag("done");
                if (flag!=null) {
                    if (flag.getValue().equals("false")) {
                        plots.add(plot);
                    }
                }
            }
        }
        return plots;
    }
    private int countApproved(UUID owner, World world) {
        int count = 0;
        for (Plot plot : PlotMain.getPlots(world).values()) {
            if (plot.owner.equals(owner)) {
                Flag flag = plot.settings.getFlag("done");
                if (flag!=null) {
                    if (flag.getValue().equals("true")) {
                        count++;
                    }
                }
            }
        }
        return count;
    }
}