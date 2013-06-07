/*
 * Copyright (C) 2013 Samuel Penn, sam@glendale.org.uk
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation version 3, or
 * any later version. See the file COPYING.
 */
package uk.org.glendale.hexweb.services

import uk.org.glendale.hexweb.Hex
import uk.org.glendale.hexweb.MapInfo

import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement
import org.hibernate.Session
import org.hibernate.SessionFactory;
import org.hibernate.jdbc.Work

/**
 * Services needed by Map.
 */
class MapService {
	def SessionFactory		sessionFactory
	
	/**
	 * Gets the map details for the map specified by unique id.
	 * 
	 * @param id	Numeric id.
	 */
	def getMapByNameOrId(int id) {
		return MapInfo.findById(id)
	}

	/**
	 * Gets the map details for the map specified by name. If the
	 * name is actually an integer, then call call the integer
	 * version of the method instead.
	 * 
	 * @param name	Name of the map, or its integer id.
	 */
    def getMapByNameOrId(String name) {
		if (name == null) {
			throw new IllegalArgumentException("Map name cannot be null")
		}
		try {
			int id = Integer.parseInt(name)
			return getMapByNameOrId(id)	
		} catch (NumberFormatException e) {
			// Not an integer, so try by name.
		}
		return MapInfo.findByName(name)
    }
	
	/**
	 * Gets the hex at the specified coordinates for the map. If there is
	 * no hex defined, then null is returned.
	 * 
	 * @param info		Map to get hex for.
	 * @param x			X coordinate.
	 * @param y			Y coordinate.
	 */
	def getHex(MapInfo info, int x, int y) {
		return Hex.find ({
			eq("mapInfo", info)
			eq("x", x)
			eq("y", y)
		});
	}

	/**
	 * Removes all hex data from a map. Uses raw SQL for performance.
	 * 
	 * @param info
	 * @return
	 */
	def clearMap(MapInfo info) {
		def session = sessionFactory.getCurrentSession()
		
		sessionFactory.currentSession.doWork(new Work() {
			public void execute(Connection connection) {
				Statement stmnt = connection.createStatement()
				stmnt.executeUpdate("DELETE FROM map WHERE mapinfo_id=${info.id}")
			}
		})
	}
	
	/**
	 * Get terrain data from the map for generating a thumbnail. Uses raw SQL
	 * in order to get the data as quickly as possible.
	 * 
	 * @param info
	 * @param resolution
	 * @return
	 */
	def getThumbData(MapInfo info, int resolution) {
		List	list = new ArrayList()
		sessionFactory.currentSession.doWork(new Work() {
			public void execute(Connection connection) {
				String sql = String.format("select x, y, terrain_id from map where mapinfo_id=%d and "+
					                       "x mod %d = 0 and y mod %d = 0 order by y, x",
										   info.id, resolution, resolution)
				Statement stmnt = connection.prepareStatement(sql)
				ResultSet rs = stmnt.executeQuery(sql)
				while (rs.next()) {
					list.add([ "x": rs.getInt(1), "y": rs.getInt(2), "t": rs.getInt(3)])
				}
				rs.close()
			}
		})
		return list
	}
	
}
