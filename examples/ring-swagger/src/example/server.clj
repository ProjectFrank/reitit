(ns example.server
  (:require [reitit.ring :as ring]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [reitit.ring.coercion :as coercion]
            [reitit.coercion.spec]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.exception :as exception]
            [reitit.ring.middleware.multipart :as multipart]
            [ring.middleware.params :as params]
            [ring.adapter.jetty :as jetty]
            [muuntaja.core :as m]
            [clojure.java.io :as io]))

(def app
  (ring/ring-handler
    (ring/router
      [["/swagger.json"
        {:get {:no-doc true
               :swagger {:info {:title "my-api"}}
               :handler (swagger/create-swagger-handler)}}]

       ["/files"
        {:swagger {:tags ["files"]}}

        ["/upload"
         {:post {:summary "upload a file"
                 :parameters {:multipart {:file multipart/temp-file-part}}
                 :responses {200 {:body {:file multipart/temp-file-part}}}
                 :handler (fn [{{{:keys [file]} :multipart} :parameters}]
                            {:status 200
                             :body {:file file}})}}]

        ["/download"
         {:get {:summary "downloads a file"
                :swagger {:produces ["image/png"]}
                :handler (fn [_]
                           {:status 200
                            :headers {"Content-Type" "image/png"}
                            :body (io/input-stream (io/resource "reitit.png"))})}}]]

       ["/math"
        {:swagger {:tags ["math"]}}

        ["/plus"
         {:get {:summary "plus with spec query parameters"
                :parameters {:query {:x int?, :y int?}}
                :responses {200 {:body {:total int?}}}
                :handler (fn [{{{:keys [x y]} :query} :parameters}]
                           {:status 200
                            :body {:total (+ x y)}})}
          :post {:summary "plus with spec body parameters"
                 :parameters {:body {:x int?, :y int?}}
                 :responses {200 {:body {:total int?}}}
                 :handler (fn [{{{:keys [x y]} :body} :parameters}]
                            {:status 200
                             :body {:total (+ x y)}})}}]]]

      {:data {:coercion reitit.coercion.spec/coercion
              :muuntaja m/instance
              :middleware [;; query-params & form-params
                           params/wrap-params
                           ;; content-negotiation
                           muuntaja/format-negotiate-middleware
                           ;; encoding response body
                           muuntaja/format-response-middleware
                           ;; exception handling
                           exception/exception-middleware
                           ;; decoding request body
                           muuntaja/format-request-middleware
                           ;; coercing response bodys
                           coercion/coerce-response-middleware
                           ;; coercing request parameters
                           coercion/coerce-request-middleware
                           ;; multipart
                           multipart/multipart-middleware]}})
    (ring/routes
      (swagger-ui/create-swagger-ui-handler
        {:path "/"
         :config {:validatorUrl nil}})
      (ring/create-default-handler))))

(defn start []
  (jetty/run-jetty #'app {:port 3000, :join? false})
  (println "server running in port 3000"))

(comment
  (start))
