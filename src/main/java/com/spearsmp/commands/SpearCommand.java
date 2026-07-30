package com.spearsmp.commands;

import com.spearsmp.SpearSMPPlugin;
import com.spearsmp.managers.ConfigManager;
import com.spearsmp.managers.PlayerDataManager;
import com.spearsmp.managers.RecipeManager;
import com.spearsmp.spears.SpearType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Handles every {@code /spear} subcommand:
 * <pre>
 * /spear give &lt;player&gt; &lt;legendary|godly&gt;
 * /spear reload
 * /spear resetcraft &lt;player&gt; [spear]
 * </pre>
 */
public final class SpearCommand implements CommandExecutor, TabCompleter {

    private final SpearSMPPlugin plugin;
    private final ConfigManager configManager;
    private final PlayerDataManager playerDataManager;
    private final RecipeManager recipeManager;

    public SpearCommand(SpearSMPPlugin plugin, ConfigManager configManager,
                         PlayerDataManager playerDataManager, RecipeManager recipeManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.playerDataManager = playerDataManager;
        this.recipeManager = recipeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(configManager.getMessage("unknown-command"));
            return true;
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "give" -> handleGive(sender, args);
            case "reload" -> handleReload(sender);
            case "resetcraft" -> handleResetCraft(sender, args);
            default -> {
                sender.sendMessage(configManager.getMessage("unknown-command"));
                yield true;
            }
        };
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("spear.admin")) {
            sender.sendMessage(configManager.getMessage("no-permission"));
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(configManager.getMessage("give-usage"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(configManager.getMessage("player-not-found"));
            return true;
        }

        var typeOpt = SpearType.fromString(args[2]);
        if (typeOpt.isEmpty()) {
            sender.sendMessage(configManager.getMessage("invalid-spear"));
            return true;
        }

        SpearType type = typeOpt.get();
        var item = recipeManager.getSpearFactory().build(type);
        target.getInventory().addItem(item);

        sender.sendMessage(configManager.getMessage("give-success",
                Map.of("spear", displayName(type), "player", target.getName())));
        target.sendMessage(configManager.getMessage("give-received",
                Map.of("spear", displayName(type))));
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("spear.admin")) {
            sender.sendMessage(configManager.getMessage("no-permission"));
            return true;
        }
        plugin.reloadEverything();
        sender.sendMessage(configManager.getMessage("reload-success"));
        return true;
    }

    private boolean handleResetCraft(CommandSender sender, String[] args) {
        if (!sender.hasPermission("spear.admin")) {
            sender.sendMessage(configManager.getMessage("no-permission"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(configManager.getMessage("resetcraft-usage"));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(configManager.getMessage("player-not-found"));
            return true;
        }

        if (args.length >= 3) {
            var typeOpt = SpearType.fromString(args[2]);
            if (typeOpt.isEmpty()) {
                sender.sendMessage(configManager.getMessage("invalid-spear"));
                return true;
            }
            playerDataManager.resetCrafted(target.getUniqueId(), typeOpt.get());
            sender.sendMessage(configManager.getMessage("resetcraft-success-single",
                    Map.of("player", String.valueOf(target.getName()), "spear", displayName(typeOpt.get()))));
        } else {
            playerDataManager.resetAllCrafted(target.getUniqueId());
            sender.sendMessage(configManager.getMessage("resetcraft-success",
                    Map.of("player", String.valueOf(target.getName()))));
        }
        return true;
    }

    private String displayName(SpearType type) {
        var section = configManager.getSpearSection(type);
        return section != null ? section.getString("display-name", type.name()) : type.name();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();

        if (args.length == 1) {
            options.addAll(List.of("give", "reload", "resetcraft"));
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("resetcraft"))) {
            Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
        } else if (args.length == 3 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("resetcraft"))) {
            Stream.of(SpearType.values()).map(SpearType::configKey).forEach(options::add);
        }

        String current = args[args.length - 1].toLowerCase(Locale.ROOT);
        options.removeIf(opt -> !opt.toLowerCase(Locale.ROOT).startsWith(current));
        return options;
    }
}
