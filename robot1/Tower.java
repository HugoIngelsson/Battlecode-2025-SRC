package robot1;

import battlecode.common.*;

public abstract class Tower extends Robot {
    MapLocation[] spawnPlaces;
    public Tower(RobotController rc) throws GameActionException {
        super(rc);

        spawnPlaces = new MapLocation[12];
        spawnPlaces[0] = rc.getLocation().add(Direction.NORTH);
        spawnPlaces[1] = rc.getLocation().add(Direction.NORTHEAST);
        spawnPlaces[2] = rc.getLocation().add(Direction.EAST);
        spawnPlaces[3] = rc.getLocation().add(Direction.SOUTHEAST);
        spawnPlaces[4] = rc.getLocation().add(Direction.SOUTH);
        spawnPlaces[5] = rc.getLocation().add(Direction.SOUTHWEST);
        spawnPlaces[6] = rc.getLocation().add(Direction.WEST);
        spawnPlaces[7] = rc.getLocation().add(Direction.NORTHWEST);
        spawnPlaces[8] = rc.getLocation().add(Direction.NORTH).add(Direction.NORTH);
        spawnPlaces[9] = rc.getLocation().add(Direction.EAST).add(Direction.EAST);
        spawnPlaces[10] = rc.getLocation().add(Direction.SOUTH).add(Direction.SOUTH);
        spawnPlaces[11] = rc.getLocation().add(Direction.WEST).add(Direction.WEST);
    }

    void turn1() throws GameActionException {
        home = rc.getLocation();
    }

    abstract void play() throws GameActionException;

    @Override
    void initTurn() throws GameActionException {
        // read from buffer
        // scan environment
    }
}
