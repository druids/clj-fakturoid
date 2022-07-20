(defproject clj-fakturoid "0.2.0"
  :description "A client for Fakturoid API based on clj-http.client"
  :url "https://github.com/druids/clj-fakturoid"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"}

  :dependencies [[clj-http "3.7.0"]
                 [com.cemerick/url "0.1.1"]
                 [cheshire "5.8.0"]
                 [org.clojure/data.xml "0.2.0-alpha6"]]

  :profiles {:dev {:plugins [[lein-cloverage "1.0.10"]
                             [lein-kibit "0.1.6"]]
                   :dependencies [[clj-http-fake "1.0.3"]
                                  [org.clojure/clojure "1.9.0"]]
                   :source-paths ["src" "dev/src"]}})
