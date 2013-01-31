(ns ring.middleware.ratelimit
  (:use [ring.middleware.ratelimit util backend local-atom]))

(defn ip-limit [n]
  {:limit n
   :key-prefix "IP"
   :getter :remote-addr})

(defn user-limit [n]
  {:limit n
   :key-prefix "U"
   :getter #(-> % :session :cemerick.friend/identity :current)})

(defn role-limit [r n]
  {:limit n
   :key-prefix (str "U" (name r))
   :getter #(when-let [cur (-> % :session :cemerick.friend/identity :current)]
              (when (contains? (get-in % [:session :cemerick.friend/identity :authentications cur :roles]) r)
                cur))})

(defn default-config []
  {:limits [(ip-limit 100)]
   :backend (local-atom-backend)
   :err-handler (fn [req]
                  {:status 429
                   :headers {"Content-Type" "application/json"}
                   :body "{\"error\": \"Too Many Requests\"}"})})

(defn wrap-ratelimit
  ([handler] (wrap-ratelimit handler {}))
  ([handler config]
   (let [config* (merge (default-config) config)
         limits (:limits config*)
         backend (:backend config*)
         err-handler (:err-handler config*)]
     (fn [req]
       (when (not= (get-hour backend) (current-hour))
         (reset-limits! backend (current-hour)))
       (let [limiter (first (filter #(not (nil? ((:getter %) req))) limits))
             limit (:limit limiter)
             thekey (str (:key-prefix limiter) ((:getter limiter) req))
             current (get-limit backend limit thekey)
             rl-headers {"X-RateLimit-Limit" (str limit)
                         "X-RateLimit-Remaining" (str (- limit current))}
             h (if (< current limit) handler err-handler)
             rsp (h req)]
         (assoc rsp :headers (merge (:headers rsp) rl-headers)))))))
