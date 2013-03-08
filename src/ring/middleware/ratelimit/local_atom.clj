(ns ring.middleware.ratelimit.local-atom
  (:use [ring.middleware.ratelimit util backend]))

(defn- update-state [state limit k]
  (assoc state k
    (if-let [v (state k)]
      (min limit (inc v))
      1)))

(deftype LocalAtomBackend [rate-map hour-atom] Backend
  (get-limit [self limit k]
    ((swap! rate-map update-state limit k) k))
  (reset-limits! [self hour]
    (swap! rate-map (constantly {}))
    (swap! hour-atom (fn [_] hour)))
  (get-hour [self] @hour-atom)
  (available? [self] true))

(defn local-atom-backend
  ([] (local-atom-backend (atom {})))
  ([rate-map] (local-atom-backend rate-map (atom (current-hour))))
  ([rate-map hour-atom] (LocalAtomBackend. rate-map hour-atom)))
