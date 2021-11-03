(ns minesweeper.game)

(defn- cell-adjacent? [{:keys [row col] :as cell} other]
  (let [min-row (- row 1)
        max-row (+ row 1)
        min-col (- col 1)
        max-col (+ col 1)]
    (and
     (or (not= row (:row other)) (not= col (:col other)))
     (<= min-row (:row other) max-row)
     (<= min-col (:col other) max-col))))

(defn- adjacent-mine-count [{:keys [col, row] :as cell} mine-cells]
  (->> mine-cells
       (filter #(cell-adjacent? cell %1))
       count))

(defn- update-game-status [{:keys [board mines] :as game}]
  (cond
    ; lost
    (some #(= :exploded %) (vals board))
    (assoc game :status :lost)

    ; won
    (= (count mines)
       (->> board vals (filter (fn [v] (#{:hidden :flagged} v))) count))
    (assoc game :status :won)

    ; still playing
    :else
    game))

(defn reveal [{:keys [board mines status] :as game} cell]
  {:pre [(= :hidden (get board cell))
         (= status :playing)]}
  (let [state (if (contains? mines cell)
                :exploded
                (adjacent-mine-count cell mines))
        new-game (-> game
                     (assoc-in [:board cell] state)
                     (update-game-status))]
    (if (= 0 state)
      (let [adjacent-cells (filter #(cell-adjacent? % cell)
                                   (-> new-game :board keys))
            reveal-fn (fn [fn-game fn-cell]
                        (if (= :hidden (-> fn-game :board (get fn-cell)))
                          (reveal fn-game fn-cell)
                          fn-game))]
        (reduce reveal-fn new-game adjacent-cells))
      new-game)))

(defn toggle-flag [{:keys [board status] :as game} cell]
  {:pre [(#{:hidden :flagged} (get board cell))
         (= status :playing)]}
  (let [state (get board cell)]
    (case state
      :flagged
      (assoc-in game [:board cell] :hidden)

      :hidden
      (assoc-in game [:board cell] :flagged))))

(defn make-game
  ([difficulty]
   (case difficulty
     :trivial (make-game 9 9 5)
     :beginner (make-game 9 9 10)
     :intermediate (make-game 16 16 40)
     :expert (make-game 16 30 99)))
  ([rows cols mine-count]
  (let [cells (for [r (range rows)
                    c (range cols)]
                {:row r :col c})
        board (zipmap cells (repeat :hidden))
        mines (into #{} (take mine-count (shuffle cells)))]
    {:mines mines
     :rows rows
     :cols cols
     :board board
     :status :playing})))
