package org.popcraft.boltchestshop;

import com.Acrobot.ChestShop.Configuration.Properties;
import com.Acrobot.ChestShop.Events.Protection.ProtectBlockEvent;
import com.Acrobot.ChestShop.Events.Protection.ProtectionCheckEvent;
import com.Acrobot.ChestShop.Events.ShopDestroyedEvent;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.popcraft.bolt.BoltAPI;
import org.popcraft.bolt.protection.BlockProtection;
import org.popcraft.bolt.protection.Protection;

import java.util.UUID;

public final class BoltChestShop extends JavaPlugin implements Listener {
    private BoltAPI bolt;

    @Override
    public void onEnable() {
        this.bolt = getServer().getServicesManager().load(BoltAPI.class);
        if (bolt == null) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        this.bolt = null;
        HandlerList.unregisterAll((Plugin) this);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onProtectionCheck(final ProtectionCheckEvent event) {
        if (event.getResult() == Event.Result.DENY && !Properties.TURN_OFF_DEFAULT_PROTECTION_WHEN_PROTECTED_EXTERNALLY) {
            return;
        }
        final Block block = event.getBlock();
        final Player player = event.getPlayer();
        if (player == null || block == null) {
            return;
        }
        final Protection protection = bolt.findProtection(block);
        if (protection == null) {
            return;
        }
        if (protection.getOwner().equals(player.getUniqueId()) || bolt.canAccess(protection, player, "chestshop")) {
            event.setResult(Event.Result.ALLOW);
        } else {
            event.setResult(Event.Result.DENY);
        }
    }

    @EventHandler
    public void onProtectBlock(final ProtectBlockEvent event) {
        if (event.isProtected()) {
            return;
        }
        final Block block = event.getBlock();
        final Player player = event.getPlayer();
        if (player == null || block == null) {
            return;
        }
        final Protection existingProtection = bolt.findProtection(block);
        if (existingProtection != null) {
            event.setProtected(true);
            return;
        }
        final UUID owner = event.getProtectionOwner();
        final String type = switch (event.getType()) {
            case PRIVATE -> "private";
            case PUBLIC -> "public";
            case DONATION -> "deposit";
            case DISPLAY -> "display";
        };
        final BlockProtection protection = bolt.createProtection(block, owner, type);
        bolt.saveProtection(protection);
        event.setProtected(true);
    }

    @EventHandler
    public void onShopDestroyed(final ShopDestroyedEvent event) {
        final BlockProtection signProtection = bolt.loadProtection(event.getSign().getBlock());
        if (signProtection != null) {
            bolt.removeProtection(signProtection);
        }
        final Container container = event.getContainer();
        if (container == null || !Properties.REMOVE_LWC_PROTECTION_AUTOMATICALLY) {
            return;
        }
        final BlockProtection containerProtection = bolt.loadProtection(container.getBlock());
        if (containerProtection != null) {
            bolt.removeProtection(containerProtection);
        }
    }
}
