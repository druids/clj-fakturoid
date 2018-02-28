(ns clj-fakturoid-test.fake-http
  (:require
    [cheshire.core :as cheshire]))


(defn json-handler
  "A helper function for defining fake routes. It returns a single route (a hash-map) that returns a given `body`
   as JSON."
  ([host path body]
   (json-handler host path body 200))
  ([host path body status]
   (json-handler host path body status :get))
  ([host path body status method]
   {(str host path)
    {method (fn [_] {:status status, :body (cheshire/generate-string body)})}}))
