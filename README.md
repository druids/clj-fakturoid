clj-fakturoid
==========

A client for [Fakturoid API](https://fakturoid.docs.apiary.io) based on [clj-http.client](https://clojars.org/clj-http).

[![CircleCI](https://circleci.com/gh/druids/clj-fakturoid.svg?style=svg)](https://circleci.com/gh/druids/clj-fakturoid)
[![Dependencies Status](https://jarkeeper.com/druids/clj-fakturoid/status.png)](https://jarkeeper.com/druids/clj-fakturoid)
[![License](https://img.shields.io/badge/MIT-Clause-blue.svg)](https://opensource.org/licenses/MIT)


Leiningen/Boot
--------------

```clojure
[clj-fakturoid "0.1.0"]
```


Documentation
-------------

All functions are designed to return errors instead of throwing exceptions (except `:pre` in a function).

To be able to run examples these lines are needed:

```clojure
(require '[clj-fakturoid.core :as fakturoid])

(def host "https://app.fakturoid.cz/api/v2")
```

### get-account
Returns an account by a given `credentials` and `slug`. Where `credentials` is a tuple with `username` and API `token`.


```clojure
(:body (fakturoid/get-account host [username token] slug))
{:invoice_language "cz", :phone "", :name "Vaše Jméno",...}
```

In case of any error while parsing a body, `:body` attribute is set to `nil` and the body is associates
 to `:body-unparsed` as a `string`.
```clojure
(def response (fakturoid/get-account host [username token] slug))

(:body response)
nil

(:body-unparsed response)
"{\"I'm not a JSON\"}"
```

### create-subject
Creates new subject in the addressbook.


```clojure
(:body (fakturoid/create-subject host [username token] slug {:street "Masarykova 1", :name "foo", ...}))
{:id 1, :street "Masarykova 1", :name "foo",...}
```
