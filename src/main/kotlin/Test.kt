import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.concurrent.ScheduledService
import javafx.concurrent.Task
import javafx.geometry.Pos
import javafx.scene.control.SelectionMode
import javafx.scene.control.TextField
import javafx.scene.control.ToggleGroup
import javafx.scene.text.FontPosture
import javafx.stage.Stage
import javafx.util.Duration
import tornadofx.*
import java.io.File


data class DataResult(val data : SimpleStringProperty)

class PollController : Controller() {

    val currentData = SimpleStringProperty()
    val stopped = SimpleBooleanProperty(true)

    val scheduledService = object : ScheduledService<DataResult>() {
        init {
            period = Duration.seconds(1.0)
        }
        override fun createTask() : Task<DataResult> = FetchDataTask()
    }

    fun start() {
        scheduledService.restart()
        stopped.value = false
    }

    inner class FetchDataTask : Task<DataResult>() {

        override fun call() : DataResult {
            return DataResult(SimpleStringProperty(File("src/interface/test.txt").readText()))
        }

        override fun succeeded() {
            this@PollController.currentData.value = value.data.value // Here is the value of the test file
        }

    }

}

class MyView : View() {

    val controller: PollController by inject()

    private lateinit var controlfiles: File
    private lateinit var treatmentfiles: File
    private lateinit var outputdirec: File
    var controls = MyFiles()
    var treatments = MyFiles()
    var output = MyFiles()
    private val toggleGroup = ToggleGroup()
    private var proteinIdList = mutableListOf<String>().observable()
    private var pID: TextField by singleAssign()

    override val root = borderpane {

        top = vbox(5, Pos.BOTTOM_CENTER) {
            button("Start") {
                disableProperty().bind( controller.stopped.not() )
                setOnAction {
                    controller.start()
                }
            }
            label().bind(controller.currentData)
            label("PeptideKerberus") {
                    style {
                        fontSize = 24.px
                    }
                }
            label("")
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
                            label("Select Output Folder", center)
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
                    radiobutton("GPM", toggleGroup)
                    radiobutton("Proteome Discoverer", toggleGroup)
                    radiobutton("Meta Morpheus", toggleGroup)
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
                                println(kToggle.value)
                            }
                        }
                        togglebutton {
                            val rToggle = selectedProperty().stringBinding {
                                if (it == true) "ON" else "OFF"
                            }
                            textProperty().bind(rToggle)
                            action {
                                println(rToggle.value)
                            }
                        }
                        togglebutton {
                            val dToggle = selectedProperty().stringBinding {
                                if (it == true) "ON" else "OFF"
                            }
                            textProperty().bind(dToggle)
                            action {
                                println(dToggle.value)
                            }
                        }
                        togglebutton {
                            val eToggle = selectedProperty().stringBinding {
                                if (it == true) "ON" else "OFF"
                            }
                            textProperty().bind(eToggle)
                            action {
                                println(eToggle.value)
                            }
                        }
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
                        label("Individual Forests")
                        label("Mega Isolation Forests")
                    }
                    vbox(20, Pos.CENTER_LEFT) {
                        togglebutton {
                            val ifToggle = selectedProperty().stringBinding {
                                if (it == true) "ON" else "OFF"
                            }
                            textProperty().bind(ifToggle)
                            action {
                                println(ifToggle.value)
                            }
                        }
                        togglebutton {
                            val mfToggle = selectedProperty().stringBinding {
                                if (it == true) "ON" else "OFF"
                            }
                            textProperty().bind(mfToggle)
                            action {
                                println(mfToggle.value)
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
                        pID = textfield()
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

                setMaxSize(250.00, 1000.00)
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
                            label("Select Output Folder", center)
                        }
                        textfield().bind(output.filePathProperty())
                    }
                    label("")
                }

                button("Start") {
                    action {
                        println(controls.getFilePathName())
                        println(treatments.getFilePathName())
                    }
                }
                scrollpane {
                    textarea("Output") {
                        style{
                            fontStyle = FontPosture.ITALIC
                        }
                        isEditable = false
                    }
                }
                progressbar()
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
        super.start(stage)
        stage.width = 1200.0
        stage.height = 600.0
    }
}