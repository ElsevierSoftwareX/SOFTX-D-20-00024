import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.concurrent.ScheduledService
import javafx.concurrent.Task
import javafx.geometry.Pos
import javafx.scene.control.SelectionMode
import javafx.scene.control.TextField
import javafx.scene.control.ToggleGroup
import javafx.scene.input.Clipboard
import javafx.scene.input.KeyCode
import javafx.scene.text.FontPosture
import javafx.stage.Stage
import javafx.util.Duration
import tornadofx.*
import java.io.File
import base.*
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
//import javafx.beans.value.ObservableBooleanValue
//import javafx.beans.value.ObservableStringValue
//import javafx.beans.value.ObservableValue
//import javafx.scene.image.Image
import kotlin.concurrent.thread

data class DataResult(val data : SimpleDoubleProperty)

class PollContext(path: String) : ViewModel() {
    val pathProperty = SimpleStringProperty(path)
    var path by pathProperty

    val currentDataProperty = SimpleDoubleProperty()
    var currentData by currentDataProperty
}

class PollController : Controller() {

    val context : PollContext by inject()

    val stopped = SimpleBooleanProperty(true)

    val scheduledService = object : ScheduledService<DataResult>() {
        init {
            period = Duration.seconds(0.01)
        }
        override fun createTask() : Task<DataResult> = FetchDataTask()
    }

    fun start() {
        scheduledService.restart()
        stopped.value = false
    }

    inner class FetchDataTask : Task<DataResult>() {

        override fun call() : DataResult {
            return DataResult(SimpleDoubleProperty(File(context.path).readText().toDouble()))
        }

        override fun succeeded() {
            this@PollController.context.currentData = value.data.value // Here is the value of the test file
        }
    }
}

data class StringResult(val data: SimpleStringProperty)

class PollContextS(path: String) : ViewModel() {
    val pathProperty = SimpleStringProperty(path)
    var path by pathProperty

    val currentDataProperty = SimpleStringProperty()
    var currentData by currentDataProperty
}

class PollControllerS : Controller() {

    val context : PollContextS by inject()

    val stopped = SimpleBooleanProperty(true)

    val scheduledService = object : ScheduledService<StringResult>() {
        init {
            period = Duration.seconds(0.01)
        }
        override fun createTask() : Task<StringResult> = FetchDataTask()
    }

    fun start() {
        scheduledService.restart()
        stopped.value = false
    }

    inner class FetchDataTask : Task<StringResult>() {

        override fun call() : StringResult {
            return StringResult(SimpleStringProperty(File(context.path).readText().toString()))
        }

        override fun succeeded() {
            this@PollControllerS.context.currentData = value.data.value // Here is the value of the test file
        }
    }
}

class MyView : View() {

    private val idcontroller: PollController by inject(Scope(PollContext("src/interface/proteinIDs.txt")))
    private val decisioncontroller: PollController by inject(Scope(PollContext("src/interface/Decision.txt")))
    private val extracontroller: PollController by inject(Scope(PollContext("src/interface/Extra.txt")))
    private val kcontroller: PollController by inject(Scope(PollContext("src/interface/K.txt")))
    private val randomcontroller: PollController by inject(Scope(PollContext("src/interface/Random.txt")))
    private val outputcontroller: PollControllerS by inject(Scope(PollContextS("src/interface/Update.txt")))

    private lateinit var controlfiles: File
    private lateinit var treatmentfiles: File
    private lateinit var outputdirec: File
    var controls = MyFiles()
    var treatments = MyFiles()
    var output = MyFiles()
    private val toggleGroup = ToggleGroup()
    var toggleoutput = SimpleStringProperty()
    var selectoutput = mutableListOf(1, 1, 1, 1, 0, 1, 1)
    private var proteinIdList = mutableListOf<String>().observable()
    private var pID: TextField by singleAssign()
    private var visiblebar = SimpleObjectProperty<Boolean>(false)
    private var startbuttondisable = SimpleObjectProperty<Boolean>(false)

    fun startControllers(types: List<PollController>) {
        types.forEach {
            it.stopped.not()
            it.start()
        }
    }

    override val root = borderpane {

        startControllers(listOf(idcontroller, decisioncontroller, extracontroller, kcontroller, randomcontroller))
        outputcontroller.stopped.not()
        outputcontroller.start()

        top = hbox(0, Pos.BOTTOM_CENTER) {
            label("")
            hbox(20, Pos.CENTER) {
                label("PeptideMind") {
                    style {
                        fontSize = 24.px
                    }
                }
                //imageview(Image("icon2.png")) {
                //    Pos.TOP_CENTER
                //    fitWidth = 50.0
                //    fitHeight = 50.0
                //}
            }
        }

        left = vbox {
            setPrefSize(20.0, 500.0)
        }

        center = hbox(20) {

            //1st Column
            vbox {
                style{
                    borderColor += box(c("#a1a1a1"))
                    paddingAll = 20
                }

                label("Select Your Protein ID Files") {
                    style {
                        fontSize = 16.px
                    }
                }

                label("")

                hbox(20, Pos.CENTER) {

                    vbox(20, Pos.CENTER) {
                        label("")
                        imageview("/folder25px.png")
                    }

                    vbox(20, Pos.CENTER) {

                        button {
                            action {
                                try {
                                    controlfiles = chooseDirectory("Select Target Directory")!!
                                    controls.setFilePathName(controlfiles.path)
                                } catch (e: KotlinNullPointerException) {
                                    controls.setFilePathName("No Folder Selected!")
                                }
                            }
                            label("Select Control Folder", center)
                        }
                        textfield().bind(controls.filePathProperty())
                    }
                }

                label("")

                hbox(20, Pos.CENTER) {

                    vbox(20, Pos.CENTER) {
                        label("")
                        imageview("/folder25px.png")
                    }

                    vbox(20, Pos.CENTER) {

                        button {
                            action {
                                try {
                                    treatmentfiles = chooseDirectory("Select Target Directory")!!
                                    treatments.setFilePathName(treatmentfiles.path)
                                } catch (e: KotlinNullPointerException) {
                                    treatments.setFilePathName("No Folder Selected!")
                                }
                            }
                            label("Select Treatment Folder", center)
                        }
                        textfield().bind(treatments.filePathProperty())
                    }
                }

                label("")
                label("")

                vbox(20, Pos.CENTER) {
                    label {
                        style {
                            fontSize = 18.px
                        }
                        text = "Engine"
                    }
                }
                vbox(20, Pos.CENTER_LEFT) {
                    label("")
                    radiobutton("GPM", toggleGroup) {
                        action {
                            if (isSelected) {
                                toggleoutput.value = "GPM"
                            }
                        }
                    }
                        radiobutton("Proteome Discoverer", toggleGroup) {
                            action {
                                if (isSelected) {
                                    toggleoutput.value = "PD"
                                }
                            }
                        }
                        radiobutton("Meta Morpheus", toggleGroup, value="MM") {
                            action {
                                if (isSelected) {
                                    toggleoutput.value = "MM"
                                }
                            }
                        }
                    }
            }

            //2nd Colummn
            vbox(20, Pos.TOP_CENTER) {

                style {
                    borderColor += box(c("#a1a1a1"))
                    paddingAll = 20
                }

                label("Machine Learning Classifiers") {
                    style {
                        fontSize = 18.px
                    }
                }

                hbox(20, Pos.CENTER) {

                    vbox(30, Pos.CENTER_LEFT) {
                        label("K Neighbours")
                        label("Random Forest")
                        label("Decision Trees")
                        label("Extra Trees")
                    }

                    vbox(20, Pos.CENTER_LEFT) {
                        togglebutton {
                            val kToggle = selectedProperty().stringBinding {
                                if (it == true) "ON" else "OFF"
                            }
                            textProperty().bind(kToggle)
                            action {
                                if (selectoutput[0] == 1) {selectoutput[0] = 0} else {selectoutput[0] = 1}
                            }
                        }
                        togglebutton {
                            val rToggle = selectedProperty().stringBinding {
                                if (it == true) "ON" else "OFF"
                            }
                            textProperty().bind(rToggle)
                            action {
                                if (selectoutput[1] == 1) {selectoutput[1] = 0} else {selectoutput[1] = 1}
                            }
                        }
                        togglebutton {
                            val dToggle = selectedProperty().stringBinding {
                                if (it == true) "ON" else "OFF"
                            }
                            textProperty().bind(dToggle)
                            action {
                                if (selectoutput[2] == 1) {selectoutput[2] = 0} else {selectoutput[2] = 1}
                            }
                        }
                        togglebutton {
                            val eToggle = selectedProperty().stringBinding {
                                if (it == true) "ON" else "OFF"
                            }
                            textProperty().bind(eToggle)
                            action {
                                if (selectoutput[3] == 1) {selectoutput[3] = 0} else {selectoutput[3] = 1}
                            }
                        }
                    }

                    vbox(30, Pos.TOP_CENTER) {
                        progressbar().bind(kcontroller.context.currentDataProperty)
                        progressbar().bind(randomcontroller.context.currentDataProperty)
                        progressbar().bind(decisioncontroller.context.currentDataProperty)
                        progressbar().bind(extracontroller.context.currentDataProperty)
                    }
                }

                label("")
                label {
                    text = "Individual Forests"
                    style {
                        fontSize = 18.px
                    }
                }

                hbox(20, Pos.CENTER) {
                    vbox(30, Pos.CENTER_LEFT) {
                        label("Individual Forests (Random)")
                        label("Individual Forests (Isolation)")
                        label("Mega Isolation Forests")
                    }
                    vbox(20, Pos.CENTER_LEFT) {
                        togglebutton {
                            this.isDisable = true
                            val ifRanToggle = selectedProperty().stringBinding {
                                    if (it == true) "OFF" else "OFF"
                            }
                            textProperty().bind(ifRanToggle)
                            action {
                                if (selectoutput[4] == 1) {selectoutput[4] = 0} else {selectoutput[4] = 1}
                            }
                        }
                        togglebutton {
                            val ifIsoToggle = selectedProperty().stringBinding {
                                if (it == true) "ON" else "OFF"
                            }
                            textProperty().bind(ifIsoToggle)
                            action {
                                if (selectoutput[5] == 1) {selectoutput[5] = 0} else {selectoutput[5] = 1}
                            }
                        }
                        togglebutton {
                            val mfToggle = selectedProperty().stringBinding {
                                if (it == true) "ON" else "OFF"
                            }
                            textProperty().bind(mfToggle)
                            action {
                                if (selectoutput[6] == 1) {selectoutput[6] = 0} else {selectoutput[6] = 1}
                            }
                        }
                    }
                }
            }

            // 3rd Column

            vbox(20, Pos.TOP_CENTER) {

                style{
                    borderColor += box(c("#a1a1a1"))
                    paddingAll = 20
                }

                label("Proteins of Interest") {
                    Pos.CENTER
                    style {
                        fontSize = 18.px
                    }
                }
                vbox(20, Pos.CENTER_LEFT) {
                    hbox(20, Pos.CENTER_LEFT) {
                        pID = textfield() {
                            setOnKeyReleased { event ->
                                    if (event.getCode() == KeyCode.ENTER) {
                                        if (pID.text != "") {
                                            proteinIdList.add(pID.text)
                                            pID.text = ""
                                        }
                                    } // Allows us to hit enter and input the text into the fieldview
                                }
                            }
                        button("Add") {
                            action {
                                if (pID.text != "") {
                                    proteinIdList.add(pID.text)
                                    pID.text = ""
                                }
                            }
                        }
                        button("Reset") {
                            action {
                                proteinIdList.clear()
                            }
                        }
                    }
                    listview(proteinIdList) {
                        setOnKeyReleased { event ->
                                if (event.getCode() == KeyCode.V && event.isControlDown()) {
                                    val clipboard = Clipboard.getSystemClipboard()
                                    if (clipboard.hasString()) {
                                        clipboard.string.split(System.lineSeparator()).forEach { proteinIdList.add(it) }
                                        proteinIdList.removeAt(proteinIdList.lastIndex)
                                    }
                                } // Allows us to paste clipboard into the box
                            }
                            selectionModel.selectionMode = SelectionMode.MULTIPLE
                    }
                }
            }

            // 4rd Column
            vbox(20, Pos.TOP_CENTER) {

                style{
                    borderColor += box(c("#a1a1a1"))
                    paddingAll = 20
                }

                setMaxSize(275.00, 1000.00)
                label("Controls") {
                    Pos.CENTER
                    style {
                        fontSize = 18.px
                    }
                }

                hbox(20, Pos.CENTER) {

                    vbox(20, Pos.CENTER) {
                        label("")
                        imageview("/folder25px.png")
                    }

                    vbox(20, Pos.CENTER) {

                        button {
                            action {
                                try {
                                    outputdirec = chooseDirectory("Select Target Directory")!!
                                    output.setFilePathName(outputdirec.path)
                                } catch (e: KotlinNullPointerException) {
                                    output.setFilePathName("No Folder Selected!")
                                }
                            }
                            label("Select Output Folder", center) {
                                style {
                                    fontSize = 16.px
                                }
                            }
                        }
                        textfield().bind(output.filePathProperty())
                    }
                    label("")
                }

                button("Start") {
                    disableProperty().bind(startbuttondisable)
                    action {
                        visiblebar.value = true
                        File("src/interface/Lookfor.txt").writeText(proteinIdList.toString())
                        val actions = arrayOf(controls.getFilePathName(),
                                treatments.getFilePathName(),
                                outputdirec.path,
                                toggleoutput.value,
                                selectoutput
                                )
                        actions.forEach {println(it)}
                        thread { main(actions) } // Wow that's so simple! This is how you run a function in parallel so that the GUI doesn't lock
                        startbuttondisable.value = true
                    }
                }
                progressbar {
                    visibleProperty().bind(visiblebar)
                }
                textfield {
                        textProperty().bind(outputcontroller.context.currentDataProperty)
                        setPrefSize(200.0, 400.0)
                        style{
                            fontStyle = FontPosture.ITALIC
                        }
                        isEditable = false
                }
            }
        }

        right = vbox {
            setPrefSize(20.0, 500.0)
        }

        bottom = vbox(0, Pos.CENTER) {
            label("")
        }
    }
}

class Main : App() {
    override val primaryView = MyView::class

    override fun start(stage: Stage) {
        File("src\\interface\\").walkTopDown().forEachIndexed { index, file ->
            if (index != 0) {
                file.writeText("0.01")
            }
        }
        super.start(stage)
        //addStageIcon(Image("icon2.png"))
        stage.width = 1350.0
        stage.height = 610.0
        stage.isResizable = false
        stage.centerOnScreen()
    }
}

fun main(args: Array<String>) {
    launch<Main>(args)
}