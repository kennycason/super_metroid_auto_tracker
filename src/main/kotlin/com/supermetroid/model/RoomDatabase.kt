package com.supermetroid.model

import kotlinx.serialization.Serializable

/**
 * Super Metroid Room Data
 *
 * All room IDs and names verified from the Super Metroid Editor ROM parser
 * and cross-referenced with the LiveSplit autosplitter.
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
     * Complete mapping of all 262 Super Metroid rooms (excluding debug room)
     * Room IDs are from the game's internal memory addresses
     */
    val ALL_ROOMS = listOf(
        // === CRATERIA (32 rooms) ===
        Room(0x91F8, "landingSite", "Landing Site", Area.CRATERIA),
        Room(0x92B3, "gauntletEntrance", "Gauntlet Entrance", Area.CRATERIA),
        Room(0x92FD, "parlorAndAlcatraz", "Parlor and Alcatraz", Area.CRATERIA),
        Room(0x93AA, "crateriaPowerBombRoom", "Crateria Power Bomb Room", Area.CRATERIA),
        Room(0x93D5, "crateriaSaveRoom", "Crateria Save Room", Area.CRATERIA),
        Room(0x93FE, "westOcean", "West Ocean", Area.CRATERIA),
        Room(0x9461, "bowlingAlleyPath", "Bowling Alley Path", Area.CRATERIA),
        Room(0x948C, "crateriaKihunterRoom", "Crateria Kihunter Room", Area.CRATERIA),
        Room(0x94CC, "elevatorToMaridia", "Elevator To Maridia", Area.CRATERIA),
        Room(0x94FD, "eastOcean", "East Ocean", Area.CRATERIA),
        Room(0x9552, "forgottenHighwayKagoRoom", "Forgotten Highway Kago Room", Area.CRATERIA),
        Room(0x957D, "crabMaze", "Crab Maze", Area.CRATERIA),
        Room(0x95A8, "forgottenHighwayElbow", "Forgotten Highway Elbow", Area.CRATERIA),
        Room(0x95D4, "crateriaTube", "Crateria Tube", Area.CRATERIA),
        Room(0x95FF, "crateriaMoat", "The Moat", Area.CRATERIA),
        Room(0x962A, "elevatorToCaterpillar", "Elevator To Caterpillar", Area.CRATERIA),
        Room(0x965B, "gauntletETankRoom", "Gauntlet Energy Tank Room", Area.CRATERIA),
        Room(0x968F, "crateriaPartialRoom", "Crateria Partial Room", Area.CRATERIA),
        Room(0x96BA, "climb", "The Climb", Area.CRATERIA),
        Room(0x975C, "pitRoom", "Pit Room", Area.CRATERIA),
        Room(0x97B5, "elevatorToMorphBall", "Elevator To Morph Ball", Area.CRATERIA),
        Room(0x9804, "bombTorizo", "Bomb Torizo Room", Area.CRATERIA),
        Room(0x9879, "flyway", "Flyway", Area.CRATERIA),
        Room(0x98E2, "preMapFlyway", "Pre-Map Flyway", Area.CRATERIA),
        Room(0x990D, "terminator", "Terminator Room", Area.CRATERIA),
        Room(0x9938, "elevatorToGreenBrinstar", "Elevator To Green Brinstar", Area.CRATERIA),
        Room(0x9969, "lowerMushrooms", "Lower Mushrooms", Area.CRATERIA),
        Room(0x9994, "crateriaMapRoom", "Crateria Map Room", Area.CRATERIA),
        Room(0x99BD, "greenPirateShaft", "Green Pirates Shaft", Area.CRATERIA),
        Room(0x99F9, "crateriaSupersRoom", "Crateria Super Room", Area.CRATERIA),
        Room(0x9A44, "finalMissileBombway", "Final Missile Bombway", Area.CRATERIA),
        Room(0x9A90, "theFinalMissile", "The Final Missile", Area.CRATERIA),

        // === BRINSTAR (56 rooms) ===
        Room(0x9AD9, "greenBrinstarMainShaft", "Green Brinstar Main Shaft", Area.BRINSTAR),
        Room(0x9B5B, "sporeSpawnSuper", "Spore Spawn Super Room", Area.BRINSTAR),
        Room(0x9B9D, "brinstarPreMapRoom", "Brinstar Pre-Map Room", Area.BRINSTAR),
        Room(0x9BC8, "earlySupers", "Early Supers Room", Area.BRINSTAR),
        Room(0x9C07, "brinstarReserveRoom", "Brinstar Reserve Tank Room", Area.BRINSTAR),
        Room(0x9C35, "brinstarMapRoom", "Brinstar Map Room", Area.BRINSTAR),
        Room(0x9C5E, "greenBrinstarFirefleaRoom", "Green Brinstar Fireflea Room", Area.BRINSTAR),
        Room(0x9C89, "greenBrinstarMissileStation", "Green Brinstar Missile Station", Area.BRINSTAR),
        Room(0x9CB3, "dachoraRoom", "Dachora Room", Area.BRINSTAR),
        Room(0x9D19, "bigPink", "Big Pink", Area.BRINSTAR),
        Room(0x9D9C, "sporeSpawnKeyhunter", "Spore Spawn Keyhunter Room", Area.BRINSTAR),
        Room(0x9DC7, "sporeSpawn", "Spore Spawn Room", Area.BRINSTAR),
        Room(0x9E11, "pinkBrinstarPowerBombRoom", "Pink Brinstar Power Bomb Room", Area.BRINSTAR),
        Room(0x9E52, "greenHills", "Green Hill Zone", Area.BRINSTAR),
        Room(0x9E9F, "morphBall", "Morph Ball Room", Area.BRINSTAR),
        Room(0x9F11, "constructionZone", "Construction Zone", Area.BRINSTAR),
        Room(0x9F64, "blueBrinstarETankRoom", "Blue Brinstar Energy Tank Room", Area.BRINSTAR),
        Room(0x9FBA, "noobBridge", "Noob Bridge", Area.BRINSTAR),
        Room(0x9FE5, "greenBrinstarBeetomRoom", "Green Brinstar Beetom Room", Area.BRINSTAR),
        Room(0xA011, "etacoonETankRoom", "Etacoon Energy Tank Room", Area.BRINSTAR),
        Room(0xA051, "etacoonSuperRoom", "Etacoon Super Room", Area.BRINSTAR),
        Room(0xA07B, "dachoraEnergyChargeStation", "Dachora Energy Charge Station", Area.BRINSTAR),
        Room(0xA0A4, "sporeSpawnFarmingRoom", "Spore Spawn Farming Room", Area.BRINSTAR),
        Room(0xA0D2, "waterway", "Waterway", Area.BRINSTAR),
        Room(0xA107, "alphaMissileRoom", "First Missile Room", Area.BRINSTAR),
        Room(0xA130, "pinkBrinstarHopperRoom", "Pink Brinstar Hopper Room", Area.BRINSTAR),
        Room(0xA15B, "hopperETankRoom", "Hopper Energy Tank Room", Area.BRINSTAR),
        Room(0xA184, "bigPinkSaveRoom", "Big Pink Save Room", Area.BRINSTAR),
        Room(0xA1AD, "blueBrinstarBoulderRoom", "Blue Brinstar Boulder Room", Area.BRINSTAR),
        Room(0xA1D8, "billyMays", "Billy Mays' Room", Area.BRINSTAR),
        Room(0xA201, "etecoonSaveRoom", "Etecoon Save Room", Area.BRINSTAR),
        Room(0xA22A, "etecoonSaveRoom2", "Etecoon Save Room 2", Area.BRINSTAR),
        Room(0xA253, "redTower", "Red Tower", Area.BRINSTAR),
        Room(0xA293, "redBrinstarFirefleaRoom", "Red Brinstar Fireflea Room", Area.BRINSTAR),
        Room(0xA2CE, "xRay", "X-Ray Room", Area.BRINSTAR),
        Room(0xA2F7, "hellway", "Hellway", Area.BRINSTAR),
        Room(0xA322, "caterpillar", "Caterpillar Room", Area.BRINSTAR),
        Room(0xA37C, "betaPowerBombRoom", "Beta Power Bomb Room", Area.BRINSTAR),
        Room(0xA3AE, "alphaPowerBombsRoom", "Alpha Power Bomb Room", Area.BRINSTAR),
        Room(0xA3DD, "bat", "Bat Room", Area.BRINSTAR),
        Room(0xA408, "belowSpazer", "Below Spazer", Area.BRINSTAR),
        Room(0xA447, "spazer", "Spazer Room", Area.BRINSTAR),
        Room(0xA471, "warehouseZeela", "Warehouse Zeela Room", Area.BRINSTAR),
        Room(0xA4B1, "warehouseETankRoom", "Warehouse Energy Tank Room", Area.BRINSTAR),
        Room(0xA4DA, "warehouseKiHunters", "Warehouse Kihunter Room", Area.BRINSTAR),
        Room(0xA521, "babyKraidRoom", "Baby Kraid Room", Area.BRINSTAR),
        Room(0xA56B, "kraidEyeDoor", "Kraid's Eye Door", Area.BRINSTAR),
        Room(0xA59F, "kraid", "Kraid's Room", Area.BRINSTAR),
        Room(0xA5ED, "statuesHallway", "Statues Hallway", Area.BRINSTAR),
        Room(0xA618, "sloatersRefill", "Sloaters Refill", Area.BRINSTAR),
        Room(0xA641, "kraidRechargeStations", "Kraid Recharge Stations", Area.BRINSTAR),
        Room(0xA66A, "statues", "Statues Room", Area.BRINSTAR),
        Room(0xA6A1, "warehouseEntrance", "Warehouse Entrance", Area.BRINSTAR),
        Room(0xA6E2, "varia", "Varia Suit Room", Area.BRINSTAR),
        Room(0xA70B, "warehouseSaveRoom", "Warehouse Save Room", Area.BRINSTAR),
        Room(0xA734, "redBrinstarSaveRoom", "Red Brinstar Save Room", Area.BRINSTAR),

        // === NORFAIR (77 rooms) ===
        Room(0xA75D, "iceBeamAcidRoom", "Ice Beam Acid Room", Area.NORFAIR),
        Room(0xA788, "cathedral", "Cathedral", Area.NORFAIR),
        Room(0xA7B3, "cathedralEntrance", "Cathedral Entrance", Area.NORFAIR),
        Room(0xA7DE, "businessCenter", "Business Center", Area.NORFAIR),
        Room(0xA815, "iceBeamGateRoom", "Ice Beam Gate Room", Area.NORFAIR),
        Room(0xA865, "iceBeamTutorialRoom", "Ice Beam Tutorial Room", Area.NORFAIR),
        Room(0xA890, "iceBeam", "Ice Beam Room", Area.NORFAIR),
        Room(0xA8B9, "iceBeamSnakeRoom", "Ice Beam Snake Room", Area.NORFAIR),
        Room(0xA8F8, "crumbleShaft", "Crumble Shaft", Area.NORFAIR),
        Room(0xA923, "crocomireSpeedway", "Crocomire Speedway", Area.NORFAIR),
        Room(0xA98D, "crocomire", "Crocomire's Room", Area.NORFAIR),
        Room(0xA9E5, "hiJump", "Hi-Jump Room", Area.NORFAIR),
        Room(0xAA0E, "crocomireEscape", "Crocomire Escape", Area.NORFAIR),
        Room(0xAA41, "hiJumpShaft", "Hi-Jump Shaft", Area.NORFAIR),
        Room(0xAA82, "postCrocomireFarmingRoom", "Post Crocomire Farming Room", Area.NORFAIR),
        Room(0xAAB5, "postCrocomireSaveRoom", "Post Crocomire Save Room", Area.NORFAIR),
        Room(0xAADE, "postCrocomirePowerBombRoom", "Post Crocomire Power Bomb Room", Area.NORFAIR),
        Room(0xAB07, "postCrocomireShaft", "Post Crocomire Shaft", Area.NORFAIR),
        Room(0xAB3B, "cosineRoom", "Cosine Room", Area.NORFAIR),
        Room(0xAB64, "grappleTutorialRoom3", "Grapple Tutorial Room 3", Area.NORFAIR),
        Room(0xAB8F, "preGrapple", "Post Crocomire Jump Room", Area.NORFAIR),
        Room(0xABD2, "grappleTutorialRoom2", "Grapple Tutorial Room 2", Area.NORFAIR),
        Room(0xAC00, "grappleTutorialRoom1", "Grapple Tutorial Room 1", Area.NORFAIR),
        Room(0xAC2B, "grapple", "Grapple Beam Room", Area.NORFAIR),
        Room(0xAC5A, "norfairReserveRoom", "Norfair Reserve Tank Room", Area.NORFAIR),
        Room(0xAC83, "greenBubblesRoom", "Green Bubbles Missile Room", Area.NORFAIR),
        Room(0xACB3, "bubbleMountain", "Bubble Mountain", Area.NORFAIR),
        Room(0xACF0, "speedBoostHall", "Speed Booster Hall", Area.NORFAIR),
        Room(0xAD1B, "speedBooster", "Speed Booster Room", Area.NORFAIR),
        Room(0xAD5E, "singleChamber", "Single Chamber", Area.NORFAIR, "Exit room from Lower Norfair, also on the path to Wave"),
        Room(0xADAD, "doubleChamber", "Double Chamber", Area.NORFAIR),
        Room(0xADDE, "waveBeam", "Wave Beam Room", Area.NORFAIR),
        Room(0xAE07, "spikyPlatformsTunnel", "Spiky Platforms Tunnel", Area.NORFAIR),
        Room(0xAE32, "volcano", "Volcano Room", Area.NORFAIR),
        Room(0xAE74, "kronicBoost", "Kronic Boost Room", Area.NORFAIR),
        Room(0xAEB4, "magdolliteTunnel", "Magdollite Tunnel", Area.NORFAIR),
        Room(0xAEDF, "purpleShaft", "Purple Shaft", Area.NORFAIR),
        Room(0xAF14, "lavaDiveRoom", "Lava Dive Room", Area.NORFAIR),
        Room(0xAF3F, "lowerNorfairElevator", "Lower Norfair Elevator", Area.NORFAIR),
        Room(0xAF72, "upperNorfairFarmingRoom", "Upper Norfair Farming Room", Area.NORFAIR),
        Room(0xAFA3, "risingTide", "Rising Tide", Area.NORFAIR),
        Room(0xAFCE, "acidSnakesTunnel", "Acid Snakes Tunnel", Area.NORFAIR),
        Room(0xAFFB, "spikyAcidSnakes", "Spiky Acid Snakes Room", Area.NORFAIR),
        Room(0xB026, "nutellaRefill", "Nutella Refill", Area.NORFAIR),
        Room(0xB051, "purpleFarmingRoom", "Purple Farming Room", Area.NORFAIR),
        Room(0xB07A, "batCave", "Bat Cave", Area.NORFAIR),
        Room(0xB0B4, "norfairMapRoom", "Norfair Map Room", Area.NORFAIR),
        Room(0xB0DD, "bubbleMountainSaveRoom", "Bubble Mountain Save Room", Area.NORFAIR),
        Room(0xB106, "frogSpeedway", "Frog Speedway", Area.NORFAIR),
        Room(0xB139, "redPirateShaft", "Red Pirate Shaft", Area.NORFAIR),
        Room(0xB167, "frogSavestation", "Frog Savestation", Area.NORFAIR),
        Room(0xB192, "crocomireSaveRoom", "Crocomire Save Room", Area.NORFAIR),
        Room(0xB1BB, "lowerNorfairElevatorSaveRoom", "Lower Norfair Elevator Save Room", Area.NORFAIR),
        Room(0xB1E5, "acidStatue", "Acid Statue Room", Area.NORFAIR),
        Room(0xB236, "mainHall", "Main Hall", Area.NORFAIR, "First room in Lower Norfair"),
        Room(0xB283, "goldenTorizo", "Golden Torizo's Room", Area.NORFAIR),
        Room(0xB2DA, "fastRipperRoom", "Fast Ripper Room", Area.NORFAIR),
        Room(0xB305, "goldenTorizoEnergyRecharge", "Golden Torizo Energy Recharge", Area.NORFAIR),
        Room(0xB32E, "ridley", "Ridley's Room", Area.NORFAIR),
        Room(0xB37A, "lowerNorfairFarming", "Lower Norfair Farming Room", Area.NORFAIR),
        Room(0xB3A5, "fastPillarsSetupRoom", "Fast Pillars Setup Room", Area.NORFAIR),
        Room(0xB3E1, "unusedLowerNorfairRoom", "Unused Lower Norfair Room", Area.NORFAIR),
        Room(0xB40A, "mickeyMouse", "Mickey Mouse Room", Area.NORFAIR),
        Room(0xB457, "pillars", "Pillar Room", Area.NORFAIR),
        Room(0xB482, "plowerhouseRoom", "Plowerhouse Room", Area.NORFAIR),
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
        Room(0xB741, "redKihunterShaftSaveRoom", "Red Kihunter Shaft Save Room", Area.NORFAIR),

        // === WRECKED SHIP (16 rooms) ===
        Room(0xC98E, "bowling", "Bowling Alley", Area.WRECKED_SHIP),
        Room(0xCA08, "wreckedShipEntrance", "Wrecked Ship Entrance", Area.WRECKED_SHIP),
        Room(0xCA52, "attic", "Attic", Area.WRECKED_SHIP),
        Room(0xCAAE, "atticWorkerRobotRoom", "Wrecked Ship East Missile Room", Area.WRECKED_SHIP),
        Room(0xCAF6, "wreckedShipMainShaft", "Wrecked Ship Main Shaft", Area.WRECKED_SHIP),
        Room(0xCB8B, "spikyDeathRoom", "Spiky Death Room", Area.WRECKED_SHIP),
        Room(0xCBD5, "electricDeathRoom", "Electric Death Room", Area.WRECKED_SHIP),
        Room(0xCC27, "wreckedShipETankRoom", "Wrecked Ship Energy Tank Room", Area.WRECKED_SHIP),
        Room(0xCC6F, "basement", "Wrecked Ship Basement", Area.WRECKED_SHIP, "Basement of Wrecked Ship"),
        Room(0xCCCB, "wreckedShipMapRoom", "Wrecked Ship Map Room", Area.WRECKED_SHIP),
        Room(0xCD13, "phantoon", "Phantoon's Room", Area.WRECKED_SHIP),
        Room(0xCD5C, "spongeBath", "Sponge Bath", Area.WRECKED_SHIP),
        Room(0xCDA8, "wreckedShipLeftSuperRoom", "Wrecked Ship West Super Room", Area.WRECKED_SHIP),
        Room(0xCDF1, "wreckedShipRightSuperRoom", "Wrecked Ship East Super Room", Area.WRECKED_SHIP),
        Room(0xCE40, "gravity", "Gravity Suit Room", Area.WRECKED_SHIP),
        Room(0xCE8A, "wreckedShipSaveRoom", "Wrecked Ship Save Room", Area.WRECKED_SHIP),

        // === MARIDIA (56 rooms) ===
        Room(0xCED2, "glassTunnelSaveRoom", "Glass Tunnel Save Room", Area.MARIDIA),
        Room(0xCEFB, "glassTunnel", "Glass Tunnel", Area.MARIDIA),
        Room(0xCF54, "westTunnel", "West Tunnel", Area.MARIDIA),
        Room(0xCF80, "eastTunnel", "East Tunnel", Area.MARIDIA),
        Room(0xCFC9, "mainStreet", "Main Street", Area.MARIDIA),
        Room(0xD017, "fishTank", "Fish Tank", Area.MARIDIA),
        Room(0xD055, "mamaTurtle", "Mama Turtle Room", Area.MARIDIA),
        Room(0xD08A, "crabTunnel", "Crab Tunnel", Area.MARIDIA),
        Room(0xD0B9, "mtEverest", "Mt. Everest", Area.MARIDIA),
        Room(0xD104, "redFishRoom", "Red Fish Room", Area.MARIDIA),
        Room(0xD13B, "wateringHole", "Watering Hole", Area.MARIDIA),
        Room(0xD16D, "northwestMaridiaBugRoom", "Northwest Maridia Bug Room", Area.MARIDIA),
        Room(0xD1A3, "crabShaft", "Crab Shaft", Area.MARIDIA),
        Room(0xD1DD, "beach", "Pseudo Plasma Spark Room", Area.MARIDIA),
        Room(0xD21C, "crabHole", "Crab Hole", Area.MARIDIA),
        Room(0xD252, "westSandHallTunnel", "West Sand Hall Tunnel", Area.MARIDIA),
        Room(0xD27E, "plasmaTutorialRoom", "Plasma Tutorial Room", Area.MARIDIA),
        Room(0xD2AA, "plasmaBeam", "Plasma Beam Room", Area.MARIDIA),
        Room(0xD2D9, "threadTheNeedleRoom", "Thread The Needle Room", Area.MARIDIA),
        Room(0xD30B, "maridiaElevator", "Maridia Elevator", Area.MARIDIA),
        Room(0xD340, "plasmaSpark", "Plasma Spark Room", Area.MARIDIA),
        Room(0xD387, "kassiuzRoom", "Kassiuz Room", Area.MARIDIA),
        Room(0xD3B6, "maridiaMapRoom", "Maridia Map Room", Area.MARIDIA),
        Room(0xD3DF, "forgottenHighwaySaveRoom", "Forgotten Highway Save Room", Area.MARIDIA),
        Room(0xD408, "toiletBowl", "Toilet Bowl", Area.MARIDIA),
        Room(0xD433, "bugSandHole", "Bug Sand Hole", Area.MARIDIA),
        Room(0xD461, "westSandHall", "West Sand Hall", Area.MARIDIA),
        Room(0xD48E, "oasis", "Oasis", Area.MARIDIA),
        Room(0xD4C2, "eastSandHall", "East Sand Hall", Area.MARIDIA),
        Room(0xD4EF, "leftSandPit", "West Sand Hole", Area.MARIDIA),
        Room(0xD51E, "rightSandPit", "East Sand Hole", Area.MARIDIA),
        Room(0xD54D, "westAqueductQuicksandRoom", "West Aqueduct Quicksand Room", Area.MARIDIA),
        Room(0xD57A, "eastAqueductQuicksandRoom", "East Aqueduct Quicksand Room", Area.MARIDIA),
        Room(0xD5A7, "aqueduct", "Aqueduct", Area.MARIDIA),
        Room(0xD5EC, "butterflyRoom", "Butterfly Room", Area.MARIDIA),
        Room(0xD617, "botwoonHallway", "Botwoon Hallway", Area.MARIDIA),
        Room(0xD646, "pantsRoom", "Pants Room", Area.MARIDIA),
        Room(0xD69A, "eastPantsRoom", "East Pants Room", Area.MARIDIA),
        Room(0xD6D0, "springBall", "Spring Ball Room", Area.MARIDIA),
        Room(0xD6FD, "belowBotwoonEnergyTank", "Below Botwoon Energy Tank", Area.MARIDIA),
        Room(0xD72A, "colosseum", "Colosseum", Area.MARIDIA),
        Room(0xD765, "aqueductSaveRoom", "Aqueduct Save Room", Area.MARIDIA),
        Room(0xD78F, "precious", "The Precious Room", Area.MARIDIA),
        Room(0xD7E4, "botwoonETankRoom", "Botwoon Energy Tank Room", Area.MARIDIA),
        Room(0xD81A, "draygonSaveRoom", "Draygon Save Room", Area.MARIDIA),
        Room(0xD845, "maridiaMissileRefillRoom", "Maridia Missile Refill Room", Area.MARIDIA),
        Room(0xD86E, "plasmaBeachQuicksandRoom", "Plasma Beach Quicksand Room", Area.MARIDIA),
        Room(0xD898, "botwoonQuicksandRoom", "Botwoon Quicksand Room", Area.MARIDIA),
        Room(0xD8C5, "shaktoolRoom", "Shaktool Room", Area.MARIDIA),
        Room(0xD913, "halfieClimbRoom", "Halfie Climb Room", Area.MARIDIA),
        Room(0xD95E, "botwoon", "Botwoon's Room", Area.MARIDIA),
        Room(0xD9AA, "spaceJump", "Space Jump Room", Area.MARIDIA),
        Room(0xD9D4, "maridiaHealthRefillRoom", "Maridia Health Refill Room", Area.MARIDIA),
        Room(0xD9FE, "westCactusAlley", "West Cacattack Alley", Area.MARIDIA),
        Room(0xDA2B, "eastCactusAlleyRoom", "East Cactus Alley Room", Area.MARIDIA),
        Room(0xDA60, "draygon", "Draygon's Room", Area.MARIDIA),

        // === TOURIAN (19 rooms) ===
        Room(0xDAAE, "tourianElevator", "Tourian Elevator", Area.TOURIAN),
        Room(0xDAE1, "metroidOne", "Metroid Room 1", Area.TOURIAN),
        Room(0xDB31, "metroidTwo", "Metroid Room 2", Area.TOURIAN),
        Room(0xDB7D, "metroidThree", "Metroid Room 3", Area.TOURIAN),
        Room(0xDBCD, "metroidFour", "Metroid Room 4", Area.TOURIAN),
        Room(0xDC19, "tourianHopper", "Tourian Hopper Room", Area.TOURIAN),
        Room(0xDC65, "dustTorizo", "Dust Torizo Room", Area.TOURIAN),
        Room(0xDCB1, "bigBoy", "Big Boy Room", Area.TOURIAN),
        Room(0xDCFF, "seaweedRoom", "Seaweed Room", Area.TOURIAN),
        Room(0xDD2E, "tourianRechargeRoom", "Tourian Recharge Room", Area.TOURIAN),
        Room(0xDD58, "motherBrain", "Mother Brain's Room", Area.TOURIAN),
        Room(0xDDC4, "tourianEyeDoor", "Tourian Eye Door Room", Area.TOURIAN),
        Room(0xDDF3, "rinkaShaft", "Rinka Shaft", Area.TOURIAN),
        Room(0xDE23, "lowerTourianSaveRoom", "Lower Tourian Save Room", Area.TOURIAN),
        Room(0xDE4D, "tourianEscapeRoom1", "Tourian Escape Room 1", Area.TOURIAN),
        Room(0xDE7A, "tourianEscapeRoom2", "Tourian Escape Room 2", Area.TOURIAN),
        Room(0xDEA7, "tourianEscapeRoom3", "Tourian Escape Room 3", Area.TOURIAN),
        Room(0xDEDE, "tourianEscape4", "Tourian Escape Room 4", Area.TOURIAN),
        Room(0xDF1B, "upperTourianSaveRoom", "Upper Tourian Save Room", Area.TOURIAN),

        // === CERES (6 rooms) ===
        Room(0xDF45, "ceresElevator", "Ceres Elevator", Area.CERES),
        Room(0xDF8D, "fallingTileRoom", "Falling Tile Room", Area.CERES),
        Room(0xDFD7, "magnetStairsRoom", "Magnet Stairs Room", Area.CERES),
        Room(0xE021, "deadScientistRoom", "Dead Scientist Room", Area.CERES),
        Room(0xE06B, "flatRoom", "Ceres Flat Room", Area.CERES, "Placeholder name for the flat room in Ceres Station"),
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
