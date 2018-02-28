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


(defn- ->http-opts
  [credentials]
  (let [[username token] credentials]
    (merge http-opts
           {:basic-auth [username token]
            :user-agent (format "clj-fakturoid (%s)" username)})))


(defn- parse-4xx-response
  "Parses a :body for all 4xx responses as JSON (with keywords). If a parser throw an exception it sets :body to `nil`
   and unparsed body is `assoc`ed into :body-unparsed.
   Thus calling any collection modifier on :body will not throw an exception."
  [response]
  (if (and (<= 400 (:status response))
           (< (:status response) 500))
    (try
      (update response :body http/json-decode true)
      (catch Exception e (-> response
                             (assoc :body-unparsed (:body response))
                             (assoc :body nil))))
    response))


(defn get-account
  "Returns an account by a given `credentials` and `slug`.
   Where `credentials` is a tuple with `username` and API `token`."
  [host credentials slug]
  {:pre [(not (blank? host))]}
  (let [[username token] credentials]
    (-> host
        str
        url/url
        (update :path str "/accounts/" slug "/account.json")
        str
        (http/get (->http-opts credentials))
        parse-4xx-response)))
