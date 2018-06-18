package permutations

fun perm() : ArrayList<List<Int>> {
        val l1 = listOf(1, 2, 3, 4)
        val l2 = listOf(2, 3, 4, 5)
        val l3 = listOf(3, 4, 5, 6)
        val perms = ArrayList<List<Int>>()
        var lock = 0
        for (i in l1) {
            for (j in l2) {
                for (k in l3) {
                        var adder = listOf(i, j, k)
                        for (entry in perms) {
                            if (adder.sorted() == entry.sorted()) {
                                lock = 1
                            }
                            if ((i == j) or (i == k) or (j == k)) {
                                lock = 1
                            }
                        }
                        if (lock != 1) {
                            perms.add(adder)
                        }
                        if (lock == 1) {
                            lock = 0
                        }
                }
            }
        }
    //println(perms.size)
    return(perms)
} // This function generates all 20 permutations of triplets between 1 and 6, non redundant.