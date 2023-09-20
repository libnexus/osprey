import OspreyClass.Companion.NameErrorOspreyClass
import java.util.*

/**
 * Name table entry
 *
 * @property value
 * @property final
 * @constructor Create empty Name table entry
 */
class NameTableEntry(var value: OspreyObject, val final: Boolean)


/**
 * Name table for mapping string names to name table entries
 *
 * @property name the name of the name table, commonly the name of the frame
 * @property closure the name table that this name table is enclosed in
 */
class NameTable(val name: String, val closure: NameTable?) {
    private val names = HashMap<String, NameTableEntry>()

    /**
     * Get entry
     *
     * @param name the name of the entry to get
     * @return the entry or throw an error
     */
    fun getEntry(name: String): NameTableEntry =
            names[name] ?: throw OspreyThrowable(NameErrorOspreyClass, "Name '%s' is not defined".format(name))

    /**
     * Set entry
     *
     * @param name the name of the entry to overwrite
     * @param entry the entry to overwrite at the name
     */
    fun setEntry(name: String, entry: NameTableEntry) {
        val currentEntry = this.names[name]
        if (currentEntry == null || !currentEntry.final) {
            this.names[name] = entry
        } else {
            throw OspreyThrowable(NameErrorOspreyClass, "Final name '%s' cannot be modified".format(name))
        }
    }

    /**
     * Gets the value from an entry with the given [name]
     *
     * @return the value of the entry
     */
    fun getValue(name: String): OspreyObject =
            this.getEntry(name).value

    /**
     * Sets the value for the given entry with finality modifier if possible
     *
     * @param name the name for the (new) entry
     * @param value the value for the (new) entry
     * @param final the finality for the new entry, old entries can't change finality
     */
    fun setValue(name: String, value: OspreyObject, final: Boolean) {
        val entry = names[name]
        if (entry == null) {
            this.names[name] = NameTableEntry(value, final)
        } else {
            if (entry.final) {
                throw OspreyThrowable(NameErrorOspreyClass, "Final name '%s' cannot be modified".format(name))
            } else if (final) {
                throw OspreyThrowable(NameErrorOspreyClass, "Name '%s' cannot have it's access modified after initial declaration".format(name))
            } else {
                entry.value = value
            }
        }
    }
}


/**
 * Frame
 *
 * @property name the name of the frame
 * @property localNames a name table of local names for the frame
 * @property globalNames the global names to check for if a name load operation isn't in locals
 * @property origin the lexeme which is responsible for the creation of the frame
 * @property closure the frame that the current frame is "enclosed in" (made from)
 */
open class Frame(val name: String, val localNames: NameTable, val globalNames: NameTable, val origin: Lexeme, val closure: Frame?) {
    private val stack = Stack<OspreyObject>()

    /**
     * Adds an object to the frame's object stack
     *
     * @param obj the object to add
     */
    fun addObject(obj: OspreyObject) =
            this.stack.add(obj)

    /**
     * Pops an object from the frame's object stack
     *
     * @return the popped object
     */
    fun popObject(): OspreyObject =
            this.stack.pop()

    /**
     * Gets the object at the top of the stack
     *
     * @return that object
     */
    fun peekObject(): OspreyObject =
            this.stack.peek()
}

/**
 * Code frame
 *
 * @property code the code object for the frame
 * @property pointer the operation for the code that the frame is currently pointing to
 *
 * @param name
 * @param localNames
 * @param globalNames
 * @param origin
 * @param closure
 */
class CodeFrame(name: String, localNames: NameTable, globalNames: NameTable, origin: Lexeme, closure: Frame?, val code: Code) : Frame(name, localNames, globalNames, origin, closure) {
    val pointer = 0
}

private val stack = Stack<Frame>()

/**
 * Creates an array of frames on the stack
 *
 * @return the array
 */
fun stackCopy(): Array<Frame> {
    val stackCopy = arrayOf<Frame>()
    stack.copyInto(arrayOf(stackCopy))
    return stackCopy
}

fun exec() {
}