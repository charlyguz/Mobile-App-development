package com.example.lab_3

import kotlin.random.Random

open class Animal(
    val name: String,
    val age: Int,
    val species: String
) {
    open fun move() {
        println("$name is moving")
    }

    override fun toString(): String {
        return "Hi. I'm a $species. I'm $age years old and my name is $name."
    }
}

class Fish(name: String, age: Int, species: String) : Animal(name, age, species) {
    override fun move() {
        println("$name is swimming")
    }
}

class Dog(name: String, age: Int, species: String) : Animal(name, age, species) {
    override fun move() {
        println("$name is running")
    }
}

fun powerFor(a: Int, b: Int): Long {
    var result = 1L
    for (i in 1..b) {
        result *= a
    }
    return result
}

fun powerWhile(a: Int, b: Int): Long {
    var result = 1L
    var i = 0
    while (i < b) {
        result *= a
        i++
    }
    return result
}

fun powerRepeat(a: Int, b: Int): Long {
    var result = 1L
    repeat(b) {
        result *= a
    }
    return result
}

fun factorial(n: Int): Long {
    if (n < 0) return -1
    var result = 1L
    for (i in 2..n) {
        result *= i
    }
    return result
}

fun calculate(a: Int, b: Int, op: String): String {
    return when (op) {
        "+" -> (a + b).toString()
        "-" -> (a - b).toString()
        "*" -> (a * b).toString()
        "/" -> if (b != 0) (a.toDouble() / b).toString() else "Division by zero"
        "^" -> powerFor(a, b).toString()
        "!" -> if (a >= 0) factorial(a).toString() else "Factorial is not defined for negative numbers"
        else -> "Invalid operator"
    }
}

fun fibonacci(n: Int): String {
    if (n <= 0) return ""
    if (n == 1) return "1"

    val result = StringBuilder()
    var a = 1
    var b = 1

    result.append(a).append(" ").append(b)

    for (i in 3..n) {
        val next = a + b
        result.append(" ").append(next)
        a = b
        b = next
    }

    return result.toString()
}

fun invertedPyramid(n: Int): String {
    val result = StringBuilder()
    for (i in n downTo 1) {
        for (j in 1..i) {
            result.append("*")
        }
        if (i != 1) result.append("\n")
    }
    return result.toString()
}

fun reverseString(text: String): String {
    var result = ""
    for (i in text.length - 1 downTo 0) {
        result += text[i]
    }
    return result
}

fun printArray(arr: IntArray) {
    for (i in arr.indices) {
        print("${arr[i]} ")
    }
    println()
}

fun insertionSort(arr: IntArray) {
    for (i in 1 until arr.size) {
        val key = arr[i]
        var j = i - 1
        while (j >= 0 && arr[j] > key) {
            arr[j + 1] = arr[j]
            j--
        }
        arr[j + 1] = key
    }
}

fun bubbleSort(arr: IntArray) {
    for (i in 0 until arr.size - 1) {
        for (j in 0 until arr.size - 1 - i) {
            if (arr[j] > arr[j + 1]) {
                val temp = arr[j]
                arr[j] = arr[j + 1]
                arr[j + 1] = temp
            }
        }
    }
}

fun variablesAndBasicOperations() {
    var a = 5
    var b = 6

    println("a = $a")
    println("b = $b")
    println(a + b)

    var c = a + b

    println("The result of " + a + " + " + b + " = " + c)
    println("The result of $a + $b = $c")

    c++
    println("The value of incremented c is $c")

    val x = 5
    val y = 6
    val z = x + y

    println("Using val:")
    println("The result of $x + $y = $z")
    println("You cannot increment z because val is immutable")
}

fun inputDifferenceDemo() {
    println("Enter a value for readln():")
    val input1 = readln()
    val number1 = input1.toIntOrNull()
    if (number1 != null) {
        println("readln() as Int: $number1")
    } else {
        println("readln() could not convert '$input1' to Int")
    }

    println("Enter a value for readLine():")
    val input2 = readLine()
    val number2 = input2?.toIntOrNull()
    if (number2 != null) {
        println("readLine() as Int: $number2")
    } else {
        println("readLine() could not convert '${input2 ?: "null"}' to Int")
    }
}

fun calculatorOnce() {
    println("Enter first number:")
    val a = readln().toInt()

    println("Enter second number:")
    val b = readln().toInt()

    println("Enter operator (+, -, *, /, ^, !):")
    val op = readln()

    println("Result: ${calculate(a, b, op)}")
}

fun powerDemo() {
    println("Enter base:")
    val a = readln().toInt()

    println("Enter exponent:")
    val b = readln().toInt()

    println("powerFor($a, $b) = ${powerFor(a, b)}")
    println("powerWhile($a, $b) = ${powerWhile(a, b)}")
    println("powerRepeat($a, $b) = ${powerRepeat(a, b)}")
}

fun loopedCalculator() {
    while (true) {
        println("Enter expression as: number number operator")
        println("Use +, -, *, /, ^ or !")
        println("Type exit to finish")

        val input = readln()

        if (input.lowercase() == "exit") {
            break
        }

        val parts = input.split(" ")

        if (parts.size == 2 && parts[1] == "!") {
            val a = parts[0].toIntOrNull()
            if (a == null) {
                println("Invalid input")
            } else {
                println("Result: ${calculate(a, 0, "!")}")
            }
        } else if (parts.size == 3) {
            val a = parts[0].toIntOrNull()
            val b = parts[1].toIntOrNull()
            val op = parts[2]

            if (a == null || b == null) {
                println("Invalid numbers")
            } else {
                println("Result: ${calculate(a, b, op)}")
            }
        } else {
            println("Invalid format")
        }
    }
}

fun fibonacciDemo() {
    println("Enter n:")
    val n = readln().toInt()
    println(fibonacci(n))
}

fun pyramidDemo() {
    println("Enter height:")
    val n = readln().toInt()
    println(invertedPyramid(n))
}

fun reverseDemo() {
    println("Enter text:")
    val text = readln()
    println(reverseString(text))
}

fun arraysDemo() {
    val names = arrayOf("Anna", "John", "Mark")

    println("All elements:")
    for (name in names) {
        println(name)
    }

    println("With indices:")
    for (i in names.indices) {
        println("$i: ${names[i]}")
    }

    var longest = names[0]
    for (name in names) {
        if (name.length > longest.length) {
            longest = name
        }
    }
    println("Longest name: $longest")

    val original = IntArray(100) { Random.nextInt(0, 1000) }

    val insertionArray = original.copyOf()
    val bubbleArray = original.copyOf()

    println("Before insertion sort:")
    printArray(insertionArray)
    insertionSort(insertionArray)
    println("After insertion sort:")
    printArray(insertionArray)

    println("Before bubble sort:")
    printArray(bubbleArray)
    bubbleSort(bubbleArray)
    println("After bubble sort:")
    printArray(bubbleArray)
}

fun classesDemo() {
    val animal1 = Animal("Puffy", 6, "spider")
    println(animal1)
    animal1.move()

    val fish = Fish("Nemo", 2, "fish")
    val dog = Dog("Buddy", 4, "dog")

    println(fish)
    fish.move()

    println(dog)
    dog.move()

    val animal: Animal = Fish("Goldie", 1, "fish")
    animal.move()
}

fun main() {
    while (true) {
        println()
        println("1. Variables and Basic Operations")
        println("2. readln() and readLine()")
        println("3. Calculator")
        println("4. Power function (3 variants)")
        println("5. Looped calculator")
        println("6. Fibonacci")
        println("7. Inverted pyramid")
        println("8. Reverse string")
        println("9. Arrays and sorting")
        println("10. Classes and objects")
        println("0. Exit")

        when (readln()) {
            "1" -> variablesAndBasicOperations()
            "2" -> inputDifferenceDemo()
            "3" -> calculatorOnce()
            "4" -> powerDemo()
            "5" -> loopedCalculator()
            "6" -> fibonacciDemo()
            "7" -> pyramidDemo()
            "8" -> reverseDemo()
            "9" -> arraysDemo()
            "10" -> classesDemo()
            "0" -> return
            else -> println("Invalid option")
        }
    }
}