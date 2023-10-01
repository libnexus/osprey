import OspreyClass.Companion.TypeErrorOspreyClass

private fun printClassArray(array: Array<OspreyClass>) {
    val string = StringBuilder()
    string.append("(")
    for (clazz: OspreyClass in array) {
        string.append("%s, ".format(clazz.name))
    }
    string.append(")")
    println(string.toString())
}

private fun printClassArrayNullable(array: Array<OspreyClass?>) {
    val string = StringBuilder()
    string.append("(")
    for (clazz: OspreyClass? in array) {
        if (clazz == null) {
            string.append("???, ")
        } else {
            string.append("%s, ".format(clazz.name))
        }
    }
    string.append(")")
    println(string.toString())
}

private class D(var bases: Array<OspreyClass>) {
    fun head(): OspreyClass? =
            if (this.bases.isNotEmpty()) {
                this.bases[0]
            } else {
                null
            }

    fun tail(): Array<OspreyClass> =
            if (this.bases.size > 1) {
                this.bases.copyOfRange(1, this.bases.size - 1)
            } else {
                arrayOf()
            }
}

private class L(lists: Array<Array<OspreyClass>>) {
    private val lists: Array<D>

    init {
        this.lists = lists.map { D(it) }.toTypedArray()
    }

    fun heads(): Array<OspreyClass?> =
            this.lists.map { it.head() }.toTypedArray()


    fun inTails(head: OspreyClass): Boolean =
            this.lists.any { it.tail().contains(head) }


    fun remove(head: OspreyClass) {
        for (d: D in this.lists) {
            if (d.bases.contains(head)) {
                d.bases = d.bases.filter { it != head }.toTypedArray()
            }
        }
    }

    fun finished(): Boolean =
            !this.lists.any { it.bases.isNotEmpty() }

}

private fun merge(lists: Array<Array<OspreyClass>>): Array<OspreyClass>? {
    val result: ArrayList<OspreyClass> = arrayListOf()
    val mroLists = L(lists)
    while (true) {
        if (mroLists.finished()) {
            return result.toTypedArray()
        }
        for (head: OspreyClass? in mroLists.heads()) {
            if ((head != null) && !mroLists.inTails(head)) {
                result.add(head)
                mroLists.remove(head)
                break
            } else {
                return null
            }
        }
    }
}

private fun determineMRO(clazz: OspreyClass): Array<OspreyClass> =
        if (clazz.bases.isNotEmpty()) {
            val mergeResult = merge(clazz.bases.map { it.methodResolutionOrder }.toTypedArray().plus(clazz.bases))
            if (mergeResult == null) {
                throw OspreyThrowable(TypeErrorOspreyClass, "Could not create consistent MRO for class '${clazz.name}'")
            } else {
                arrayOf(clazz).plus(mergeResult)
            }
        } else {
            arrayOf(clazz)
        }

/**
 * Class
 *
 * @property name the name of the class
 * @property bases an array of the bases for the class
 * @property final the finality of the class ie if it can be subclassed
 * @property immutable the mutability of the class ie if it's attributes can be changed
 * @property cooperative the cooperativeness of the class, ie if it can work with non-cooperative classes. It should be noted that
 * if a class X subclasses a non-cooperative Y class then X should be cooperative as it is working with the non-cooperative class Y. If both
 * X and Y are non-cooperative then the safe bases checker assumes that they can't work together. Finality, mutability and cooperativeness are
 * not properties that are inherited. Inheritance is just ordered cooperation.
 * @property typeOspreyObject the type object that represents the class
 * @see TypeOspreyObject
 * @property methodResolutionOrder the class's bases and sub-bases... laid out in a satisfactory order for resolving method lookups
 * resolved using C3 linearization
 *
 * @constructor if safe bases is true, checks that no 2 bases in the MRO are uncooperative and that no base is final
 *
 * @param safeBases if the bases should be checked for finality and cooperativeness conflicts
 */
class OspreyClass(
    val name: String,
    val bases: Array<OspreyClass>,
    val final: Boolean = false,
    val immutable: Boolean = false,
    val cooperative: Boolean = true,
    safeBases: Boolean = true
) {

    val typeOspreyObject: OspreyObject by lazy { TypeOspreyObject(TypeOspreyClass, this) }
    val methodResolutionOrder: Array<OspreyClass> = determineMRO(this)

    init {
        if (safeBases) {
            var uncooperativeBase: OspreyClass? = null
            if (!this.cooperative) {
                uncooperativeBase = this
            }

            for (base: OspreyClass in methodResolutionOrder) {
                if (!base.cooperative) {
                    if (uncooperativeBase != null) {
                        setThrowable(
                                TypeErrorOspreyClass,
                                "Couldn't create class '%s' with uncooperative bases, '%s' and '%s'".format(
                                        this.name, uncooperativeBase.name, base.name
                                )
                        )
                    } else {
                        uncooperativeBase = base
                    }
                } else if (base.final) {
                    setThrowable(
                            TypeErrorOspreyClass,
                            "Couldn't create class '%s' by subclassing final class '%s'".format(
                                    this.name, base.name
                            )
                    )
                }
            }
        }
    }

    /**
     * Companion
     *
     * @property ObjectOspreyClass the base object class
     * @property objectBase a single array of the object class since it's the most common base for kotlamingo
     * @property TypeOspreyClass the base type class for type objects
     *
     * @property ThrowableOspreyClass the base throwable class for all throwables, main subclasses are Fatal, Error and Exception
     * @property throwableBase a single array of the throwable class since it's used thrice for Fatal, Error and Exception
     *
     * @property FatalOspreyClass the fatal throwable class for throwables of a fatal severity, e.g. system exit or raw usage
     *
     * @property ErrorOspreyClass the error throwable class for throwables of a severity that shouldn't attempt recovery
     * @property errorBase a single array of the error class used for all errors
     * @property TypeErrorOspreyClass the type error class for errors which revolve around the wrong type of object literally or semantically
     * @property SyntaxErrorOspreyClass the syntax error class for errors which revolve around syntactic issues e.g. unexpected lexemes
     * @property NameErrorOspreyClass the name error class for errors which revolve around the wrong name for or of something being used
     *
     * @property ExceptionOspreyClass the exception throwable class for throwables of a severity that recovery can be attempted, e.g. division by zero
     *
     * @property StringOspreyClass the base string class for string objects for wrapping kotlin strings
     * @property NumberClass the base number class for number objects for wrapping kotlin numbers, unified for integers and doubles
     * @property BooleanOspreyClass the base boolean class for representing true false values using the 1 and 0 number
     * @property NothingOspreyClass the final Nothing class for single use of the nothing object representing a nothing value
     *
     * @property NOTHING the single nothing object, only instance of the Nothing class
     * @property TRUE the single true object, only instance of the boolean class representing true values
     * @property FALSE the single false object, only instance of the boolean class representing false values
     */
    companion object {
        val ObjectOspreyClass: OspreyClass = OspreyClass("Object", arrayOf(), immutable = true, safeBases = false)
        val objectBase: Array<OspreyClass> = arrayOf(ObjectOspreyClass)

        val TypeOspreyClass = OspreyClass("Type", objectBase, immutable = true, cooperative = false, safeBases = false)

        val ThrowableOspreyClass = OspreyClass("Throwable", objectBase, immutable = true, cooperative = false, safeBases = false)
        val throwableBase = arrayOf(ThrowableOspreyClass)

        val FatalOspreyClass = OspreyClass("Fatal", throwableBase, immutable = true, safeBases = false)

        val ErrorOspreyClass = OspreyClass("Error", throwableBase, immutable = true, safeBases = false)
        val errorBase = arrayOf(ErrorOspreyClass)
        val TypeErrorOspreyClass = OspreyClass("TypeError", errorBase, immutable = true, safeBases = false)
        val SyntaxErrorOspreyClass = OspreyClass("SyntaxError", errorBase, immutable = true, safeBases = false)
        val NameErrorOspreyClass = OspreyClass("NameError", errorBase, immutable = true, safeBases = false)

        val ExceptionOspreyClass = OspreyClass("Exception", throwableBase, immutable = true, safeBases = false)

        val StringOspreyClass = OspreyClass("String", objectBase, immutable = true, cooperative = false, safeBases = false)
        val FloatOspreyClass = OspreyClass("Float", objectBase, immutable = true, cooperative = false, safeBases = false)
        val IntegerOspreyClass = OspreyClass("Integer", objectBase, immutable = true, cooperative = false, safeBases = false)
        val BooleanOspreyClass = OspreyClass("Boolean", arrayOf(IntegerOspreyClass), immutable = true, cooperative = false, safeBases = false)
        val NothingOspreyClass = OspreyClass("Nothing", objectBase, immutable = true, safeBases = false, final = true)

        val NOTHING = OspreyObject(NothingOspreyClass)
        val TRUE = IntegerOspreyObject(BooleanOspreyClass, 1)
        val FALSE = IntegerOspreyObject(BooleanOspreyClass, 0)
    }
}