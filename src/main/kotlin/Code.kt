/**
 * Address class for storing an integer position for jump operations
 *
 * @property address
 * @constructor Create empty Address
 */
class Address(private var address: Int = -1) {
    /**
     * Sets the pointing to address to the given [address]
     */
    fun to(address: Int) {
        this.address = address
    }

    /**
     * Getter method for the address
     *
     * @return
     */
    fun whereTo(): Int = this.address

}

/**
 * Opcode enum class for the VM opcodes
 *
 * @constructor Create empty Opcode
 */
enum class Opcode {
    ADD, BUILD_DICTIONARY, BUILD_LIST, BUILD_TUPLE, CALL, COMPARE_EQUALS, COMPARE_LESS_THAN, COMPARE_LESS_THAN_OR_EQUALS,
    COMPARE_MORE_THAN, COMPARE_MORE_THAN_OR_EQUALS, COMPARE_NOT_EQUALS, DIVIDE, EVAL_THROWABLE, GET_ITERATOR, IMPORT_FROM,
    IMPORT_NAME, ITERATOR_NEXT, JUMP_ABSOLUTE, JUMP_IF_FALSE_OR_POP, JUMP_IF_TRUE_OR_POP, LOAD_ATTR, LOAD_CONST, LOAD_NAME,
    LOAD_STRING, MAKE_CLASS, MAKE_FUNCTION, MAKE_GENERATOR, MAKE_SUITE, MINUS, MULTIPLY, NO_OP, POP_JUMP_IF_FALSE, POP_TOP,
    POP_THROWABLE, POW, REMAINDER, RETURN_VALUE, SETUP_THROWABLE_HANDLER, STORE_ATTR, STORE_ATTR_LAZY, STORE_FUNCTION_NAME, STORE_NAME,
    STORE_NAME_FINAL, STORE_NAME_LAZY, STORE_NAME_LAZY_FINAL, THROW, UNPACK_SEQUENCE_REVERSE, GET_REF_NAME, GET_DEREF_VALUE, GENERATOR_EXIT, GET_ITEM, LOAD_PROPERTY, IS, IS_NOTHING, JUMP_IF_NOTHING_OR_POP, RANGE_BETWEEN, IS_INSTANCE, IS_IN, IS_NOT, IS_NOT_INSTANCE, IS_NOT_IN, MORE_THAN_OR_EQUAL, LESS_THAN_OR_EQUAL, MORE_THAN, LESS_THAN, NOT_EQUAL, EQUAL, JUMP_IF_NOT_NOTHING_OR_POP, JUMP_IF_NOT_NOTHING, UNPACK_SEQUENCE_REVERSE_LAZY, PROPAGATE,
}


/**
 * Operation
 *
 * @property opcode the opcode of the operation
 * @property at the lexeme that represents where the operation was generated
 * @constructor Create empty Operation
 */
open class Operation(val opcode: Opcode, val at: Lexeme)

/**
 * Jump operation
 *
 * @property to the address for where the jump operation is jumping to
 *
 * @param opcode
 * @param at
 */
class JumpOperation(opcode: Opcode, at: Lexeme, val to: Address) : Operation(opcode, at)

/**
 * Str operation
 *
 * @property string the string for the operation to: load on the stack, lookup the name for etc
 *
 * @param opcode
 * @param at
 */
class StrOperation(opcode: Opcode, at: Lexeme, val string: String) : Operation(opcode, at)

/**
 * Int operation
 *
 * @property int the int for the operation to use
 *
 * @param opcode
 * @param at
 */
class IntOperation(opcode: Opcode, at: Lexeme, val int: Int) : Operation(opcode, at)

/**
 * Const operation
 *
 * @property const the kotlamingo object created at parse time or before to use
 *
 * @param at
 */
class ConstOperation(at: Lexeme, val const: OspreyObject) : Operation(Opcode.LOAD_CONST, at)

/**
 * Make class operation
 *
 * @property name the name of the class
 * @property bases the number of bases the class has
 * @property suite the code if any to use to generate class methods
 *
 * @param at
 */
class MakeClassOperation(at: Lexeme, val name: String, val bases: Int, val suite: Code?) : Operation(Opcode.MAKE_CLASS, at)

/**
 * Make function operation
 *
 * @property name the name of the function
 * @property suite the code of the function
 * @property args the names of the positional arguments or null for no arguments
 * @property varArgs the name of the variable arguments or null for none
 * @property defaults the names of the default arguments or null for no defaults
 * @property keywords the name of the variable keyword arguments or null for none
 * @property annotations the amount of annotations for arguments of the function
 * @property functionAnnotation whether the function itself is annotated
 *
 * @param opcode
 * @param at
 */
class MakeFunctionOperation(
        opcode: Opcode,
        at: Lexeme,
        val name: String,
        val suite: Code,
        val args: Array<String>?,
        val varArgs: String?,
        val defaults: Array<String>?,
        val keywords: String?,
        val annotations: Int,
        val functionAnnotation: Boolean,
) : Operation(opcode, at)

/**
 * Make suite operation
 *
 * @property suite the code object of the suite
 *
 * @param at
 */
class MakeSuiteOperation(at: Lexeme, val suite: Code) : Operation(Opcode.MAKE_SUITE, at)

/**
 * Call operation
 *
 * @property args the number of arguments on the stack
 * @property keywords the keyword arguments to get values from the stack for
 *
 * @param at
 */
class CallOperation(at: Lexeme, val args: Int, val keywords: Array<String>) : Operation(Opcode.CALL, at)


/**
 * Code class for storing a run of operations utilising a hash map for jump index optimization
 *
 * @property name the name of the code object
 * @property block the of operations using a hashmap of {index: operation}
 * @property pointer a pointer to what will be the next instruction, therefore the length of the block
 */
class Code(val name: String) {
    val block = HashMap<Int, Operation>()
    var pointer = 0

    /**
     * Gets the last operation that was added to the block
     *
     * @param dummy a dummy operation that is required by kotlin for safety
     * @return the last operation added
     */
    fun lastOperation(dummy: Operation): Operation = this.block.getOrDefault(this.pointer - 1, dummy)

    /**
     * Removes the last operation that was added to the block
     *
     * @param dummy a dummy operation that is required by kotlin for safety
     * @return the removed operation
     */
    fun removeLastOperation(dummy: Operation): Operation {
        this.pointer--
        val operation = this.block.getOrDefault(this.pointer, dummy)
        this.block.remove(this.pointer)
        return operation
    }

    /**
     * Adds an operation at the end of the block
     *
     * @param operation the operation to add
     */
    fun addOperation(operation: Operation) {
        this.block[this.pointer] = operation
        this.pointer++
    }

    /**
     * Adds all the operations from one code block to this code block sequentially
     *
     * @param code the code block to add from
     */
    fun migrateCode(code: Code) {
        var i = 0
        while (i < code.pointer) {
            code.block[i]?.let { this.addOperation(it) }
            i++
        }
    }

    /**
     * Adds all operations from a collection of operations to this code block sequentially
     *
     * @param operations the collection of operations to add form
     */
    fun migrateOperations(operations: Collection<Operation>) {
        for (operation in operations) {
            this.addOperation(operation)
        }
    }
}