package com.supermetroid.model

import kotlinx.serialization.Serializable

/**
 * Super Metroid Room Data
 * 
 * All room IDs and names verified from the LiveSplit autosplitter
 * Reference: https://github.com/UNHchabo/AutoSplitters
 * ROM Map: https://jathys.zophar.net/supermetroid/kejardon/RAMMap.txt
 */

@Serializable
data class Room(
    val id: Int,
    val handle: String,
    val name: String,
    val area: Area,
    val comment: String? = null
)

enum class Area {
    CRATERIA,
    BRINSTAR,
    NORFAIR,
    WRECKED_SHIP,
    MARIDIA,
    TOURIAN,
    CERES
}

object RoomDatabase {
    
    /**
     * Complete mapping of all 143 Super Metroid rooms
     * Room IDs are from the game's internal memory addresses
     */
    val ALL_ROOMS = listOf(
        // === CRATERIA ===
        Room(0x91F8, "landingSite", "Landing Site", Area.CRATERIA),
        Room(0x93AA, "crateriaPowerBombRoom", "Crateria Power Bomb Room", Area.CRATERIA),
        Room(0x93FE, "westOcean", "West Ocean", Area.CRATERIA),
        Room(0x94CC, "elevatorToMaridia", "Elevator To Maridia", Area.CRATERIA),
        Room(0x95FF, "crateriaMoat", "The Moat", Area.CRATERIA),
        Room(0x962A, "elevatorToCaterpillar", "Elevator To Caterpillar", Area.CRATERIA),
        Room(0x965B, "gauntletETankRoom", "Gauntlet Energy Tank Room", Area.CRATERIA),
        Room(0x96BA, "climb", "The Climb", Area.CRATERIA),
        Room(0x975C, "pitRoom", "Pit Room", Area.CRATERIA),
        Room(0x97B5, "elevatorToMorphBall", "Elevator To Morph Ball", Area.CRATERIA),
        Room(0x9804, "bombTorizo", "Bomb Torizo Room", Area.CRATERIA),
        Room(0x990D, "terminator", "Terminator Room", Area.CRATERIA),
        Room(0x9938, "elevatorToGreenBrinstar", "Elevator To Green Brinstar", Area.CRATERIA),
        Room(0x99BD, "greenPirateShaft", "Green Pirates Shaft", Area.CRATERIA),
        Room(0x99F9, "crateriaSupersRoom", "Crateria Super Room", Area.CRATERIA),
        Room(0x9A90, "theFinalMissile", "The Final Missile", Area.CRATERIA),
        
        // === BRINSTAR ===
        Room(0x9AD9, "greenBrinstarMainShaft", "Green Brinstar Main Shaft", Area.BRINSTAR),
        Room(0x9B5B, "sporeSpawnSuper", "Spore Spawn Super Room", Area.BRINSTAR),
        Room(0x9BC8, "earlySupers", "Early Supers Room", Area.BRINSTAR),
        Room(0x9C07, "brinstarReserveRoom", "Brinstar Reserve Tank Room", Area.BRINSTAR),
        Room(0x9D19, "bigPink", "Big Pink", Area.BRINSTAR),
        Room(0x9D9C, "sporeSpawnKeyhunter", "Spore Spawn Keyhunter Room", Area.BRINSTAR),
        Room(0x9DC7, "sporeSpawn", "Spore Spawn Room", Area.BRINSTAR),
        Room(0x9E11, "pinkBrinstarPowerBombRoom", "Pink Brinstar Power Bomb Room", Area.BRINSTAR),
        Room(0x9E52, "greenHills", "Green Hill Zone", Area.BRINSTAR),
        Room(0x9E9F, "morphBall", "Morph Ball Room", Area.BRINSTAR),
        Room(0x9F64, "blueBrinstarETankRoom", "Blue Brinstar Energy Tank Room", Area.BRINSTAR),
        Room(0x9FBA, "noobBridge", "Noob Bridge", Area.BRINSTAR),
        Room(0xA011, "etacoonETankRoom", "Etacoon Energy Tank Room", Area.BRINSTAR),
        Room(0xA051, "etacoonSuperRoom", "Etacoon Super Room", Area.BRINSTAR),
        Room(0xA0D2, "waterway", "Waterway", Area.BRINSTAR),
        Room(0xA107, "alphaMissileRoom", "First Missile Room", Area.BRINSTAR),
        Room(0xA15B, "hopperETankRoom", "Hopper Energy Tank Room", Area.BRINSTAR),
        Room(0xA1D8, "billyMays", "Billy Mays' Room", Area.BRINSTAR),
        Room(0xA253, "redTower", "Red Tower", Area.BRINSTAR),
        Room(0xA2CE, "xRay", "X-Ray Room", Area.BRINSTAR),
        Room(0xA322, "caterpillar", "Caterpillar Room", Area.BRINSTAR),
        Room(0xA37C, "betaPowerBombRoom", "Beta Power Bomb Room", Area.BRINSTAR),
        Room(0xA3AE, "alphaPowerBombsRoom", "Alpha Power Bomb Room", Area.BRINSTAR),
        Room(0xA3DD, "bat", "Bat Room", Area.BRINSTAR),
        Room(0xA447, "spazer", "Spazer Room", Area.BRINSTAR),
        Room(0xA471, "warehouseZeela", "Warehouse Zeela Room", Area.BRINSTAR),
        Room(0xA4B1, "warehouseETankRoom", "Warehouse Energy Tank Room", Area.BRINSTAR),
        Room(0xA4DA, "warehouseKiHunters", "Warehouse Kihunter Room", Area.BRINSTAR),
        Room(0xA56B, "kraidEyeDoor", "Kraid's Eye Door", Area.BRINSTAR),
        Room(0xA59F, "kraid", "Kraid's Room", Area.BRINSTAR),
        Room(0xA5ED, "statuesHallway", "Statues Hallway", Area.BRINSTAR),
        Room(0xA66A, "statues", "Statues Room", Area.BRINSTAR),
        Room(0xA6A1, "warehouseEntrance", "Warehouse Entrance", Area.BRINSTAR),
        Room(0xA6E2, "varia", "Varia Suit Room", Area.BRINSTAR),
        
        // === NORFAIR ===
        Room(0xA788, "cathedral", "Cathedral", Area.NORFAIR),
        Room(0xA7DE, "businessCenter", "Business Center", Area.NORFAIR),
        Room(0xA890, "iceBeam", "Ice Beam Room", Area.NORFAIR),
        Room(0xA8F8, "crumbleShaft", "Crumble Shaft", Area.NORFAIR),
        Room(0xA923, "crocomireSpeedway", "Crocomire Speedway", Area.NORFAIR),
        Room(0xA98D, "crocomire", "Crocomire's Room", Area.NORFAIR),
        Room(0xA9E5, "hiJump", "Hi-Jump Room", Area.NORFAIR),
        Room(0xAA0E, "crocomireEscape", "Crocomire Escape", Area.NORFAIR),
        Room(0xAA41, "hiJumpShaft", "Hi-Jump Shaft", Area.NORFAIR),
        Room(0xAADE, "postCrocomirePowerBombRoom", "Post Crocomire Power Bomb Room", Area.NORFAIR),
        Room(0xAB3B, "cosineRoom", "Cosine Room", Area.NORFAIR),
        Room(0xAB8F, "preGrapple", "Post Crocomire Jump Room", Area.NORFAIR),
        Room(0xAC2B, "grapple", "Grapple Beam Room", Area.NORFAIR),
        Room(0xAC5A, "norfairReserveRoom", "Norfair Reserve Tank Room", Area.NORFAIR),
        Room(0xAC83, "greenBubblesRoom", "Green Bubbles Missile Room", Area.NORFAIR),
        Room(0xACB3, "bubbleMountain", "Bubble Mountain", Area.NORFAIR),
        Room(0xACF0, "speedBoostHall", "Speed Booster Hall", Area.NORFAIR),
        Room(0xAD1B, "speedBooster", "Speed Booster Room", Area.NORFAIR),
        Room(0xAD5E, "singleChamber", "Single Chamber", Area.NORFAIR),
        Room(0xADAD, "doubleChamber", "Double Chamber", Area.NORFAIR),
        Room(0xADDE, "waveBeam", "Wave Beam Room", Area.NORFAIR),
        Room(0xAE32, "volcano", "Volcano Room", Area.NORFAIR),
        Room(0xAE74, "kronicBoost", "Kronic Boost Room", Area.NORFAIR),
        Room(0xAEB4, "magdolliteTunnel", "Magdollite Tunnel", Area.NORFAIR),
        Room(0xAF3F, "lowerNorfairElevator", "Lower Norfair Elevator", Area.NORFAIR),
        Room(0xAFA3, "risingTide", "Rising Tide", Area.NORFAIR),
        Room(0xAFFB, "spikyAcidSnakes", "Spiky Acid Snakes Room", Area.NORFAIR),
        Room(0xB1E5, "acidStatue", "Acid Statue Room", Area.NORFAIR),
        Room(0xB236, "mainHall", "Main Hall", Area.NORFAIR, "First room in Lower Norfair"),
        Room(0xB283, "goldenTorizo", "Golden Torizo's Room", Area.NORFAIR),
        Room(0xB32E, "ridley", "Ridley's Room", Area.NORFAIR),
        Room(0xB37A, "lowerNorfairFarming", "Lower Norfair Farming Room", Area.NORFAIR),
        Room(0xB40A, "mickeyMouse", "Mickey Mouse Room", Area.NORFAIR),
        Room(0xB457, "pillars", "Pillar Room", Area.NORFAIR),
        Room(0xB4AD, "writg", "Worst Room in the Game", Area.NORFAIR),
        Room(0xB4E5, "amphitheatre", "Amphitheatre", Area.NORFAIR),
        Room(0xB510, "lowerNorfairSpringMaze", "Lower Norfair Springball Maze Room", Area.NORFAIR),
        Room(0xB55A, "lowerNorfairEscapePowerBombRoom", "Lower Norfair Escape Power Bomb Room", Area.NORFAIR),
        Room(0xB585, "redKiShaft", "Red Kihunter Shaft", Area.NORFAIR),
        Room(0xB5D5, "wasteland", "Wasteland", Area.NORFAIR),
        Room(0xB62B, "metalPirates", "Metal Pirates Room", Area.NORFAIR),
        Room(0xB656, "threeMusketeers", "The Musketeers' Room", Area.NORFAIR),
        Room(0xB698, "ridleyETankRoom", "Ridley Tank Room", Area.NORFAIR),
        Room(0xB6C1, "screwAttack", "Screw Attack Room", Area.NORFAIR),
        Room(0xB6EE, "lowerNorfairFireflea", "Lower Norfair Fireflea Room", Area.NORFAIR),
        
        // === WRECKED SHIP ===
        Room(0xC98E, "bowling", "Bowling Alley", Area.WRECKED_SHIP),
        Room(0xCA08, "wreckedShipEntrance", "Wrecked Ship Entrance", Area.WRECKED_SHIP),
        Room(0xCA52, "attic", "Attic", Area.WRECKED_SHIP),
        Room(0xCAAE, "atticWorkerRobotRoom", "Wrecked Ship East Missile Room", Area.WRECKED_SHIP),
        Room(0xCAF6, "wreckedShipMainShaft", "Wrecked Ship Main Shaft", Area.WRECKED_SHIP),
        Room(0xCC27, "wreckedShipETankRoom", "Wrecked Ship Energy Tank Room", Area.WRECKED_SHIP),
        Room(0xCC6F, "basement", "Wrecked Ship Basement", Area.WRECKED_SHIP),
        Room(0xCD13, "phantoon", "Phantoon's Room", Area.WRECKED_SHIP),
        Room(0xCDA8, "wreckedShipLeftSuperRoom", "Wrecked Ship West Super Room", Area.WRECKED_SHIP),
        Room(0xCDF1, "wreckedShipRightSuperRoom", "Wrecked Ship East Super Room", Area.WRECKED_SHIP),
        Room(0xCE40, "gravity", "Gravity Suit Room", Area.WRECKED_SHIP),
        
        // === MARIDIA ===
        Room(0xCEFB, "glassTunnel", "Glass Tunnel", Area.MARIDIA),
        Room(0xCFC9, "mainStreet", "Main Street", Area.MARIDIA),
        Room(0xD055, "mamaTurtle", "Mama Turtle Room", Area.MARIDIA),
        Room(0xD13B, "wateringHole", "Watering Hole", Area.MARIDIA),
        Room(0xD1DD, "beach", "Pseudo Plasma Spark Room", Area.MARIDIA),
        Room(0xD2AA, "plasmaBeam", "Plasma Beam Room", Area.MARIDIA),
        Room(0xD30B, "maridiaElevator", "Maridia Elevator", Area.MARIDIA),
        Room(0xD340, "plasmaSpark", "Plasma Spark Room", Area.MARIDIA),
        Room(0xD408, "toiletBowl", "Toilet Bowl", Area.MARIDIA),
        Room(0xD48E, "oasis", "Oasis", Area.MARIDIA),
        Room(0xD4EF, "leftSandPit", "West Sand Hole", Area.MARIDIA),
        Room(0xD51E, "rightSandPit", "East Sand Hole", Area.MARIDIA),
        Room(0xD5A7, "aqueduct", "Aqueduct", Area.MARIDIA),
        Room(0xD5EC, "butterflyRoom", "Butterfly Room", Area.MARIDIA),
        Room(0xD617, "botwoonHallway", "Botwoon Hallway", Area.MARIDIA),
        Room(0xD6D0, "springBall", "Spring Ball Room", Area.MARIDIA),
        Room(0xD78F, "precious", "The Precious Room", Area.MARIDIA),
        Room(0xD7E4, "botwoonETankRoom", "Botwoon Energy Tank Room", Area.MARIDIA),
        Room(0xD95E, "botwoon", "Botwoon's Room", Area.MARIDIA),
        Room(0xD9AA, "spaceJump", "Space Jump Room", Area.MARIDIA),
        Room(0xD9FE, "westCactusAlley", "West Cacattack Alley", Area.MARIDIA),
        Room(0xDA60, "draygon", "Draygon's Room", Area.MARIDIA),
        
        // === TOURIAN ===
        Room(0xDAAE, "tourianElevator", "Tourian Elevator", Area.TOURIAN),
        Room(0xDAE1, "metroidOne", "Metroid Room 1", Area.TOURIAN),
        Room(0xDB31, "metroidTwo", "Metroid Room 2", Area.TOURIAN),
        Room(0xDB7D, "metroidThree", "Metroid Room 3", Area.TOURIAN),
        Room(0xDBCD, "metroidFour", "Metroid Room 4", Area.TOURIAN),
        Room(0xDC19, "tourianHopper", "Tourian Hopper Room", Area.TOURIAN),
        Room(0xDC65, "dustTorizo", "Dust Torizo Room", Area.TOURIAN),
        Room(0xDCB1, "bigBoy", "Big Boy Room", Area.TOURIAN),
        Room(0xDD58, "motherBrain", "Mother Brain's Room", Area.TOURIAN),
        Room(0xDDC4, "tourianEyeDoor", "Tourian Eye Door Room", Area.TOURIAN),
        Room(0xDDF3, "rinkaShaft", "Rinka Shaft", Area.TOURIAN),
        Room(0xDEDE, "tourianEscape4", "Tourian Escape Room 4", Area.TOURIAN),
        
        // === CERES ===
        Room(0xDF45, "ceresElevator", "Ceres Elevator", Area.CERES),
        Room(0xE06B, "flatRoom", "Ceres Flat Room", Area.CERES),
        Room(0xE0B5, "ceresRidley", "Ceres Ridley Room", Area.CERES),
    )
    
    // Indexed maps for fast lookup
    val BY_ID: Map<Int, Room> = ALL_ROOMS.associateBy { it.id }
    val BY_HANDLE: Map<String, Room> = ALL_ROOMS.associateBy { it.handle }
    val BY_AREA: Map<Area, List<Room>> = ALL_ROOMS.groupBy { it.area }
    
    /**
     * Get a room by its memory ID
     */
    fun getRoomById(id: Int): Room? = BY_ID[id]
    
    /**
     * Get a room by its handle (e.g., "landingSite")
     */
    fun getRoomByHandle(handle: String): Room? = BY_HANDLE[handle]
    
    /**
     * Get all rooms in a specific area
     */
    fun getRoomsByArea(area: Area): List<Room> = BY_AREA[area] ?: emptyList()
    
    /**
     * Search rooms by name (case-insensitive partial match)
     */
    fun searchRoomsByName(query: String): List<Room> {
        val lowerQuery = query.lowercase()
        return ALL_ROOMS.filter { it.name.lowercase().contains(lowerQuery) }
    }
}
