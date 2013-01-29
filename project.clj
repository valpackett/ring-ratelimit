(defproject ring-ratelimit "0.1.0-SNAPSHOT"
  :description "Rate limit middleware for Ring"
  :url "https://github.com/myfreeweb/ring-ratelimit"
  :license {:name "WTFPL"
            :url "https://en.wikipedia.org/wiki/WTFPL"}
  :dependencies [[org.clojure/clojure "1.4.0"]]
  :plugins [[speclj "2.5.0"]]
  :bootclasspath true
  :profiles {:dev {:dependencies [[speclj "2.5.0"]
                                  [ring-mock "0.1.3"]
                                  [com.taoensso/carmine "1.3.0"]]}}
  :test-paths ["spec/"])
