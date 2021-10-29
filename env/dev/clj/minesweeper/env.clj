(ns minesweeper.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [minesweeper.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[minesweeper started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[minesweeper has shut down successfully]=-"))
   :middleware wrap-dev})
