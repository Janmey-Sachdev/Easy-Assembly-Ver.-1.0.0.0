# Enhanced Easy Assembly Language Features

This document describes the new language features added to Easy Assembly (EA).

## New Commands

### ARRAY - Array Declaration
Declare an array with optional size (default: 10 elements).
```
ARRAY myArray 5
ASSIGN myArray[0] 100
ASSIGN myArray[1] 200
DISPLAY myArray[0]
```

### LOOP/ENDLOOP - While Loop
Execute a block repeatedly while a condition is true.
```
DECLARE counter
ASSIGN counter 0
LOOP counter < 5
  DISPLAY counter
  CALC counter counter + 1
ENDLOOP
```

### FOR/ENDFOR - For Loop
Execute a block a specific number of times.
```
FOR i 1 10 1
  DISPLAY i
ENDFOR
```
Syntax: `FOR variable start end [step]`

### BREAK - Exit Loop
Exit the current loop early.
```
FOR i 1 100 1
  CHECK i == 50
    BREAK
  ENDCHECK
ENDFOR
```

### CONTINUE - Skip to Next Iteration
Skip to the next iteration of a loop.
```
FOR i 1 10 1
  CHECK i == 5
    CONTINUE
  ENDCHECK
  DISPLAY i
ENDFOR
```

### INPUT - Read Input
Declare a variable for user input (placeholder).
```
INPUT username
```

### CHECK/ELSE/ELSEIF/ENDCHECK - If-Else Statements
Conditional execution with optional ELSE and ELSEIF branches.
```
CHECK age >= 18
  DISPLAY "Adult"
ELSE
  DISPLAY "Minor"
ENDCHECK
```

```
CHECK score >= 90
  DISPLAY "Grade A"
ELSEIF score >= 80
  DISPLAY "Grade B"
ELSEIF score >= 70
  DISPLAY "Grade C"
ELSE
  DISPLAY "Grade F"
ENDCHECK
```

## Built-in Functions

Math functions can be used in CALC expressions:

- `ABS(n)` - Absolute value
- `SQRT(n)` - Square root
- `MIN(a, b)` - Minimum of two values
- `MAX(a, b)` - Maximum of two values
- `ROUND(n)` - Round to nearest integer
- `FLOOR(n)` - Round down
- `CEIL(n)` - Round up
- `POW(base, exp)` - Exponentiation

String functions:

- `LEN(str)` - Length of string
- `SUBSTR(str, start, len)` - Substring extraction
- `UPPER(str)` - Convert to uppercase
- `LOWER(str)` - Convert to lowercase
- `TRIM(str)` - Remove whitespace

### Examples
```
DECLARE x
CALC x ABS(-42)         # x = 42
CALC x SQRT(16)         # x = 4
CALC x MIN(5, 3)        # x = 3
CALC x MAX(5, 3)        # x = 5
CALC x POW(2, 3)        # x = 8
CALC x LEN("hello")     # x = 5
ASSIGN msg "HELLO"
ASSIGN msg LOWER(msg)   # msg = "hello"
```

## Comments

Two styles of comments are supported:

```
# This is a comment
// This is also a comment
DECLARE x # inline comments work too
```

## Expression Support

All CALC expressions support:
- Arithmetic operators: +, -, *, /, %
- Comparison operators: ==, !=, <, >, <=, >=
- Logical operators: &&, ||, !
- Parentheses for grouping
- Variable substitution
- Built-in functions

Examples:
```
CALC result (a + b) * 2
CALC product x * y / z
CALC doubled value * 2 + 10
```

## Array Operations

Arrays can store and retrieve values by index:

```
ARRAY scores 100
ASSIGN scores[0] 95
ASSIGN scores[1] 87
DISPLAY scores[0]

CALC average (scores[0] + scores[1]) / 2
```

## Control Flow Summary

- **Sequential**: Execute lines in order
- **Conditional**: CHECK/ELSE/ELSEIF/ENDCHECK
- **Loops**: 
  - REPEAT/ENDREPEAT - Execute block N times
  - LOOP/ENDLOOP - While loop
  - FOR/ENDFOR - For loop with range
- **Loop Control**: BREAK, CONTINUE
- **Exit**: HALT

## Example Program

```
# Array manipulation example
ARRAY numbers 10
ARRAY doubled 10

FOR i 0 9 1
  ASSIGN numbers[i] i + 1
ENDFOR

DECLARE sum
ASSIGN sum 0

FOR i 0 9 1
  CALC doubled[i] numbers[i] * 2
  CALC sum sum + numbers[i]
ENDFOR

DISPLAY "Sum: "
DISPLAY sum

DECLARE avg
CALC avg sum / 10
DISPLAY "Average: "
DISPLAY avg
```
