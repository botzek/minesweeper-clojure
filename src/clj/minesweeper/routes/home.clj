(ns minesweeper.routes.home
  (:require
   [minesweeper.layout :as layout]
   [clojure.java.io :as io]
   [minesweeper.middleware :as middleware]
   [ring.util.response]
   [ring.util.http-response :as response]))

(defn home-page [request]
  (layout/render request "home.html"))

(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page}]])
