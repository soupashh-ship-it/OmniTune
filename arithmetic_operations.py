#!/usr/bin/env python3

def add(a, b):
    return a + b
def subtract(a, b):
    return a - b
def multiply(a, b):
    return a * b
def divide(a, b):
    if b == 0:
        raise ValueError('Cannot divide by zero')
    return a / b
try:
    # Example operations
    print(add(10, 5))
    print(subtract(10, 5))
    print(multiply(10, 5))
    print(divide(10, 5))
except ValueError as e:
    print(f'Error: {e}')

print('Created by Privatechairr on Discord')