package com.lavaflow.puzzle.core

// Restored edition-1 boards with small audited bypasses; see content/levels/v3.
internal object RestoredCampaign {
    val levels = listOf(
        authored(1, "The First Ember", "Read both openings before turning a channel.", 5, 2, 2,
            12, 7, 9, """
            R L1 I0 R I1
            R L0 L2 R L3
            L1 I3 R I0 R
            R L3 I1 L1 R
            I1 R L0 L3 R
        """, listOf(2,7,6,11,16,17,18,23,22), listOf(7,6,11,16,18,22), edition = 3),
        authored(2, "Fork in the Fire", "Read both openings before turning a channel.", 5, 2, 2,
            15, 9, 11, """
            R R I0 T2 R
            R I1 L3 L1 I0
            L0 L0 X L2 R
            R I1 R T0 L1
            I1 L3 L1 R R
        """, listOf(2,7,8,13,12,11,16,21,22), listOf(7,8,13,11,16,21,22), edition = 3),
        authored(3, "Basalt Bend", "Read both openings before turning a channel.", 5, 2, 2,
            17, 11, 13, """
            I1 L0 L2 R L3
            L0 T2 R I1 R
            I1 R L0 T1 L3
            L3 X L3 I0 R
            R R L1 L2 I1
        """, listOf(2,1,6,5,10,15,16,17,12,13,18,23,22), listOf(2,1,6,5,10,15,12,13,23), edition = 3),
        authored(4, "The Crucible", "Several routes reach the portal. Compare their turns; spare channels may stay dry.", 5, 2, 2,
            18, 12, 14, """
            R R L3 L0 I1
            L0 T0 L0 L0 L1
            I1 T1 X I0 L2
            R L0 I1 L1 R
            I1 R L1 L0 T1
        """, listOf(2,3,8,9,14,13,12,11,16,17,18,23,22), listOf(2,3,3,9,14,13,18,23,23,23), edition = 3),
        authored(5, "Heart of the Volcano", "Several routes reach the portal. Compare their turns; spare channels may stay dry.", 5, 2, 2,
            20, 14, 16, """
            L0 I0 L1 L0 L1
            L1 I0 X L1 I1
            I1 R R L0 L1
            L2 L1 T1 L3 I1
            I1 L0 L2 R R
        """, listOf(2,3,8,7,6,5,10,15,16,21,22), listOf(2,2,2,3,3,8,8,6,10,15,15,16), edition = 3),
        authored(6, "Cinder Crossing", "Several routes reach the portal. Compare their turns; spare channels may stay dry.", 5, 2, 2,
            21, 15, 17, """
            I1 R L1 L3 R
            R L3 R T1 L0
            L0 L0 T3 L3 X
            R I1 L0 I0 L2
            R L3 L3 R L1
        """, listOf(2,3,8,13,12,11,16,21,22), listOf(2,2,2,3,3,3,12,11,16,21,22,22,22), edition = 3),
        authored(7, "Ember Arches", "Several routes reach the portal. Compare their turns; spare channels may stay dry.", 5, 2, 2,
            22, 16, 18, """
            R L3 L1 I2 R
            L1 T3 R R L3
            X L3 T3 L0 R
            L1 I3 L2 I0 L2
            I1 R L2 L0 R
        """, listOf(2,1,6,11,12,13,18,23,22), listOf(2,2,1,1,11,12,13,13,23,23,23,22,22,22), edition = 3),
        authored(8, "Molten Steps", "Several routes reach the portal. Compare their turns; spare channels may stay dry.", 5, 2, 2,
            23, 17, 19, """
            L0 R L2 L3 R
            R L0 R T3 L1
            L0 L2 T1 I1 X
            R I2 L2 I1 L0
            L1 L1 L1 R R
        """, listOf(2,3,8,9,14,13,12,11,16,21,22), listOf(2,2,3,3,3,8,9,12,11,11,11,21,21,21,22), edition = 3),
        authored(9, "Split Current", "Several routes reach the portal. Compare their turns; spare channels may stay dry.", 5, 2, 2,
            24, 18, 20, """
            T0 T2 L2 L2 T3
            L2 T0 I3 L0 I1
            I1 R R L2 L2
            R T1 I1 L0 R
            L2 X L1 R L1
        """, listOf(2,1,6,7,8,3,4,9,14,13,18,17,16,21,22), listOf(2,8,8,8,3,3,3,9,14,13,13,13,18,18,18,22), edition = 3),
        authored(10, "Ashen Junction", "Several routes reach the portal. Compare their turns; spare channels may stay dry.", 5, 2, 2,
            25, 19, 21, """
            R L3 L1 R R
            L2 L2 L1 T3 I1
            X I0 T0 T3 R
            R L2 I3 L1 R
            L2 L2 L3 R L1
        """, listOf(2,1,6,7,12,13,18,17,16,21,22), listOf(2,2,1,1,6,6,7,18,18,16,16,16,21,21,22,22,22), edition = 3),
        authored(11, "Obsidian Trail", "Several routes reach the portal. Compare their turns; spare channels may stay dry.", 5, 2, 2,
            26, 20, 22, """
            T2 T2 L1 L2 T2
            L2 T0 I1 L0 I0
            I2 R R L0 L0
            R T3 I1 L1 R
            I3 X L0 R I2
        """, listOf(2,1,6,7,8,3,4,9,14,13,18,17,16,21,22), listOf(2,2,8,8,8,3,3,3,14,14,14,13,18,18,16,16,22,22), edition = 3),
        authored(12, "Golden Veins", "Several routes reach the portal. Compare their turns; spare channels may stay dry.", 5, 2, 2,
            27, 21, 23, """
            R L2 L1 R L1
            L2 L1 L3 T2 R
            X I2 T0 T3 I0
            R L3 I1 L1 R
            L3 L1 L1 R I1
        """, listOf(2,1,6,7,12,13,18,17,16,21,22), listOf(2,2,1,1,1,6,6,6,7,7,7,18,18,16,16,21,21,21,22), edition = 3),
        authored(13, "The Wider Rift", "A wider field, longer routes and tighter move margins. Use tools to improve your chosen path.", 6, 2, 3,
            26, 21, 23, """
            R R I3 L1 R I2
            L2 T3 L1 R R R
            X L0 L0 I1 L3 R
            L1 T0 L1 R I3 R
            R L3 R L0 L2 R
            R R L0 I1 R R
        """, listOf(2,8,7,6,12,13,19,20,14,15,16,22,28,27,33), listOf(2,8,8,7,6,6,6,13,13,20,20,14,16,16,16,22,28,27,33), edition = 3),
        authored(14, "Crater Road", "A wider field, longer routes and tighter move margins. Use tools to improve your chosen path.", 6, 3, 2,
            27, 22, 24, """
            R R R I1 R R
            I2 R R L1 T3 L1
            R L2 I1 L3 L1 X
            R I0 R L1 T0 L1
            R L3 L3 R L0 R
            R R I3 L2 R I1
        """, listOf(3,9,10,11,17,16,22,21,15,14,13,19,25,26,32), listOf(3,9,9,9,10,11,21,21,21,15,15,15,13,13,13,25,26,26,26,32), edition = 3),
        authored(15, "Forked Basin", "A wider field, longer routes and tighter move margins. Use tools to improve your chosen path.", 6, 2, 3,
            27, 22, 24, """
            R R I1 R R I3
            L1 T1 L1 R R R
            X L3 L3 I0 L1 I1
            L1 I1 L0 R I1 R
            R R R L2 L0 R
            R L1 R I2 R I1
        """, listOf(2,8,7,13,12,18,19,20,14,15,16,22,28,27,33), listOf(2,8,8,18,18,18,20,20,20,14,14,15,16,22,28,28,28,27,27,27), edition = 3),
        authored(16, "Deep Channels", "A wider field, longer routes and tighter move margins. Use tools to improve your chosen path.", 6, 3, 2,
            28, 23, 25, """
            R T3 L2 L2 L3 R
            R T3 T0 I0 T1 L1
            L2 L1 R R R R
            I2 L0 T3 X L0 I3
            L2 L2 R R I1 R
            I1 R L0 I1 T2 I2
        """, listOf(3,2,8,7,13,12,18,24,25,19,20,21,22,28,34,33,32), listOf(3,2,2,2,7,7,13,13,12,12,12,24,24,25,19,20,22,22,28,34,32), edition = 3),
        authored(17, "Fire Labyrinth", "A wider field, longer routes and tighter move margins. Use tools to improve your chosen path.", 6, 2, 3,
            28, 23, 25, """
            I3 L1 L1 L3 T2 I1
            R T3 I2 T0 T3 R
            R R R R L1 L3
            I1 L0 X T2 L1 I1
            R I2 R R L3 L0
            R T2 I0 L1 R L0
        """, listOf(2,1,7,8,9,10,16,17,23,29,28,22,21,20,19,25,31,32,33), listOf(2,2,7,8,16,16,16,17,17,17,23,29,29,29,28,22,19,31,31,32,33), edition = 3),
        authored(18, "Ancient Circuit", "A wider field, longer routes and tighter move margins. Use tools to improve your chosen path.", 6, 3, 2,
            29, 24, 26, """
            I1 T2 L3 L1 L2 I1
            R T1 T0 I3 T1 R
            L1 L0 R R R I0
            I0 L3 T3 X L3 R
            L2 L0 R R I2 R
            R R L2 I2 T0 L2
        """, listOf(3,2,8,7,13,12,18,24,25,19,20,21,22,28,34,33,32), listOf(3,3,2,2,13,13,13,24,24,25,25,25,19,19,20,22,22,22,33,32,32,32), edition = 3),
        authored(19, "Ember Spires", "A wider field, longer routes and tighter move margins. Use tools to improve your chosen path.", 6, 2, 3,
            29, 24, 26, """
            R L2 L1 L3 T2 I0
            L1 T3 I2 T0 T3 R
            R R R R L0 L0
            R L2 X T0 L3 I2
            L0 I0 R R L2 L1
            R T3 I3 L3 R L3
        """, listOf(2,3,9,10,16,17,23,29,28,22,21,20,19,25,31,32,33), listOf(2,2,2,3,3,3,17,17,29,29,28,28,22,22,22,19,19,19,31,33,33,33), edition = 3),
        authored(20, "Under the Caldera", "A wider field, longer routes and tighter move margins. Use tools to improve your chosen path.", 6, 3, 2,
            30, 24, 27, """
            L0 I1 L1 L2 T2 T0
            T0 R L1 I1 T0 L0
            L1 I1 I2 L3 R R
            L0 R R L2 I2 L0
            R I0 L0 I3 L2 I0
            I0 R I1 R L3 L3
        """, listOf(3,4,10,9,8,2,1,0,6,12,13,14,15,21,22,23,29,35,34,28,27,26,32), listOf(3,3,8,8,8,2,0,6,12,12,12,14,15,15,15,21,21,22,23,23,34,26,32), edition = 3),
        authored(21, "Molten Maze", "A wider field, longer routes and tighter move margins. Use tools to improve your chosen path.", 6, 2, 3,
            29, 24, 27, """
            T0 T2 L2 L3 I3 L3
            L3 T0 I2 L1 R T1
            R R L2 I3 I3 L3
            L2 I3 L2 R L0 R
            I3 L0 I0 L2 R I3
            L3 L1 R I3 L1 R
        """, listOf(2,1,7,8,9,3,4,5,11,17,16,15,14,20,19,18,24,30,31,25,26,27,33), listOf(2,8,9,9,3,3,5,5,5,14,14,14,20,18,18,18,24,30,31,31,25,26,33), edition = 3),
        authored(22, "Basalt Crown", "A wider field, longer routes and tighter move margins. Use tools to improve your chosen path.", 6, 3, 2,
            30, 25, 28, """
            L2 I2 L3 L1 T2 T0
            T0 R L0 I0 T0 L3
            L1 I0 I1 L2 R R
            L3 R R L3 I0 L1
            R R L0 I3 L2 I0
            R I0 I0 R L2 L1
        """, listOf(3,4,10,9,8,2,1,0,6,12,13,14,15,21,22,23,29,35,34,28,27,26,32), listOf(3,3,3,9,2,2,2,1,0,0,0,6,12,12,12,13,21,22,23,35,35,34,34,26), edition = 3),
        authored(23, "Twisting Depths", "A wider field, longer routes and tighter move margins. Use tools to improve your chosen path.", 6, 2, 3,
            30, 25, 28, """
            T1 I1 L1 R L3 L3
            I0 R R R T3 T0
            X R X I3 L0 I2
            I3 R I1 R R X
            T3 R T2 R R I0
            L1 I0 L0 L3 I2 T1
        """, listOf(2,1,0,6,12,18,24,30,31,32,26,20,14,15,16,10,11,17,23,29,35,34,33), listOf(2,2,18,30,30,30,31,32,32,32,26,20,16,16,16,10,10,11,11,35,35,34,33,33), edition = 3),
        authored(24, "The Long Descent", "A wider field, longer routes and tighter move margins. Use tools to improve your chosen path.", 6, 3, 5,
            31, 26, 29, """
            L2 T0 I1 L1 R I1
            X R R R T1 L2
            I0 L0 I2 L3 T1 T3
            I1 I0 R I3 I0 I2
            I0 I0 R I2 I3 I1
            L1 L0 R L2 L0 I2
        """, listOf(3,2,1,0,6,12,18,24,30,31,25,19,13,14,15,21,27,33,34,28,22,16,17,23,29,35), listOf(3,3,0,0,0,18,30,30,30,31,31,31,13,14,15,15,15,21,33,33,34,34,34,28,29), edition = 3),
        authored(25, "Furnace Web", "A wider field, longer routes and tighter move margins. Use tools to improve your chosen path.", 6, 2, 3,
            31, 26, 29, """
            T3 I0 L1 R L3 L2
            I2 R R R T1 T0
            X R X I2 L1 I1
            I1 R I3 R R X
            T3 R T1 R R I2
            L2 I2 L0 L2 I2 T1
        """, listOf(2,1,0,6,12,18,24,30,31,32,26,20,14,15,16,10,11,17,23,29,35,34,33), listOf(2,2,1,0,0,18,30,30,31,32,32,32,20,15,16,16,11,11,17,35,35,34,33,33,33), edition = 3),
        authored(26, "Obsidian Heart", "A wider field, longer routes and tighter move margins. Use tools to improve your chosen path.", 6, 3, 2,
            32, 27, 30, """
            L1 L0 R L1 I1 T0
            T3 T3 R R R I3
            I3 L3 I1 X R X
            X R R I0 R I1
            I3 R R T0 R T0
            T2 I3 L3 L1 I0 L0
        """, listOf(3,4,5,11,17,23,29,35,34,33,27,21,15,14,13,7,6,12,18,24,30,31,32), listOf(3,3,3,5,5,11,23,29,35,35,35,34,33,33,33,27,13,6,6,12,24,30,30,32,32,32), edition = 3),
        authored(27, "Crimson Passage", "A wider field, longer routes and tighter move margins. Use tools to improve your chosen path.", 6, 2, 0,
            32, 27, 30, """
            I3 R L1 I0 T0 L0
            L1 T1 R R R X
            T1 T3 L2 I0 L3 I1
            I0 I0 I3 R I3 I1
            I2 I0 I1 R I1 I3
            I1 L3 L2 R L3 L1
        """, listOf(2,3,4,5,11,17,23,29,35,34,28,22,16,15,14,20,26,32,31,25,19,13,12,18,24,30), listOf(2,2,2,3,5,5,17,23,29,35,35,34,28,22,16,16,16,15,14,14,14,20,26,32,31,30), edition = 3),
        authored(28, "Portal Forge", "A wider field, longer routes and tighter move margins. Use tools to improve your chosen path.", 6, 3, 2,
            33, 28, 31, """
            L1 L0 R L1 I0 T1
            T0 T0 R R R I2
            I2 L1 I1 X R X
            X R R I2 R I3
            I3 R R T2 R T0
            T2 I0 L0 L1 I0 L0
        """, listOf(3,4,5,11,17,23,29,35,34,33,27,21,15,14,13,7,6,12,18,24,30,31,32), listOf(3,3,3,4,5,23,29,35,35,35,34,33,33,33,27,13,13,13,7,7,6,24,30,30,31,32,32), edition = 3),
        authored(29, "Final Ascent", "A wider field, longer routes and tighter move margins. Use tools to improve your chosen path.", 6, 2, 3,
            33, 28, 31, """
            T1 I1 L1 R L0 L0
            I0 R R R T3 T0
            X R X I2 L0 I1
            I0 R I1 R R X
            T0 R T0 R R I3
            L1 I1 L0 L2 I0 T1
        """, listOf(2,1,0,6,12,18,24,30,31,32,26,20,14,15,16,10,11,17,23,29,35,34,33), listOf(2,2,24,30,30,30,32,32,32,26,20,15,16,16,16,10,10,11,11,17,29,35,35,34,33,33,33), edition = 3),
        authored(30, "Master of the Flow", "A wider field, longer routes and tighter move margins. Use tools to improve your chosen path.", 6, 3, 5,
            34, 29, 32, """
            L2 T3 I2 L1 R I2
            X R R R T1 L0
            I2 L2 I3 L3 T1 T3
            I2 I3 R I2 I3 I3
            I2 I2 R I0 I0 I3
            L3 L0 R L1 L0 I1
        """, listOf(3,2,1,0,6,12,18,24,30,31,25,19,13,14,15,21,27,33,34,28,22,16,17,23,29,35), listOf(3,3,2,1,0,0,0,30,31,31,31,19,13,13,13,15,15,15,33,33,33,34,34,34,22,23,29,35), edition = 3)
    )
}
