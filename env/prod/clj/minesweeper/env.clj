(ns minesweeper.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[minesweeper started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[minesweeper has shut down successfully]=-"))
   :middleware identity})
