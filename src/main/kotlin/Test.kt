import javafx.geometry.Pos
import javafx.scene.control.ToggleGroup
import javafx.stage.Stage
import tornadofx.*
import java.io.File

class MyView : View() {

    private lateinit var controlfiles: File
    private lateinit var treatmentfiles: File
    var controls = MyFiles()
    var treatments = MyFiles()
    private val toggleGroup = ToggleGroup()

    fun switchBox(box: Int): Int {
        if (box == 0) {return 1}
        else {return 0}
    }

    override val root = borderpane {

        top = vbox(20, Pos.CENTER) {
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

                label("Select Your Protein ID Files") {
                    style {
                        fontSize = 18.px
                    }
                }

                label("")

                vbox(20, Pos.CENTER_LEFT) {
                    button {
                        action {
                            try {
                                controlfiles = chooseDirectory("Select Target Directory")!!
                                controls.setFilePathName(controlfiles.path)
                            } catch(e: KotlinNullPointerException) {
                                controls.setFilePathName("No Folder Selected!")
                            }
                        }
                        label("Select Control Folder", center)
                    }
                    hbox(20, Pos.CENTER) {
                        imageview("/folder25px.png")
                        textfield().bind(controls.filePathProperty())
                    }
                    label("")
                }

                vbox(20, Pos.CENTER_LEFT) {
                    button {
                        action {
                            try {
                                treatmentfiles = chooseDirectory("Select Target Directory")!!
                                treatments.setFilePathName(treatmentfiles.path)
                            } catch(e: KotlinNullPointerException) {
                                controls.setFilePathName("No Folder Selected!")
                            }
                        }
                        label("Select Treatment Folder", center)
                    }
                    hbox(20, Pos.CENTER) {
                        imageview("/folder25px.png")
                        textfield().bind(treatments.filePathProperty())
                    }
                    label("")
                }

                vbox(20, Pos.CENTER_LEFT) {
                    label {
                        style {
                            fontSize = 20.px
                        }
                        text = "Engine"
                    }
                    radiobutton("GPM", toggleGroup)
                    radiobutton("Proteome Discoverer", toggleGroup)
                    radiobutton("Meta Morpheus", toggleGroup)
                }
            }

            //2nd Colummn
            vbox(20, Pos.TOP_CENTER) {
                label("Machine Learning Classifiers") {
                    style {
                        fontSize = 24.px
                    }
                }
                togglebutton {
                    text = "K Neighbours"
                    val kToggle = selectedProperty().stringBinding {
                        if (it == true) "ON" else "OFF"
                    }
                    textProperty().bind(kToggle)
                    action {
                        println(kToggle)
                    }
                }
                togglebutton("Extra Trees")
                togglebutton("Random Forest")
                togglebutton("Decision Tree")
            }
        }

        right = vbox(20) {
            button("Load") {
                useMaxWidth = true
                action {
                    println(controls.getFilePathName())
                    println(treatments.getFilePathName())
                }
            }
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
        stage.width = 700.0
        stage.height = 550.0
    }
}