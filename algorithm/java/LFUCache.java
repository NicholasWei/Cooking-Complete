package algorithm.java;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * LFU(Least Frequently Used，最不经常使用)缓存算法的实现。
 *
 * <p>当缓存达到容量上限时，淘汰访问次数最少的元素；若存在多个访问次数相同的元素，
 * 则淘汰其中最久未被使用的那个（即在相同频率下退化为 LRU）。</p>
 *
 * <p>本实现的 {@link #get(int)} 与 {@link #put(int, int)} 操作的平均时间复杂度均为 O(1)。
 * 核心思路：</p>
 * <ul>
 *   <li>{@code keyToNode}：保存 key -> 节点(值、频率) 的映射，用于 O(1) 查找。</li>
 *   <li>{@code freqToKeys}：保存 频率 -> 该频率下所有 key 的有序集合(LinkedHashSet)，
 *       LinkedHashSet 既能 O(1) 增删，又能维持插入顺序以支持同频率下的 LRU 淘汰。</li>
 *   <li>{@code minFreq}：记录当前缓存中最小的访问频率，淘汰时直接定位到该频率集合的头部。</li>
 * </ul>
 */
public class LFUCache {

    /** 缓存节点，保存值以及该 key 当前的访问频率。 */
    private static class Node {
        int value;
        int freq;

        Node(int value, int freq) {
            this.value = value;
            this.freq = freq;
        }
    }

    private final int capacity;
    private int minFreq;
    private final Map<Integer, Node> keyToNode;
    private final Map<Integer, LinkedHashSet<Integer>> freqToKeys;

    public LFUCache(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity must be non-negative");
        }
        this.capacity = capacity;
        this.minFreq = 0;
        this.keyToNode = new HashMap<>();
        this.freqToKeys = new HashMap<>();
    }

    /**
     * 获取 key 对应的值，并将其访问频率加一。
     *
     * @return 若 key 存在返回对应的值，否则返回 -1。
     */
    public int get(int key) {
        Node node = keyToNode.get(key);
        if (node == null) {
            return -1;
        }
        increaseFreq(key, node);
        return node.value;
    }

    /**
     * 写入或更新 key 对应的值。
     *
     * <p>若 key 已存在则更新其值并将频率加一；否则插入新元素。
     * 当容量已满时，先淘汰当前频率最低（同频率则最久未使用）的元素。</p>
     */
    public void put(int key, int value) {
        if (capacity == 0) {
            return;
        }

        Node node = keyToNode.get(key);
        if (node != null) {
            node.value = value;
            increaseFreq(key, node);
            return;
        }

        if (keyToNode.size() >= capacity) {
            evict();
        }

        Node newNode = new Node(value, 1);
        keyToNode.put(key, newNode);
        freqToKeys.computeIfAbsent(1, k -> new LinkedHashSet<>()).add(key);
        minFreq = 1;
    }

    /** 将指定 key 的频率从 f 提升到 f+1，并维护频率桶与 minFreq。 */
    private void increaseFreq(int key, Node node) {
        int freq = node.freq;
        LinkedHashSet<Integer> keys = freqToKeys.get(freq);
        keys.remove(key);
        if (keys.isEmpty()) {
            freqToKeys.remove(freq);
            if (minFreq == freq) {
                minFreq++;
            }
        }

        node.freq = freq + 1;
        freqToKeys.computeIfAbsent(node.freq, k -> new LinkedHashSet<>()).add(key);
    }

    /** 淘汰当前最小频率桶中最久未使用的元素（LinkedHashSet 的第一个元素）。 */
    private void evict() {
        LinkedHashSet<Integer> keys = freqToKeys.get(minFreq);
        int evictKey = keys.iterator().next();
        keys.remove(evictKey);
        if (keys.isEmpty()) {
            freqToKeys.remove(minFreq);
        }
        keyToNode.remove(evictKey);
    }

    /** 当前缓存中的元素个数。 */
    public int size() {
        return keyToNode.size();
    }

    public static void main(String[] args) {
        // 演示用例，对应 LeetCode 460. LFU 缓存 的示例。
        LFUCache cache = new LFUCache(2);
        cache.put(1, 1);
        cache.put(2, 2);
        System.out.println(cache.get(1));   // 返回 1 (key=1 频率变为 2)
        cache.put(3, 3);                     // 淘汰 key=2 (频率最低)
        System.out.println(cache.get(2));   // 返回 -1 (已被淘汰)
        System.out.println(cache.get(3));   // 返回 3 (key=3 频率变为 2)
        cache.put(4, 4);                     // key=1 与 key=3 频率同为 2，淘汰更久未用的 key=1
        System.out.println(cache.get(1));   // 返回 -1 (已被淘汰)
        System.out.println(cache.get(3));   // 返回 3
        System.out.println(cache.get(4));   // 返回 4
    }
}
