import javafx.beans.property.SimpleStringProperty
import java.io.File

class MyFiles {

    private var filePath = SimpleStringProperty(this, "filepath", "")

    public fun filePathProperty(): SimpleStringProperty {
        return filePath
    }

    public fun getFilePathName(): String {
        return filePath.get()
    }

    public fun setFilePathName(filePath: String) {
        this.filePath.set(filePath)
    }
}