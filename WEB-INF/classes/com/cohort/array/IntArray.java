/* This file is part of the EMA project and is 
 * Copyright (c) 2005 Robert Alten Simons (info@cohort.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohort.com or contact info@cohort.com.
 */
package com.cohort.array;

import com.cohort.util.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * IntArray is a thin shell over an int[] with methods like ArrayList's 
 * methods; it extends PrimitiveArray.
 *
 * <p>This class uses Integer.MAX_VALUE to represent a missing value (NaN).
 */
public class IntArray extends PrimitiveArray {

    /**
     * This is the main data structure.
     * This should be private, but is public so you can manipulate it if you 
     * promise to be careful.
     * Note that if the PrimitiveArray's capacity is increased,
     * the PrimitiveArray will use a different array for storage.
     */
    public int[] array;

    /**
     * A constructor for a capacity of 8 elements. The initial 'size' will be 0.
     */
    public IntArray() {
        array = new int[8];
    }

    /**
     * This constructs a IntArray by copying elements from the incoming
     * PrimitiveArray (using append()).
     *
     * @param primitiveArray a primitiveArray of any type 
     */
    public IntArray(PrimitiveArray primitiveArray) {
        array = new int[8];
        append(primitiveArray);
    }

    /**
     * A constructor for a specified number of elements. The initial 'size' will be 0.
     *
     * @param capacity creates an IntArray with the specified initial capacity.
     * @param active if true, size will be set to capacity and all elements 
     *    will equal 0; else size = 0.
     * @throws Exception if trouble.
     */
    public IntArray(int capacity, boolean active) {
        Math2.ensureMemoryAvailable(4L * capacity, "IntArray");
        array = new int[capacity];
        if (active) 
            size = capacity;
    }

    /**
     * A special constructor for IntArray: first, last, with an increment of 1.
     *
     * @param first the value of the first element.
     * @param last the value of the last element (inclusive!).
     * @throws Exception if trouble.
     */
    public IntArray(int first, int last) {
        size = last - first + 1;
        array = new int[size];
        for (int i = 0; i < size; i++) 
            array[i] = first + i;
    }

    /**
     * A constructor which (at least initially) uses the array and all its 
     * elements ('size' will equal anArray.length).
     *
     * @param anArray the array to be used as this object's array.
     */
    public IntArray(int[] anArray) {
        array = anArray;
        size = anArray.length;
    }

    /**
     * This returns the current capacity (number of elements) of the internal data array.
     * 
     * @return the current capacity (number of elements) of the internal data array.
     */
    public int capacity() {
        return array.length;
    }

    /**
     * This returns the hashcode for this byteArray (dependent only on values,
     * not capacity).
     * WARNING: the algorithm used may change in future versions.
     *
     * @return the hashcode for this byteArray (dependent only on values,
     * not capacity)
     */
    public int hashCode() {
        //see http://download.oracle.com/javase/1.5.0/docs/api/java/util/List.html#hashCode()
        //and http://stackoverflow.com/questions/299304/why-does-javas-hashcode-in-string-use-31-as-a-multiplier
        int code = 0;
        for (int i = 0; i < size; i++)
            code = 31*code + array[i];
        return code;
    }

    /**
     * This makes a new subset of this PrimitiveArray based on startIndex, stride,
     * and stopIndex.
     *
     * @param startIndex must be a valid index
     * @param stride   must be at least 1
     * @param stopIndex (inclusive) If &gt;= size, it will be changed to size-1.
     * @return a new PrimitiveArray with the desired subset.
     *    It will have a new backing array with a capacity equal to its size.
     *    If stopIndex &lt; startIndex, this returns PrimitiveArray with size=0;
     */
    public PrimitiveArray subset(int startIndex, int stride, int stopIndex) {
        if (startIndex < 0)
            throw new IndexOutOfBoundsException(String2.ERROR + 
                " in IntArray.subset: startIndex=" + startIndex +
                " must be at least 0.");
        if (stride < 1)
            throw new IllegalArgumentException(String2.ERROR + 
                " in IntArray.subset: stride=" + stride +
                " must greater than 0.");
        if (stopIndex >= size)
            stopIndex = size - 1;
        if (stopIndex < startIndex)
            return new IntArray(new int[0]);

        int willFind = strideWillFind(stopIndex - startIndex + 1, stride);
        Math2.ensureMemoryAvailable(4L * willFind, "IntArray"); 
        int tar[] = new int[willFind];
        if (stride == 1) {
            System.arraycopy(array, startIndex, tar, 0, willFind);
        } else {
            int po = 0;
            for (int i = startIndex; i <= stopIndex; i+=stride) 
                tar[po++] = array[i];
        }
        return new IntArray(tar);
    }

    /**
     * This returns the class (int.class) of the element type.
     *
     * @return the class (int.class) of the element type.
     */
    public Class elementClass() {
        return int.class;
    }

    /**
     * This returns the class index (CLASS_INDEX_INT) of the element type.
     *
     * @return the class index (CLASS_INDEX_INT) of the element type.
     */
    public int elementClassIndex() {
        return CLASS_INDEX_INT;
    }

    /**
     * This adds an item to the array (increasing 'size' by 1).
     *
     * @param value the value to be added to the array
     */
    public void add(int value) {
        if (size == array.length) //if we're at capacity
            ensureCapacity(size + 1L);
        array[size++] = value;
    }

    /**
     * This adds all the values from ar.
     *
     * @param ar an array
     */
    public void add(int ar[]) {
        int arSize = ar.length; 
        ensureCapacity(size + (long)arSize);
        System.arraycopy(ar, 0, array, size, arSize);
        size += arSize;
    }    

    /**
     * This adds n copies of value to the array (increasing 'size' by n).
     *
     * @param n  if less than 0, this throws Exception
     * @param value the value to be added to the array.
     *    n &lt; 0 throws an Exception.
     */
    public void addN(int n, int value) {
        if (n == 0) return;
        if (n < 0)
            throw new IllegalArgumentException(String2.ERROR + " in IntArray.addN: n (" + n + ") < 0");
        ensureCapacity(size + (long)n);
        Arrays.fill(array, size, size + n, value);
        size += n;
    }

    /**
     * This inserts an item into the array (increasing 'size' by 1).
     *
     * @param index the position where the value should be inserted.
     * @param value the value to be inserted into the array
     */
    public void add(int index, int value) {
        if (size == array.length) //if we're at capacity
            ensureCapacity(size + 1L);
        System.arraycopy(array, index, array, index + 1, size - index);
        size++;
        array[index] = value;
    }

    /**
     * This adds an element to the array at the specified index.
     *
     * @param index 0..
     * @param value the value, as a String.
     */
    public void addString(int index, String value) {
        add(index, String2.parseInt(value));
    }

    /**
     * This adds n Strings to the array.
     *
     * @param n the number of times 'value' should be added
     * @param value the value, as a String.
     */
    public void addNStrings(int n, String value) {
        addN(n, String2.parseInt(value));
    }

    /**
     * This adds an element to the array.
     *
     * @param value the value, as a String.
     */
    public void addString(String value) {
        add(String2.parseInt(value));
    }

    /**
     * This adds an element to the array.
     *
     * @param value the float value
     */
    public void addFloat(float value) {
        add(Math2.roundToInt(value));
    }

    /**
     * This adds an element to the array.
     *
     * @param value the value, as a double.
     */
    public void addDouble(double value) {
        add(Math2.roundToInt(value));
    }

    /**
     * This adds n doubles to the array.
     *
     * @param n the number of times 'value' should be added
     * @param value the value, as a double.
     */
    public void addNDoubles(int n, double value) {
        addN(n, Math2.roundToInt(value));
    }

    /**
     * This adds an element to the array.
     *
     * @param value the value, as an int.
     */
    public void addInt(int value) {
        add(value);
    }

    /**
     * This adds an element to the array.
     *
     * @param value the value, as a long.
     */
    public void addLong(long value) {
        add(Math2.narrowToInt(value));
    }

    /**
     * This adds an element from another PrimitiveArray.
     *
     * @param otherPA
     * @param otherIndex
     */
    public void addFromPA(PrimitiveArray otherPA, int otherIndex) {
        add(otherPA.getInt(otherIndex));
    }

    /**
     * This sets an element from another PrimitiveArray.
     *
     * @param index the index to be set
     * @param otherPA
     * @param otherIndex
     */
    public void setFromPA(int index, PrimitiveArray otherPA, int otherIndex) {
        set(index, otherPA.getInt(otherIndex));
    }

    /**
     * This removes the specified element.
     *
     * @param index the element to be removed, 0 ... size-1
     * @throws Exception if trouble.
     */
    public void remove(int index) {
        if (index >= size)
            throw new IllegalArgumentException(String2.ERROR + " in IntArray.remove: index (" + 
                index + ") >= size (" + size + ").");
        System.arraycopy(array, index + 1, array, index, size - index - 1);
        size--;

        //for object types, nullify the object at the end
    }

    /**
     * This removes the specified range of elements.
     *
     * @param from the first element to be removed, 0 ... size
     * @param to one after the last element to be removed, from ... size
     * @throws Exception if trouble.
     */
    public void removeRange(int from, int to) {
        if (to > size)
            throw new IllegalArgumentException(String2.ERROR + " in IntArray.removeRange: to (" + 
                to + ") > size (" + size + ").");
        if (from >= to) {
            if (from == to) 
                return;
            throw new IllegalArgumentException(String2.ERROR + " in IntArray.removeRange: from (" + 
                from + ") > to (" + to + ").");
        }
        System.arraycopy(array, to, array, from, size - to);
        size -= to - from;

        //for object types, nullify the objects at the end
    }

    /**
     * Moves elements 'first' through 'last' (inclusive)
     *   to 'destination'.
     *
     * @param first  the first to be move
     * @param last  (exclusive)
     * @param destination the destination, can't be in the range 'first+1..last-1'.
     * @throws Exception if trouble
     */
    public void move(int first, int last, int destination) {
        String errorIn = String2.ERROR + " in IntArray.move:\n";

        Test.ensureTrue(first >= 0, 
            errorIn + "first (" + first + ") must be >= 0.");
        Test.ensureTrue(last >= first && last <= size, 
            errorIn + "last (" + last + ") must be >= first (" + first + ") and <= size (" + size + ").");
        Test.ensureTrue(destination >= 0 && destination <= size, 
            errorIn + "destination (" + destination + ") must be between 0 and size (" + size + ").");
        Test.ensureTrue(destination <= first || destination >= last, 
            errorIn + "destination (" + destination + ") must be <= first (" + first + ") or >= last (" + last + ").");
        if (first == last || destination == first || destination == last) 
            return; //nothing to do
        //String2.log("move first=" + first + " last=" + last + " dest=" + destination);
        //String2.log("move initial " + String2.toCSSVString(array));

        //store the range to be moved
        int nToMove = last - first;
        int[] temp = new int[nToMove];
        System.arraycopy(array, first, temp, 0, nToMove);

        //if moving to left...    (draw diagram to visualize this)
        if (destination < first) {
            System.arraycopy(array, destination, array, destination + nToMove, first - destination);
            //String2.log("move after shift " + String2.toCSSVString(array));

            //copy temp data into place
            System.arraycopy(temp, 0, array, destination, nToMove);
        } else {
            //moving to right
            System.arraycopy(array, last, array, first, destination - last);
            //String2.log("move after shift " + String2.toCSSVString(array));

            //copy temp data into place
            System.arraycopy(temp, 0, array, destination - nToMove, nToMove);
        }
        //String2.log("move done " + String2.toCSSVString(array));


    }

    /**
     * This just keeps the rows for the 'true' values in the bitset.
     * Rows that aren't kept are removed.
     * The resulting PrimitiveArray is compacted (i.e., it has a smaller size()).
     *
     * @param bitset
     */
    public void justKeep(BitSet bitset) {
        int newSize = 0;
        for (int row = 0; row < size; row++) {
            if (bitset.get(row)) 
                array[newSize++] = array[row];
        }
        removeRange(newSize, size);
    }    

    /**
     * This ensures that the capacity is at least 'minCapacity'.
     *
     * @param minCapacity the minimum acceptable capacity.
     *    minCapacity is type long, but &gt;= Integer.MAX_VALUE will throw exception.
     */
    public void ensureCapacity(long minCapacity) {
        if (array.length < minCapacity) {
            //ensure minCapacity is < Integer.MAX_VALUE
            Math2.ensureArraySizeOkay(minCapacity, "IntArray");  
            //caller may know exact number needed, so don't double above 2x current size
            int newCapacity = (int)Math.min(Integer.MAX_VALUE - 1, array.length + (long)array.length); 
            if (newCapacity < minCapacity) 
                newCapacity = (int)minCapacity; //safe since checked above
            Math2.ensureMemoryAvailable(4L * newCapacity, "IntArray");
            int[] newArray = new int[newCapacity];
            System.arraycopy(array, 0, newArray, 0, size);
            array = newArray;
        }
    }

    /**
     * This returns an array (perhaps 'array') which has 'size' elements.
     *
     * @return an array (perhaps 'array') which has 'size' elements.
     */
    public int[] toArray() {
        if (array.length == size)
            return array;
        Math2.ensureMemoryAvailable(4L * size, "IntArray.toArray");
        int[] tArray = new int[size];
        System.arraycopy(array, 0, tArray, 0, size);
        return tArray;
    }
   
    /**
     * This returns a primitive[] (perhaps 'array') which has 'size' 
     * elements.
     *
     * @return a primitive[] (perhaps 'array') which has 'size' elements.
     */
    public Object toObjectArray() {
        return toArray();
    }

    /**
     * This returns a BitSet with 
     * the bit associated with each element in this IntArray is set.
     * Negative numbers are skipped.
     * The bitSet will be sized so that that last bit is set.
     * For example, {1,4,5} will return 0,1,0,0,1,1
     *
     * @return an array (perhaps 'array') which has 'size' elements.
     */
    public BitSet toBitSet() {
        double stats[] = calculateStats();
        int max = Math2.roundToInt(stats[PrimitiveArray.STATS_MAX]);
        BitSet bitSet = new BitSet(max + 1);  //+1 so 0..max
        for (int i = 0; i < size; i++) {
            int ti = array[i];
            if (ti >= 0)
                bitSet.set(ti);
        }
        return bitSet;
    }
   
    /**
     * This returns a double[] (perhaps 'array') which has 'size' elements.
     *
     * @return a double[] (perhaps 'array') which has 'size' elements.
     *   Integer.MAX_VALUE is converted to Double.NaN.
     */
    public double[] toDoubleArray() {
        Math2.ensureMemoryAvailable(8L * size, "IntArray.toDoubleArray");
        double dar[] = new double[size];
        for (int i = 0; i < size; i++) {
            int j = array[i];
            dar[i] = j == Integer.MAX_VALUE? Double.NaN : j;
        }
        return dar;
    }

    /**
     * This returns a String[] which has 'size' elements.
     *
     * @return a String[] which has 'size' elements.
     *    Integer.MAX_VALUE appears as "".
     */
    public String[] toStringArray() {
        Math2.ensureMemoryAvailable(8L * size, "IntArray.toStringArray"); //8L is feeble minimal estimate
        String sar[] = new String[size];
        for (int i = 0; i < size; i++) {
            int j = array[i];
            sar[i] = j == Integer.MAX_VALUE? "" : String.valueOf(j);
        }
        return sar;
    }

    /**
     * This gets a specified element.
     *
     * @param index 0 ... size-1
     * @throws Exception if trouble.
     */
    public int get(int index) {
        if (index >= size)
            throw new IllegalArgumentException(String2.ERROR + " in IntArray.get: index (" + 
                index + ") >= size (" + size + ").");
        return array[index];
    }

    /**
     * This sets a specified element.
     *
     * @param index 0 ... size-1
     * @param value the value for that element
     * @throws Exception if trouble.
     */
    public void set(int index, int value) {
        if (index >= size)
            throw new IllegalArgumentException(String2.ERROR + " in IntArray.set: index (" + 
                index + ") >= size (" + size + ").");
        array[index] = value;
    }


    /**
     * Return a value from the array as an int.
     * 
     * @param index the index number 0 ... size-1
     * @return the value as an int. 
     * @throws Exception if trouble.
     */
    public int getInt(int index) {
        return get(index);
    }

    /**
     * Set a value in the array as an int.
     * 
     * @param index the index number 0 .. size-1
     * @param i the value.
     * @throws Exception if trouble.
     */
    public void setInt(int index, int i) {
        set(index, i);
    }

    /**
     * Return a value from the array as a long.
     * 
     * @param index the index number 0 ... size-1
     * @return the value as a long. 
     *   Integer.MAX_VALUE is returned as Long.MAX_VALUE.
     * @throws Exception if trouble.
     */
    public long getLong(int index) {
        int i = get(index);
        return i == Integer.MAX_VALUE? Long.MAX_VALUE : i;
    }

    /**
     * Set a value in the array as a long.
     * 
     * @param index the index number 0 .. size-1
     * @param i the value. For numeric PrimitiveArray's, it is narrowed 
     *   if needed by methods like Math2.narrowToInt(long).
     * @throws Exception if trouble.
     */
    public void setLong(int index, long i) {
        set(index, Math2.narrowToInt(i));
    }


    /**
     * Return a value from the array as a float.
     * 
     * @param index the index number 0 .. size-1
     * @return the value as a float. String values are parsed
     *   with String2.parseFloat and so may return Float.NaN.
     *   Int.MAX_VALUE is returned as Float.NaN.
     * @throws Exception if trouble.
     */
    public float getFloat(int index) {
        int i = get(index);
        return i == Integer.MAX_VALUE? Float.NaN : i;
    }

    /**
     * Set a value in the array as a float.
     * 
     * @param index the index number 0 .. size-1
     * @param d the value. For numeric PrimitiveArray, it is narrowed 
     *   if needed by methods like Math2.roundToInt(d).
     * @throws Exception if trouble.
     */
    public void setFloat(int index, float d) {
        set(index, Math2.roundToInt(d));
    }

    /**
     * Return a value from the array as a double.
     * 
     * @param index the index number 0 .. size-1
     * @return the value as a double. String values are parsed
     *   with String2.parseDouble and so may return Double.NaN.
     *   Int.MAX_VALUE is returned as Double.NaN.
     * @throws Exception if trouble.
     */
    public double getDouble(int index) {
        int i = get(index);
        return i == Integer.MAX_VALUE? Double.NaN : i;
    }

    /**
     * Set a value in the array as a double.
     * 
     * @param index the index number 0 .. size-1
     * @param d the value. For numeric PrimitiveArray, it is narrowed 
     *   if needed by methods like Math2.roundToInt(d).
     * @throws Exception if trouble.
     */
    public void setDouble(int index, double d) {
        set(index, Math2.roundToInt(d));
    }

    /**
     * Return a value from the array as a String.
     * 
     * @param index the index number 0 .. 
     * @return For numeric types, this returns (String.valueOf(ar[index])), or "" for NaN or infinity.
     * @throws Exception if trouble.
     */
    public String getString(int index) {
        int b = get(index);
        return b == Integer.MAX_VALUE? "" : String.valueOf(b);
    }

    /**
     * Set a value in the array as a String.
     * 
     * @param index the index number 0 .. 
     * @param s the value. For numeric PrimitiveArray's, it is parsed
     *   with String2.parseInt.
     * @throws Exception if trouble.
     */
    public void setString(int index, String s) {
        set(index, String2.parseInt(s));
    }


    /**
     * This finds the first value which equals 'lookFor' starting at index 'startIndex'.
     *
     * @param lookFor the value to be looked for
     * @return the index where 'lookFor' is found, or -1 if not found.
     */
    public int indexOf(int lookFor) {
        return indexOf(lookFor, 0);
    }

    /**
     * This finds the first value which equals 'lookFor' starting at index 'startIndex'.
     *
     * @param lookFor the value to be looked for
     * @param startIndex 0 ... size-1
     * @return the index where 'lookFor' is found, or -1 if not found.
     */
    public int indexOf(int lookFor, int startIndex) {
        for (int i = startIndex; i < size; i++) 
            if (array[i] == lookFor) 
                return i;
        return -1;
    }

    /**
     * This finds the first value which equals 'lookFor' starting at index 'startIndex'.
     *
     * @param lookFor the value to be looked for
     * @param startIndex 0 ... size-1
     * @return the index where 'lookFor' is found, or -1 if not found.
     */
    public int indexOf(String lookFor, int startIndex) {
        return indexOf(String2.parseInt(lookFor), startIndex);
    }

    /**
     * This finds the last value which equals 'lookFor' starting at index 'startIndex'.
     *
     * @param lookFor the value to be looked for
     * @param startIndex 0 ... size-1. The search progresses towards 0.
     * @return the index where 'lookFor' is found, or -1 if not found.
     */
    public int lastIndexOf(int lookFor, int startIndex) {
        if (startIndex >= size)
            throw new IllegalArgumentException(String2.ERROR + " in IntArray.get: startIndex (" + 
                startIndex + ") >= size (" + size + ").");
        for (int i = startIndex; i >= 0; i--) 
            if (array[i] == lookFor) 
                return i;
        return -1;
    }

    /**
     * This finds the last value which equals 'lookFor' starting at index 'startIndex'.
     *
     * @param lookFor the value to be looked for
     * @param startIndex 0 ... size-1. The search progresses towards 0.
     * @return the index where 'lookFor' is found, or -1 if not found.
     */
    public int lastIndexOf(String lookFor, int startIndex) {
        return lastIndexOf(String2.parseInt(lookFor), startIndex);
    }

    /**
     * If size != capacity, this makes a new 'array' of size 'size'
     * so capacity will equal size.
     */
    public void trimToSize() {
        array = toArray();
    }

    /**
     * Test if o is an IntArray with the same size and values.
     *
     * @param o
     * @return true if equal.  o=null throws an exception.
     */
    public boolean equals(Object o) {
        return testEquals(o).length() == 0;
    }

    /**
     * Test if o is an IntArray with the same size and values,
     * but returns a String describing the difference (or "" if equal).
     *
     * @param o
     * @return a String describing the difference (or "" if equal).
     *   o=null throws an exception.
     */
    public String testEquals(Object o) {
        if (!(o instanceof IntArray))
            return "The two objects aren't equal: this object is a IntArray; the other is a " + 
                o.getClass().getName() + ".";
        IntArray other = (IntArray)o;
        if (other.size() != size)
            return "The two IntArrays aren't equal: one has " + size + " value" +
               (size == 0? "s" :
                size == 1? " (" + array[0] + ")" : 
                           "s (from " + array[0] + " to " + array[size - 1] + ")") +
               "; the other has " + other.size() + " value" +
               (other.size == 0? "s" :
                other.size == 1? " (" + other.array[0] + ")" : 
                                 "s (from " + other.array[0] + " to " + other.array[other.size - 1] + ")") +
               ".";
        for (int i = 0; i < size; i++)
            if (array[i] != other.array[i])
                return "The two IntArrays aren't equal: this[" + i + "]=" + array[i] + 
                                                    "; other[" + i + "]=" + other.array[i] + ".";
        return "";
    }

    /** 
     * This converts the elements into a Comma-Space-Separated-Value (CSSV) String.
     *
     * @return a Comma-Space-Separated-Value (CSSV) String representation 
     */
    public String toString() {
        return String2.toCSSVString(toArray()); //toArray() get just 'size' elements
    }

    /** 
     * This sorts the elements in ascending order.
     * To get the elements in reverse order, just read from the end of the list
     * to the beginning.
     */
    public void sort() {
        Arrays.sort(array, 0, size);
    }

    /**
     * This compares the values in row1 and row2 for SortComparator,
     * and returns a negative integer, zero, or a positive integer if the 
     * value at index1 is less than, equal to, or greater than 
     * the value at index2.
     * Currently, this does not checking of the range of index1 and index2,
     * so the caller should be careful.
     *
     * @param index1 an index number 0 ... size-1
     * @param index2 an index number 0 ... size-1
     * @return returns a negative integer, zero, or a positive integer if the 
     *   value at index1 is less than, equal to, or greater than 
     *   the value at index2.  
     *   Think "array[index1] - array[index2]".
     */
    public int compare(int index1, int index2) {
        return array[index1] - array[index2];
    }

    /**
     * This copies the value in row 'from' to row 'to'.
     * This does not check that 'from' and 'to' are valid;
     * the caller should be careful.
     * The value for 'from' is unchanged.
     *
     * @param from an index number 0 ... size-1
     * @param to an index number 0 ... size-1
     */
    public void copy(int from, int to) {
        array[to] = array[from];
    }

    /**
     * This reorders the values in 'array' based on rank.
     *
     * @param rank is an int with values (0 ... size-1) 
     * which points to the row number for a row with a specific 
     * rank (e.g., rank[0] is the row number of the first item 
     * in the sorted list, rank[1] is the row number of the
     * second item in the sorted list, ...).
     */
    public void reorder(int rank[]) {
        int n = rank.length;
        //new length could be n, but I'll keep it the same array.length as before
        int newArray[] = new int[array.length]; 
        for (int i = 0; i < n; i++)
            newArray[i] = array[rank[i]];
        array = newArray;
    }

    /**
     * This writes one element to a DataOutputStream.
     *
     * @param dos the DataOutputStream
     * @param i the index of the element to be written
     * @return the number of bytes used for this element
     *    (for Strings, this varies; for others it is consistent)
     * @throws Exception if trouble
     */
    public int writeDos(DataOutputStream dos, int i) throws Exception {
        dos.writeInt(array[i]);
        return 4;
    }

    /**
     * This reads/adds n elements from a DataInputStream.
     *
     * @param dis the DataInputStream
     * @param n the number of elements to be read/added
     * @throws Exception if trouble
     */
    public void readDis(DataInputStream dis, int n) throws Exception {
        ensureCapacity(size + (long)n);
        for (int i = 0; i < n; i++)
            array[size++] = dis.readInt();
    }

    /**
     * This reads/appends int values to this PrimitiveArray from a DODS DataInputStream,
     * and is thus the complement of externalizeForDODS.
     *
     * @param dis
     * @throws IOException if trouble
     */
    public void internalizeFromDODS(DataInputStream dis) throws java.io.IOException {
        int nValues = dis.readInt();
        dis.readInt(); //skip duplicate of nValues
        ensureCapacity(size + (long)nValues);
        for (int i = 0; i < nValues; i++) 
            array[size++] = dis.readInt();
    }

    /**
     * This reads one value from a randomAccessFile.
     *
     * @param raf the RandomAccessFile
     * @param start the raf offset of the start of the array (nBytes)
     * @param index the index of the desired value (0..)
     * @return the requested value as a double
     * @throws Exception if trouble
     */
    public static double rafReadDouble(RandomAccessFile raf, long start, long index) 
        throws Exception {
 
        raf.seek(start + 4*index);
        int i = raf.readInt();
        return i == Integer.MAX_VALUE? Double.NaN : i;
    }

    /**
     * This writes one value to a randomAccessFile at the current position.
     *
     * @param raf the RandomAccessFile
     * @param value the value which will be converted to this PrimitiveArray's 
     *    type and then stored
     * @throws Exception if trouble
     */
    public static void rafWriteDouble(RandomAccessFile raf, double value) throws Exception {
        raf.writeInt(Math2.roundToInt(value));
    }

    /**
     * This writes one value to a randomAccessFile.
     *
     * @param raf the RandomAccessFile
     * @param start the raf offset of the start of the array (nBytes)
     * @param index the index of the desired value (0..)
     * @param value the value which will be converted to this PrimitiveArray's 
     *    type and then stored
     * @throws Exception if trouble
     */
    public static void rafWriteDouble(RandomAccessFile raf, long start, long index,
        double value) throws Exception {
 
        raf.seek(start + 4*index);
        raf.writeInt(Math2.roundToInt(value));
    }

    /**
     * This fully skips 'n' elements in the dataInputStream.
     *
     * @param dis
     * @param n the number of elements to skip
     * @throws Exception if trouble
     */
    public static void disSkip(DataInputStream dis, int n) throws Exception {
        int nBytes = 4 * n;
        while (nBytes > 0) nBytes -= dis.skipBytes(nBytes);
    }
    
    /**
     * This reads an elements from the dataInputStream.
     *
     * @param dis
     * @return param n the number of elements to skip
     * @throws Exception if trouble
     */
    public static double readDisAsDouble(DataInputStream dis) throws Exception {
        int i = dis.readInt();
        return i == Integer.MAX_VALUE? Double.NaN : i;
    }
    
    /**
     * This appends the data in another primitiveArray to the current data.
     * WARNING: information may be lost from the incoming primitiveArray if this
     * primitiveArray is of a simpler type.
     *
     * @param primitiveArray primitiveArray must be the same or a narrower 
     *  data type, or the data will be narrowed with primitiveArray.getInt.
     */
    public void append(PrimitiveArray primitiveArray) {
        int otherSize = primitiveArray.size(); //this avoids infinite loop if primitiveArray == this
        ensureCapacity(size + (long)otherSize);
        if (primitiveArray instanceof IntArray) {
            System.arraycopy(((IntArray)primitiveArray).array, 0, array, size, otherSize);
        } else {
            for (int i = 0; i < otherSize; i++)
                array[size + i] = primitiveArray.getInt(i);  //this converts mv's
        }
        size += otherSize;
    }    

    /**
     * This populates 'indices' with the indices (ranks) of the values in this IntArray
     * (ties get the same index). For example, 10,10,25,3 returns 1,1,2,0.
     *
     * @param indices the intArray that will capture the indices of the values 
     *  (ties get the same index). For example, 10,10,25,3 returns 1,1,2,0.
     * @return a PrimitveArray (the same type as this class) with the unique values, sorted.
     *     If all the values are unique and already sorted, this returns 'this'.
     */
    public PrimitiveArray makeIndices(IntArray indices) {
        indices.clear();
        if (size == 0) {
            return new IntArray();
        }

        //make a hashMap with all the unique values (associated values are initially all dummy)
        Integer dummy = new Integer(-1);
        HashMap hashMap = new HashMap(Math2.roundToInt(1.4 * size));
        int lastValue = array[0]; //since lastValue often equals currentValue, cache it
        hashMap.put(new Integer(lastValue), dummy);
        boolean alreadySorted = true;
        for (int i = 1; i < size; i++) {
            int currentValue = array[i];
            if (currentValue != lastValue) {
                if (currentValue < lastValue) 
                    alreadySorted = false;
                lastValue = currentValue;
                hashMap.put(new Integer(lastValue), dummy);
            }
        }

        //quickly deal with: all unique and already sorted
        Set keySet = hashMap.keySet();
        int nUnique = keySet.size();
        if (nUnique == size && alreadySorted) {
            indices.ensureCapacity(size);
            for (int i = 0; i < size; i++)
                indices.add(i);           
            return this; //the PrimitiveArray with unique values
        }

        //store all the elements in an array
        Object unique[] = new Object[nUnique];
        Iterator iterator = keySet.iterator();
        int count = 0;
        while (iterator.hasNext())
            unique[count++] = iterator.next();
        Test.ensureEqual(nUnique, count, "IntArray.makeRankArray nUnique != count!");

        //sort them
        Arrays.sort(unique);

        //put the unique values back in the hashMap with the ranks as the associated values
        //and make tUnique 
        int tUnique[] = new int[nUnique];
        for (int i = 0; i < count; i++) {
            hashMap.put(unique[i], new Integer(i));
            tUnique[i] = ((Integer)unique[i]).intValue();
        }

        //convert original values to ranks
        int ranks[] = new int[size];
        lastValue = array[0];
        ranks[0] = ((Integer)hashMap.get(new Integer(lastValue))).intValue();
        int lastRank = ranks[0];
        for (int i = 1; i < size; i++) {
            if (array[i] == lastValue) {
                ranks[i] = lastRank;
            } else {
                lastValue = array[i];
                ranks[i] = ((Integer)hashMap.get(new Integer(lastValue))).intValue();
                lastRank = ranks[i];
            }
        }

        //store the results in ranked
        indices.append(new IntArray(ranks));

        return new IntArray(tUnique);

    }

    /**
     * This changes all instances of the first value to the second value.
     *
     * @param tFrom the original value (use "" or "NaN"  for standard missingValue)
     * @param tTo   the new value (use "" or "NaN"  for standard missingValue)
     * @return the number of values switched
     */
    public int switchFromTo(String tFrom, String tTo) {
        int from = Math2.roundToInt(String2.parseDouble(tFrom));
        int to   = Math2.roundToInt(String2.parseDouble(tTo));
        if (from == to)
            return 0;
        int count = 0;
        for (int i = 0; i < size; i++) {
            if (array[i] == from) {
                array[i] = to;
                count++;
            }
        }
        return count;
    }


    /**
     * This tests if the values in the array are sorted in ascending order (tied is ok).
     * The details of this test are geared toward determining if the 
     * values are suitable for binarySearch.
     *
     * @return "" if the values in the array are sorted in ascending order (or tied);
     *   or an error message if not (i.e., if descending or unordered).
     *   If size is 0 or 1 (non-missing value), this returns "".
     *   A missing value returns an error message.
     */
    public String isAscending() {
        if (size == 0)
            return "";
        for (int i = 1; i < size; i++) {
            if (array[i - 1] > array[i]) {
                return "IntArray isn't sorted in ascending order: [" + (i-1) + "]=" + array[i-1] + 
                    " > [" + i + "]=" + array[i] + ".";
            }
        }
        if (array[size - 1] == Integer.MAX_VALUE) 
            return "IntegerArray isn't sorted in ascending order: [" + (size-1) + 
                "]=(missing value).";
        return "";
    }

    /**
     * This tests if the values in the array are sorted in descending order (tied is ok).
     *
     * @return "" if the values in the array are sorted in descending order (or tied);
     *   or an error message if not (i.e., if ascending or unordered).
     *   If size is 0 or 1 (non-missing value), this returns "".
     *   A missing value returns an error message.
     */
    public String isDescending() {
        if (size == 0)
            return "";
        if (array[0] == Integer.MAX_VALUE) 
            return "IntegerArray isn't sorted in descending order: [0]=(missing value).";
        for (int i = 1; i < size; i++) {
            if (array[i - 1] < array[i]) {
                return "IntArray isn't sorted in descending order: [" + (i-1) + "]=" + array[i-1] + 
                    " < [" + i + "]=" + array[i] + ".";
            }
        }
        return "";
    }

    /**
     * This tests for adjacent tied values and returns the index of the first tied value.
     * Adjacent NaNs are treated as ties.
     *
     * @return the index of the first tied value (or -1 if none).
     */
    public int firstTie() {
        for (int i = 1; i < size; i++) {
            if (array[i - 1] == array[i]) {
                return i - 1;
            }
        }
        return -1;
    }

    /**
     * This tests if the values in the array are exactly evenly spaced (ascending or descending).
     *
     * @return "" if the values in the array are exactly evenly spaced;
     *   or an error message if not.
     *   If size is 0 or 1, this returns "".
     */
    public String isEvenlySpaced() {
        if (size <= 2)
            return "";
        int expected = array[1] - array[0];
        for (int i = 2; i < size; i++) {
            if (array[i] - array[i - 1] != expected) {
                return "IntArray isn't evenly spaced: #" + 
                    (i - 1) + "=" + array[i - 1] + 
                    ", #" + i + "=" + array[i] + 
                    ", spacing=" + (array[i] - array[i-1]) +
                    ", expected spacing=" + expected + ".";
            }
        }
        return "";
    }

    /** This returns the minimum value that can be held by this class. */
    public String minValue() {return "" + Integer.MIN_VALUE;}

    /** This returns the maximum value that can be held by this class. */
    public String maxValue() {return "" + (Integer.MAX_VALUE - 1);}

    /**
     * This finds the number of non-missing values, and the index of the min and
     *    max value.
     *
     * @return int[3], [0]=the number of non-missing values, 
     *    [1]=index of min value (if tie, index of last found; -1 if all mv),
     *    [2]=index of max value (if tie, index of last found; -1 if all mv).
     */
    public int[] getNMinMaxIndex() {
        int n = 0, tmini = -1, tmaxi = -1;
        int tmin = Integer.MAX_VALUE - 1;
        int tmax = Integer.MIN_VALUE;
        for (int i = 0; i < size; i++) {
            int v = array[i];
            if (v != Integer.MAX_VALUE) {
                n++;
                if (v <= tmin) {tmini = i; tmin = v; }
                if (v >= tmax) {tmaxi = i; tmax = v; }
            }
        }
        return new int[]{n, tmini, tmaxi};
    }

    /**
     * This tests the methods of this class.
     *
     * @throws Throwable if trouble.
     */
    public static void test() throws Throwable{
        String2.log("*** Testing IntArray");

        //** test default constructor and many of the methods
        IntArray anArray = new IntArray();
        Test.ensureEqual(anArray.size(), 0, "");
        anArray.add(2000000000);
        Test.ensureEqual(anArray.size(), 1, "");
        Test.ensureEqual(anArray.get(0), 2000000000, "");
        Test.ensureEqual(anArray.getInt(0), 2000000000, "");
        Test.ensureEqual(anArray.getFloat(0), 2000000000, "");
        Test.ensureEqual(anArray.getDouble(0), 2000000000, "");
        Test.ensureEqual(anArray.getString(0), "2000000000", "");
        Test.ensureEqual(anArray.elementClass(), int.class, "");
        int tArray[] = anArray.toArray();
        Test.ensureEqual(tArray, new int[]{2000000000}, "");

        //intentional errors
        try {anArray.get(1);              throw new Throwable("It should have failed.");} catch (Exception e) {}
        try {anArray.set(1, 100);         throw new Throwable("It should have failed.");} catch (Exception e) {}
        try {anArray.getInt(1);           throw new Throwable("It should have failed.");} catch (Exception e) {}
        try {anArray.setInt(1, 100);      throw new Throwable("It should have failed.");} catch (Exception e) {}
        try {anArray.getLong(1);          throw new Throwable("It should have failed.");} catch (Exception e) {}
        try {anArray.setLong(1, 100);     throw new Throwable("It should have failed.");} catch (Exception e) {}
        try {anArray.getFloat(1);         throw new Throwable("It should have failed.");} catch (Exception e) {}
        try {anArray.setFloat(1, 100);    throw new Throwable("It should have failed.");} catch (Exception e) {}
        try {anArray.getDouble(1);        throw new Throwable("It should have failed.");} catch (Exception e) {}
        try {anArray.setDouble(1, 100);   throw new Throwable("It should have failed.");} catch (Exception e) {}
        try {anArray.getString(1);        throw new Throwable("It should have failed.");} catch (Exception e) {}
        try {anArray.setString(1, "100"); throw new Throwable("It should have failed.");} catch (Exception e) {}

        //set NaN returned as NaN
        anArray.setDouble(0, Double.NaN);   Test.ensureEqual(anArray.getDouble(0), Double.NaN, ""); 
        anArray.setDouble(0, -1e300);       Test.ensureEqual(anArray.getDouble(0), Double.NaN, ""); 
        anArray.setDouble(0, 2.2);          Test.ensureEqual(anArray.getDouble(0), 2,          ""); 
        anArray.setFloat( 0, Float.NaN);    Test.ensureEqual(anArray.getFloat(0),  Float.NaN,  ""); 
        anArray.setFloat( 0, -1e33f);       Test.ensureEqual(anArray.getFloat(0),  Float.NaN,  ""); 
        anArray.setFloat( 0, 3.3f);         Test.ensureEqual(anArray.getFloat(0),  3,          ""); 
        anArray.setLong(0, Long.MAX_VALUE); Test.ensureEqual(anArray.getLong(0),   Long.MAX_VALUE, ""); 
        anArray.setLong(0, 9123456789L);    Test.ensureEqual(anArray.getLong(0),   Long.MAX_VALUE, ""); 
        anArray.setLong(0, 4);              Test.ensureEqual(anArray.getLong(0),   4, ""); 
        anArray.setInt(0,Integer.MAX_VALUE);Test.ensureEqual(anArray.getInt(0),    Integer.MAX_VALUE, ""); 
        anArray.setInt(0, 1123456789);      Test.ensureEqual(anArray.getInt(0),    1123456789, ""); 
        anArray.setInt(0, 5);               Test.ensureEqual(anArray.getInt(0),    5, ""); 


        //** test capacity constructor, test expansion, test clear
        anArray = new IntArray(2, false);
        Test.ensureEqual(anArray.size(), 0, "");
        for (int i = 0; i < 10; i++) {
            anArray.add(i);   
            Test.ensureEqual(anArray.get(i), i, "");
            Test.ensureEqual(anArray.size(), i+1, "");
        }
        Test.ensureEqual(anArray.size(), 10, "");
        anArray.clear();
        Test.ensureEqual(anArray.size(), 0, "");

        //active
        anArray = new IntArray(3, true);
        Test.ensureEqual(anArray.size(), 3, "");
        Test.ensureEqual(anArray.get(2), 0, "");


        //** test array constructor
        anArray = new IntArray(new int[]{0,2,4,6,8});
        Test.ensureEqual(anArray.size(), 5, "");
        Test.ensureEqual(anArray.get(0), 0, "");
        Test.ensureEqual(anArray.get(1), 2, "");
        Test.ensureEqual(anArray.get(2), 4, "");
        Test.ensureEqual(anArray.get(3), 6, "");
        Test.ensureEqual(anArray.get(4), 8, "");

        //test compare
        Test.ensureEqual(anArray.compare(1, 3), -4, "");
        Test.ensureEqual(anArray.compare(1, 1),  0, "");
        Test.ensureEqual(anArray.compare(3, 1),  4, "");

        //test toString
        Test.ensureEqual(anArray.toString(), "0, 2, 4, 6, 8", "");

        //test calculateStats
        anArray.addString("");
        double stats[] = anArray.calculateStats();
        anArray.remove(5);
        Test.ensureEqual(stats[STATS_N], 5, "");
        Test.ensureEqual(stats[STATS_MIN], 0, "");
        Test.ensureEqual(stats[STATS_MAX], 8, "");
        Test.ensureEqual(stats[STATS_SUM], 20, "");

        //test indexOf(int) indexOf(String)
        Test.ensureEqual(anArray.indexOf(0, 0),  0, "");
        Test.ensureEqual(anArray.indexOf(0, 1), -1, "");
        Test.ensureEqual(anArray.indexOf(8, 0),  4, "");
        Test.ensureEqual(anArray.indexOf(9, 0), -1, "");

        Test.ensureEqual(anArray.indexOf("0", 0),  0, "");
        Test.ensureEqual(anArray.indexOf("0", 1), -1, "");
        Test.ensureEqual(anArray.indexOf("8", 0),  4, "");
        Test.ensureEqual(anArray.indexOf("9", 0), -1, "");

        //test remove
        anArray.remove(1);
        Test.ensureEqual(anArray.size(), 4, "");
        Test.ensureEqual(anArray.get(0), 0, "");
        Test.ensureEqual(anArray.get(1), 4, "");
        Test.ensureEqual(anArray.get(3), 8, "");

        //test add(index, value)
        anArray.add(1, 22);
        Test.ensureEqual(anArray.size(), 5, "");
        Test.ensureEqual(anArray.get(0), 0, "");
        Test.ensureEqual(anArray.get(1),22, "");
        Test.ensureEqual(anArray.get(2), 4, "");
        Test.ensureEqual(anArray.get(4), 8, "");
        anArray.remove(1);

        //test removeRange
        anArray.removeRange(4, 4); //make sure it is allowed
        anArray.removeRange(1, 3);
        Test.ensureEqual(anArray.size(), 2, "");
        Test.ensureEqual(anArray.get(0), 0, "");
        Test.ensureEqual(anArray.get(1), 8, "");

        //test (before trimToSize) that toString, toDoubleArray, and toStringArray use 'size'
        Test.ensureEqual(anArray.toString(), "0, 8", "");
        Test.ensureEqual(anArray.toDoubleArray(), new double[]{0, 8}, "");
        Test.ensureEqual(anArray.toStringArray(), new String[]{"0", "8"}, "");

        //test trimToSize
        anArray.trimToSize();
        Test.ensureEqual(anArray.array.length, 2, "");

        //test equals
        IntArray anArray2 = new IntArray();
        anArray2.add(0); 
        Test.ensureEqual(anArray.testEquals("A String"), 
            "The two objects aren't equal: this object is a IntArray; the other is a java.lang.String.", "");
        Test.ensureEqual(anArray.testEquals(anArray2), 
            "The two IntArrays aren't equal: one has 2 values (from 0 to 8); the other has 1 value (0).", "");
        Test.ensureTrue(!anArray.equals(anArray2), "");
        anArray2.addString("7");
        Test.ensureEqual(anArray.testEquals(anArray2), 
            "The two IntArrays aren't equal: this[1]=8; other[1]=7.", "");
        Test.ensureTrue(!anArray.equals(anArray2), "");
        anArray2.setString(1, "8");
        Test.ensureEqual(anArray.testEquals(anArray2), "", "");
        Test.ensureTrue(anArray.equals(anArray2), "");

        //test toObjectArray
        Test.ensureEqual(anArray.toArray(), anArray.toObjectArray(), "");

        //test toDoubleArray
        Test.ensureEqual(anArray.toDoubleArray(), new double[]{0, 8}, "");

        //test reorder
        int rank[] = {1, 0};
        anArray.reorder(rank);
        Test.ensureEqual(anArray.toDoubleArray(), new double[]{8, 0}, "");


        //** test append and clone
        anArray = new IntArray(new int[]{1});
        anArray.append(new ByteArray(new byte[]{5, -5}));
        Test.ensureEqual(anArray.toDoubleArray(), new double[]{1, 5, -5}, "");
        anArray.append(new StringArray(new String[]{"a", "9"}));
        Test.ensureEqual(anArray.toDoubleArray(), new double[]{1, 5, -5, Double.NaN, 9}, "");
        anArray2 = (IntArray)anArray.clone();
        Test.ensureEqual(anArray2.toDoubleArray(), new double[]{1, 5, -5, Double.NaN, 9}, "");

        //test constructor(first, last)
        anArray = new IntArray(10, 15);
        Test.ensureEqual(anArray.toString(), "10, 11, 12, 13, 14, 15", "");

        //test move
        anArray = new IntArray(new int[]{0,1,2,3,4});
        anArray.move(1,3,0);
        Test.ensureEqual(anArray.toArray(), new int[]{1,2,0,3,4}, "");

        anArray = new IntArray(new int[]{0,1,2,3,4});
        anArray.move(1,2,4);
        Test.ensureEqual(anArray.toArray(), new int[]{0,2,3,1,4}, "");

        //move does nothing, but is allowed
        anArray = new IntArray(new int[]{0,1,2,3,4});
        anArray.move(1,1,0);
        Test.ensureEqual(anArray.toArray(), new int[]{0,1,2,3,4}, "");
        anArray.move(1,2,1);
        Test.ensureEqual(anArray.toArray(), new int[]{0,1,2,3,4}, "");
        anArray.move(1,2,2);
        Test.ensureEqual(anArray.toArray(), new int[]{0,1,2,3,4}, "");
        anArray.move(5,5,0);
        Test.ensureEqual(anArray.toArray(), new int[]{0,1,2,3,4}, "");
        anArray.move(3,5,5);
        Test.ensureEqual(anArray.toArray(), new int[]{0,1,2,3,4}, "");

        //makeIndices
        anArray = new IntArray(new int[] {25,1,1,10});
        IntArray indices = new IntArray();
        Test.ensureEqual(anArray.makeIndices(indices).toString(), "1, 10, 25", "");
        Test.ensureEqual(indices.toString(), "2, 0, 0, 1", "");

        anArray = new IntArray(new int[] {35,35,Integer.MAX_VALUE,1,2});
        indices = new IntArray();
        Test.ensureEqual(anArray.makeIndices(indices).toString(), "1, 2, 35, 2147483647", "");
        Test.ensureEqual(indices.toString(), "2, 2, 3, 0, 1", "");

        anArray = new IntArray(new int[] {10,20,30,40});
        Test.ensureEqual(anArray.makeIndices(indices).toString(), "10, 20, 30, 40", "");
        Test.ensureEqual(indices.toString(), "0, 1, 2, 3", "");

        //switchToFakeMissingValue
        anArray = new IntArray(new int[] {Integer.MAX_VALUE,1,2,Integer.MAX_VALUE,3,Integer.MAX_VALUE});
        Test.ensureEqual(anArray.switchFromTo("", "75"), 3, "");
        Test.ensureEqual(anArray.toString(), "75, 1, 2, 75, 3, 75", "");
        anArray.switchFromTo("75", "");
        Test.ensureEqual(anArray.toString(), "2147483647, 1, 2, 2147483647, 3, 2147483647", "");
        Test.ensureEqual(anArray.getNMinMaxIndex(), new int[]{3, 1, 4}, "");

        //addN
        anArray = new IntArray(new int[] {25});
        anArray.addN(2, 5);
        Test.ensureEqual(anArray.toString(), "25, 5, 5", "");
        Test.ensureEqual(anArray.getNMinMaxIndex(), new int[]{3, 2, 0}, "");

        //add array
        anArray.add(new int[]{17, 19});
        Test.ensureEqual(anArray.toString(), "25, 5, 5, 17, 19", "");

        //subset
        PrimitiveArray ss = anArray.subset(1, 3, 4);
        Test.ensureEqual(ss.toString(), "5, 19", "");
        ss = anArray.subset(0, 1, 0);
        Test.ensureEqual(ss.toString(), "25", "");
        ss = anArray.subset(0, 1, -1);
        Test.ensureEqual(ss.toString(), "", "");
        ss = anArray.subset(1, 1, 0);
        Test.ensureEqual(ss.toString(), "", "");

        //evenlySpaced
        anArray = new IntArray(new int[] {10,20,30});
        Test.ensureEqual(anArray.isEvenlySpaced(), "", "");
        anArray.set(2, 31);
        Test.ensureEqual(anArray.isEvenlySpaced(), 
            "IntArray isn't evenly spaced: #1=20, #2=31, spacing=11, expected spacing=10.", "");

        //isAscending
        anArray = new IntArray(new int[] {10,10,30});
        Test.ensureEqual(anArray.isAscending(), "", "");
        anArray.set(2, Integer.MAX_VALUE);
        Test.ensureEqual(anArray.isAscending(), 
            "IntegerArray isn't sorted in ascending order: [2]=(missing value).", "");
        anArray.set(1, 9);
        Test.ensureEqual(anArray.isAscending(), 
            "IntArray isn't sorted in ascending order: [0]=10 > [1]=9.", "");

        //isDescending
        anArray = new IntArray(new int[] {30,10,10});
        Test.ensureEqual(anArray.isDescending(), "", "");
        anArray.set(2, Integer.MAX_VALUE);
        Test.ensureEqual(anArray.isDescending(), 
            "IntArray isn't sorted in descending order: [1]=10 < [2]=2147483647.", "");
        anArray.set(1, 35);
        Test.ensureEqual(anArray.isDescending(), 
            "IntArray isn't sorted in descending order: [0]=30 < [1]=35.", "");

        //firstTie
        anArray = new IntArray(new int[] {30,35,10});
        Test.ensureEqual(anArray.firstTie(), -1, "");
        anArray.set(1, 30);
        Test.ensureEqual(anArray.firstTie(), 0, "");

        //hashcode
        anArray = new IntArray();
        for (int i = 5; i < 1000; i++)
            anArray.add(i);
        String2.log("hashcode1=" + anArray.hashCode());
        anArray2 = (IntArray)anArray.clone();
        Test.ensureEqual(anArray.hashCode(), anArray2.hashCode(), "");
        anArray.add(0, 2);
        Test.ensureTrue(anArray.hashCode() != anArray2.hashCode(), "");

        //justKeep
        BitSet bitset = new BitSet();
        anArray = new IntArray(new int[] {0, 11, 22, 33, 44});
        bitset.set(1);
        bitset.set(4);
        anArray.justKeep(bitset);
        Test.ensureEqual(anArray.toString(), "11, 44", "");

        //min max
        anArray = new IntArray();
        anArray.addString(anArray.minValue());
        anArray.addString(anArray.maxValue());
        Test.ensureEqual(anArray.getString(0), anArray.minValue(), "");
        Test.ensureEqual(anArray.getString(0), "-2147483648", "");
        Test.ensureEqual(anArray.getString(1), anArray.maxValue(), "");
    }

}
