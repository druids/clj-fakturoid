(ns clj-fakturoid.core
  (:require
   [cemerick.url :as url]
   [clj-http.client :as http]
   [clojure.data.xml :as xml]
   [clojure.java.io :as io]
   [clojure.string :refer [blank?]]
   [clojure.data :as data])
  (:import [java.lang Math]
           [java.time LocalDate]
           [java.time.format DateTimeFormatter]
           [java.time.temporal TemporalAdjusters]))

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
  (-> host
      str
      url/url
      (update :path str "/accounts/" slug "/account.json")
      str
      (http/get (->http-opts credentials))
      parse-4xx-response))

(defn create-subject
  "Create new `subject` in address book"
  [host credentials slug subject]
  {:pre [(not (blank? host))]}
  (-> host
      str
      url/url
      (update :path str "/accounts/" slug "/subjects.json")
      str
      (http/post (merge (->http-opts credentials)
                        {:form-params subject}))
      parse-4xx-response))

(defn create-invoice
  "Create new `invoice` in address book"
  [host credentials slug invoice]
  {:pre [(not (blank? host))]}
  (-> host
      str
      url/url
      (update :path str "/accounts/" slug "/invoices.json")
      str
      (http/post (merge (->http-opts credentials)
                        {:form-params invoice}))
      parse-4xx-response))

(defn get-invoices
  [host credentials slug query]
  {:pre [(not (blank? host))]}
  (-> host
      str
      url/url
      ;; (update :path str "/" slug "/search/invoices/results")
      (update :path str "/accounts/" slug "/invoices.json")
      ;; (assoc :query
      ;;        {"invoice_search_params[date_column]" "taxable_fulfillment_due"
      ;;         "invoice_search_params[range_from]" "01. 04. 2022",
      ;;         "invoice_search_params[range_to]" "30. 04. 2022",})
      str
      (http/get (merge (->http-opts credentials)
                       {:query-params query}))
      parse-4xx-response
      :body))

(defn in-range?
  "Returns `true` if a given point is in the range
  (<= since point until)"
  [since until point]
  (and (or (.isBefore since point)
           (= since point))
       (.isBefore point until)))

(defn filter-by-taxable-fulfillment-due
  [since until coll]
  (filter (comp (partial in-range? since until) #(LocalDate/parse %) :taxable_fulfillment_due) coll))

(defn math-round
  [n]
  (Math/round (float (with-precision 2 n))))

(defn- calculate-sums
  [invoices]
  (reduce (fn [acc {:keys [native_subtotal native_total]}]
            (-> acc
                (update :total + (bigdec native_subtotal))
                (update :tax + (- (bigdec native_total)
                                  (bigdec native_subtotal)))))
          {:total 0M, :tax 0M}
          invoices))

(defn dphdp3-report
  [personal-data today since small-costs invoices]
  (let [{:keys [total tax]} (calculate-sums invoices)
        {:keys [cost-base cost-tax]}
        (reduce (fn [acc {:keys [base tax]}]
                  (-> acc
                      (update :cost-base + base)
                      (update :cost-tax + tax)))
                {:cost-base 0M, :cost-tax 0M}
                small-costs)]
    (merge (assoc personal-data
                  :today today
                  :since since
                  :veta1 {:obrat23 (math-round total)
                          :dan23 (math-round tax)}
                  :veta6 {:dan_zocelk (math-round tax)
                          :dano 0
                          :dano_da (math-round (- tax cost-tax))
                          :dano_no 0
                          :odp_zocelk (math-round cost-tax)})
           (when (pos? cost-tax)
             {:veta4 {:odp_sum_nar (math-round cost-tax)
                      :odp_tuz23_nar (math-round cost-tax)
                      :pln23 (math-round cost-base)}}))))

(defn dphkh1-report
  [personal-data today since small-costs invoices]
  (let [{:keys [total]} (calculate-sums invoices)
        veta-b3 (when (seq small-costs)
                  (reduce (fn [acc {:keys [base tax]}]
                            (-> acc
                                (update :zakl_dane1 + base)
                                (update :dan1 + tax)))
                          {:zakl_dane1 0M
                           :dan1 0M}
                          small-costs))]
    (assoc personal-data
           :today today
           :since since
           :veta-a4 (map-indexed (fn [i invoice]
                                   {:c_radku (inc i)
                                    :dic_odb (:client_vat_no invoice)
                                    :c_evid_dd (:number invoice)
                                    :dppd (LocalDate/parse (:taxable_fulfillment_due invoice))
                                    :zakl_dane1 (:native_subtotal invoice)
                                    :dan1 (- (bigdec (:native_total invoice))
                                             (bigdec (:native_subtotal invoice)))
                                    :kod_rezim_pl "0"
                                    :zdph_44 "N"})
                                 invoices)
           :veta-c {:obrat23 total
                    :pln23 (if (some? veta-b3) (:zakl_dane1 veta-b3) 0)}
           :veta-b3 veta-b3)))


(def date-formatter (DateTimeFormatter/ofPattern "dd.MM.YYYY"))

(defn format-date
  [date]
  (.format date date-formatter))

(def vetad-dp3-keys
  [:c_okec :d_poddp :dapdph_forma :dokument :k_uladis :mesic :rok :trans :typ_platce])

(def vetad-kh1-keys
  [:d_poddp :dapdph_forma :dokument :k_uladis :mesic :rok :trans :typ_platce])

(def vetap-keys
  [:c_orient :c_pop :c_telef :c_ufo :c_pracufo :dic :email :jmeno :naz_obce :prijmeni :psc :stat :titul :typ_ds :ulice])

(defn dphdp3-report-xml
  [{:keys [veta1 veta6 veta4 today since] :as personal-info}]
  (let [until
        (.with since
               (TemporalAdjusters/lastDayOfMonth))]
    {:tag :Pisemnost
     :attrs {:nazevSW "EPO MF ČR" :verzeSW "42.7.1"}
     :content
     [{:tag :DPHDP3
       :attrs {:verzePis "01.02"}
       :content (concat [{:tag :VetaD, :attrs (merge {:zdobd_od (format-date since)
                                                      :zdobd_do (format-date until)
                                                      :dapdph_forma "B"
                                                      :d_poddp (format-date today)
                                                      :k_uladis "DPH"
                                                      :typ_platce "P"
                                                      :dokument "DP3"
                                                      :trans "A"
                                                      :mesic (-> since .getMonth .getValue)
                                                      :rok (.getYear today)}
                                                     (select-keys personal-info vetad-dp3-keys))
                          :content []}
                         {:tag :VetaP, :attrs (merge {:typ_ds "F"}
                                                     (select-keys personal-info vetap-keys))
                          :content []}
                         {:tag :Veta1, :attrs veta1, :content []}]
                        (when (some? veta4)
                          [{:tag :Veta4, :attrs veta4, :content []}])
                        [{:tag :Veta6, :attrs veta6, :content []}])}]}))

(defn dphkh1-report-xml
  [{:keys [vetap vetad veta-a4 veta-c today since veta-b3] :as personal-info}]
  (let [until
        (.with since
               (TemporalAdjusters/lastDayOfMonth))]
    {:tag :Pisemnost
     :attrs {:nazevSW "EPO MF ČR" :verzeSW "42.7.1"}
     :content
     [{:tag :DPHKH1
       :attrs {:verzePis "03.01"}
       :content (concat [{:tag :VetaD, :attrs (merge {:dokument "KH1"
                                                      :k_uladis "DPH"
                                                      :mesic (-> since .getMonth .getValue)
                                                      :rok (.getYear today)
                                                      :zdobd_od (format-date since)
                                                      :zdobd_do (format-date until)
                                                      :d_poddp (format-date today)
                                                      :khdph_forma "B"}
                                                    (select-keys personal-info vetad-kh1-keys))}
                         {:tag :VetaP, :attrs (merge {:typ_ds "F"}
                                                     (select-keys personal-info vetap-keys))}]
                        (map (fn [row]
                               {:tag :VetaA4
                                :attrs (-> row
                                           (update :dic_odb subs 2)
                                           (update :dppd format-date))})
                             veta-a4)
                        (when veta-b3
                          [{:tag :VetaB3
                            :attrs veta-b3}])
                        [{:tag :VetaC
                          :attrs veta-c}])}]}))

(defn report-tax-filter
  [now]
  (let [today (now)
        since (.with (.minusMonths today 1)
                     (TemporalAdjusters/firstDayOfMonth))]
    {:today today
     :since since
     :until (.plusMonths since 1)
     :query
     {"sort[column]" "taxable_fulfillment_due"
      "sort[order]" "desc"}}))

(defn generate-report
  [generate-fn {:keys [host username token slug]} personal-data small-costs output-path now]
  (let [{:keys [today since until query]}
        (report-tax-filter now)]
    (->> (get-invoices host [username token] slug query)
         (filter-by-taxable-fulfillment-due since until)
         (generate-fn personal-data today since small-costs)
         xml/emit-str
         (spit output-path))
    output-path))

(def generate-dphdp3-report
  (partial generate-report (comp dphdp3-report-xml dphdp3-report)))

(def generate-dphkh1-report
  (partial generate-report (comp dphkh1-report-xml dphkh1-report)))
