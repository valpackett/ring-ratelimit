(ns ring.middleware.ratelimit.limits)

(defn limit [n]
  {:limit n
   :key-prefix ""
   :filter (constantly true)
   :getter (constantly "")})

(defn wrap-limit-ip [lim]
  (merge lim
         {:key-prefix (str (:key-prefix lim) "I")
          :getter #(str (:remote-addr %) ((lim :getter) %))}))

(defn wrap-limit-header [lim hdr]
  (merge lim
         {:key-prefix (str (:key-prefix lim) "H" hdr)
          :filter #(and (get-in % [:headers hdr]) ((:filter lim) %))
          :getter #(str (get-in % [:headers hdr]) ((:getter lim) %))}))

(defn wrap-limit-param [lim par]
  (merge lim
         {:key-prefix (str (:key-prefix lim) "P" par)
          :filter #(and (get-in % [:params par]) ((:filter lim) %))
          :getter #(str (get-in % [:params par]) ((:getter lim) %))}))

(defn wrap-limit-user [lim]
  (merge lim
         {:key-prefix (str (:key-prefix lim) "U")
          :filter #(and (-> % :session :cemerick.friend/identity :current)
                        ((:filter lim) %))
          :getter #(str (-> % :session :cemerick.friend/identity :current)
                        ((:getter lim) %))}))

(defn wrap-limit-role [lim role]
  (merge lim
         {:key-prefix (str (:key-prefix lim) "R" (name role))
          :filter #(and (when-let [cur (-> % :session :cemerick.friend/identity :current)]
                          (contains? (get-in % [:session :cemerick.friend/identity
                                                :authentications cur :roles]) role))
                        ((:filter lim) %))
          :getter #(str (-> % :session :cemerick.friend/identity :current)
                        ((:getter lim) %))}))
