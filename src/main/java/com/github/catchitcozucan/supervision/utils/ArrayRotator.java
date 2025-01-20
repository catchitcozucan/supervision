/**
 * Original work by Ola Aronsson 2020
 * Courtesy of nollettnoll AB &copy; 2012 - 2020
 * <p>
 * Licensed under the Creative Commons Attribution 4.0 International (the "License")
 * you may not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 * <p>
 * https://creativecommons.org/licenses/by/4.0/
 * <p>
 * The software is provided “as is”, without warranty of any kind, express or
 * implied, including but not limited to the warranties of merchantability,
 * fitness for a particular purpose and noninfringement. In no event shall the
 * authors or copyright holders be liable for any claim, damages or other liability,
 * whether in an action of contract, tort or otherwise, arising from, out of or
 * in connection with the software or the use or other dealings in the software.
 */
package com.github.catchitcozucan.supervision.utils;

import java.util.Random;

public class ArrayRotator<T> {

    private final T[] array;
    private int index;
    private static final Random RANDOM = new Random(731111245L);

    private enum Direction {
        FORWARD, BACKWARD
    }

    private Direction direction;

    public ArrayRotator(T[] array) {
        if (array != null && array.length >= 2) {
            this.array = array;
            this.index = -1;
            this.direction = Direction.FORWARD;
        } else {
            throw new IllegalArgumentException("You need top ship an array of no less than 2 elements!");
        }
    }

    public T getNext() {
        ++this.index;
        if (this.index > this.array.length - 1) {
            this.index = 0;
        }

        return this.array[this.index];
    }

    public T getRandom() {
        return this.array[RANDOM.nextInt(this.array.length - 1 + 1)];
    }

    public T getActual(int index) {
        return array[index];
    }


    public T getNextPhading() {
        if (this.direction == Direction.FORWARD) {
            ++this.index;
        } else {
            --this.index;
            if (this.index < 0) {
                this.direction = Direction.FORWARD;
                ++this.index;
            }
        }

        if (this.index > this.array.length - 1) {
            this.direction = Direction.BACKWARD;
            --this.index;
        }

        return this.array[this.index];
    }
}
