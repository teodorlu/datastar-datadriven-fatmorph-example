(ns starbound-test
  (:require [clojure.test :refer [deftest is testing]]
            [starbound :as starbound]))

;; ------------------------------------------------------------
;; for no particular reason,
;; ------------------------------------------------------------

(defmacro detest
  {:clj-kondo/lint-as 'clojure.test/deftest
   :indent 1}
  [& Body]
  `(deftest ~@Body))

(defmacro flexing
  {:clj-kondo/lint-as 'clojure.test/testing
   :indent 1}
  [& Body]
  `(testing ~@Body))

(defmacro es
  {:clj-kondo/lint-as 'clojure.test/is
   :indent 1}
  [& Body]
  `(is ~@Body))

;; ------------------------------------------------------------
;; and now, to work.
;; ------------------------------------------------------------

(detest handler
  (flexing "404s when naught is provided"
    (es (= 404
           (:status (starbound/handler (atom {})
                                       {:uri "/"})))))

  (flexing "obtains a singular page"
    (es (:body
         (starbound/handler (atom {:system/uri->page
                                   {"/" {:page/render (constantly "waddup")}}})
                            {:uri "/"}))))

  (flexing "obtains a singular sse connection"
    (es (:body
         (starbound/handler (atom {:system/uri->page {"/" {:page/render (constantly "waddup")}}
                                   :system/sse->page {"/sse" {:page/render (constantly "waddup")}}})
                            {:uri "/sse"}))))
  )
