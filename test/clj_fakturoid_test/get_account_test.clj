(ns clj-fakturoid-test.get-account-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [clj-http.fake :refer [with-fake-routes]]
    [clj-fakturoid.core :as fakturoid]
    [clj-fakturoid-test.fake-http :as fake-http]))


(def fakturoid-host "https://api-staging.fakturoid.localhost/api/v2")

(def get-account (partial fakturoid/get-account fakturoid-host ["username" "token"]))

(def json-handler (partial fake-http/json-handler fakturoid-host))



(def account-response
  {:html_url "https://app.fakturoid.cz/slug/account"
   :invoice_payment_method nil
   :send_thank_you_email false
   :eet false
   :invoice_number_format "#yyyy#-#dddd#"
   :email "muj@email.cz"
   :eet_invoice_default false
   :invoice_language "cz"
   :phone ""
   :name "Vaše Jméno"
   :swift_bic nil
   :vat_rate 21
   :city "Adresa"
   :invoice_email "muj@email.cz"
   :plan_price 0
   :send_invoice_from_proforma_email false
   :due 14
   :street2 nil
   :bank_account "0000000000/0000"
   :street "Adresa 1"
   :subdomain "slug"
   :displayed_note "Fyzická osoba zapsaná v živnostenském rejstříku."
   :updated_at "2017-04-18T09:06:00.832+01:00"
   :currency "CZK"
   :vat_no "CZ0000000000"
   :vat_price_mode "without_vat"
   :zip "00000"
   :unit_name ""
   :url "https://app.fakturoid.cz/api/v2/accounts/slug/account.json"
   :invoice_note nil
   :registration_no "00000000"
   :vat_mode "vat_payer"
   :invoice_gopay false
   :iban nil
   :full_name nil
   :invoice_paypal false
   :overdue_email_text (str "Zdravím,\n\nmůj fakturační robot mě upozornil, že faktura č. #no# je po splatnosti.\n"
                            "Fakturu najdete na #link#\n\nZkuste se na to mrknout. Díky.\n\nVaše Jméno"),
   :send_overdue_email false
   :plan "Zdarma"
   :custom_email_text "Hezký den,\n\nvystavil jsem pro Vás fakturu \n#link#\n\nDíky!\n\nVaše Jméno"
   :country "CZ"
   :invoice_proforma false
   :created_at "2017-04-18T09:04:31.764+01:00"
   :web ""})


(deftest get-account-test
  (testing "should return account"
    (with-fake-routes (json-handler "/accounts/slug/account.json" account-response 200)
      (let [response (get-account "slug")]
        (is (= 200 (:status response)))
        (is (= account-response (:body response))))))

  (testing "should return not found"
    (with-fake-routes (json-handler "/accounts/nonexisting/account.json" {} 404)
      (let [response (get-account "nonexisting")]
        (is (= 404 (:status response)))))))
