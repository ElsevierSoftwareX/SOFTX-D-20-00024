import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.ToggleGroup
import javafx.stage.Stage
import tornadofx.*
import java.io.File

class MyView : View() {

    private lateinit var controlfiles: File
    private lateinit var treatmentfiles: File
    var controls = MyFiles()
    var treatment = MyFiles()

    private val toggleGroup = ToggleGroup()

    override val root = borderpane {

        top = hbox (100, Pos.CENTER) {
            label("PeptideKerberus") {
                style{
                fontSize = 20.px
                }
                hboxConstraints {
                    margin = Insets(25.0) // This contrains --> margin = adds padding around elements
                }
            }
        }

        left = vbox () {
            setPrefSize(20.0, 500.0)
        }

        center = vbox(20) {

                vbox(20, Pos.CENTER_LEFT) {
                    label {
                        style {
                            fontSize = 18.px
                        }
                        text = "Select Your Protein ID Files"
                    }
                }

                    hbox(20, Pos.CENTER_LEFT) {
                        button {
                            action {
                                controlfiles = chooseDirectory("Select Target Directory")!!
                                controls.setFilePathName(controlfiles.path)
                            }
                            label("Select Control Folder", center)
                        }
                    }

                    hbox(20, Pos.CENTER_LEFT) {
                        imageview("/folder25px.png")
                        textfield().bind(controls.filePathProperty())
                    }

                    hbox(20, Pos.CENTER_LEFT) {
                        button {
                            action {
                                treatmentfiles = chooseDirectory("Select Target Directory")!!
                                treatment.setFilePathName(treatmentfiles.path)
                            }
                            label("Select Treatment Folder", center)
                        }
                    }

                    hbox(20, Pos.CENTER_LEFT) {
                        imageview("/folder25px.png")
                        textfield().bind(treatment.filePathProperty())
                    }

                    vbox(50, Pos.CENTER_LEFT) {
                        label {
                            style {
                                fontSize = 20.px
                            }
                            text = "Engine"
                        }
                    }


                    vbox(20) {
                        radiobutton("GPM", toggleGroup)
                        radiobutton("Proteome Discoverer", toggleGroup)
                        radiobutton("Meta Morpheus", toggleGroup)
                    }
        }



        right = vbox (20){
                button("Load") {
                    useMaxWidth = true
                    action {
                        println(controls.getFilePathName())
                        println(treatment.getFilePathName())
                    }
                }
            }
        }
}

class Main : App() {
    override val primaryView = MyView::class

    override fun start(stage: Stage) {
        super.start(stage)
        stage.width = 700.0
        stage.height = 500.0
    }
}