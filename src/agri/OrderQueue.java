package agri;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * CUSTOM QUEUE built on a plain CIRCULAR ARRAY (no java.util.Queue / LinkedList is used).
 *
 *   data  : [ o1 ][ o2 ][ o3 ][    ][    ]      fixed size = capacity
 *             ^front          ^rear
 *
 *   enqueue : rear  = (rear  + 1) % capacity   -> new order joins at the REAR
 *   dequeue : front = (front + 1) % capacity   -> the FRONT order is served first
 *
 * Modes
 *   FIFO mode     : first come, first served (a pure queue).
 *   Priority mode : Urgent(1) -> Bulk(2) -> Normal(3); inside the same priority it is still FIFO.
 *
 * Operations: enqueue, dequeue, peek (front), rear, size, isEmpty, isFull, remove (cancel), positionOf
 */
public class OrderQueue {
    public static final int DEFAULT_CAPACITY = 10;

    private final Models.Order[] data;
    private final int capacity;
    private int front = 0;      // index of the first order
    private int rear = -1;      // index of the last order (-1 = nothing inserted yet)
    private int count = 0;      // size
    private boolean priorityMode = true;

    public OrderQueue() { this(DEFAULT_CAPACITY); }
    public OrderQueue(int capacity) { this.capacity = capacity; this.data = new Models.Order[capacity]; }

    public boolean isPriorityMode() { return priorityMode; }
    public int size() { return count; }
    public int capacity() { return capacity; }
    public boolean isEmpty() { return count == 0; }
    public boolean isFull() { return count == capacity; }
    public int frontIndex() { return front; }
    public int rearIndex() { return rear; }

    /** Array index of the k-th order counted from the front (k = 0 is the front). */
    private int idx(int k) { return (front + k) % capacity; }

    /** Adds a new order. Throws IllegalStateException("Queue Overflow") when the array is full. */
    public void enqueue(Models.Order o) {
        if (isFull()) throw new IllegalStateException("Queue Overflow: the queue is full (" + capacity + " orders)");
        int pos = count;                                           // normal queue: join at the rear
        if (priorityMode)                                          // priority mode: find the right place
            while (pos > 0 && data[idx(pos - 1)].priority > o.priority) pos--;
        for (int i = count; i > pos; i--) data[idx(i)] = data[idx(i - 1)];   // shift orders towards the rear
        data[idx(pos)] = o;
        rear = (rear + 1) % capacity;
        count++;
    }

    /** Removes and returns the FRONT order. Returns null when the queue is empty (underflow). */
    public Models.Order dequeue() {
        if (isEmpty()) return null;
        Models.Order o = data[front];
        front = (front + 1) % capacity;       // only the pointer moves, the old slot stays "stale"
        count--;
        return o;
    }

    /** Returns the FRONT order without removing it (null when empty). */
    public Models.Order peek() { return isEmpty() ? null : data[front]; }

    /** Returns the REAR (last) order without removing it (null when empty). */
    public Models.Order rear() { return isEmpty() ? null : data[rear]; }

    /** Cancel: removes an order from the middle of the queue and closes the gap. */
    public boolean remove(int orderId) {
        for (int k = 0; k < count; k++) {
            if (data[idx(k)].id == orderId) {
                for (int i = k; i < count - 1; i++) data[idx(i)] = data[idx(i + 1)];
                data[idx(count - 1)] = null;
                rear = (rear - 1 + capacity) % capacity;
                count--;
                return true;
            }
        }
        return false;
    }

    /** Position of an order in the queue (1 = front), or -1. */
    public int positionOf(int orderId) {
        for (int k = 0; k < count; k++) if (data[idx(k)].id == orderId) return k + 1;
        return -1;
    }

    public List<Models.Order> toList() {
        List<Models.Order> l = new ArrayList<>();
        for (int k = 0; k < count; k++) l.add(data[idx(k)]);
        return l;
    }

    // ---- raw array access, used only to draw the "array inspector" on the Queue page
    public Models.Order slot(int i) { return data[i]; }
    public boolean isActive(int i) { return ((i - front + capacity) % capacity) < count; }

    public void clear() { Arrays.fill(data, null); front = 0; rear = -1; count = 0; }

    /** Changes the mode; the queue is rebuilt in arrival (id) order. */
    public void setPriorityMode(boolean pm) {
        List<Models.Order> all = toList();
        all.sort((a, b) -> Integer.compare(a.id, b.id));
        clear();
        priorityMode = pm;
        for (Models.Order o : all) enqueue(o);
    }
}
