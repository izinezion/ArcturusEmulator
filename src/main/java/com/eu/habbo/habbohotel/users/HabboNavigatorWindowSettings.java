package com.eu.habbo.habbohotel.users;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.navigation.DisplayMode;
import com.eu.habbo.habbohotel.navigation.ListMode;
import gnu.trove.map.hash.THashMap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class HabboNavigatorWindowSettings
{
    private final int userId;
    public int x = 100;
    public int y = 100;
    public int width = 425;
    public int height = 535;
    public boolean openSearches = false;
    public int unknown = 0;
    public final THashMap<String, HabboNavigatorPersonalDisplayMode> displayModes = new THashMap<>(2);

    public HabboNavigatorWindowSettings(int userId)
    {
        this.userId = userId;
    }

    public HabboNavigatorWindowSettings(ResultSet set) throws SQLException
    {
        this.userId = set.getInt("user_id");
        this.x = set.getInt("x");
        this.y = set.getInt("y");
        this.width = set.getInt("width");
        this.height = set.getInt("height");
        this.openSearches = set.getString("open_searches").equals("1");
        this.unknown = 0;
    }

    public void addDisplayMode(String category, HabboNavigatorPersonalDisplayMode displayMode)
    {
        this.displayModes.put(category, displayMode);
    }

    public boolean hasDisplayMode(String category)
    {
        return this.displayModes.containsKey(category);
    }

    public void insertDisplayMode(String category, ListMode listMode, DisplayMode displayMode)
    {
        if (!this.displayModes.containsKey(category))
        {
            try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
                 PreparedStatement statement = connection.prepareStatement("INSERT INTO users_navigator_settings (user_id, caption, list_type, display) VALUES (?, ?, ?, ?)"))
            {
                statement.setInt(1, this.userId);
                statement.setString(2, category);
                statement.setString(3, listMode.name().toLowerCase());
                statement.setString(4, displayMode.name().toLowerCase());
                statement.execute();
            }
            catch (SQLException e)
            {
                Emulator.getLogging().logSQLException(e);
            }

            this.displayModes.put(category, new HabboNavigatorPersonalDisplayMode(listMode, displayMode));
        }
    }

    public void setDisplayMode(String category, DisplayMode displayMode)
    {
        HabboNavigatorPersonalDisplayMode personalDisplayMode = this.displayModes.get(category);

        if (personalDisplayMode != null)
        {
            personalDisplayMode.displayMode = displayMode;
        }
        else
        {
            insertDisplayMode(category, ListMode.LIST, displayMode);
        }
    }

    public void setListMode(String category, ListMode listMode)
    {
        HabboNavigatorPersonalDisplayMode personalDisplayMode = this.displayModes.get(category);

        if (personalDisplayMode != null)
        {
            personalDisplayMode.listMode = listMode;
        }
        else
        {
            insertDisplayMode(category, listMode, DisplayMode.VISIBLE);
        }
    }

    public DisplayMode getDisplayModeForCategory(String category)
    {
        if (this.displayModes.containsKey(category))
        {
            return this.displayModes.get(category).displayMode;
        }

        return DisplayMode.VISIBLE;
    }

    public ListMode getListModeForCategory(String category)
    {
        if (this.displayModes.containsKey(category))
        {
            return this.displayModes.get(category).listMode;
        }

        return ListMode.LIST;
    }

    public void save(Connection connection)
    {
        try (PreparedStatement statement = connection.prepareStatement("UPDATE users_navigator_settings SET list_type = ?, display = ? WHERE user_id = ? AND caption = ? LIMIT 1"))
        {
            for (Map.Entry<String, HabboNavigatorPersonalDisplayMode> set : this.displayModes.entrySet())
            {
                statement.setString(1, set.getValue().listMode.name().toLowerCase());
                statement.setString(2, set.getValue().displayMode.name().toLowerCase());
                statement.setInt(3, this.userId);
                statement.setString(4, set.getKey());
                statement.execute();
            }
        }
        catch (SQLException e)
        {
            Emulator.getLogging().logSQLException(e);
        }
    }
}
