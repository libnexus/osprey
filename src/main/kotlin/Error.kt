import java.util.*

/**
 * The OspreyThrowable class for handling throwing errors natively from kotlin ie in builtin functions
 * and classes
 *
 * @property throwableClass the class of throwable that is being thrown (for error handling)
 * @property OspreyMessage the error message to accompany the throwable
 * @property throwableObject the throwable object generated to incorporate the state of the stack of the VM
 */
class OspreyThrowable(throwableOspreyClass: OspreyClass, OspreyMessage: String) : Throwable() {
    val throwableObject: ThrowableOspreyObject = ThrowableOspreyObject(throwableOspreyClass, OspreyMessage, stackCopy())

    init {
        setThrowable(throwableOspreyClass, OspreyMessage)
    }

    /**
     * Helper method to check if the thrown error happened whilst a normal kotlamingo program is executing
     */
    fun isOnStack(): Boolean = this.throwableObject.traceback.isNotEmpty()
}

/**
 * The error stack which holds all the errors that the VM or native error handling needs to handle
 */
val errorStack: Stack<ThrowableOspreyObject> = Stack()

/**
 * Utility method to print all errors that exist on the stack by looping through the throwable objects on the stack with
 * their traceback context and then looping through the frames in that traceback and printing out their info
 */
fun printErrorStack() {
    for (throwable: ThrowableOspreyObject in errorStack) {
        System.err.println("Traceback (most recent call last):")
        for (frame: Frame in throwable.traceback) {
            if (frame is CodeFrame) {
                System.err.println(""""    File "%s", line %s, %s in %s """.format(
                        frame.origin.lexer.fileName, frame.origin.lineNo, frame.origin.linePos, frame.name))
            } else {
                System.err.println(""""    File "%s", line %s, %s in %s:\n    %s\n    %s""".format(
                        frame.origin.lexer.fileName, frame.origin.lineNo, frame.origin.linePos, frame.name,
                        frame.origin.lexer.lineTextOfLine(frame.origin.lineNo), " ".repeat(frame.origin.linePos) + "^"))
            }
        }
        System.err.println("%s : %s".format(throwable.type.name, throwable.message))
    }
}

/**
 * Creates a throwable object and pushes it on the error stack with the [throwableOspreyClass] and [message]
 */
fun setThrowable(throwableOspreyClass: OspreyClass, message: String) {
    if (!throwableOspreyClass.methodResolutionOrder.contains(OspreyClass.ThrowableOspreyClass)) {
        throw OspreyThrowable(OspreyClass.TypeErrorOspreyClass, "The thrown '%s' must subclass the base 'Throwable' class".format(throwableOspreyClass.name))
    } else {
        errorStack.push(ThrowableOspreyObject(throwableOspreyClass, message, stackCopy()))
    }
}
