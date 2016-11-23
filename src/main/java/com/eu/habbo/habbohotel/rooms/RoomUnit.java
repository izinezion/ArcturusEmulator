package com.eu.habbo.habbohotel.rooms;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionGuildGate;
import com.eu.habbo.habbohotel.items.interactions.InteractionMultiHeight;
import com.eu.habbo.habbohotel.items.interactions.InteractionTeleport;
import com.eu.habbo.habbohotel.items.interactions.games.freeze.InteractionFreezeBlock;
import com.eu.habbo.habbohotel.users.DanceType;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserStatusComposer;
import com.eu.habbo.plugin.Event;
import com.eu.habbo.plugin.events.roomunit.RoomUnitLookAtPointEvent;
import com.eu.habbo.plugin.events.roomunit.RoomUnitSetGoalEvent;
import com.eu.habbo.plugin.events.users.UserIdleEvent;
import com.eu.habbo.plugin.events.users.UserTakeStepEvent;
import com.eu.habbo.threading.runnables.RoomUnitKick;
import com.eu.habbo.util.pathfinding.Node;
import com.eu.habbo.util.pathfinding.PathFinder;
import com.eu.habbo.util.pathfinding.Rotation;
import gnu.trove.map.TMap;
import gnu.trove.map.hash.THashMap;

public class RoomUnit
{
    private int id;
    private RoomTile startLocation;
    private RoomTile previousLocation;
    private RoomTile currentLocation;
    private RoomTile goalLocation;

    private double z;

    private int tilesWalked;

    private boolean inRoom;
    private boolean canWalk;
    private boolean fastWalk = false;
    public boolean animateWalk = false;
    public boolean cmdTeleport = false;
    public boolean cmdSit = false;
    public boolean cmdLay = false;
    public boolean sitUpdate = false;
    public boolean isTeleporting = false;
    public boolean isKicked = false;
    public int talkTimeOut;
    public volatile short talkCounter;

    private final THashMap<String, String> status;
    private final THashMap<String, Object> cacheable;
    private RoomUserRotation bodyRotation;
    private RoomUserRotation headRotation;
    private DanceType danceType;
    private RoomUnitType roomUnitType;
    private PathFinder pathFinder;
    private int handItem;
    private long handItemTimestamp;
    private int walkTimeOut;
    private int effectId;

    public boolean wiredMuted;
    public boolean modMuted;
    public int modMuteTime;
    private int idleTimer;
    private Room room;

    public RoomUnit()
    {
        this.id = 0;
        this.inRoom = false;
        this.canWalk = true;
        this.status = new THashMap<String, String>();
        this.cacheable = new THashMap<String, Object>();
        this.roomUnitType = RoomUnitType.UNKNOWN;
        this.bodyRotation = RoomUserRotation.NORTH;
        this.bodyRotation = RoomUserRotation.NORTH;
        this.danceType = DanceType.NONE;
        this.pathFinder = new PathFinder(this);
        this.handItem = 0;
        this.handItemTimestamp = 0;
        this.walkTimeOut = Emulator.getIntUnixTimestamp();
        this.effectId = 0;
        this.wiredMuted = false;
        this.modMuted = false;
        this.isKicked = false;
    }

    public void clearWalking()
    {
        this.goalLocation = null;
        this.startLocation = this.currentLocation;
        this.inRoom = false;

        synchronized (this.status)
        {
            this.status.clear();
        }

        this.cacheable.clear();
    }

    public void stopWalking()
    {
        synchronized (this.status)
        {
            this.status.remove("mv");
            this.setGoalLocation(this.currentLocation);
        }
    }

    public boolean cycle(Room room)
    {
        try
        {
            /**
             * !this.getStatus().containsKey("mv") &&
             */
            if (!this.isWalking() && !isKicked)
            {
                this.getStatus().remove("mv");
                return false;
            }

            if (this.getStatus().containsKey("mv"))
            {
                this.getStatus().remove("mv");
            }
            if (this.getStatus().containsKey("lay"))
            {
                this.getStatus().remove("lay");
            }
            if (this.getStatus().containsKey("sit"))
            {
                this.getStatus().remove("sit");
            }

            if(this.pathFinder == null)
                return true;

            if(this.fastWalk && this.getPathFinder().getPath().size() >= 3)
            {
                this.getPathFinder().getPath().poll();
                this.getPathFinder().getPath().poll();
            }

            Node next = this.getPathFinder().getPath().poll();

            if (next == null)
                return true;

            Habbo habbo = room.getHabbo(this);

            if(this.getStatus().containsKey("ded"))
            {
                this.getStatus().remove("ded");
            }

            if(habbo != null)
            {
                if(this.isIdle())
                {
                    UserIdleEvent event = new UserIdleEvent(habbo, UserIdleEvent.IdleReason.WALKED, false);
                    Emulator.getPluginManager().fireEvent(event);

                    if (!event.isCancelled())
                    {
                        if (!event.idle)
                        {
                            room.unIdle(habbo);
                            this.idleTimer = 0;
                        }
                    }
                }

                if (Emulator.getPluginManager().isRegistered(UserTakeStepEvent.class, false))
                {
                    Event e = new UserTakeStepEvent(habbo, room.getLayout().getTile(this.getX(), this.getY()), next);
                    Emulator.getPluginManager().fireEvent(e);

                    if (e.isCancelled())
                        return true;
                }
            }

            HabboItem item = room.getTopItemAt(next.getX(), next.getY());

            boolean canSitNextTile = room.canSitAt(next.getX(), next.getY());

            if(canSitNextTile)
            {
                HabboItem lowestChair = room.getLowestChair(next.getX(), next.getY());

                if(lowestChair != null)
                    item = lowestChair;
            }

            if(!(this.getPathFinder().getPath().size() == 1 && canSitNextTile))
            {
                if (!room.tileWalkable((short) next.getX(), (short) next.getY()) && !(item instanceof InteractionTeleport))
                {
                    this.getPathFinder().findPath();
                    if (this.getPathFinder().getPath().isEmpty())
                    {
                        room.sendComposer(new RoomUserStatusComposer(this).compose());
                        return false;
                    }

                    next = this.getPathFinder().getPath().poll();
                }
            }

            double zHeight = 0.0D;

            if(habbo != null)
            {
                if (habbo.getHabboInfo().getRiding() != null)
                {
                    zHeight += 1.0D;
                }
            }

            HabboItem habboItem = room.getTopItemAt(this.getX(), this.getY());
            if(habboItem != null)
            {
                if(habboItem != item || !PathFinder.pointInSquare(habboItem.getX(), habboItem.getY(), habboItem.getX() + habboItem.getBaseItem().getWidth() - 1, habboItem.getY() + habboItem.getBaseItem().getLength() - 1, next.getX(), next.getY()))
                    habboItem.onWalkOff(this, room, null);
            }

            this.tilesWalked++;

            RoomUserRotation oldRotation = this.getBodyRotation();
            this.setRotation(RoomUserRotation.values()[Rotation.Calculate(this.getX(), this.getY(), next.getX(), next.getY())]);
            if (item != null)
            {
                if(item != habboItem || !PathFinder.pointInSquare(item.getX(), item.getY(), item.getX() + item.getBaseItem().getWidth() - 1, item.getY() + item.getBaseItem().getLength() - 1, this.getX(), this.getY()))
                {
                    if(item.canWalkOn(this, room, null))
                    {
                        item.onWalkOn(this, room, null);
                    }
                    else if(item instanceof InteractionGuildGate)
                    {
                        this.setRotation(oldRotation);
                        this.tilesWalked--;
                        this.setGoalLocation(this.currentLocation);
                        this.getStatus().remove("mv");
                        room.sendComposer(new RoomUserStatusComposer(this).compose());
                        return false;
                    }
                }
                else
                {
                    item.onWalk(this, room, null);
                }

                zHeight += item.getZ();

                if(!item.getBaseItem().allowSit() && !item.getBaseItem().allowLay())
                {
                    zHeight += item.getBaseItem().getHeight();

                    if(item instanceof InteractionMultiHeight)
                    {
                        if(item.getExtradata().length() == 0)
                        {
                            item.setExtradata("0");
                        }
                        zHeight += Item.getCurrentHeight(item);
                    }
                    else if(item instanceof InteractionFreezeBlock)
                    {
                        zHeight -= item.getBaseItem().getHeight();
                    }
                }
            } else
            {
                zHeight += room.getLayout().getHeightAtSquare(next.getX(), next.getY());
            }

            this.previousLocation = this.currentLocation;
            this.getStatus().put("mv", next.getX() + "," + next.getY() + "," + zHeight);
            //room.sendComposer(new RoomUserStatusComposer(this).compose());

            this.setZ(zHeight);
            this.setCurrentLocation(room.getLayout().getTile((short) next.getX(), (short) next.getY()));
            this.resetIdleTimer();

            if (habbo != null)
            {
                if (next.getX() == room.getLayout().getDoorX() && next.getY() == room.getLayout().getDoorY())
                {
                    Emulator.getThreading().run(new RoomUnitKick(habbo, room, false), 500);
                }
            }

            return false;

        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public RoomTile getCurrentLocation()
    {
        return this.currentLocation;
    }

    public short getX()
    {
        return this.currentLocation.x;
    }

    public short getY()
    {
        return this.currentLocation.y;
    }

    public double getZ()
    {
        return this.z;
    }

    public void setZ(double z)
    {
        this.z = z;
    }

    public boolean isInRoom() {
        return inRoom;
    }

    public synchronized void setInRoom(boolean inRoom) {
        this.inRoom = inRoom;
    }

    public RoomUnitType getRoomUnitType() {
        return roomUnitType;
    }

    public synchronized void setRoomUnitType(RoomUnitType roomUnitType) {
        this.roomUnitType = roomUnitType;
    }

    public void setRotation(RoomUserRotation rotation)
    {
        this.bodyRotation = rotation;
        this.headRotation = rotation;
    }

    public RoomUserRotation getBodyRotation() {
        return bodyRotation;
    }

    public synchronized void setBodyRotation(RoomUserRotation bodyRotation) {
        this.bodyRotation = bodyRotation;
    }

    public RoomUserRotation getHeadRotation() {
        return headRotation;
    }

    public synchronized void setHeadRotation(RoomUserRotation headRotation) {
        this.headRotation = headRotation;
    }

    public DanceType getDanceType() {
        return danceType;
    }

    public synchronized void setDanceType(DanceType danceType) {
        this.danceType = danceType;
    }

    public void setCanWalk(boolean value)
    {
        this.canWalk = value;
    }

    public boolean canWalk()
    {
        return this.canWalk;
    }

    public void setFastWalk(boolean fastWalk)
    {
        this.fastWalk = fastWalk;
    }

    public boolean isFastWalk()
    {
        return this.fastWalk;
    }

    public RoomTile getStartLocation()
    {
        return this.startLocation;
    }

    public int tilesWalked()
    {
        return this.tilesWalked;
    }

    public RoomTile getGoal()
    {
        return this.goalLocation;
    }

    public void setGoalLocation(RoomTile goalLocation)
    {
        if(Emulator.getPluginManager().isRegistered(RoomUnitSetGoalEvent.class, false))
        {
            Event event = new RoomUnitSetGoalEvent(this.room, this, goalLocation);
            Emulator.getPluginManager().fireEvent(event);

            if(event.isCancelled())
                return;
        }

        this.startLocation = this.currentLocation;
        this.goalLocation = goalLocation;
        this.tilesWalked = 0;
        this.pathFinder.findPath();
        this.cmdSit = false;
    }

    public void setLocation(RoomTile location)
    {
        if (location != null)
        {
            this.startLocation    = location;
            this.previousLocation = location;
            this.currentLocation  = location;
            this.goalLocation     = location;
        }
    }

    public void setCurrentLocation(RoomTile location)
    {
        if (location != null)
        {
            this.currentLocation = location;
        }
    }

    public RoomTile getPreviousLocation()
    {
        return this.previousLocation;
    }

    public void setPreviousLocation(RoomTile previousLocation)
    {
        this.previousLocation = previousLocation;
    }

    public PathFinder getPathFinder() {
        return pathFinder;
    }

    public void setPathFinder(PathFinder pathFinder) { this.pathFinder = pathFinder; }

    public void setPathFinderRoom(Room room)
    {
        this.pathFinder.setRoom(room);
        this.room = room;
    }

    public boolean isAtGoal()
    {
        return this.currentLocation.equals(this.goalLocation);
    }

    public boolean isWalking()
    {
        return !isAtGoal() && this.canWalk;
    }

    public synchronized TMap<String, String> getStatus()
    {
        return this.status;
    }

    public synchronized boolean hasStatus(String key)
    {
        return this.status.containsKey(key);
    }

    public TMap<String, Object> getCacheable()
    {
        return this.cacheable;
    }

    public int getHandItem()
    {
        return this.handItem;
    }

    public void setHandItem(int handItem)
    {
        this.handItem = handItem;
        this.handItemTimestamp = System.currentTimeMillis();
    }

    public long getHandItemTimestamp()
    {
        return this.handItemTimestamp;
    }

    public int getEffectId()
    {
        return this.effectId;
    }

    public void setEffectId(int effectId)
    {
        this.effectId = effectId;
    }

    public int getWalkTimeOut()
    {
        return this.walkTimeOut;
    }

    public void setWalkTimeOut(int walkTimeOut)
    {
        this.walkTimeOut = walkTimeOut;
    }

    public boolean canTalk()
    {
        if(this.wiredMuted)
            return false;

        return !this.isModMuted();
    }

    public boolean isModMuted()
    {
        if(this.modMuted)
        {
            if (this.modMuteTime < Emulator.getIntUnixTimestamp())
            {
                this.modMuted = false;
            }
        }

        return this.modMuted;
    }

    public void increaseIdleTimer()
    {
        this.idleTimer++;
    }

    public boolean isIdle()
    {
        return this.idleTimer > Emulator.getConfig().getInt("hotel.roomuser.idle.cycles", 240); //Amount of room cycles / 2 = seconds.
    }

    public int getIdleTimer()
    {
        return this.idleTimer;
    }

    public void resetIdleTimer()
    {
        this.idleTimer = 0;
    }

    public void setIdle()
    {
        this.idleTimer = Emulator.getConfig().getInt("hotel.roomuser.idle.cycles", 240);
    }

    public void lookAtPoint(RoomTile location)
    {
        if(Emulator.getPluginManager().isRegistered(RoomUnitLookAtPointEvent.class, false))
        {
            Event lookAtPointEvent = new RoomUnitLookAtPointEvent(this.room, this, location);
            Emulator.getPluginManager().fireEvent(lookAtPointEvent);

            if(lookAtPointEvent.isCancelled())
                return;
        }

        this.bodyRotation = (RoomUserRotation.values()[Rotation.Calculate(this.getX(), this.getY(), location.x, location.y)]);
        this.headRotation = (RoomUserRotation.values()[Rotation.Calculate(this.getX(), this.getY(), location.x, location.y)]);
    }
}
