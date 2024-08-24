/*
 * Licensed to ObjectStyle LLC under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ObjectStyle LLC licenses
 * this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.bootique.cayenne.v50.commitlog;

import io.bootique.BootiqueException;
import org.apache.cayenne.commitlog.CommitLogListener;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @since 3.0
 */
class CommitLogListenerGraph {

    static List<CommitLogListener> resolveAndSort(List<MappedCommitLogListener> unsorted) {
        if (unsorted.isEmpty()) {
            return Collections.emptyList();
        } else if (unsorted.size() == 1) {
            return List.of(unsorted.get(0).getListener());
        }

        // separately handling the case of no sorting (because of the sorting limitations below)
        boolean hasSort = false;
        for (MappedCommitLogListener listener : unsorted) {
            if (listener.getAfter() != null) {
                hasSort = true;
                break;
            }
        }

        if (!hasSort) {
            return unsorted.stream().map(MappedCommitLogListener::getListener).collect(Collectors.toList());
        }

        // Second-guessing the DI-resolved "after" instances. "After" types may not be declared as singletons, but we
        // need to resolve them to the List listeners... This will only work as long as each listener is of a unique
        // type

        Map<Class<?>, CommitLogListener> listenersByType = new HashMap<>();
        unsorted.forEach(l -> {
            if (listenersByType.put(l.getListener().getClass(), l.getListener()) != null) {
                throw new IllegalStateException(
                        "Can't sort a list of listeners when they are not of unique types. " +
                                "More than one instance found of " + l.getListener().getClass());
            }
        });

        CommitLogListenerGraph graph = new CommitLogListenerGraph(unsorted.size());
        unsorted.forEach(l -> graph.add(l.getListener()));
        unsorted.stream()
                .filter(l -> listenersByType.get(l.getAfter()) != null)
                .forEach(l -> graph.add(l.getListener(), listenersByType.get(l.getAfter())));

        return graph.topSort();
    }

    private final Map<CommitLogListener, List<CommitLogListener>> neighbors;

    CommitLogListenerGraph(int size) {
        neighbors = new LinkedHashMap<>(size);
    }

    void add(CommitLogListener vertex) {
        neighbors.putIfAbsent(vertex, new ArrayList<>(0));
    }

    void add(CommitLogListener from, CommitLogListener to) {
        neighbors.computeIfAbsent(from, k -> new ArrayList<>()).add(to);
        this.add(to);
    }

    private Map<CommitLogListener, Integer> inDegree() {

        Map<CommitLogListener, Integer> result = new LinkedHashMap<>();

        for (CommitLogListener v : neighbors.keySet()) {
            result.put(v, 0);
        }

        for (CommitLogListener from : neighbors.keySet()) {
            for (CommitLogListener to : neighbors.get(from)) {
                result.put(to, result.get(to) + 1);
            }
        }

        return result;
    }

    /**
     * Return (as a List) the topological sort of the vertices. Throws an exception if cycles are detected.
     */
    List<CommitLogListener> topSort() {
        Map<CommitLogListener, Integer> degree = inDegree();
        Deque<CommitLogListener> zeroDegree = new ArrayDeque<>(neighbors.size());
        List<CommitLogListener> result = new ArrayList<>(neighbors.size());

        degree.forEach((k, v) -> {
            if (v == 0) {
                zeroDegree.push(k);
            }
        });

        while (!zeroDegree.isEmpty()) {
            CommitLogListener v = zeroDegree.pop();
            result.add(v);

            neighbors.get(v).forEach(neighbor ->
                    degree.computeIfPresent(neighbor, (k, oldValue) -> {
                        int newValue = --oldValue;
                        if (newValue == 0) {
                            zeroDegree.push(k);
                        }
                        return newValue;
                    })
            );
        }

        // Check that we have used the entire graph (if not, there was a cycle)
        if (result.size() != neighbors.size()) {
            Set<CommitLogListener> remainingKeys = new HashSet<>(neighbors.keySet());
            String cycleString = remainingKeys.stream()
                    .filter(o -> !result.contains(o))
                    .map(l -> l.getClass().getSimpleName())
                    .collect(Collectors.joining(" -> "));
            throw new BootiqueException(1, "Circular override dependency between listeners: " + cycleString);
        }

        Collections.reverse(result);
        return result;
    }
}
