(ns starbound
  "Opinionated, immediate-mode, data driven web framework for Clojure"
  (:require
   [lambdaisland.uri]
   [org.httpkit.server :as server]
   [replicant.string]
   [ring.middleware.content-type]
   [ring.middleware.resource]
   [starfederation.datastar.clojure.adapter.http-kit :as datastar-httpkit]
   [starfederation.datastar.clojure.api :as datastar]))

;; ------------------------------------------------------------
;; Page machinery
;; ------------------------------------------------------------

(def page-props-required (sorted-set :page/uri :page/render :page/id))
(def page-props-optional (sorted-set :page/styles))

(defn define-page [page]
  (doseq [required page-props-required]
    (when-not (contains? page required)
      (throw (ex-info "Page misses required property"
                      {:page page
                       :required required}))))
  (doseq [prop (keys page)]
    (when-not (or (contains? page-props-required prop)
                  (contains? page-props-optional prop))
      (println "Warning: page prop" prop "not recognized")))
  page)

;; ------------------------------------------------------------
;; Handler from pages
;; ------------------------------------------------------------

(defn create-uri-map [page-vars]
  (into {}
        (for [v page-vars]
          (let [page (deref v)]
            (when-not (:page/uri page)
              (throw (ex-info "missing page URI" {:page page})))
            [(:page/uri page) page]))))

(defn wrap-page
  {:arglists '([system
                {:as page :page/keys [render styles uri]}
                {:as rendered :keys [hiccup title]}])}
  [state {:page/keys [styles]} {:keys [hiccup title]}]
  (str "<!DOCTYPE HTML>\n"
       (replicant.string/render
        [:html
         [:head
          [:title title]
          [:script {:type "module" :src "/datastar.js"}]
          (for [style styles]
            (if-let [href (get-in state [:system/styles style :style/uri])]
              [:link {:rel "stylesheet" :href href}]
              (println "WARNING: no href found for" style)))]
         [:body
          [:div {:data-on-load "@post('/sse')"}]
          hiccup]])))

(defn system->state [system]
  (deref system))

(defonce sse-connections
  (atom {}))

(defn page-updated
  "Trigger client side reloads for all browsers connected to this page"
  [page]
  (let [{:page/keys [uri render]} (deref page)
        {:keys [hiccup]} (render {})
        html (replicant.string/render [:html hiccup])]
    (doseq [[_id [path sse]] @sse-connections]
      (when (= path uri)
        (datastar/patch-elements! sse html)))))

(defn handle-sse [{:as req :keys [headers]}]
  (let [id (gensym)]
    (datastar-httpkit/->sse-response
     req
     {datastar-httpkit/on-open
      #(swap! sse-connections assoc id [(-> (get headers "referer") (lambdaisland.uri/uri) :path) %])
      datastar-httpkit/on-close
      #(swap! sse-connections dissoc id)})))

(defn handler [system {:as req :keys [uri]}]
  (let [state (system->state system)]
    (or
     (when-let [{:page/keys [render] :as page} (get-in state [:system/uri->page uri])]
       {:status 200
        :headers {"Content-Type" "text/html; charset=utf-8"}
        :body (wrap-page state page (render req))})

     (when (= uri "/sse")
       (handle-sse req))

     (-> (ring.middleware.resource/resource-request req "")
         (ring.middleware.content-type/content-type-response req))

     {:status 404
      :headers {"Content-Type" "text/html; charset=utf-8"}
      :body (wrap-page state {} {:title "Page not found"
                                 :hiccup (str "Page not found: " (:uri req))})})))

;; ------------------------------------------------------------
;; Server stop/start
;; ------------------------------------------------------------

(defonce server (atom nil))

(defn stop []
  (when-let [s @server]
    (server/server-stop! s)
    (reset! server nil)))

(defn start [system opts]
  (stop)
  (reset! server
          (server/run-server #(handler system %)
                             (merge {:port 7777}
                                    opts
                                    {:legacy-return-value? false}))))

(comment
  (require 'clojure.repl.deps)
  (clojure.repl.deps/sync-deps)
  )
