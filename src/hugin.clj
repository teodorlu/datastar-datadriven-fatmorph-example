(ns hugin
  (:require
   [hugin.index]
   [replicant.string]
   [starbound]))

(def page-vars
  [#'hugin.index/page])

(def styles
  {:styles/page {:style/uri "page.css"}})

(def system
  {:system/uri->page (starbound/create-uri-map page-vars)
   :system/styles styles})

(defn start [opts]
  (starbound/start #'system opts))

(comment
  (set! *print-namespace-maps* false)
  (start {})
  (starbound/stop)

  (starbound/handler #'system {:uri "/lol"})
  system

  )
