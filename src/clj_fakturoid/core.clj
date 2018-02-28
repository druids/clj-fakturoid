(ns clj-fakturoid.core
  (:require
    [clojure.string :refer [blank?]]
    [cemerick.url :as url]
    [clj-http.client :as http]))


(def http-opts
  {:as :json
   :accept :json
   :content-type :json
   :throw-exceptions false})
