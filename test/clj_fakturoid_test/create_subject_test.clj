(ns clj-fakturoid-test.create-subject-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [clj-http.fake :refer [with-fake-routes]]
    [clj-fakturoid.core :as fakturoid]
    [clj-fakturoid-test.fake-http :as fake-http]))


(def fakturoid-host "https://api-staging.fakturoid.localhost/api/v2")

(def create-subject (partial fakturoid/create-subject fakturoid-host ["username" "token"]))

(def json-handler (partial fake-http/json-handler fakturoid-host))

(def subject
  {:email ""
   :phone ""
   :name "MICROSOFT s.r.o."
   :city "Praha"
   :custom_id nil,
   :street2 nil,
   :bank_account ""
   :street "Vyskočilova 1461/2a"
   :vat_no "CZ47123737"
   :zip "14000"
   :email_copy ""
   :registration_no "47123737"
   :iban ""
   :full_name ""
   :variable_symbol "1234567890"
   :country "Česká republika"
   :web ""})


(def subject-response (assoc subject :id 1))


(deftest create-subject-test
  (testing "should return subject"
    (with-fake-routes (json-handler "/accounts/slug/subjects.json" subject-response 201 :post)
      (let [response (create-subject "slug" subject)]
        (is (= 201 (:status response)))
        (is (= subject-response (:body response))))))

  (testing "should return bad request"
    (with-fake-routes (json-handler "/accounts/slug/subjects.json" {} 400 :post)
      (let [response (create-subject "slug" {})]
        (is (= 400 (:status response)))))))
