(ns clj-fakturoid-test.create-invoice-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [clj-http.fake :refer [with-fake-routes]]
    [clj-fakturoid.core :as fakturoid]
    [clj-fakturoid-test.fake-http :as fake-http]))


(def fakturoid-host "https://api-staging.fakturoid.localhost/api/v2")

(def create-invoice (partial fakturoid/create-invoice fakturoid-host ["username" "token"]))

(def json-handler (partial fake-http/json-handler fakturoid-host))

(def invoice
  {:subject_custom_id "643"
   :payment_method "bank"
   :number "VF1-00012014"
   :custom-id "59"
   :lines [{:unit_price "1.0", :name "Programming", :quantity "1.0"}]
   :issued_on "2014-02-08"
   :subject_id 1234
   :variable_symbol "100012014"})

(def invoice-response (assoc invoice :id 1))


(deftest create-invoice-test
  (testing "should return invoice"
    (with-fake-routes (json-handler "/accounts/slug/invoices.json" invoice-response 201 :post)
      (let [response (create-invoice "slug" invoice)]
        (is (= 201 (:status response)))
        (is (= invoice-response (:body response))))))

  (testing "should return bad request"
    (with-fake-routes (json-handler "/accounts/slug/invoices.json" {} 400 :post)
      (let [response (create-invoice "slug" {})]
        (is (= 400 (:status response)))))))
