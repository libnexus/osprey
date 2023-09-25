import OspreyClass.Companion.FloatOspreyClass
import OspreyClass.Companion.IntegerOspreyClass
import OspreyClass.Companion.StringOspreyClass

/**
 * The base object type for the kotlamingo OOP architecture which at it's base requires only a class as it's object type
 *
 * @property type the type of object the object is
 */
open class OspreyObject(val type: OspreyClass) {
    private val uniqueID: Int

    /**
     * Creates a unique instances ID for the object which will obviously persist past the lifetime of the object
     */
    init {
        instances++
        uniqueID = instances
    }

    /**
     * Gets the unique ID number of the object
     * @return the UID of the object
     */
    fun getUID(): Int = this.uniqueID

    /**
     * Utility method to check if the given [clazz] is in the MRO of the object which would qualify it as an instance of
     * that class
     */
    fun isInstanceOf(clazz: OspreyClass): Boolean {
        return this.type.methodResolutionOrder.contains(clazz)
    }

    fun getAttribute(name: String): OspreyObject? {
        return null
    }

    fun setAttribute(name: String, value: OspreyObject): OspreyObject? {
        return null
    }

    /**
     * Debugging toString method for giving a rudimentary string representation of an object
     */
    override fun toString(): String = "Object(class=%s)".format(this.type.name)

    companion object {
        private var instances = 0
    }
}

/**
 * Creates a simple caching util for a kotlin class [KTT] and a kotlamingo object type [OT] which stores inputs to the
 * [cache] method in a private object cache so that when the same value is input it can be returned based on the given [newObject]
 * transformer
 */
open class ObjectCache<KTT, OT>(val newObject: (cacheable: KTT) -> OT) {
    private val objectCache = HashMap<KTT, OT>()

    /**
     * Cache method of the ObjectCache instance
     *
     * @param cacheable the object to cache or generate an object for
     * @return the cached or newly cached object
     */
    fun cache(cacheable: KTT): OT {
        val cachedObject = objectCache[cacheable] ?: newObject(cacheable)

        if (!objectCache.containsKey(cacheable)) {
            objectCache[cacheable] = cachedObject
        }

        return cachedObject
    }
}

/**
 * Type object for representing classes as kotlamingo objects
 *
 * @property wrappedOspreyClass the class that's being represented
 * @param type
 */
class TypeOspreyObject(type: OspreyClass, val wrappedOspreyClass: OspreyClass) : OspreyObject(type)

/**
 * Throwable object for representing throwables as kotlamingo objects
 *
 * @property message the message sent with the throwable
 * @property traceback a snapshot copy of the stack as an array of frames
 *
 * @param type
 */
class ThrowableOspreyObject(type: OspreyClass, val message: String, val traceback: Array<Frame>) : OspreyObject(type)

/**
 * String object for representing kotlin strings as kotlamingo objects
 *
 * @property string the string being wrapped
 *
 * @param type
 */
class StringOspreyObject(type: OspreyClass, val string: String) : OspreyObject(type) {
    /**
     * Companion
     *
     * @constructor creates the cache for string objects
     */
    companion object {
        val cache = ObjectCache { string: String -> StringOspreyObject(StringOspreyClass, string) }
    }
}

/**
 * Float object
 *
 * @property float
 * @constructor
 *
 * @param type
 */
class FloatOspreyObject(type: OspreyClass, var float: Float) : OspreyObject(type) {
    /**
     * Companion
     *
     * @constructor creates the cache for number objects
     */
    companion object {
        val cache = ObjectCache { number: Float -> FloatOspreyObject(FloatOspreyClass, number) }
    }
}

/**
 * Integer object
 *
 * @property int
 * @constructor
 *
 * @param type
 */
class IntegerOspreyObject(type: OspreyClass, var int: Int) : OspreyObject(type) {
    /**
     * Companion
     *
     * @constructor creates the cache for number objects
     */
    companion object {
        val cache = ObjectCache { number: Int -> IntegerOspreyObject(IntegerOspreyClass, number) }
    }
}