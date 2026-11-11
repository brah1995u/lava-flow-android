package com.lavaflow.puzzle.core

// Compiled from explicit content/levels/v2/blueprints.json. No seeded runtime generation.
internal object ReviewedCampaign {
    val levels = listOf(
        authored(1, "The First Ember", "Learn facing openings and spare channels.", 5, 2, 2,
            12, 7, 9, """
            R L1 I0 R I1
            R L0 L2 R L3
            L1 I3 R I0 R
            R L3 I1 L1 R
            I1 R L0 L3 R
        """, listOf(2,7,6,11,16,17,18,23,22), listOf(7,6,11,16,18,22)),
        authored(2, "Turn Toward the Light", "Read each bend before rotating it.", 5, 1, 3,
            14, 8, 10, """
            R I0 R R R
            R L3 I0 L0 R
            R R R I3 R
            R R R I0 R
            R R R I3 R
        """, listOf(1,6,7,8,13,18,23), listOf(6,7,8,8,13,23)),
        authored(3, "Read Both Ends", "Work backward from the portal when the next turn is unclear.", 5, 3, 2,
            14, 8, 10, """
            R R R I0 R
            R L3 I0 L2 R
            R I0 R R R
            R L3 L1 R R
            R R I0 R R
        """, listOf(3,8,7,6,11,16,17,22), listOf(8,7,6,6,16,17)),
        authored(4, "The Joined Bends", "Combine familiar bends; a connected route wins immediately after arrival.", 5, 2, 3,
            15, 9, 11, """
            R R I0 R R
            R L0 L2 R R
            R I3 R R R
            R L2 I1 L1 R
            R R R I3 R
        """, listOf(2,7,6,11,16,17,18,23), listOf(7,6,11,16,16,18,23)),
        authored(5, "Two Ways Forward", "A T-junction can split the lava. Either route may reach the portal.", 5, 2, 2,
            11, 5, 7, """
            R R I0 R R
            R L3 T2 R R
            R I3 I3 R R
            R L3 T2 R R
            R R I0 R R
        """, listOf(2,7,12,17,22), listOf(7,12,17)),
        authored(6, "The Inner Bend", "Compare the inner connection with the outer detour.", 5, 4, 1,
            15, 9, 11, """
            R R R L0 L3
            L1 L1 L0 T2 R
            I3 L0 T3 L2 R
            I0 R R R R
            L3 L1 R R R
        """, listOf(4,3,8,7,12,11,6,5,10,15,20,21), listOf(3,8,7,6,10,20,21)),
        authored(7, "The Still Cross", "A cross joins all four arms and does not need rotating.", 5, 2, 2,
            11, 5, 7, """
            R R I0 R R
            R R T0 L0 R
            R L0 X L2 R
            R L2 T2 R R
            R R I3 R R
        """, listOf(2,7,12,17,22), listOf(7,17,22)),
        authored(8, "Choose Your Descent", "One well-read junction can replace a long detour.", 5, 1, 2,
            17, 11, 13, """
            R I0 R R R
            L3 X L1 L0 L0
            I3 T0 T3 L1 I3
            L3 L0 R R I0
            R R L1 I0 L2
        """, listOf(1,6,11,12,13,8,9,14,19,24,23,22), listOf(12,13,13,8,9,9,14,24,23)),
        authored(9, "Under the Arch", "Follow the destination before choosing the left or right branch.", 5, 3, 3,
            19, 13, 15, """
            R R L0 L3 R
            L0 I3 T3 L3 R
            L3 I1 L1 I3 R
            R R L2 T3 L1
            R R R L0 L1
        """, listOf(3,2,7,8,13,18,19,24,23), listOf(2,7,8,8,8,13,18,19,24,24,23)),
        authored(10, "The Short Passage", "A short connection can save several rotations; spare pipes may stay dry.", 5, 0, 2,
            19, 13, 15, """
            L3 L0 R L0 L0
            R L3 T1 L1 I3
            R L3 T3 T0 L2
            R L3 I1 T2 R
            R R L0 L1 R
        """, listOf(0,1,6,7,8,13,18,23,22), listOf(0,1,1,6,7,8,13,18,23,23,22)),
        authored(11, "The Basin Choice", "Several junctions offer real route choices, not extra victory conditions.", 5, 1, 3,
            15, 9, 11, """
            R I0 R R R
            R T0 I0 L0 R
            R T2 I0 T2 R
            R T0 I3 L2 R
            R L2 I0 L1 R
        """, listOf(1,6,11,16,21,22,23), listOf(6,11,16,21,21,22,23)),
        authored(12, "Heart of the Crater", "Use the central cross to compare two approaches to the portal.", 5, 3, 1,
            14, 8, 10, """
            R R R I3 R
            R L3 I0 T1 R
            R I3 R T0 L1
            R L1 I0 X L1
            R L3 I0 L2 R
        """, listOf(3,8,13,18,23,22,21), listOf(3,13,23,22,21,21)),
        authored(13, "The Wider Rift", "The same facing-port rule works on the wider board.", 6, 2, 3,
            20, 12, 15, """
            R R I0 R R R
            R R I3 R R R
            L0 I0 L1 R R R
            I3 R R R R R
            L2 I1 I0 L1 R R
            R R R I2 R R
        """, listOf(2,8,14,13,12,18,24,25,26,27,33), listOf(8,14,14,13,12,18,24,24,26,27)),
        authored(14, "Forked Basin", "Read the long branch before committing moves to it.", 6, 1, 3,
            17, 9, 12, """
            R I0 R R R R
            R T0 I0 L0 R R
            R I3 R I3 R R
            R T3 I0 L0 R R
            R I3 R R R R
            R L2 I0 L1 R R
        """, listOf(1,7,13,19,25,31,32,33), listOf(7,13,25,31,31,32,33)),
        authored(15, "Crossing Streams", "All four arms meet inside the fixed cross; follow the incoming stream.", 6, 2, 3,
            22, 14, 17, """
            R L3 L2 R R R
            R T0 L0 R R R
            L2 X L2 L0 L1 R
            L3 T1 I0 L2 I3 R
            R R R R I3 R
            R R R L0 L2 R
        """, listOf(2,1,7,13,19,20,21,15,16,22,28,34,33), listOf(2,1,1,7,20,21,15,16,22,28,34,33)),
        authored(16, "The Upper Shelf", "An upward branch is valid, but the portal still determines the useful route.", 6, 4, 2,
            19, 11, 14, """
            R R L3 L1 I3 R
            R R I3 L2 T0 R
            R R I3 R I3 R
            R R L1 I0 T2 R
            R R R R I3 R
            R R L0 I0 L1 R
        """, listOf(4,10,16,22,28,34,33,32), listOf(4,10,16,22,28,34,34,33,32)),
        authored(17, "Twin Chambers", "Solve one useful connection at a time through two separate choices.", 6, 1, 3,
            21, 13, 16, """
            R I3 R R R R
            R T3 L0 R R R
            R L3 T1 L0 R R
            R R L0 T2 R R
            R L0 T1 L1 R R
            R L2 I0 L1 R R
        """, listOf(1,7,13,14,20,21,27,26,25,31,32,33), listOf(1,13,14,27,27,26,25,31,31,32,33)),
        authored(18, "The Meeting Point", "Streams can merge at a cross without creating a second task.", 6, 0, 3,
            14, 6, 9, """
            I3 R R R R R
            T3 I0 L0 R R R
            T0 I0 X L3 R R
            L2 I0 T1 T2 R R
            R R R I3 R R
            R R R I3 R R
        """, listOf(0,6,12,13,14,20,21,27,33), listOf(0,13,27,33)),
        authored(19, "Around the Obsidian", "The nearest-looking branch is not always the least work.", 6, 3, 2,
            16, 8, 11, """
            R R R I3 R R
            R L3 I0 T1 R R
            R I3 R T0 L3 R
            R L2 T1 T1 L2 R
            R R I3 R R R
            R R I3 R R R
        """, listOf(3,9,15,21,20,26,32), listOf(3,15,21,21,26,32)),
        authored(20, "The Lower Galleries", "Compare two local bypasses before taking the final turn.", 6, 1, 4,
            18, 10, 13, """
            R I3 R R R R
            R T2 L0 R R R
            R T0 L2 R R R
            R T3 I0 L3 R R
            R T0 I0 L2 R R
            R L2 I0 I1 L1 R
        """, listOf(1,7,13,19,25,31,32,33,34), listOf(1,7,13,25,31,31,32,34)),
        authored(21, "The Outer Rim", "Borders stop a branch; choose a route that really faces the next channel.", 6, 4, 2,
            25, 17, 20, """
            R R R L0 L2 R
            R R R T2 L0 R
            R L3 I0 T2 L0 R
            R T0 L0 R R R
            L3 T1 L2 R R R
            L3 I0 L1 R R R
        """, listOf(4,3,9,15,14,13,19,25,24,30,31,32), listOf(4,3,9,15,14,13,13,19,25,25,24,24,30,31,32)),
        authored(22, "Deep Galleries", "Trace through the crossing before spending moves on a remote section.", 6, 0, 2,
            18, 10, 13, """
            I3 R R R R R
            T2 I0 L0 R R R
            T0 I0 X L0 R R
            T3 I2 L2 I3 R R
            T0 I3 I0 L2 R R
            L2 I0 L1 R R R
        """, listOf(0,6,12,18,24,30,31,32), listOf(0,6,12,24,30,30,31,32)),
        authored(23, "The Central Spine", "Fixed crosses carry the flow; spend rotations on the connecting bends.", 6, 2, 3,
            16, 8, 11, """
            R R I3 R R R
            L2 I3 X L0 R R
            I3 R T0 T2 L3 R
            L2 T1 T0 R I3 R
            R L3 L0 L0 L1 R
            R R R I3 R R
        """, listOf(2,8,14,20,26,27,33), listOf(2,14,20,27,27,33)),
        authored(24, "Lava Terrace", "Choose where to leave the main descent and approach the far portal.", 6, 1, 5,
            20, 12, 15, """
            R I3 R R R R
            R T3 I0 L3 R R
            R T2 I0 X L0 R
            R T0 I0 L2 I3 R
            R I3 R R I3 R
            R L2 I0 I1 T3 L0
        """, listOf(1,7,13,19,25,31,32,33,34,35), listOf(1,13,19,25,31,31,32,34,35,35)),
        authored(25, "Across the Furnace", "Two central approaches lead to different costs at the lower turn.", 6, 5, 0,
            21, 13, 16, """
            R R R R R I3
            R R R L3 I0 T0
            R R L2 X I0 T2
            R R I3 T3 I0 L1
            R R I3 I3 R R
            L3 I0 T3 L1 R R
        """, listOf(5,11,10,9,15,21,27,33,32,31,30), listOf(5,10,9,9,27,33,33,32,31,30,30)),
        authored(26, "Three Passages", "Plan the approach to the portal before choosing among three passages.", 6, 1, 1,
            26, 18, 21, """
            R I3 R R R R
            L3 T2 L0 R R R
            I3 R T0 I0 L3 R
            L1 T1 T1 R T3 L0
            R L3 L0 R I3 I3
            R L3 I0 I0 T1 L0
        """, listOf(1,7,8,14,15,16,22,28,34,33,32,31), listOf(1,7,7,8,8,15,16,16,16,28,34,34,33,32,31,31)),
        authored(27, "Under the Caldera", "A cross can join routes that look separate; inspect its four neighbors.", 6, 3, 3,
            23, 15, 18, """
            R R R I3 R R
            R L3 I0 T2 L0 R
            R T0 L3 L3 X L3
            R L1 T2 T2 I3 I3
            R R R L2 L2 I3
            R R R L3 I0 L1
        """, listOf(3,9,15,16,17,23,29,35,34,33), listOf(3,9,15,17,17,17,23,29,35,35,34,33,33)),
        authored(28, "The Connected Terraces", "Combine earlier shortcuts; unused terraces do not prevent a valid finish.", 6, 1, 5,
            18, 10, 13, """
            R I3 R R R R
            R T2 T1 I0 L3 R
            R T3 T1 I0 T1 R
            R T0 L1 R I3 R
            R T2 L1 R I3 R
            R L2 I0 I1 T2 L1
        """, listOf(1,7,13,19,25,31,32,33,34,35), listOf(1,7,19,25,31,31,32,35)),
        authored(29, "The Last Approach", "Compare a direct finish with the linked side passages; bonuses can save a chosen route.", 6, 4, 5,
            22, 14, 17, """
            R R L0 L3 I3 R
            R R T3 T1 T1 R
            R R T0 I0 L0 R
            L3 I0 T1 L0 R R
            T0 I0 T0 T3 L0 R
            L2 I0 L0 R L3 L0
        """, listOf(4,10,9,15,21,20,26,27,28,34,35), listOf(4,10,10,21,21,21,27,28,28,34,35,35)),
        authored(30, "Master of the Flow", "Use the rules you know: choose a continuous route, follow facing ports, and reach the portal.", 6, 2, 3,
            21, 13, 16, """
            L3 I0 L2 R L3 L1
            I3 R L2 I0 T2 I3
            T2 I3 X I0 T0 I3
            T0 I0 T1 R I3 I3
            I3 R I3 R L2 T1
            L2 I0 L0 L3 I0 L0
        """, listOf(2,1,0,6,12,13,14,20,26,32,33), listOf(2,1,0,0,6,12,12,26,33,33,33))
    )
}
