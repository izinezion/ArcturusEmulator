package com.eu.habbo.habbohotel.navigation;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NavigatorFavoriteFilter extends NavigatorFilter
{
    public final static String name = "favorites";

    public NavigatorFavoriteFilter()
    {
        super(name);
    }

    @Override
    public List<SearchResultList> getResult(Habbo habbo)
    {
        List<SearchResultList> resultLists = new ArrayList<SearchResultList>();
        List<Room> rooms = Emulator.getGameEnvironment().getRoomManager().getRoomsFavourite(habbo);
        Collections.sort(rooms);
        resultLists.add(new SearchResultList(0, "favorites", "", SearchAction.NONE, ListMode.LIST, DisplayMode.VISIBLE, rooms, true, true));
        return resultLists;
    }
}