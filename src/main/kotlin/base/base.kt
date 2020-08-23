package base

import java.util.HashSet
import java.io.BufferedReader
import java.io.FileReader
import java.util.ArrayList
import java.io.File
import java.lang.management.ManagementFactory
import org.apache.commons.math3.stat.inference.TestUtils
import java.util.stream.IntStream
import kotlin.math.ln
import org.apache.commons.collections4.ListUtils
import org.apache.commons.io.IOUtils

fun starttimer(): Long {
    val threadMX = ManagementFactory.getThreadMXBean()
    assert(threadMX.isCurrentThreadCpuTimeSupported)
    threadMX.isThreadCpuTimeEnabled = true
    return threadMX.currentThreadCpuTime
}
fun endtimer(time: Long) {
    val threadMX = ManagementFactory.getThreadMXBean()
    val end = threadMX.currentThreadCpuTime
    println("${(end-time)/1000000}ms")
}

data class Protein(val id: String, val score: Double, val molw: Double, val spc: Int, var SAF: Double, var logNSAF: Double) {
    override fun hashCode() = id.hashCode()
    override fun equals(other: Any?) = other?.let { id == (it as Protein).id } ?: false
    fun giveMeIDandSPC(): String {return "$id: $spc"}
}
data class UniqueProtein(var id: String, var cumulativespc: Int, var r: DoubleArray, var SAF: DoubleArray, var logNSAF: DoubleArray) {
    override fun hashCode() = id.hashCode()
    override fun equals(other: Any?) = other?.let { id == (it as UniqueProtein).id } ?: false
    fun giveMeIDandSPC(): String {return "$id: total: $cumulativespc, replicates: $r"}
}
data class TTestProtein(var id: String, var state1spc: DoubleArray, var state2spc: DoubleArray, val state1lognsaf: DoubleArray, val state2lognsaf: DoubleArray) {
    override fun hashCode() = id.hashCode()
    override fun equals(other: Any?) = other?.let { id == (it as TTestProtein).id } ?: false
}
data class TTestResult(var id: String, var ttestspcval: Double, var ttestlognsafval: Double, var state1spc: DoubleArray, var state2spc: DoubleArray, val state1lognsaf: DoubleArray, val state2lognsaf: DoubleArray) {
    override fun hashCode() = id.hashCode()
    override fun equals(other: Any?) = other?.let { id == (it as TTestResult).id } ?: false}

fun engineLoop(): String {
    while (1 != 2) {
        var engine = readLine()!!
        if (engine == "GPM" || engine == "PD" || engine == "MM") {
            return engine
        } else {
            println("Invalid input, try again")
        }
    }
}

fun declareLine(eng: String, line: String): List<String> {

    if (eng == "GPM") {
        return line.split(",")
    }

    if (eng == "PD") {
        return line.split(",(?=(?:[^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*\$)".toRegex()) // Yes, really.
    }

    else { // For MetaMorpheus and those horrible pipes
        return line.split(",(?=(?:[^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*\$)".toRegex())
        }
}

fun pullCSV(path: String, eng: String) : ArrayList<HashSet<Protein>> {
    val state = ArrayList<HashSet<Protein>>()
    val totalmolw = ArrayList<Double>()
    val totalspc = ArrayList<Int>()
    try {
        File(path).list().forEach{
            try {
                var intertotalspc = 0
                var intertotalmolw = 0.00
                var intersumSAF = 0.00
                val proteins = HashSet<Protein>() // Initialises the Hash Set for re-use within the new CSV file
                val fileReader = BufferedReader(FileReader("$path$it"))
                fileReader.readLine() // Reads and skips the header
                var line = fileReader.readLine()
                while (line != null) {
                    val linevalue = declareLine(eng, line)
                    if (linevalue.isNotEmpty()) {
                        if (eng == "GPM") {
                            val protein = Protein(linevalue[0], // ID
                                    linevalue[3].toDouble(), // Score
                                    linevalue[5].toDouble(), // MolW
                                    linevalue[2].toInt(), // SpC
                                    0.00, 0.00) // Dummy values for SAFs and logNSAFs
                            proteins.add(protein)
                            intertotalspc += linevalue[2].toInt()
                            intertotalmolw += linevalue[5].toDouble()
                        }
                        if (eng == "PD") {
                            val protein = Protein(linevalue[0], // ID
                                    linevalue[2].toDouble(), // Score
                                    linevalue[7].toDouble(), // MolW
                                    linevalue[4].toInt(), // SpC
                                    0.00, 0.00) // Dummy values for SAFs and logNSAFs
                            proteins.add(protein)
                            intertotalspc += linevalue[4].toInt()
                            intertotalmolw += linevalue[7].toDouble()
                        }
                        if (eng == "MM") {
                            var breaker = 0
                            linevalue.forEach {
                                if (it.contains("NaN"))
                                        {
                                            breaker = 1
                                        }
                            }
                            if (breaker != 1) {
                                if (linevalue[4].contains('|')) {
                                    val specialweight = linevalue[4].split('|').map { it.toDouble() }.average()
                                    val protein = Protein(linevalue[0], // ID
                                            linevalue[27].toDouble(), // Score
                                            specialweight, // MolW
                                            linevalue[8].toInt(), // SpC
                                            0.00, 0.00) // Dummy values for SAFs and logNSAFs
                                    proteins.add(protein)
                                    intertotalspc += linevalue[8].toInt()
                                    intertotalmolw += specialweight
                                } else {
                                    val protein = Protein(linevalue[0], // ID
                                            linevalue[27].toDouble(), // Score
                                            linevalue[4].toDouble(), // MolW
                                            linevalue[8].toInt(), // SpC
                                            0.00, 0.00) // Dummy values for SAFs and logNSAFs
                                    proteins.add(protein)
                                    intertotalspc += linevalue[8].toInt()
                                    intertotalmolw += linevalue[4].toDouble()
                                }
                            }
                        }
                    }
                    line = fileReader.readLine() // Pushes the reader down one line
                }

                for (protein in proteins) {
                    protein.SAF = ( (protein.spc / (protein.molw*1000/110)) / (intertotalspc / (intertotalmolw*1000/110)) )
                    intersumSAF += protein.SAF
                } // Calculates the SAF for every protein and also produces a counter to help with normalisation

                for (protein in proteins) {
                    protein.logNSAF = ln(protein.SAF / intersumSAF)
                } // Re-calculates the lnNSAF for every protein

                state.add(proteins) // Adds the total list of proteins to the state
                totalmolw.add(intertotalmolw) // Just in case we need to use the total molw later
                totalspc.add(intertotalspc)// Just in case we need to use the total spc later

            } catch (e: Exception) {
                println("CSV read error")
                e.printStackTrace()
            }
        }
    }
    catch(e: Exception) {
        println("No Files in Folder, you goose")
        e.printStackTrace()
    }

    return state
}

fun intersection(data: List<HashSet<Protein>>) =
        data.reduce { acc, it -> acc.apply { retainAll(it) } } // These intersection functions produce a list of Unique IDs from all 6 replicates
fun intersection(data: List<HashSet<Protein>>, combination: List<Int>) =
        intersection(combination.map { data[it - 1] })

fun spcSum(uniquedata: HashSet<Protein>, inputdata: ArrayList<HashSet<Protein>>, permutations: List<Int>): HashMap<UniqueProtein, UniqueProtein> {

    val permsize = permutations.size

    val datastore = ArrayList<HashMap<Protein, Protein>>()
    for (i in permutations) {
        val data = HashMap<Protein, Protein>()
        for (q in inputdata[i-1]) {
            data.put(q, q)
        }
        datastore.add(data)
    }

    val newuniqueprotein = HashMap<UniqueProtein, UniqueProtein>()
    for (p in uniquedata) {
        var sumspc = 0
        val a = UniqueProtein(p.id, permsize, DoubleArray(permsize), DoubleArray(permsize), DoubleArray(permsize)) // Initialises Values
        for ((index, entry) in datastore.withIndex()) {
            a.r[index] = entry.getValue(p).spc.toDouble()
            a.SAF[index] = entry.getValue(p).SAF
            a.logNSAF[index] = entry.getValue(p).logNSAF
            sumspc += entry.getValue(p).spc}
        a.cumulativespc = sumspc
        newuniqueprotein.put(a,a)
    }
    return newuniqueprotein
}

fun dofullsixttest(control: HashMap<UniqueProtein, UniqueProtein>, treatment: HashMap<UniqueProtein, UniqueProtein>, output: String) {

    val ttestarray = dottest(control, treatment)

    File(output + "/6by6/result.csv").printWriter().use { out ->
            ttestarray.forEach {
                out.println("${it.id}, ${it.ttestlognsafval}, ${it.ttestspcval}")
            }
        }
}

fun dottest(control: HashMap<UniqueProtein, UniqueProtein>, treatment: HashMap<UniqueProtein, UniqueProtein>): ArrayList<TTestResult> {

    val combineddata2: Set<UniqueProtein> = control.keys.intersect(treatment.keys)
    val doubledata = ArrayList<TTestProtein>()
    for (protein in combineddata2) {
        doubledata.add(TTestProtein(protein.id,
                control.get(protein)!!.r,
                treatment.get(protein)!!.r,
                control.get(protein)!!.logNSAF,
                treatment.get(protein)!!.logNSAF
        ))
    }

    val ttestarray = ArrayList<TTestResult>()
    for (protein in doubledata) {
        ttestarray.add(TTestResult(protein.id, (TestUtils.pairedTTest(protein.state1spc, protein.state2spc)),
                (TestUtils.pairedTTest(protein.state1lognsaf, protein.state2lognsaf)),
                protein.state1spc,
                protein.state2spc,
                protein.state1lognsaf,
                protein.state2lognsaf
        ))
    }
    return ttestarray
}

fun printOutput(ttdata: ArrayList<ArrayList<TTestResult>>, fdr: Double) {

    println("Finding the Kerberus IDs")

    val kerberusSet = HashMap<String, Double>()

    for (combo in ttdata) {
        for (test in combo) {
            if (test.ttestlognsafval < (fdr/100)) {
                if (kerberusSet.contains(test.id) == false) {
                    kerberusSet.put(test.id, 1.00)
                }
                else {
                    kerberusSet.replace(test.id, kerberusSet.getValue(test.id) + 1.00)
                }
            }
        }
    }

    val outputList = ArrayList<String>()

    for (id in kerberusSet.keys) {
        if (kerberusSet.getValue(id) > ((fdr)/100) * 400)
            outputList.add(id)
    }
    println(outputList)
    println(outputList.size)
}

fun makeOutput(ttdata: ArrayList<ArrayList<TTestResult>>, perms: ArrayList<List<Int>>, output: String) {

    println("Making the 400 .csv files")

    IntStream.range(0, ttdata.size).forEach{
        File(output + "/400/$it.csv").printWriter().use { out ->
            ttdata[it].forEach {
                out.println("${it.id}, ${it.ttestlognsafval}, ${it.ttestspcval}")
            }
        }
    }

    val fourhundredlist = ArrayList<List<Int>>()
    for (x in 1..perms.size) {
        for (y in 1..perms.size) {
            fourhundredlist.add(ListUtils.union(perms[x-1], perms[y-1].map {it + 6}))
        }

    }

    File(output + "/permcategories/permnumbers.csv").printWriter().use { out ->
        for(x in fourhundredlist) {
            out.println(x.toString().replace('[', ' ').replace(']', ' '))
        }
    }

    val permMap = mapOf("1" to "One", "2" to "Two", "3" to "Three", "4" to "Four", "5" to "Five",
            "6" to "Six", "7" to "Seven", "8" to "Eight", "9" to "Nine", "10" to "Ten", "11" to "Eleven",
            "12" to "Twelve", "[" to "", "]" to "")

    File(output + "/permcategories/permcategories.csv").printWriter().use { out ->
        for(x in fourhundredlist) {
            var line = x.map { it.toString() }
            var newline = StringBuilder()
            line.forEach { newline.append(permMap.get(it)).append(",") }
            out.println(newline)
        }
    }

    File(output + "/permcategories/permtotal.csv").printWriter().use { out ->
        for(x in fourhundredlist) {
            var line = x.map { it.toString() }
            var newline = StringBuilder()
            var newlist:MutableList<String> = ArrayList()
            IntStream.range(1, 13).forEach {
                if(line.contains(it.toString())) {
                    newlist.add("1")
                }
                else {
                    newlist.add("0")
                }
            }
            IntStream.range(0, 12).forEach {newline.append(newlist[it]).append(",")}
            out.println(newline)
        }
    }
}

fun runMachineBase(args: List<String>, algorithms: String) {
    val projectpath = System.getProperty("user.dir")

    val projectpythoncall = Runtime.getRuntime().exec("cmd /c cd $projectpath & poetry env info -p")
    projectpythoncall.waitFor()
    val projectpython = IOUtils.toString(projectpythoncall.inputStream, Charsets.UTF_8).trim()

    val carrier = "cd $projectpath & $projectpython\\Scripts\\python.exe $projectpath\\src\\python\\machinebase.py " + (args + algorithms).toString().replace("[", "").replace("]", "")
    println("Passing control to python")
    println(carrier)

    val pythonpass = Runtime.getRuntime().exec("cmd /C $carrier")
    pythonpass.waitFor()

    //ProcessBuilder("cmd", "/C", "start", carrier)
    //       .redirectOutput(ProcessBuilder.Redirect.INHERIT)
    //        .start()
    //        .waitFor()
}

fun main(args: Array<Any>){

    //Setting up the timer, permutations and control code
    val permutations = permutationmaker.perm()
    val perftime = starttimer()
    val kerberusFDR = .5
    //val engine = engineLoop()
    val engine = args[3].toString()
    val machineargs = args[4].toString().replace("]", "").replace("[", "")

    //Setting up csv inputs
    val controlpath = args[0].toString() + '\\'
    val treatmentpath = args[1].toString() + '\\'
    val outputpath = args[2].toString() + '\\'
    val directorylist = listOf(outputpath + "6by6\\", outputpath + "400\\",
            outputpath + "permcategories\\", outputpath + "Results\\")
    directorylist.forEach { File(it).mkdirs() }
    val controlIDList = pullCSV(controlpath, engine)
    val treatmentIDList = pullCSV(treatmentpath, engine)
    val basicList = listOf(1,2,3,4,5,6)

    //Performing the 6 by 6 full TTest
    dofullsixttest(spcSum(intersection(controlIDList, basicList), controlIDList, basicList),
            spcSum(intersection(treatmentIDList, basicList), treatmentIDList, basicList), outputpath)

    //Performing the 6 by 6 plex TTest array
    val ttestgroups = ArrayList<ArrayList<TTestResult>>()
    for (i in 1..permutations.size) {
        val reproducibleControl: HashMap<UniqueProtein, UniqueProtein> = spcSum(intersection(controlIDList, permutations[i-1]), controlIDList, permutations[i-1])
        for (q in 1..permutations.size) {
            val reproducibleTreatment: HashMap<UniqueProtein, UniqueProtein> = spcSum(intersection(treatmentIDList, permutations[q-1]), treatmentIDList, permutations[q-1])
            ttestgroups.add(dottest(reproducibleControl, reproducibleTreatment))
        }
    }

    //Printing the output
    printOutput(ttestgroups, kerberusFDR)
    makeOutput(ttestgroups, permutations, outputpath)
    endtimer(perftime)

    // Passing command over to python
    runMachineBase(directorylist, machineargs)
}