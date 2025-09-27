(ns hugin.index
  (:require [starbound]))

(def nexus [:a {:href "https://github.com/cjohansen/nexus"} "Nexus"])

(defn render [_]
  {:title "Hugin"
   :hiccup
   (list
    [:p "Goals:"]
    [:ul
     [:li "Heavily data-driven"]
     [:li "Fat morph render untill proven too slow"]
     [:li "Action dispatch powered by " nexus
      " (planned)"]
     [:li "Queues in the middle"]
     [:li "Pure functions and data anywhere possible"]])})

(def page
  (starbound/define-page
    {:page/id :pages/index
     :page/render #'render
     :page/styles #{:styles/page}
     ;; "uri" should be "path" here
     :page/uri "/"
     ;; It is an URI, but "path" is more narrow, precise term.
     }))

(starbound/page-updated #'page)
;; put here so that when you eval this buffer, your HTML live reloads.
