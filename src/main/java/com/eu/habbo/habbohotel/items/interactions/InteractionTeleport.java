package com.eu.habbo.habbohotel.items.interactions;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.threading.runnables.RoomUnitTeleportWalkToAction;
import com.eu.habbo.threading.runnables.teleport.TeleportActionOne;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

public class InteractionTeleport extends HabboItem
{
    private int targetId;
    private int targetRoomId;

    public InteractionTeleport(ResultSet set, Item baseItem) throws SQLException
    {
        super(set, baseItem);
    }

    public InteractionTeleport(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells)
    {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public void serializeExtradata(ServerMessage serverMessage)
    {
        serverMessage.appendInt((this.isLimited() ? 256 : 0));
        serverMessage.appendString(this.getExtradata());

        super.serializeExtradata(serverMessage);
    }

    @Override
    public boolean canWalkOn(RoomUnit roomUnit, Room room, Object[] objects)
    {
        return true;
    }

    @Override
    public boolean isWalkable()
    {
        return false;
    }

    @Override
    public void onClick(GameClient client, Room room, Object[] objects) throws Exception
    {
        super.onClick(client, room, objects);

        if(room != null && client != null && objects.length <= 1)
        {
            client.getHabbo().getRoomUnit().cmdTeleport = false;
            RoomTile loc = room.getLayout().getTile(this.getX(), this.getY());
            if (loc.equals(room.getLayout().getDoorTile()))
                return;

            if (!this.getBaseItem().allowWalk())
            {
                loc = HabboItem.getSquareInFront(room.getLayout(), this);
            }

            if (loc != null)
            {
                if (this.canUseTeleport(client, loc, room))
                {
                    client.getHabbo().getRoomUnit().isTeleporting = true;
                    //new TeleportInteraction(room, client, this).run();
                    this.setExtradata("1");
                    room.updateItem(this);

                    Emulator.getThreading().run(new TeleportActionOne(this, room, client), 500);
                }
                else
                {
                    client.getHabbo().getRoomUnit().setGoalLocation(loc);
                    Emulator.getThreading().run(new RoomUnitTeleportWalkToAction(client.getHabbo(), this, room), client.getHabbo().getRoomUnit().getPath().size() + 2 * 510);
                }
            }
        }
    }

    @Override
    public void onWalk(RoomUnit roomUnit, Room room, Object[] objects) throws Exception
    {
    }

    @Override
    public void onWalkOn(RoomUnit roomUnit, Room room, Object[] objects) throws Exception
    {
        super.onWalkOn(roomUnit, room, objects);
    }

    @Override
    public void onWalkOff(RoomUnit roomUnit, Room room, Object[] objects) throws Exception
    {
        super.onWalkOff(roomUnit, room, objects);
    }

    @Override
    public void run()
    {
        if(!this.getExtradata().equals("0"))
        {
            this.setExtradata("0");

            Room room = Emulator.getGameEnvironment().getRoomManager().getRoom(this.getRoomId());
            if(room != null)
            {
                room.updateItem(this);
            }
        }
        super.run();
    }

    @Override
    public void onPickUp(Room room)
    {
        this.targetId = 0;
        this.targetRoomId = 0;
        this.setExtradata("0");
    }

    private boolean canUseTeleport(GameClient client, RoomTile front, Room room)
    {
        if(!this.getExtradata().equals("0"))
            return false;

        if(client.getHabbo().getRoomUnit().isTeleporting)
            return false;

        if (client.getHabbo().getRoomUnit().getCurrentLocation().is(this.getX(), this.getY()))
            return true;

        if(client.getHabbo().getRoomUnit().getX() != front.x)
            return false;

        if(client.getHabbo().getRoomUnit().getY() != front.y)
            return false;

        Set<Habbo> habbos = room.getHabbosAt(this.getX(), this.getY());
        habbos.remove(client.getHabbo());
        if(!habbos.isEmpty())
            return false;

        return true;
    }

    public int getTargetId()
    {
        return this.targetId;
    }

    public void setTargetId(int targetId)
    {
        this.targetId = targetId;
    }

    public int getTargetRoomId()
    {
        return this.targetRoomId;
    }

    public void setTargetRoomId(int targetRoomId)
    {
        this.targetRoomId = targetRoomId;
    }

}
