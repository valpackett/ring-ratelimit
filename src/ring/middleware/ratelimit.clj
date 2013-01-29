(ns ring.middleware.ratelimit
  (:import [java.util Date]))

(defn get-hour []
  (.getHours (Date.)))

(def rate-map (atom {}))
(def last-hour (atom (get-hour)))

(def default-config
  {:limit 100
   :err-handler (fn [req]
                  {:status 429
                   :headers {"Content-Type" "application/json"}
                   :body "{\"error\": \"Too Many Requests\"}"})})

(defn reset-ratelimit! []
  (swap! rate-map (constantly {})))

(defn- update-state [state limit k]
  (assoc state k
    (if-let [v (state k)] (min limit (inc v)) 1)))

(defn wrap-ratelimit
  ([handler] (wrap-ratelimit handler {}))
  ([handler config]
   (fn [req]
     (when (not= @last-hour (get-hour))
       (reset-ratelimit!)
       (swap! last-hour (fn [_] (get-hour))))
     (let [config* (merge default-config config)
           limit (:limit config*)
           err-handler (:err-handler config*)
           ip (:remote-addr req)
           current ((swap! rate-map update-state limit ip) ip)
           rl-headers {"X-RateLimit-Limit" (str limit)
                       "X-RateLimit-Remaining" (str (- limit current))}]
       (let [h (if (< current limit) handler err-handler)
             rsp (h req)]
         (assoc rsp :headers (merge (:headers rsp) rl-headers)))))))
