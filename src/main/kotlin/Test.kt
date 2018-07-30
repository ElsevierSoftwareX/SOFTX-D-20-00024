import javafx.stage.Stage
import tornadofx.*


class ApplicationView: View() {
    override val root = vbox {
        button("Press me")
        label("Waiting")
    }
}

fun main(args: Array<String>) {
    launch<Application>()
}