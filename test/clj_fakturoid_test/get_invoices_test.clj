(ns clj-fakturoid-test.get-invoices-test
  (:require [clojure.test :as t]
            [clj-fakturoid.core :as fakturoid])
  (:import [java.time LocalDate]))

(def invoices
  [{:taxable_fulfillment_due "2022-04-30"
    :client_vat_no "CZ12345678"
    :number "VF1-00022022"
    :subtotal "1000.0"
    :remaining_native_amount "1210.0"
    :total "1210.0"
    :native_subtotal "1000.0"
    :native_total "1210.0"}
   {:taxable_fulfillment_due "2022-04-30"
    :client_vat_no "CZ12345678"
    :number "VF1-00012022"
    :subtotal "1000.0"
    :remaining_native_amount "1210.0"
    :total "1210.0"
    :native_subtotal "1000.0"
    :native_total "1210.0"}])

(def personal-data
  {:c_okec "620000"
   :c_orient "1"
   :c_pop "2"
   :c_telef "987654321"
   :c_ufo "123"
   :c_pracufo "2000"
   :dic "9877654432"
   :email "email@example.com"
   :jmeno "Lars"
   :naz_obce "PRAHA 1"
   :prijmeni "Larsson"
   :psc "11000"
   :stat "ČESKÁ REPUBLIKA"
   :titul ""
   :ulice "Ulice"})

(def dphdp3
  {:ulice "Ulice"
   :naz_obce "PRAHA 1"
   :since (LocalDate/of 2022 4 1)
   :email "email@example.com"
   :stat "ČESKÁ REPUBLIKA"
   :c_telef "987654321"
   :dic "9877654432"
   :titul ""
   :c_pop "2"
   :c_orient "1"
   :c_okec "620000"
   :jmeno "Lars"
   :today (LocalDate/of 2022 5 12)
   :psc "11000"
   :veta6
   {:dan_zocelk 420
    :dano 0
    :dano_da 420
    :dano_no 0
    :odp_zocelk 0}
   :c_ufo "123"
   :prijmeni "Larsson"
   :c_pracufo "2000"
   :veta1 {:obrat23 2000 :dan23 420}})

(def today (LocalDate/of 2022 5 12))

(def since (LocalDate/of 2022 4 1))

(t/deftest dphdp3-report-test
  (t/is (= dphdp3
           (fakturoid/dphdp3-report personal-data
                                    today
                                    since
                                    invoices))))

(t/deftest dphdp3-report-xml-test
  (let [expected-report
        {:tag :Pisemnost
         :attrs {:nazevSW "EPO MF ČR", :verzeSW "42.7.1"}
         :content
         [{:tag :DPHDP3
           :attrs {:verzePis "01.02"}
           :content
           [{:tag :VetaD
             :attrs {:dapdph_forma "B"
                     :d_poddp "12.05.2022"
                     :k_uladis "DPH"
                     :zdobd_do "30.04.2022"
                     :typ_platce "P"
                     :c_okec "620000"
                     :zdobd_od "01.04.2022"
                     :dokument "DP3"
                     :trans "A"
                     :mesic 4
                     :rok 2022}
             :content []}
            {:tag :VetaP
             :attrs
             {:ulice "Ulice"
              :naz_obce "PRAHA 1"
              :email "email@example.com"
              :stat "ČESKÁ REPUBLIKA"
              :c_telef "987654321"
              :dic "9877654432"
              :titul ""
              :c_pop "2"
              :c_orient "1"
              :jmeno "Lars"
              :typ_ds "F"
              :psc "11000"
              :c_ufo "123"
              :prijmeni "Larsson"
              :c_pracufo "2000"}
             :content []}
            {:tag :Veta1
             :attrs {:obrat23 2000
                     :dan23 420}
             :content []}
            {:tag :Veta4
             :attrs {:odp_sum_nar 0}
             :content []}
            {:tag :Veta6
             :attrs {:dan_zocelk 420
                     :dano 0
                     :dano_da 420
                     :dano_no 0
                     :odp_zocelk 0}
             :content []}]}]}]
    (t/is (= expected-report
             (fakturoid/dphdp3-report-xml dphdp3)))))

(t/deftest is-range?-test
  (t/testing "should be in range"
    (t/are [point] (fakturoid/in-range? (LocalDate/of 2022 4 1)
                                        (LocalDate/of 2022 4 30)
                                        point)
      (LocalDate/of 2022 4 1)
      (LocalDate/of 2022 4 10)))

  (t/testing "should not be in range"
    (t/are [point] (not (fakturoid/in-range? (LocalDate/of 2022 4 1)
                                             (LocalDate/of 2022 4 30)
                                             point))
      (LocalDate/of 2022 3 30)
      (LocalDate/of 2022 4 30)
      (LocalDate/of 2022 5 10))))

(t/deftest filter-by-taxable-fulfillment-due-test
  (t/is (= invoices
           (->> invoices
                (concat (map #(assoc % :taxable_fulfillment_due "2022-03-30")
                             invoices))
                (fakturoid/filter-by-taxable-fulfillment-due since
                                                             (LocalDate/of 2022 5 1))))))

(t/deftest report-tax-filter-test
  (let [expected
        {:today (LocalDate/of 2022 5 12)
         :since (LocalDate/of 2022 4 1)
         :until (LocalDate/of 2022 5 1)
         :query {"sort[column]" "taxable_fulfillment_due"
                 "sort[order]" "desc"}}]
    (t/is (= expected
             (fakturoid/report-tax-filter (constantly today))))))
