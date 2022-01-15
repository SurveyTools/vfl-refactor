package org.surveytools.flightlogger.geo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import org.surveytools.flightlogger.geo.data.Route;
import org.surveytools.flightlogger.geo.data.Transect;

import android.content.res.Resources.NotFoundException;
import android.location.Location;
import android.util.Log;

public class GPSUtils {

	public final static float FEET_PER_METER = 3.2808399f;
	public final static float MILES_PER_METER = 0.000621371192f;
	public final static float KILOMETERS_PER_METER = 0.001f;
	public final static float NAUTICAL_MILES_PER_METER = 0.000539956803f;
	public final static float M_PER_SEC_TO_KILOMETERS_PER_HOUR = 3.6f;
	public final static float M_PER_SEC_TO_MILES_PER_HOUR = 2.23693629f;
	public final static float M_PER_SEC_TO_NAUTICAL_MILES_PER_PER_HOUR = 1.94384449f;
	public static final double EARTH_RADIUS_METERS = 6371008.7714; // mean avg for WGS84 projection

	private static final String TAG = NavigationService.class.getSimpleName();

	public enum TransectParsingMethod {
		// note: immutable since these are stored as prefs
		USE_DEFAULT, ADJACENT_PAIRS, ANGLES_OVER_5_NO_DUPS, ANGLES_OVER_10_NO_DUPS, ANGLES_OVER_15_NO_DUPS, ANGLES_OVER_20_NO_DUPS, ANGLES_OVER_30_NO_DUPS,
	}

	// PREF_UNITS
	public enum DistanceUnit {
		FEET, METERS, KILOMETERS, MILES, NAUTICAL_MILES
	}

	// PREF_UNITS
	public enum VelocityUnit {
		METERS_PER_SECOND, NAUTICAL_MILES_PER_HOUR, KILOMETERS_PER_HOUR, MILES_PER_HOUR
	}

	public enum DataAveragingMethod {
		// note: immutable since these are stored as prefs
		MEDIAN, MEAN
	}

	public enum DataAveragingWindow {
		// note: immutable since these are stored as prefs
		N_3_SAMPLES, N_5_SAMPLES, N_7_SAMPLES, N_9_SAMPLES
	}

	/**
	 * Parses GPX routes for use in navigation. Expected format of the form: <rte><name>Session 1</name> <rtept lat="-3.4985590" lon="38.9554692"><ele> -32768.000</ele><name>T01_S</name><sym>Waypoint</sym></rtept> <rtept lat="-3.0642325" lon="39.2115345"><ele>-32768.000</ele><name>T01_N</name>< sym>Waypoint</sym></rtept> <rtept lat="-3.0546140" lon="39.1860712"><ele>- 32768.000</ele><name>T02_N</name><sym>Waypoint</sym></rtept> <rtept lat="-3.5290935"
	 * lon="38.9157593"><ele>-32768.000</ele><name>T02_S</name>< sym>Waypoint</sym></rtept> <rtept lat="-3.5115202" lon="38.8969005"><ele>- 32768.000</ele><name>T03_S</name><sym>Waypoint</sym></rtept> <rtept lat="-3.0044045" lon="39.1936147"><ele>-32768.000</ele><name>T03_N</name>< sym>Waypoint</sym></rtept> </rte>
	 * 
	 * @author jayl
	 * 
	 */
	public static List<Route> parseRoute(File gpxFile) {
		List<Route> routeList = new ArrayList<Route>();

		if (gpxFile == null)
			return routeList;

		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			FileInputStream fileInputStream = new FileInputStream(gpxFile);
			Document document = documentBuilder.parse(fileInputStream);
			Element elementRoot = document.getDocumentElement();

			NodeList nodelist_routes = elementRoot.getElementsByTagName("rte");

			// any route
			if (nodelist_routes.getLength() > 0) {
				routeList.addAll(parseRoutes(elementRoot, nodelist_routes, gpxFile.getName()));
			} else // build a route out of waypoints
			{
				NodeList nodelist_waypts = elementRoot.getElementsByTagName("wpt");
				if (nodelist_waypts.getLength() > 0) {
					routeList.add(parseWaypointsAsRoute(elementRoot, nodelist_waypts, gpxFile.getName()));
				}
			}
			fileInputStream.close();

		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return routeList;
	}

	private static Route parseWaypointsAsRoute(Element elementRoot, NodeList waypts, String gpxFileName) {
		Route route = new Route();
		route.gpxFile = gpxFileName;
		route.mName = "Waypoint Route";

		for (int j = 0; j < waypts.getLength(); j++) {

			Node node = waypts.item(j);
			NamedNodeMap attributes = node.getAttributes();

			String newLatitude = attributes.getNamedItem("lat").getTextContent();
			Double newLatitude_double = Double.parseDouble(newLatitude);

			String newLongitude = attributes.getNamedItem("lon").getTextContent();
			Double newLongitude_double = Double.parseDouble(newLongitude);

			// for now, use the name field of the location to store
			// the waypt ordinal. TODO: Need to really go through the
			// child nodes and find 'name'
			Element e = (Element) node;
			String wayptName = e.getElementsByTagName("name").item(0).getTextContent();
			Location loc = new Location(wayptName);
			loc.setLatitude(newLatitude_double);
			loc.setLongitude(newLongitude_double);

			route.addWayPoint(loc);
		}
		// XXX short term hack - this really needs to be in the transect
		if (waypts.getLength() % 2 != 0)
			route.setParseErrorMsg("Uneven number of transect waypoints");

		return route;
	}

	private static List<Route> parseRoutes(Element elementRoot, NodeList routes, String gpxFileName) {
		List<Route> routeMap = new ArrayList<Route>();
		for (int i = 0; i < routes.getLength(); i++) {
			Route r = new Route();
			r.gpxFile = gpxFileName;

			Node rtNode = elementRoot.getElementsByTagName("rte").item(i);
			NamedNodeMap attribs = rtNode.getAttributes();
			Node nameAttrib = attribs.getNamedItem("name");
			r.mName = (nameAttrib == null) ? "Route " + (i + 1) : nameAttrib.getTextContent();
			// see if there are waypoints marked by the element 'rtept'
			NodeList nodelist_rtkpt = elementRoot.getElementsByTagName("rtept");

			for (int j = 0; j < nodelist_rtkpt.getLength(); j++) {

				Node node = nodelist_rtkpt.item(j);
				NamedNodeMap attributes = node.getAttributes();

				String newLatitude = attributes.getNamedItem("lat").getTextContent();
				Double newLatitude_double = Double.parseDouble(newLatitude);

				String newLongitude = attributes.getNamedItem("lon").getTextContent();
				Double newLongitude_double = Double.parseDouble(newLongitude);

				// for now, use the name field of the location to store
				// the waypt ordinal. TODO: Need to really go through the
				// child nodes and find 'name'
				Element e = (Element) node;
				String wayptName = e.getElementsByTagName("name").item(0).getTextContent();
				Location loc = new Location(wayptName);
				loc.setLatitude(newLatitude_double);
				loc.setLongitude(newLongitude_double);

				r.addWayPoint(loc);
			}
			// XXX Short term hack to indicate error - this really should be
			// bound to the transect
			if (nodelist_rtkpt.getLength() % 2 != 0)
				r.setParseErrorMsg("Uneven number of transect route points");
			routeMap.add(r);
		}
		return routeMap;
	}

	public static List<Transect> parseTransectsUsingPairs(Route route) {
		List<Transect> transects = new ArrayList<Transect>();

		if (route == null)
			return transects;

		int transectIndex = 1;
		List<Location> wp = route.mWayPoints;
		for (int i = 0; i < wp.size() - 1; i += 2) {
			transects.add(new Transect(wp.get(i), wp.get(i + 1), route.gpxFile, route.mName, transectIndex++));
		}
		return transects;
	}

	public static List<Transect> parseTransectsUsingAngles(Route route, double maxAngle, boolean allowDuplicates) {
		List<Transect> transects = new ArrayList<Transect>();

		if ((route == null) || (route.mWayPoints == null) || (route.mWayPoints.size() <= 1))
			return transects;

		int transectIndex = 1;
		List<Location> wp = route.mWayPoints;

		int n = wp.size();
		Location start = null;
		Location end = null;
		float lastAngle = 0;

		for (int i = 0; i < n; i++) {

			if (start == null) {
				// start a brand new transect
				start = wp.get(i);
			} else if (end == null) {
				// finish making the brand new transect (easy index checking)
				end = wp.get(i);
				lastAngle = start.bearingTo(end);
			} else {
				// get the current loc
				Location cur = wp.get(i);
				float curAngle = end.bearingTo(cur);
				float deviationAngle = Math.abs(curAngle - lastAngle);
				boolean isDuplicate = (curAngle < .01) && (end.distanceTo(cur) < .01);

				if ((deviationAngle > maxAngle) || (isDuplicate && !allowDuplicates)) {
					// make a new transect
					transects.add(new Transect(start, end, route.gpxFile, route.mName, transectIndex++));

					// start a new transect at the current point
					start = cur;
					end = null;
				} else {
					// current point is the new end of the transect
					end = cur;
					// adjust the angle accordingly
					lastAngle = curAngle;
				}
			}
		}

		// open transect or 2-point route
		if ((start != null) && (end != null)) {
			transects.add(new Transect(start, end, route.gpxFile, route.mName, transectIndex++));
			end = null;
		} else if (end != null) {
			// TODO - start with no end... waypoint mismatch
		}

		return transects;
	}

	public static List<Transect> parseTransects(Route route, TransectParsingMethod parsingMethod) {
		switch (parsingMethod) {

		case ADJACENT_PAIRS:
			return parseTransectsUsingPairs(route);
			
        case ANGLES_OVER_5_NO_DUPS:
            return parseTransectsUsingAngles(route, 5, false);

		case ANGLES_OVER_10_NO_DUPS:
			return parseTransectsUsingAngles(route, 10, false);

		case USE_DEFAULT:
		case ANGLES_OVER_15_NO_DUPS:
			return parseTransectsUsingAngles(route, 15, false);

		case ANGLES_OVER_20_NO_DUPS:
			return parseTransectsUsingAngles(route, 20, false);

		case ANGLES_OVER_30_NO_DUPS:
			return parseTransectsUsingAngles(route, 30, false);
		}

		// no dice - return empty list
		return new ArrayList<Transect>();
	}

	public static Route getDefaultRouteFromList(List<Route> routes) {
		if (routes != null) {
			if (!routes.isEmpty()) {
				return routes.get(0);
			}
		}

		// no dice
		return null;
	}

	public static Route getDefaultRouteFromFile(File gpxFileObj) {
		if (gpxFileObj != null) {
			return getDefaultRouteFromList(GPSUtils.parseRoute(gpxFileObj));
		}

		// no dice
		return null;
	}

	public static Route findRouteByName(String targetRouteName, List<Route> routesList) {
		if ((targetRouteName != null) && (routesList != null)) {

			Iterator<Route> iterator = routesList.iterator();
			while (iterator.hasNext()) {
				Route routeItem = iterator.next();
				if (routeItem.matchesByName(targetRouteName)) {
					// winner!
					return routeItem;
				}
			}
		}

		// no dice
		return null;
	}

	public static Route findRouteInFile(String targetRouteName, File gpxFileObj) {
		if (gpxFileObj != null) {
			List<Route> routes = GPSUtils.parseRoute(gpxFileObj);

			if (routes != null) {
				if (!routes.isEmpty())
					return routes.get(0);
			}
		}

		// no dice
		return null;
	}

	public static Transect findTransectInList(String targetTransectName, List<Transect> transects) {
		if (transects != null) {
			Iterator<Transect> iterator = transects.iterator();
			while (iterator.hasNext()) {
				Transect transItem = iterator.next();
				if (transItem.matchesByName(targetTransectName)) {
					// winner!
					return transItem;
				}
			}
		}

		// no dice
		return null;
	}

	public static Transect findTransectInRoute(String targetTransectName, Route route, TransectParsingMethod parsingMethod) {
		if (route != null) {
			return findTransectInList(targetTransectName, GPSUtils.parseTransects(route, parsingMethod));
		}

		// no dice
		return null;
	}

	public static Route getDefaultRouteFromFilename(String gpxFilename) {
		if (gpxFilename != null) {
			return getDefaultRouteFromFile(new File(gpxFilename));
		}

		// no dice
		return null;
	}

	public static Transect getDefaultTransectFromList(List<Transect> transects) {
		if (transects != null) {
			if (!transects.isEmpty())
				return transects.get(0);
		}

		// no dice
		return null;
	}

	public static Transect getNextTransectFromList(List<Transect> transects, Transect curTransect) {
		boolean takeTheNextOne = false;
		if (transects != null) {
			Iterator<Transect> iterator = transects.iterator();
			while (iterator.hasNext()) {
				Transect transItem = iterator.next();
				if (takeTheNextOne) {
					// winner!
					return transItem;
				} else if (transItem.matchesByName(curTransect.mName)) {
					// almost there
					takeTheNextOne = true;
				}
			}
		}

		// no dice
		return null;
	}

	// APP_SETTINGS_WIP - hash table
	public static String getKeyForTransectParsingMethod(TransectParsingMethod tpm) throws NotFoundException {

		switch(tpm) {
		case ADJACENT_PAIRS:
			return "tpm_adjacent_pairs";
			
        case ANGLES_OVER_5_NO_DUPS:
            return"tpm_angles_5";
	
		case ANGLES_OVER_10_NO_DUPS:
			return"tpm_angles_10";
	
		case USE_DEFAULT:
		case ANGLES_OVER_15_NO_DUPS:
			return "tpm_angles_15";
	
		case ANGLES_OVER_20_NO_DUPS:
			return "tpm_angles_20";
	
		case ANGLES_OVER_30_NO_DUPS:
			return "tpm_angles_30";
	}

		throw new NotFoundException("transect parsing method not found");
	}

	// APP_SETTINGS_WIP - hash table
	public static TransectParsingMethod getTransectParsingMethodForKey(String tpmKey) throws NotFoundException {

		if (tpmKey != null) {
			if (tpmKey.equalsIgnoreCase("tpm_adjacent_pairs"))
				return TransectParsingMethod.ADJACENT_PAIRS;
            else if (tpmKey.equalsIgnoreCase("tpm_angles_5"))
                return TransectParsingMethod.ANGLES_OVER_5_NO_DUPS;
			else if (tpmKey.equalsIgnoreCase("tpm_angles_10"))
				return TransectParsingMethod.ANGLES_OVER_10_NO_DUPS;
			else if (tpmKey.equalsIgnoreCase("tpm_angles_15"))
				return TransectParsingMethod.ANGLES_OVER_15_NO_DUPS;
			else if (tpmKey.equalsIgnoreCase("tpm_angles_20"))
				return TransectParsingMethod.ANGLES_OVER_20_NO_DUPS;
			else if (tpmKey.equalsIgnoreCase("tpm_angles_30"))
				return TransectParsingMethod.ANGLES_OVER_30_NO_DUPS;
		}

		throw new NotFoundException("transect parsing key not found");
	}

	// PREF_UNITS
	public static DistanceUnit getDistanceUnitForKey(String tpmKey) throws NotFoundException {

		if (tpmKey != null) {
			if (tpmKey.equalsIgnoreCase("distance_feet"))
				return DistanceUnit.FEET;
			else if (tpmKey.equalsIgnoreCase("distance_meters"))
				return DistanceUnit.METERS;
			else if (tpmKey.equalsIgnoreCase("distance_kilometers"))
				return DistanceUnit.KILOMETERS;
			else if (tpmKey.equalsIgnoreCase("distance_miles"))
				return DistanceUnit.MILES;
			else if (tpmKey.equalsIgnoreCase("distance_nautical_miles"))
				return DistanceUnit.NAUTICAL_MILES;
		}

		throw new NotFoundException("distance unit key not found");
	}

	// PREF_UNITS
	public static String getKeyForDistanceUnit(DistanceUnit unit) throws NotFoundException {
		switch (unit) {
			case FEET: return "distance_feet";
			case METERS: return "distance_meters";
			case KILOMETERS: return "distance_kilometers";
			case MILES: return "distance_miles";
			case NAUTICAL_MILES: return "distance_nautical_miles";
		}
		
		Log.e(TAG, "getKeyForDistanceUnit (unit not recognized: " + unit + ")");

		throw new NotFoundException("distance unit not found");
	}

	// PREF_UNITS
	public static VelocityUnit getVelocityUnitForKey(String tpmKey) throws NotFoundException {

		if (tpmKey != null) {
			if (tpmKey.equalsIgnoreCase("velocity_meters_per_second"))
				return VelocityUnit.METERS_PER_SECOND;
			else if (tpmKey.equalsIgnoreCase("velocity_nautical_miles_per_hour"))
				return VelocityUnit.NAUTICAL_MILES_PER_HOUR;
			else if (tpmKey.equalsIgnoreCase("velocity_kilmeters_per_hour"))
				return VelocityUnit.KILOMETERS_PER_HOUR;
			else if (tpmKey.equalsIgnoreCase("velocity_miles_per_hour"))
				return VelocityUnit.MILES_PER_HOUR;
		}

		throw new NotFoundException("speed unit key not found");
	}


	// PREF_UNITS
	public static DataAveragingMethod getDataAveragingMethodForKey(String tpmKey) throws NotFoundException {

		if (tpmKey != null) {
			if (tpmKey.equalsIgnoreCase("dam_median"))
				return DataAveragingMethod.MEDIAN;
			else if (tpmKey.equalsIgnoreCase("dam_mean"))
				return DataAveragingMethod.MEAN;
		}

		throw new NotFoundException("dam unit key not found");
	}

	// PREF_UNITS
	public static DataAveragingWindow getDataAveragingWindowForKey(String tpmKey) throws NotFoundException {

		if (tpmKey != null) {
			if (tpmKey.equalsIgnoreCase("daw_3"))
				return DataAveragingWindow.N_3_SAMPLES;
			else if (tpmKey.equalsIgnoreCase("daw_5"))
				return DataAveragingWindow.N_5_SAMPLES;
			else if (tpmKey.equalsIgnoreCase("daw_7"))
				return DataAveragingWindow.N_7_SAMPLES;
			else if (tpmKey.equalsIgnoreCase("daw_9"))
				return DataAveragingWindow.N_9_SAMPLES;
		}

		throw new NotFoundException("distance unit key not found");
	}

	public static int convertDataAveragingWindowToInteger(DataAveragingWindow v) {
		switch (v) {

		case N_3_SAMPLES: return 3; 
		case N_5_SAMPLES: return 5; 
		case N_7_SAMPLES: return 7; 
		case N_9_SAMPLES: return 9; 

		default:
			// fail
			Log.e(TAG, "convertDataAveragingWindowToInteger (value not recognized: " + v + ")");
			break;
		}

		return 1;
	}

	
	public static double convertMetersToDistanceUnits(double meters, DistanceUnit units) {
		double v = 0;

		switch (units) {

		case FEET:
			v = meters * FEET_PER_METER;
			break;

		case METERS:
			v = meters;
			break;

		case KILOMETERS:
			v = meters * KILOMETERS_PER_METER;
			break;

		case MILES:
			v = meters * MILES_PER_METER;
			break;

		case NAUTICAL_MILES:
			v = meters * NAUTICAL_MILES_PER_METER;
			break;

		default:
			// fail
			Log.e(TAG, "metersToDistanceUnits (units not recognized: " + units + ")");
			break;
		}

		return v;
	}

	public static double convertDistanceUnitsToMeters(double value, DistanceUnit units) {
		double meters = 0;

		switch (units) {

		case FEET:
			meters = value / FEET_PER_METER;
			break;

		case METERS:
			meters = value;
			break;

		case KILOMETERS:
			meters = value / KILOMETERS_PER_METER;
			break;

		case MILES:
			meters = value / MILES_PER_METER;
			break;

		case NAUTICAL_MILES:
			meters = value / NAUTICAL_MILES_PER_METER;
			break;

		default:
			// fail
			Log.e(TAG, "convertDistanceUnitsToMeters (units not recognized: " + units + ")");
			break;
		}

		return meters;
	}
	
	public static double convertDistanceUnits(double value, DistanceUnit oldUnits, DistanceUnit newUnits) {
		// same?
		if (oldUnits == newUnits)
			return value;
		
		// convert -> meters -> dest
		return GPSUtils.convertMetersToDistanceUnits(GPSUtils.convertDistanceUnitsToMeters(value, oldUnits), newUnits);
	}


	public static double convertMetersPerSecondToVelocityUnits(double metersPerSecond, VelocityUnit units) {
		double v = 0;

		switch (units) {

		case METERS_PER_SECOND:
			v = metersPerSecond;
			break;

		case NAUTICAL_MILES_PER_HOUR:
			v = metersPerSecond * M_PER_SEC_TO_NAUTICAL_MILES_PER_PER_HOUR;
			break;

		case KILOMETERS_PER_HOUR:
			v = metersPerSecond * M_PER_SEC_TO_KILOMETERS_PER_HOUR;
			break;

		case MILES_PER_HOUR:
			v = metersPerSecond * M_PER_SEC_TO_MILES_PER_HOUR;
			break;

		default:
			// fail
			Log.e(TAG, "metersPerSecondToVelocityUnits (units not recognized: " + units + ")");
			break;
		}

		return v;
	}

	public static double feetToMeters(double feet) {
		return feet / FEET_PER_METER;
	}
}
