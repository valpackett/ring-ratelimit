(ns ring.middleware.ratelimit.local-atom
  (:use [ring.middleware.ratelimit backend]))

(defn- update-state [state limit k]
  (assoc state k
    (if-let [v (state k)]
      (min limit (inc v))
      1)))

(deftype LocalAtomBackend [rate-map] Backend
  (get-limit [self limit k]
    ((swap! rate-map update-state limit k) k))
  (reset-limits! [self]
    (swap! rate-map (constantly {}))))

(defn local-atom-backend
  ([] (local-atom-backend (atom {})))
  ([rate-map] (LocalAtomBackend. rate-map)))
