clj-fakturoid
==========

A client for [Fakturoid API](https://fakturoid.docs.apiary.io) based on [clj-http.client](https://clojars.org/clj-http).

[![CircleCI](https://circleci.com/gh/druids/clj-fakturoid.svg?style=svg)](https://circleci.com/gh/druids/clj-fakturoid)
[![Dependencies Status](https://jarkeeper.com/druids/clj-fakturoid/status.png)](https://jarkeeper.com/druids/clj-fakturoid)
[![License](https://img.shields.io/badge/MIT-Clause-blue.svg)](https://opensource.org/licenses/MIT)


Leiningen/Boot
--------------

```clojure
[clj-fakturoid "0.0.0"]
```


Documentation
-------------

All functions are designed to return errors instead of throwing exceptions (except `:pre` in a function).

To be able to run examples these lines are needed:

```clojure
(require '[clj-fakturoid.core :as fakturoid])

(def host "https://app.fakturoid.cz/api/v2")
```
