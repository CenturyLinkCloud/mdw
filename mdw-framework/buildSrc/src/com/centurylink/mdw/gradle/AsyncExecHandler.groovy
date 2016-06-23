import org.gradle.api.internal.file.FileResolver
import org.gradle.process.ExecResult
import org.gradle.process.internal.ExecHandleBuilder
import org.gradle.process.internal.ExecAction
import org.gradle.process.internal.ExecHandle

class AsyncExecHandler extends ExecHandleBuilder implements ExecAction {
    public AsyncExecHandler(FileResolver fileResolver) {
        super(fileResolver)
    }

    public ExecResult execute() {
        ExecHandle execHandle = build()
        execHandle.start()
        return null
    }
}