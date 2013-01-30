(ns ring.middleware.ratelimit-spec
  (:import [java.util Date])
  (:require [clojure.string :as string]
            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds]
                             [openid :as openid]))
  (:use [speclj core]
        [ring.middleware ratelimit]
        [ring.middleware.ratelimit util backend local-atom redis]
        [ring.mock request]))

(doseq [backend [(local-atom-backend) (redis-backend)]]
  (let [app (-> (fn [req] {:status 418
                           :headers {"Content-Type" "air/plane"}
                           :body "Hello"})
                (wrap-ratelimit {:limits [(ip-limit 5)]
                                 :backend backend}))]

    (describe (str "ratelimit <" (last (string/split (str (type backend)) #"\.")) ">")
      (before
        (reset-limits! backend (current-hour)))

      (it "shows the rate limit"
        (let [rsp (-> (request :get "/") app)]
          (should= 418 (:status rsp))
          (should= "5" (get-in rsp [:headers "X-RateLimit-Limit"]))
          (should= "4" (get-in rsp [:headers "X-RateLimit-Remaining"]))))

      (it "returns 429 when there are no requests left"
        (dotimes [_ 5] (-> (request :get "/") app))
        (let [rsp (-> (request :get "/") app)]
          (should= 429 (:status rsp))
          (should= "0" (get-in rsp [:headers "X-RateLimit-Remaining"]))))

      (it "resets the limit every hour"
        (with-redefs [current-hour (fn [] (- (.getHours (Date.)) 1))]
          (dotimes [_ 5] (-> (request :get "/") app))
          (should= 429 (-> (request :get "/") app :status)))
        (should= 418 (-> (request :get "/") app :status))))))

; why do I test with real friend?

(let [api-users {"api-key" {:username "api-key"
                            :password (creds/hash-bcrypt "api-pass")
                            :roles #{:api}}
                 "admin" {:username "admin"
                          :password (creds/hash-bcrypt "admin-pass")
                          :roles #{:admin}}}
      app (-> (fn [req] {:status 200
                         :headers {"Content-Type" "text/plain"}
                         :body (with-out-str (pr req))})
              (wrap-ratelimit {:limits [(role-limit :admin 20) (user-limit 10) (ip-limit 5)]
                               :backend (local-atom-backend)})
              (friend/authenticate {:allow-anon? true
                                    :workflows [(workflows/http-basic
                                                  :credential-fn (partial creds/bcrypt-credential-fn api-users)
                                                  :realm "test-realm")]}))]
  (describe "ratelimit with 3 limiters"
    (it "uses the admin limit for admins"
      (let [rsp (-> (request :get "/")
                    (header "Authorization" "Basic YWRtaW46YWRtaW4tcGFzcw==")
                    app)]
        (should= 200 (:status rsp))
        (should= "19" (get-in rsp [:headers "X-RateLimit-Remaining"]))))
    (it "uses the user limit for authenticated requests"
      (let [rsp (-> (request :get "/")
                    (header "Authorization" "Basic YXBpLWtleTphcGktcGFzcw==")
                    app)]
        (should= 200 (:status rsp))
        (should= "9" (get-in rsp [:headers "X-RateLimit-Remaining"]))))
    (it "uses the ip limit for unauthenticated requests"
      (let [rsp (-> (request :get "/") app)]
        (should= 200 (:status rsp))
        (should= "4" (get-in rsp [:headers "X-RateLimit-Remaining"]))))))
