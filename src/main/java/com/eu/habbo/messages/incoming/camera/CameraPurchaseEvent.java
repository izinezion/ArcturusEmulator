package com.eu.habbo.messages.incoming.camera;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.achievements.AchievementManager;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.camera.CameraPurchaseSuccesfullComposer;
import com.eu.habbo.messages.outgoing.inventory.AddHabboItemComposer;
import com.eu.habbo.messages.outgoing.inventory.InventoryRefreshComposer;

public class CameraPurchaseEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        if (this.client.getHabbo().getHabboInfo().getPhotoTimestamp() != 0) {
            HabboItem photoItem = Emulator.getGameEnvironment().getItemManager().createItem(this.client.getHabbo().getHabboInfo().getId(), Emulator.getGameEnvironment().getItemManager().getItem(Emulator.getConfig().getInt("camera.item_id")), 0, 0, this.client.getHabbo().getHabboInfo().getPhotoJSON());

            if (photoItem != null) {
                this.client.getHabbo().getInventory().getItemsComponent().addItem(photoItem);

                this.client.sendResponse(new CameraPurchaseSuccesfullComposer());
                this.client.sendResponse(new AddHabboItemComposer(photoItem));
                this.client.sendResponse(new InventoryRefreshComposer());

                this.client.getHabbo().giveCredits(-Emulator.getConfig().getInt("camera.price.credits"));
                this.client.getHabbo().givePixels(-Emulator.getConfig().getInt("camera.price.points"));

                AchievementManager.progressAchievement(this.client.getHabbo(), Emulator.getGameEnvironment().getAchievementManager().getAchievement("CameraPhotoCount"));
            }
        }
    }
}