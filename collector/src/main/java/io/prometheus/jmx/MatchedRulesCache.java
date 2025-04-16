/*
 * Copyright (C) 2020-present The Prometheus jmx_exporter Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.prometheus.jmx;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MatchedRulesCache is a cache for bean name to configured rule mapping (See
 * JmxCollector.Receiver). The cache also retains unmatched entries (a bean name not matching a rule
 * pattern) to avoid matching against the same pattern in later bean collections.
 */
public class MatchedRulesCache {

    private final Map<CacheKey, MatchedRule> cache;

    /** Constructs an empty cache */
    public MatchedRulesCache() {
        this.cache = new ConcurrentHashMap<>();
    }

    /**
     * Adds a rule match to the cache
     *
     * @param beanName
     * @param attributeName
     * @param matchedRule
     */
    public void put(
            final String beanName, final String attributeName, final MatchedRule matchedRule) {
        cache.put(new CacheKey(beanName, attributeName), matchedRule);
    }

    /**
     * Retrieves the cached MatchedRule
     *
     * @param beanName
     * @param attributeName
     * @return
     */
    public MatchedRule get(final String beanName, final String attributeName) {
        return cache.get(new CacheKey(beanName, attributeName));
    }

    /**
     * Method to remove stale rules (in the cache but not collected in the last run of the
     * collector)
     *
     * @param stalenessTracker stalenessTracker
     */
    public void evictStaleEntries(final StalenessTracker stalenessTracker) {
        for (CacheKey key : cache.keySet()) {
            if (!stalenessTracker.isFresh(key)) {
                cache.remove(key);
            }
        }
    }

    /**
     * Tracks which cache entries were touched during current scrape, so that all that were not can
     * be evicted from the cache
     */
    public static class StalenessTracker {

        private final Set<CacheKey> freshEntries;

        /** Constructor */
        public StalenessTracker() {
            this.freshEntries = new HashSet<>();
        }

        /** Marks a cache key as fresh (not stale) */
        public void markAsFresh(final String beanName, final String attributeName) {
            freshEntries.add(new CacheKey(beanName, attributeName));
        }

        /** Returns true if {@link #markAsFresh(String, String)) was called for that key */
        boolean isFresh(final CacheKey key) {
            return freshEntries.contains(key);
        }

        /**
         * Returns the number of fresh rules
         *
         * @return the number of fresh rules
         */
        public long freshCount() {
            return freshEntries.size();
        }
    }

    private static class CacheKey {
        private final String beanName;
        private final String attributeName;

        public CacheKey(String beanName, String attributeName) {
            this.beanName = beanName;
            this.attributeName = attributeName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CacheKey cacheKey = (CacheKey) o;
            return Objects.equals(beanName, cacheKey.beanName)
                    && Objects.equals(attributeName, cacheKey.attributeName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(beanName, attributeName);
        }
    }
}
